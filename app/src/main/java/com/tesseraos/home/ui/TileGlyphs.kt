package com.tesseraos.home.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

object TileGlyphs {
    fun resolve(packageName: String, label: String): ImageVector? {
        val p = packageName.lowercase()
        val l = label.lowercase()
        return when {
            p.contains("dialer") || p.contains("telecom") || l == "phone" || l == "telefone" -> Icons.Outlined.Call
            p.contains("whatsapp") || p.contains("telegram") -> Icons.Outlined.Forum
            p.contains("messaging") || p.contains("mms") || p.contains("sms") || l.contains("mensag") -> Icons.Outlined.ChatBubbleOutline
            p.contains("mail") || p.contains("outlook") -> Icons.Outlined.MailOutline
            p.contains("chrome") || p.contains("firefox") || p.contains("brave") || p.contains("browser") || p.contains("opera") -> Icons.Outlined.Language
            p.contains("camera") || l.contains("câmera") || l.contains("camera") -> Icons.Outlined.CameraAlt
            p.contains("photos") || p.contains("gallery") || l.contains("galeria") || l.contains("fotos") -> Icons.Outlined.PhotoLibrary
            p.contains("spotify") || p.contains("music") || p.contains("ytmusic") || l.contains("música") -> Icons.Outlined.MusicNote
            p.contains("maps") || p.contains("waze") || l.contains("mapas") -> Icons.Outlined.Explore
            p.contains("clock") || p.contains("alarm") || l.contains("relógio") -> Icons.Outlined.Alarm
            p.contains("calendar") || l.contains("agenda") || l.contains("calendário") -> Icons.Outlined.CalendarMonth
            p.contains("calc") || l.contains("calculadora") -> Icons.Outlined.Calculate
            p.contains("contacts") || l.contains("contatos") -> Icons.Outlined.PeopleOutline
            p.contains("vending") || p.contains("aurora") || l.contains("store") || l.contains("loja") -> Icons.Outlined.ShoppingBag
            p.contains("settings") || l.contains("config") -> Icons.Outlined.Settings
            p.contains("files") || p.contains("documentsui") || l.contains("arquivos") -> Icons.Outlined.Folder
            p.contains("notes") || p.contains("keep") || l.contains("notas") -> Icons.Outlined.Description
            p.contains("weather") || l.contains("clima") || l.contains("tempo") -> Icons.Outlined.Cloud
            p.contains("youtube") || p.contains("vlc") || p.contains("video") -> Icons.Outlined.PlayArrow
            p.contains("termux") || p.contains("terminal") -> Icons.Outlined.Terminal
            else -> null
        }
    }
}
