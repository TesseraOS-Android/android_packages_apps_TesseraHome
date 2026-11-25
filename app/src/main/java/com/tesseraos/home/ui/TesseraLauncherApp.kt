package com.tesseraos.home.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.tesseraos.home.model.AppEntry
import com.tesseraos.home.model.LauncherSettings
import com.tesseraos.home.model.LiveNotificationData
import com.tesseraos.home.model.TileItem
import com.tesseraos.home.theme.MetroPalette
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun TesseraLauncherApp(
    apps: List<AppEntry>,
    tiles: List<TileItem>,
    loadIcon: suspend (String) -> ImageBitmap?,
    notifications: Map<String, LiveNotificationData>,
    settings: LauncherSettings,
    wallpaperBitmap: ImageBitmap?,
    notificationAccessGranted: Boolean,
    resumeTick: Int,
    onUpdateSettings: (LauncherSettings) -> Unit,
    onCycleTileSize: (String) -> Unit,
    onCycleTileColor: (String) -> Unit,
    onUnpinTile: (String) -> Unit,
    onMoveTile: (String, Int) -> Unit,
    onAddCustomTile: (String, String, com.tesseraos.home.model.TileSize) -> Unit,
    onPinToStart: (AppEntry) -> Unit,
    onResetDefaultTiles: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onOpenAppInfo: (String) -> Unit,
    onUninstallApp: (String) -> Unit,
    onPickWallpaper: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenHomeSettings: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    var editMode by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var closingSettings by remember { mutableStateOf(false) }

    val turnstileRotation = remember { Animatable(0f) }
    val turnstileAlpha = remember { Animatable(1f) }
    val settingsRotation = remember { Animatable(72f) }
    val settingsAlpha = remember { Animatable(0f) }
    val metroEase = remember { CubicBezierEasing(0.1f, 0.9f, 0.2f, 1.0f) }

    LaunchedEffect(resumeTick) {
        if (turnstileRotation.value != 0f || turnstileAlpha.value != 1f) {
            turnstileRotation.snapTo(48f)
            turnstileAlpha.snapTo(0.15f)
            launch { turnstileRotation.animateTo(0f, tween(300, easing = metroEase)) }
            launch { turnstileAlpha.animateTo(1f, tween(240)) }
        }
    }

    LaunchedEffect(showSettings) {
        if (showSettings) {
            closingSettings = false
            settingsRotation.snapTo(72f)
            settingsAlpha.snapTo(0f)
            launch { settingsRotation.animateTo(0f, tween(300, easing = metroEase)) }
            launch { settingsAlpha.animateTo(1f, tween(220)) }
        }
    }

    val closeSettingsAnimated: () -> Unit = {
        if (!closingSettings) {
            closingSettings = true
            scope.launch {
                coroutineScope {
                    launch { settingsRotation.animateTo(82f, tween(200, easing = FastOutLinearInEasing)) }
                    launch { settingsAlpha.animateTo(0f, tween(180, easing = FastOutLinearInEasing)) }
                }
                showSettings = false
                closingSettings = false
            }
        }
    }

    val launchWithTurnstile: (String) -> Unit = { pkg ->
        if (settings.turnstileEnabled) {
            scope.launch {
                launch { turnstileRotation.animateTo(-82f, tween(230, easing = metroEase)) }
                launch { turnstileAlpha.animateTo(0f, tween(200)) }
                delay(190)
                onLaunchApp(pkg)
            }
        } else {
            onLaunchApp(pkg)
        }
    }

    BackHandler(enabled = showSettings || editMode || pagerState.currentPage != 0) {
        when {
            showSettings -> closeSettingsAnimated()
            editMode -> editMode = false
            pagerState.currentPage != 0 -> scope.launch {
                pagerState.animateScrollToPage(0, animationSpec = tween(380, easing = metroEase))
            }
        }
    }

    val accent = Color(settings.accentColorHex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroPalette.PureBlack)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0f, 0.5f)
                rotationY = turnstileRotation.value
                alpha = turnstileAlpha.value
                cameraDistance = 12f * density
            }
    ) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !editMode && !showSettings
        ) { page ->
            val offsetProvider = remember(pagerState, page) {
                { (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageOffset = offsetProvider()
                        val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)
                        cameraDistance = 14f * density
                        if (page == 0) {
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            rotationY = pageOffset * 22f
                            scaleX = 1f - (absOffset * 0.06f)
                            scaleY = 1f - (absOffset * 0.06f)
                            alpha = 1f - (absOffset * 0.45f)
                        } else {
                            transformOrigin = TransformOrigin(1f, 0.5f)
                            rotationY = pageOffset * 18f
                            scaleX = 1f - (absOffset * 0.05f)
                            scaleY = 1f - (absOffset * 0.05f)
                            alpha = 1f - (absOffset * 0.35f)
                        }
                    }
            ) {
                if (page == 0) {
                    StartScreen(
                        tiles = tiles,
                        apps = apps,
                        loadIcon = loadIcon,
                        notifications = notifications,
                        settings = settings,
                        wallpaperBitmap = wallpaperBitmap,
                        editMode = editMode,
                        onExitEditMode = { editMode = false },
                        onEnterEditMode = { editMode = true },
                        onLaunchApp = launchWithTurnstile,
                        onCycleTileSize = onCycleTileSize,
                        onCycleTileColor = onCycleTileColor,
                        onUnpinTile = onUnpinTile,
                        onMoveTile = onMoveTile,
                        onAddCustomTile = onAddCustomTile,
                        onOpenAppList = {
                            scope.launch {
                                pagerState.animateScrollToPage(1, animationSpec = tween(400, easing = metroEase))
                            }
                        },
                        onOpenSettings = { showSettings = true }
                    )
                } else {
                    AppListScreen(
                        apps = apps,
                        accentColor = accent,
                        loadIcon = loadIcon,
                        pageOffsetProvider = offsetProvider,
                        onLaunchApp = launchWithTurnstile,
                        onPinToStart = { app ->
                            onPinToStart(app)
                            scope.launch {
                                pagerState.animateScrollToPage(0, animationSpec = tween(380, easing = metroEase))
                            }
                        },
                        onOpenAppInfo = onOpenAppInfo,
                        onUninstallApp = onUninstallApp
                    )
                }
            }
        }

        if (showSettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        rotationY = settingsRotation.value
                        alpha = settingsAlpha.value
                        cameraDistance = 12f * density
                    }
            ) {
                MetroSettingsScreen(
                    settings = settings,
                    notificationAccessGranted = notificationAccessGranted,
                    onUpdateSettings = onUpdateSettings,
                    onPickWallpaper = onPickWallpaper,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onOpenHomeSettings = onOpenHomeSettings,
                    onResetDefaultTiles = onResetDefaultTiles,
                    onClose = closeSettingsAnimated
                )
            }
        }
    }
}
