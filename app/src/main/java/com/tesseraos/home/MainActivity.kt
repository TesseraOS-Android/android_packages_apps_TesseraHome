package com.tesseraos.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.lifecycleScope
import com.tesseraos.home.data.AppRepository
import com.tesseraos.home.data.SettingsRepository
import com.tesseraos.home.model.AppEntry
import com.tesseraos.home.model.LauncherSettings
import com.tesseraos.home.model.TileItem
import com.tesseraos.home.model.TileSize
import com.tesseraos.home.service.LiveTileBus
import com.tesseraos.home.ui.TesseraLauncherApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var appRepo: AppRepository
    private lateinit var settingsRepo: SettingsRepository

    private var appsState by mutableStateOf<List<AppEntry>>(emptyList())
    private var tilesState by mutableStateOf<List<TileItem>>(emptyList())
    private var settingsState by mutableStateOf(LauncherSettings())
    private var wallpaperBitmapState by mutableStateOf<ImageBitmap?>(null)
    private var notificationAccessState by mutableStateOf(false)
    private var resumeTickState by mutableIntStateOf(0)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshAppsAsync()
        }
    }

    private val pickWallpaperLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val updated = settingsState.copy(
                parallaxEnabled = true,
                wallpaperUri = uri.toString()
            )
            settingsState = updated
            settingsRepo.saveSettings(updated)
            loadWallpaperBitmapAsync(uri.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.BLACK),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.BLACK)
        )
        requestHighestRefreshRate()

        appRepo = AppRepository(this)
        settingsRepo = SettingsRepository(this)

        settingsState = settingsRepo.loadSettings()
        notificationAccessState = LiveTileBus.isListenerEnabled(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(packageReceiver, filter)
        }

        refreshAppsAsync(loadTilesIfEmpty = true)
        loadWallpaperBitmapAsync(settingsState.wallpaperUri)

        setContent {
            val notifications by LiveTileBus.notifications.collectAsState()
            TesseraLauncherApp(
                apps = appsState,
                tiles = tilesState,
                loadIcon = { pkg -> appRepo.loadAppIcon(pkg) },
                notifications = notifications,
                settings = settingsState,
                wallpaperBitmap = wallpaperBitmapState,
                notificationAccessGranted = notificationAccessState,
                resumeTick = resumeTickState,
                onUpdateSettings = { updated ->
                    settingsState = updated
                    settingsRepo.saveSettings(updated)
                },
                onCycleTileSize = { tileId ->
                    val next = tilesState.map {
                        if (it.id == tileId) it.copy(size = it.size.next()) else it
                    }
                    tilesState = next
                    settingsRepo.saveTiles(next)
                },
                onCycleTileColor = { tileId ->
                    val palette = listOf<Long?>(null, 0xFFD83B01, 0xFF008A00, 0xFF1BA1E2, 0xFFD80073, 0xFFFA6800, 0xFF6A00FF)
                    val next = tilesState.map { tile ->
                        if (tile.id == tileId) {
                            val currentIdx = palette.indexOf(tile.customAccentHex)
                            val nextColor = palette[(currentIdx + 1) % palette.size]
                            tile.copy(customAccentHex = nextColor)
                        } else {
                            tile
                        }
                    }
                    tilesState = next
                    settingsRepo.saveTiles(next)
                },
                onUnpinTile = { tileId ->
                    val next = tilesState.filterNot { it.id == tileId }
                    tilesState = next
                    settingsRepo.saveTiles(next)
                },
                onMoveTile = { tileId, delta ->
                    val idx = tilesState.indexOfFirst { it.id == tileId }
                    if (idx != -1) {
                        val target = (idx + delta).coerceIn(0, tilesState.lastIndex)
                        if (target != idx) {
                            val mutable = tilesState.toMutableList()
                            val item = mutable.removeAt(idx)
                            mutable.add(target, item)
                            tilesState = mutable
                            settingsRepo.saveTiles(mutable)
                        }
                    }
                },
                onAddCustomTile = { pkg, label, size ->
                    val newTile = TileItem(
                        id = "tile_${System.currentTimeMillis()}_$pkg",
                        packageName = pkg,
                        label = label,
                        size = size
                    )
                    val next = tilesState + newTile
                    tilesState = next
                    settingsRepo.saveTiles(next)
                },
                onPinToStart = { app ->
                    val newTile = TileItem(
                        id = "tile_${System.currentTimeMillis()}_${app.packageName}",
                        packageName = app.packageName,
                        label = app.label,
                        size = TileSize.MEDIUM
                    )
                    val next = tilesState + newTile
                    tilesState = next
                    settingsRepo.saveTiles(next)
                },
                onResetDefaultTiles = {
                    getSharedPreferences("tessera_home_prefs", MODE_PRIVATE)
                        .edit()
                        .remove("pinned_tiles_json")
                        .apply()
                    tilesState = settingsRepo.loadTiles(appsState)
                },
                onLaunchApp = { pkg -> appRepo.launchApp(pkg) },
                onOpenAppInfo = { pkg -> appRepo.openAppInfo(pkg) },
                onUninstallApp = { pkg -> appRepo.uninstallApp(pkg) },
                onPickWallpaper = { pickWallpaperLauncher.launch(arrayOf("image/*")) },
                onOpenNotificationSettings = { appRepo.openNotificationListenerSettings() },
                onOpenHomeSettings = { appRepo.openHomeSettings() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        requestHighestRefreshRate()
        resumeTickState++
        notificationAccessState = LiveTileBus.isListenerEnabled(this)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(packageReceiver) }
        super.onDestroy()
    }

    private fun requestHighestRefreshRate() {
        runCatching {
            val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            val maxRate = currentDisplay?.supportedModes?.maxOfOrNull { it.refreshRate } ?: return
            val lp = window.attributes
            if (lp.preferredRefreshRate != maxRate) {
                lp.preferredRefreshRate = maxRate
                window.attributes = lp
            }
        }
    }

    private fun refreshAppsAsync(loadTilesIfEmpty: Boolean = false) {
        lifecycleScope.launch {
            val loadedApps = withContext(Dispatchers.IO) {
                appRepo.loadInstalledApps()
            }
            appsState = loadedApps
            if (loadTilesIfEmpty || tilesState.isEmpty()) {
                tilesState = withContext(Dispatchers.IO) {
                    settingsRepo.loadTiles(loadedApps)
                }
            }
        }
    }

    private fun loadWallpaperBitmapAsync(uriStr: String?) {
        if (uriStr.isNullOrBlank()) {
            wallpaperBitmapState = null
            return
        }
        lifecycleScope.launch {
            val decoded = withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(uriStr)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, bounds)
                    }
                    val reqW = resources.displayMetrics.widthPixels.coerceAtLeast(1080)
                    val reqH = resources.displayMetrics.heightPixels.coerceAtLeast(2400)
                    var sample = 1
                    while (bounds.outWidth / sample > reqW * 2 || bounds.outHeight / sample > reqH * 2) {
                        sample *= 2
                    }
                    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                    contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                    }
                }.getOrNull()
            }
            wallpaperBitmapState = decoded
        }
    }
}
