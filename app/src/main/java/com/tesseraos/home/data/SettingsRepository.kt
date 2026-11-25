package com.tesseraos.home.data

import android.content.Context
import com.tesseraos.home.model.AppEntry
import com.tesseraos.home.model.LauncherSettings
import com.tesseraos.home.model.TileItem
import com.tesseraos.home.model.TileSize
import com.tesseraos.home.theme.MetroPalette
import org.json.JSONArray
import org.json.JSONObject

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("tessera_home_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): LauncherSettings {
        return LauncherSettings(
            accentColorHex = prefs.getLong("accent_hex", MetroPalette.DefaultAccentHex),
            gridColumns = prefs.getInt("grid_columns", 4),
            parallaxEnabled = prefs.getBoolean("parallax_enabled", false),
            wallpaperUri = prefs.getString("wallpaper_uri", null),
            tileOpacity = prefs.getFloat("tile_opacity", 0.72f),
            turnstileEnabled = prefs.getBoolean("turnstile_enabled", true),
            liveFlipEnabled = prefs.getBoolean("live_flip_enabled", true)
        )
    }

    fun saveSettings(settings: LauncherSettings) {
        prefs.edit()
            .putLong("accent_hex", settings.accentColorHex)
            .putInt("grid_columns", settings.gridColumns)
            .putBoolean("parallax_enabled", settings.parallaxEnabled)
            .putString("wallpaper_uri", settings.wallpaperUri)
            .putFloat("tile_opacity", settings.tileOpacity)
            .putBoolean("turnstile_enabled", settings.turnstileEnabled)
            .putBoolean("live_flip_enabled", settings.liveFlipEnabled)
            .apply()
    }

    fun loadTiles(installedApps: List<AppEntry>): List<TileItem> {
        val raw = prefs.getString("pinned_tiles_json", null)
        if (raw != null) {
            val parsed = parseTiles(raw)
            if (parsed.isNotEmpty()) return parsed
        }
        val defaults = buildDefaultTiles(installedApps)
        saveTiles(defaults)
        return defaults
    }

    fun saveTiles(tiles: List<TileItem>) {
        val arr = JSONArray()
        for (tile in tiles) {
            val obj = JSONObject()
            obj.put("id", tile.id)
            obj.put("pkg", tile.packageName)
            obj.put("label", tile.label)
            obj.put("size", tile.size.name)
            if (tile.customAccentHex != null) {
                obj.put("accent", tile.customAccentHex)
            }
            arr.put(obj)
        }
        prefs.edit().putString("pinned_tiles_json", arr.toString()).apply()
    }

    private fun parseTiles(raw: String): List<TileItem> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<TileItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val sizeName = obj.optString("size", TileSize.MEDIUM.name)
                val size = runCatching { TileSize.valueOf(sizeName) }.getOrDefault(TileSize.MEDIUM)
                val accent = if (obj.has("accent")) obj.getLong("accent") else null
                list.add(
                    TileItem(
                        id = obj.getString("id"),
                        packageName = obj.getString("pkg"),
                        label = obj.getString("label"),
                        size = size,
                        customAccentHex = accent
                    )
                )
            }
            list
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun buildDefaultTiles(installedApps: List<AppEntry>): List<TileItem> {
        val tiles = mutableListOf<TileItem>()
        val used = mutableSetOf<String>()

        fun pick(keywords: List<String>, size: TileSize, fallbackLabel: String? = null) {
            val match = installedApps.firstOrNull { app ->
                val p = app.packageName.lowercase()
                val l = app.label.lowercase()
                app.packageName !in used && keywords.any { k -> p.contains(k) || l.contains(k) }
            }
            if (match != null) {
                used.add(match.packageName)
                tiles.add(
                    TileItem(
                        id = "tile_${tiles.size}_${match.packageName}",
                        packageName = match.packageName,
                        label = fallbackLabel ?: match.label,
                        size = size
                    )
                )
            }
        }

        pick(listOf("dialer", "telecom", "phone", "telefone"), TileSize.MEDIUM)
        pick(listOf("whatsapp"), TileSize.SMALL)
        pick(listOf("messaging", "mms", "sms", "mensagens"), TileSize.SMALL)
        pick(listOf("gmail", "mail", "outlook"), TileSize.SMALL)
        pick(listOf("chrome", "firefox", "brave", "browser", "opera"), TileSize.SMALL)
        pick(listOf("camera", "cam"), TileSize.WIDE)
        pick(listOf("photos", "gallery", "galeria"), TileSize.MEDIUM)
        pick(listOf("spotify", "music", "ytmusic", "musica"), TileSize.MEDIUM)
        pick(listOf("maps", "waze"), TileSize.MEDIUM)
        pick(listOf("deskclock", "clock", "relogio"), TileSize.SMALL)
        pick(listOf("calculator", "calculadora"), TileSize.SMALL)
        pick(listOf("calendar", "agenda"), TileSize.SMALL)
        pick(listOf("vending", "play store", "aurora"), TileSize.SMALL)
        pick(listOf("com.android.settings"), TileSize.MEDIUM)

        if (tiles.size < 8) {
            for (app in installedApps) {
                if (tiles.size >= 12) break
                if (app.packageName !in used) {
                    used.add(app.packageName)
                    tiles.add(
                        TileItem(
                            id = "tile_${tiles.size}_${app.packageName}",
                            packageName = app.packageName,
                            label = app.label,
                            size = if (tiles.size % 3 == 0) TileSize.MEDIUM else TileSize.SMALL
                        )
                    )
                }
            }
        }
        return tiles
    }
}
