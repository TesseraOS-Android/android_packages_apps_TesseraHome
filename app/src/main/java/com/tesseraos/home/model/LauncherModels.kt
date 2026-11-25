package com.tesseraos.home.model

import android.graphics.drawable.Drawable

enum class TileSize(val colSpan: Int, val rowSpan: Int) {
    SMALL(1, 1),
    MEDIUM(2, 2),
    WIDE(4, 2);

    fun next(): TileSize = when (this) {
        SMALL -> MEDIUM
        MEDIUM -> WIDE
        WIDE -> SMALL
    }
}

data class TileItem(
    val id: String,
    val packageName: String,
    val label: String,
    val size: TileSize,
    val customAccentHex: Long? = null
)

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null
) {
    val sortKey: String
        get() {
            val first = label.trim().firstOrNull()?.lowercaseChar() ?: '#'
            return if (first in 'a'..'z') first.toString() else "#"
        }
}

data class LiveNotificationData(
    val packageName: String,
    val count: Int,
    val title: String,
    val text: String,
    val timestamp: Long
)

data class LauncherSettings(
    val accentColorHex: Long = 0xFF0050EF,
    val gridColumns: Int = 4,
    val parallaxEnabled: Boolean = false,
    val wallpaperUri: String? = null,
    val tileOpacity: Float = 0.72f,
    val turnstileEnabled: Boolean = true,
    val liveFlipEnabled: Boolean = true
)
