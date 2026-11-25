package com.tesseraos.home.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesseraos.home.model.AppEntry
import com.tesseraos.home.model.LauncherSettings
import com.tesseraos.home.model.LiveNotificationData
import com.tesseraos.home.model.TileItem
import com.tesseraos.home.model.TileSize
import com.tesseraos.home.theme.MetroOutlineButton
import com.tesseraos.home.theme.MetroPageHeader
import com.tesseraos.home.theme.MetroPalette

private data class PlacedTile(
    val tile: TileItem,
    val col: Int,
    val row: Int,
    val colSpan: Int,
    val rowSpan: Int
)

private fun packTiles(tiles: List<TileItem>, columns: Int): Pair<List<PlacedTile>, Int> {
    val grid = mutableListOf<BooleanArray>()
    val placed = mutableListOf<PlacedTile>()

    fun ensureRows(needed: Int) {
        while (grid.size < needed) {
            grid.add(BooleanArray(columns))
        }
    }

    fun fits(r: Int, c: Int, w: Int, h: Int): Boolean {
        if (c + w > columns) return false
        ensureRows(r + h)
        for (y in r until r + h) {
            for (x in c until c + w) {
                if (grid[y][x]) return false
            }
        }
        return true
    }

    fun occupy(r: Int, c: Int, w: Int, h: Int) {
        for (y in r until r + h) {
            for (x in c until c + w) {
                grid[y][x] = true
            }
        }
    }

    for (tile in tiles) {
        val w = tile.size.colSpan.coerceAtMost(columns)
        val h = tile.size.rowSpan
        var found = false
        var row = 0
        while (!found) {
            for (col in 0..(columns - w)) {
                if (fits(row, col, w, h)) {
                    occupy(row, col, w, h)
                    placed.add(PlacedTile(tile, col, row, w, h))
                    found = true
                    break
                }
            }
            if (!found) row++
        }
    }
    val totalRows = placed.maxOfOrNull { it.row + it.rowSpan } ?: 0
    return placed to totalRows
}

@Composable
fun StartScreen(
    tiles: List<TileItem>,
    apps: List<AppEntry>,
    loadIcon: suspend (String) -> ImageBitmap?,
    notifications: Map<String, LiveNotificationData>,
    settings: LauncherSettings,
    wallpaperBitmap: ImageBitmap?,
    editMode: Boolean,
    onExitEditMode: () -> Unit,
    onEnterEditMode: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onCycleTileSize: (String) -> Unit,
    onCycleTileColor: (String) -> Unit,
    onUnpinTile: (String) -> Unit,
    onMoveTile: (String, Int) -> Unit,
    onAddCustomTile: (String, String, TileSize) -> Unit,
    onOpenAppList: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val accent = Color(settings.accentColorHex)
    val scrollState = rememberScrollState()
    val scrollOffsetProvider = remember(scrollState) { { scrollState.value.toFloat() } }
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.roundToPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.roundToPx() }
    val columns = settings.gridColumns
    val (placedTiles, totalRows) = remember(tiles, columns) { packTiles(tiles, columns) }
    var showAddPicker by remember { mutableStateOf(false) }

    BackHandler(enabled = showAddPicker) {
        showAddPicker = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroPalette.PureBlack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = editMode && !showAddPicker
            ) { onExitEditMode() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            if (editMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EDITAR TILES",
                        fontFamily = MetroPalette.Selawik,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = accent,
                        letterSpacing = 1.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .background(accent, RectangleShape)
                                .clickable { showAddPicker = true }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = MetroPalette.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ADICIONAR",
                                fontFamily = MetroPalette.Selawik,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MetroPalette.White
                            )
                        }
                        Text(
                            text = "CONCLUIR",
                            fontFamily = MetroPalette.Selawik,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MetroPalette.White,
                            modifier = Modifier
                                .border(1.dp, MetroPalette.White)
                                .clickable(onClick = onExitEditMode)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val gutter = 8.dp
                val cellSize = (maxWidth - gutter * (columns - 1)) / columns
                val totalHeight = if (totalRows == 0) {
                    cellSize * 2
                } else {
                    cellSize * totalRows + gutter * (totalRows - 1)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(totalHeight)
                ) {
                    for (item in placedTiles) {
                        val x = (cellSize + gutter) * item.col
                        val y = (cellSize + gutter) * item.row
                        val w = cellSize * item.colSpan + gutter * (item.colSpan - 1)
                        val h = cellSize * item.rowSpan + gutter * (item.rowSpan - 1)

                        LiveTileView(
                            tile = item.tile,
                            accentColor = accent,
                            loadIcon = loadIcon,
                            notification = notifications[item.tile.packageName],
                            parallaxEnabled = settings.parallaxEnabled,
                            wallpaperBitmap = wallpaperBitmap,
                            tileOpacity = settings.tileOpacity,
                            scrollOffsetProvider = scrollOffsetProvider,
                            screenWidthPx = screenWidthPx,
                            screenHeightPx = screenHeightPx,
                            liveFlipEnabled = settings.liveFlipEnabled,
                            editMode = editMode,
                            onTap = { onLaunchApp(item.tile.packageName) },
                            onLongPress = onEnterEditMode,
                            onCycleSize = { onCycleTileSize(item.tile.id) },
                            onCycleColor = { onCycleTileColor(item.tile.id) },
                            onUnpin = { onUnpinTile(item.tile.id) },
                            onMoveUp = { onMoveTile(item.tile.id, -1) },
                            onMoveDown = { onMoveTile(item.tile.id, 1) },
                            modifier = Modifier
                                .offset(x = x, y = y)
                                .width(w)
                                .height(h)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onOpenSettings)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .border(2.dp, MetroPalette.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Configuracoes",
                            tint = MetroPalette.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Tessera",
                        fontFamily = MetroPalette.Selawik,
                        fontWeight = FontWeight.Light,
                        fontSize = 18.sp,
                        color = MetroPalette.MutedLabel
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(2.dp, MetroPalette.White, CircleShape)
                        .clickable(onClick = onOpenAppList),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "Todos os apps",
                        tint = MetroPalette.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        AnimatedVisibility(
            visible = showAddPicker,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(140))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MetroPalette.PureBlack)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                MetroPageHeader(
                    category = "TESSERAOS",
                    title = "Adicionar Tile"
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Text(
                            text = "LIVE TILES NATIVOS",
                            fontFamily = MetroPalette.Selawik,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = accent,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        NativeTilePickerRow(
                            icon = Icons.Outlined.Alarm,
                            title = "Relogio + Data ao vivo",
                            subtitle = "Tile largo 4x2 com hora e data em tempo real",
                            accent = accent,
                            onClick = {
                                onAddCustomTile("tessera://clock", "Relogio", TileSize.WIDE)
                                showAddPicker = false
                            }
                        )
                        NativeTilePickerRow(
                            icon = Icons.Outlined.CalendarMonth,
                            title = "Calendario ao vivo",
                            subtitle = "Tile 2x2 com dia da semana, numero e mes",
                            accent = accent,
                            onClick = {
                                onAddCustomTile("tessera://calendar", "Calendario", TileSize.MEDIUM)
                                showAddPicker = false
                            }
                        )
                        NativeTilePickerRow(
                            icon = Icons.Outlined.PeopleOutline,
                            title = "Mosaico de Contatos",
                            subtitle = "Tile 2x2 com grade mosaico animada",
                            accent = accent,
                            onClick = {
                                onAddCustomTile("tessera://people", "Contatos", TileSize.MEDIUM)
                                showAddPicker = false
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "APLICATIVOS INSTALADOS",
                            fontFamily = MetroPalette.Selawik,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = accent,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(apps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAddCustomTile(app.packageName, app.label, TileSize.MEDIUM)
                                    showAddPicker = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(accent, RectangleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = TileGlyphs.resolve(app.packageName, app.label)
                                        ?: Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = MetroPalette.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = app.label,
                                fontFamily = MetroPalette.Selawik,
                                fontWeight = FontWeight.Light,
                                fontSize = 20.sp,
                                color = MetroPalette.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    MetroOutlineButton(
                        text = "Fechar",
                        onClick = { showAddPicker = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun NativeTilePickerRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(accent, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MetroPalette.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontFamily = MetroPalette.Selawik,
                fontWeight = FontWeight.Light,
                fontSize = 20.sp,
                color = MetroPalette.White
            )
            Text(
                text = subtitle,
                fontFamily = MetroPalette.Selawik,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = MetroPalette.MutedLabel
            )
        }
    }
}
