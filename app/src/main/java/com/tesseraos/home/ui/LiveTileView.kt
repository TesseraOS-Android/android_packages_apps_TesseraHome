package com.tesseraos.home.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesseraos.home.model.LiveNotificationData
import com.tesseraos.home.model.TileItem
import com.tesseraos.home.model.TileSize
import com.tesseraos.home.theme.MetroPalette
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LiveTileView(
    tile: TileItem,
    accentColor: Color,
    loadIcon: suspend (String) -> ImageBitmap?,
    notification: LiveNotificationData?,
    parallaxEnabled: Boolean,
    wallpaperBitmap: ImageBitmap?,
    tileOpacity: Float,
    scrollOffsetProvider: () -> Float,
    screenWidthPx: Int,
    screenHeightPx: Int,
    liveFlipEnabled: Boolean,
    editMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onCycleSize: () -> Unit,
    onCycleColor: () -> Unit,
    onUnpin: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tileColor = tile.customAccentHex?.let { Color(it) } ?: accentColor
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    var rootX by remember { mutableFloatStateOf(0f) }
    var rootY by remember { mutableFloatStateOf(0f) }
    var showBackFace by remember { mutableStateOf(false) }

    val lowerPkg = tile.packageName.lowercase()
    val lowerLabel = tile.label.lowercase()
    val isClockTile = lowerPkg == "tessera://clock"
    val isCalendarTile = lowerPkg == "tessera://calendar" || lowerPkg.contains("calendar") || lowerLabel.contains("agenda") || lowerLabel.contains("calend")
    val isPeopleTile = lowerPkg == "tessera://people" || lowerPkg.contains("contacts") || lowerLabel.contains("contatos") || lowerLabel.contains("people")

    val animTiltX by animateFloatAsState(
        targetValue = tiltX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.65f),
        label = "tiltX"
    )
    val animTiltY by animateFloatAsState(
        targetValue = tiltY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.65f),
        label = "tiltY"
    )
    val editScale by animateFloatAsState(
        targetValue = if (editMode) 0.93f else 1f,
        animationSpec = tween(180),
        label = "editScale"
    )

    var mosaicTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(isPeopleTile, liveFlipEnabled, editMode) {
        if (isPeopleTile && liveFlipEnabled && !editMode && tile.size != TileSize.SMALL) {
            while (true) {
                delay(3200L)
                mosaicTick++
            }
        }
    }

    var clockNow by remember { mutableStateOf(Date()) }
    LaunchedEffect(isClockTile) {
        if (isClockTile) {
            while (true) {
                clockNow = Date()
                delay(15000L)
            }
        }
    }

    LaunchedEffect(notification?.timestamp, liveFlipEnabled, editMode) {
        showBackFace = false
        if (notification != null && liveFlipEnabled && !editMode && tile.size != TileSize.SMALL) {
            val seedDelay = (tile.id.hashCode().and(0xFF) * 18L) + 3500L
            delay(seedDelay)
            while (true) {
                showBackFace = true
                delay(4500L)
                showBackFace = false
                delay(6500L)
            }
        }
    }

    val flipRotation by animateFloatAsState(
        targetValue = if (showBackFace) 180f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "liveFlip"
    )
    val showingBack = flipRotation > 90f

    val glyph = remember(tile.packageName, tile.label) {
        TileGlyphs.resolve(tile.packageName, tile.label)
    }
    val fallbackBitmap by produceState<ImageBitmap?>(initialValue = null, tile.packageName, glyph) {
        if (glyph == null) {
            value = loadIcon(tile.packageName)
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                rootX = pos.x
                rootY = pos.y
            }
            .graphicsLayer {
                scaleX = editScale
                scaleY = editScale
                rotationX = animTiltX + flipRotation
                rotationY = animTiltY
                cameraDistance = 12f * density
            }
            .clipToBounds()
            .pointerInput(editMode, tile.id) {
                detectTapGestures(
                    onPress = { offset ->
                        val nx = ((offset.x / size.width.coerceAtLeast(1)) - 0.5f) * 2f
                        val ny = ((offset.y / size.height.coerceAtLeast(1)) - 0.5f) * 2f
                        tiltY = nx * 7f
                        tiltX = -ny * 7f
                        tryAwaitRelease()
                        tiltX = 0f
                        tiltY = 0f
                    },
                    onTap = {
                        if (!editMode) onTap()
                    },
                    onLongPress = {
                        onLongPress()
                    }
                )
            }
    ) {
        if (parallaxEnabled && wallpaperBitmap != null && screenWidthPx > 0 && screenHeightPx > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val imgW = wallpaperBitmap.width.toFloat()
                val imgH = wallpaperBitmap.height.toFloat()
                val targetW = screenWidthPx.toFloat()
                val targetH = screenHeightPx.toFloat() * 1.35f
                val scale = maxOf(targetW / imgW, targetH / imgH)
                val drawW = (imgW * scale).roundToInt()
                val drawH = (imgH * scale).roundToInt()
                val offsetX = -rootX - ((drawW - targetW) / 2f)
                val parallaxShift = (scrollOffsetProvider() * 0.35f).coerceAtMost(screenHeightPx * 0.3f)
                val offsetY = -rootY - parallaxShift
                drawImage(
                    image = wallpaperBitmap,
                    dstOffset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
                    dstSize = IntSize(drawW, drawH)
                )
                drawRect(color = tileColor.copy(alpha = (1f - tileOpacity).coerceIn(0.08f, 0.75f)))
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tileColor, RectangleShape)
            )
        }

        if (showingBack && notification != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationX = 180f }
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = notification.title.ifBlank { tile.label },
                        fontFamily = MetroPalette.Selawik,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MetroPalette.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = notification.text,
                        fontFamily = MetroPalette.Selawik,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        color = MetroPalette.White.copy(alpha = 0.92f),
                        maxLines = if (tile.size == TileSize.WIDE) 3 else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = tile.label,
                        fontFamily = MetroPalette.Selawik,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = MetroPalette.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (notification.count > 0) {
                        Text(
                            text = notification.count.toString(),
                            fontFamily = MetroPalette.Selawik,
                            fontWeight = FontWeight.Light,
                            fontSize = 22.sp,
                            color = MetroPalette.White
                        )
                    }
                }
            }
        } else if (isClockTile) {
            val timeStr = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(clockNow)
            val dateStr = SimpleDateFormat("EEE, dd MMM", Locale("pt", "BR")).format(clockNow)
                .replaceFirstChar { it.uppercase() }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (tile.size == TileSize.SMALL) 6.dp else 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = timeStr,
                    fontFamily = MetroPalette.Selawik,
                    fontWeight = FontWeight.Light,
                    fontSize = when (tile.size) {
                        TileSize.SMALL -> 22.sp
                        TileSize.MEDIUM -> 42.sp
                        TileSize.WIDE -> 52.sp
                    },
                    lineHeight = when (tile.size) {
                        TileSize.SMALL -> 24.sp
                        TileSize.MEDIUM -> 44.sp
                        TileSize.WIDE -> 54.sp
                    },
                    color = MetroPalette.White,
                    maxLines = 1
                )
                if (tile.size != TileSize.SMALL) {
                    Text(
                        text = dateStr,
                        fontFamily = MetroPalette.Selawik,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = MetroPalette.White.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else if (isCalendarTile && tile.size != TileSize.SMALL) {
            val now = remember { Date() }
            val dayNum = remember(now) { SimpleDateFormat("dd", Locale("pt", "BR")).format(now) }
            val weekDay = remember(now) {
                SimpleDateFormat("EEEE", Locale("pt", "BR")).format(now)
                    .replaceFirstChar { it.uppercase() }
            }
            val monthName = remember(now) {
                SimpleDateFormat("MMMM", Locale("pt", "BR")).format(now)
                    .replaceFirstChar { it.uppercase() }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = weekDay,
                        fontFamily = MetroPalette.Selawik,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = MetroPalette.White.copy(alpha = 0.9f),
                        maxLines = 1
                    )
                    Text(
                        text = dayNum,
                        fontFamily = MetroPalette.Selawik,
                        fontWeight = FontWeight.Light,
                        fontSize = 44.sp,
                        lineHeight = 46.sp,
                        color = MetroPalette.White
                    )
                }
                Text(
                    text = "$monthName - ${tile.label}",
                    fontFamily = MetroPalette.Selawik,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = MetroPalette.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else if (isPeopleTile && tile.size != TileSize.SMALL) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    for (r in 0..2) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            for (c in 0..2) {
                                val shadeIdx = (r * 3 + c + mosaicTick) % 5
                                val alphaShade = when (shadeIdx) {
                                    0 -> 0.12f
                                    1 -> 0.24f
                                    2 -> 0.06f
                                    3 -> 0.30f
                                    else -> 0.18f
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .padding(1.dp)
                                        .background(MetroPalette.White.copy(alpha = alphaShade), RectangleShape)
                                )
                            }
                        }
                    }
                }
                Text(
                    text = tile.label,
                    fontFamily = MetroPalette.Selawik,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = MetroPalette.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 6.dp, end = 8.dp)
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                val iconSize = when (tile.size) {
                    TileSize.SMALL -> 30.dp
                    TileSize.MEDIUM -> 46.dp
                    TileSize.WIDE -> 52.dp
                }
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (glyph != null) {
                        Icon(
                            imageVector = glyph,
                            contentDescription = tile.label,
                            tint = MetroPalette.White,
                            modifier = Modifier.size(iconSize)
                        )
                    } else if (fallbackBitmap != null) {
                        Image(
                            bitmap = fallbackBitmap!!,
                            contentDescription = tile.label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    if (notification != null && notification.count > 0) {
                        Spacer(modifier = Modifier.width(if (tile.size == TileSize.SMALL) 4.dp else 8.dp))
                        Text(
                            text = notification.count.toString(),
                            fontFamily = MetroPalette.Selawik,
                            fontWeight = FontWeight.ExtraLight,
                            fontSize = if (tile.size == TileSize.SMALL) 18.sp else 26.sp,
                            color = MetroPalette.White
                        )
                    }
                }

                if (tile.size != TileSize.SMALL) {
                    Text(
                        text = tile.label,
                        fontFamily = MetroPalette.Selawik,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = MetroPalette.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 6.dp, end = 8.dp)
                    )
                }
            }
        }

        if (editMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, MetroPalette.White, RectangleShape)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .background(MetroPalette.PureBlack)
                        .border(1.dp, MetroPalette.White, RectangleShape)
                        .clickable(onClick = onUnpin),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = MetroPalette.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .background(MetroPalette.PureBlack)
                        .border(1.dp, MetroPalette.White, RectangleShape)
                        .clickable(onClick = onCycleSize),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AspectRatio,
                        contentDescription = null,
                        tint = MetroPalette.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(28.dp)
                        .background(MetroPalette.PureBlack)
                        .border(1.dp, MetroPalette.White, RectangleShape)
                        .clickable(onClick = onCycleColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = MetroPalette.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Row(modifier = Modifier.align(Alignment.TopStart)) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(MetroPalette.PureBlack)
                            .border(1.dp, MetroPalette.White, RectangleShape)
                            .clickable(onClick = onMoveUp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowUp,
                            contentDescription = null,
                            tint = MetroPalette.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(MetroPalette.PureBlack)
                            .border(1.dp, MetroPalette.White, RectangleShape)
                            .clickable(onClick = onMoveDown),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MetroPalette.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
