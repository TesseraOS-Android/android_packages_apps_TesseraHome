package com.tesseraos.home.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesseraos.home.model.AppEntry
import com.tesseraos.home.theme.MetroPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface AppListRow {
    data class Header(val key: String) : AppListRow
    data class Item(val app: AppEntry) : AppListRow
}

@Composable
fun AppListScreen(
    apps: List<AppEntry>,
    accentColor: Color,
    loadIcon: suspend (String) -> ImageBitmap?,
    pageOffsetProvider: () -> Float,
    onLaunchApp: (String) -> Unit,
    onPinToStart: (AppEntry) -> Unit,
    onOpenAppInfo: (String) -> Unit,
    onUninstallApp: (String) -> Unit
) {
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showJumpList by remember { mutableStateOf(false) }
    var contextApp by remember { mutableStateOf<AppEntry?>(null) }
    var lastContextApp by remember { mutableStateOf<AppEntry?>(null) }

    if (contextApp != null) {
        lastContextApp = contextApp
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val metroEase = remember { CubicBezierEasing(0.1f, 0.9f, 0.2f, 1.0f) }

    BackHandler(enabled = contextApp != null || showJumpList || searchOpen) {
        when {
            contextApp != null -> contextApp = null
            showJumpList -> showJumpList = false
            searchOpen -> {
                searchOpen = false
                query = ""
            }
        }
    }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    val (rows, headerIndices) = remember(filtered, query) {
        val out = mutableListOf<AppListRow>()
        val idxMap = mutableMapOf<String, Int>()
        if (query.isBlank()) {
            val grouped = filtered.groupBy { it.sortKey }
            val keys = grouped.keys.sortedWith(compareBy { if (it == "#") "0" else it })
            for (k in keys) {
                idxMap[k] = out.size
                out.add(AppListRow.Header(k))
                grouped[k].orEmpty().forEach { out.add(AppListRow.Item(it)) }
            }
        } else {
            filtered.forEach { out.add(AppListRow.Item(it)) }
        }
        out to idxMap
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroPalette.PureBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = -pageOffsetProvider() * 90f
                    }
                    .padding(top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .border(2.dp, MetroPalette.White, CircleShape)
                        .clickable {
                            searchOpen = !searchOpen
                            if (!searchOpen) query = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (searchOpen) Icons.Outlined.Close else Icons.Outlined.Search,
                        contentDescription = "Buscar",
                        tint = MetroPalette.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (searchOpen) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(MetroPalette.SurfaceDark, RectangleShape)
                            .border(1.dp, accentColor, RectangleShape)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Buscar apps...",
                                fontFamily = MetroPalette.Selawik,
                                fontWeight = FontWeight.Light,
                                fontSize = 18.sp,
                                color = MetroPalette.MutedLabel
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = MetroPalette.Selawik,
                                fontWeight = FontWeight.Normal,
                                fontSize = 18.sp,
                                color = MetroPalette.White
                            ),
                            cursorBrush = SolidColor(accentColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = rows,
                    key = { _, row ->
                        when (row) {
                            is AppListRow.Header -> "hdr_${row.key}"
                            is AppListRow.Item -> "app_${row.app.packageName}"
                        }
                    }
                ) { idx, row ->
                    val staggerFactor = 36f + (idx.coerceAtMost(10) * 14f)
                    when (row) {
                        is AppListRow.Header -> {
                            Box(
                                modifier = Modifier
                                    .graphicsLayer { translationX = -pageOffsetProvider() * staggerFactor }
                                    .padding(top = 12.dp, bottom = 6.dp)
                                    .size(44.dp)
                                    .border(2.dp, accentColor, RectangleShape)
                                    .clickable { showJumpList = true },
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Text(
                                    text = row.key,
                                    fontFamily = MetroPalette.Selawik,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 26.sp,
                                    color = accentColor,
                                    modifier = Modifier.padding(start = 7.dp, bottom = 2.dp)
                                )
                            }
                        }

                        is AppListRow.Item -> {
                            AppListRowItem(
                                app = row.app,
                                accentColor = accentColor,
                                loadIcon = loadIcon,
                                pageOffsetProvider = pageOffsetProvider,
                                staggerFactor = staggerFactor,
                                onTap = { onLaunchApp(row.app.packageName) },
                                onLongPress = { contextApp = row.app }
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }

        AnimatedVisibility(
            visible = showJumpList,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(160))
        ) {
            val alphabet = remember { listOf("#") + ('a'..'z').map { it.toString() } }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MetroPalette.PureBlack.copy(alpha = 0.96f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showJumpList = false }
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(alphabet) { index, letter ->
                        val targetIndex = headerIndices[letter]
                        val enabled = targetIndex != null
                        val swivel = remember { Animatable(-75f) }
                        val itemAlpha = remember { Animatable(0f) }

                        LaunchedEffect(showJumpList) {
                            delay((index * 10L).coerceAtMost(220L))
                            launch { swivel.animateTo(0f, tween(280, easing = metroEase)) }
                            launch { itemAlpha.animateTo(1f, tween(200)) }
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .graphicsLayer {
                                    rotationX = swivel.value
                                    alpha = itemAlpha.value
                                    cameraDistance = 12f * density
                                }
                                .background(
                                    if (enabled) accentColor else MetroPalette.SurfaceDark,
                                    RectangleShape
                                )
                                .clickable(enabled = enabled) {
                                    showJumpList = false
                                    if (targetIndex != null) {
                                        scope.launch { listState.scrollToItem(targetIndex) }
                                    }
                                },
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(
                                text = letter,
                                fontFamily = MetroPalette.Selawik,
                                fontWeight = FontWeight.Light,
                                fontSize = 30.sp,
                                color = if (enabled) MetroPalette.White else MetroPalette.HelperGray,
                                modifier = Modifier.padding(start = 10.dp, bottom = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = contextApp != null,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(140))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MetroPalette.PureBlack.copy(alpha = 0.84f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { contextApp = null },
                contentAlignment = Alignment.Center
            ) {
                val activeApp = contextApp ?: lastContextApp
                AnimatedVisibility(
                    visible = contextApp != null,
                    enter = scaleIn(initialScale = 0.92f, animationSpec = tween(200, easing = metroEase)) + fadeIn(tween(150)),
                    exit = scaleOut(targetScale = 0.94f, animationSpec = tween(140)) + fadeOut(tween(120))
                ) {
                    if (activeApp != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .background(MetroPalette.SurfaceDark, RectangleShape)
                                .border(2.dp, MetroPalette.White, RectangleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {}
                                .padding(20.dp)
                        ) {
                            Text(
                                text = activeApp.label.uppercase(),
                                fontFamily = MetroPalette.Selawik,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = accentColor,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            ContextActionItem("Fixar na tela inicial") {
                                onPinToStart(activeApp)
                                contextApp = null
                            }
                            ContextActionItem("Informacoes do app") {
                                onOpenAppInfo(activeApp.packageName)
                                contextApp = null
                            }
                            ContextActionItem("Desinstalar") {
                                onUninstallApp(activeApp.packageName)
                                contextApp = null
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppListRowItem(
    app: AppEntry,
    accentColor: Color,
    loadIcon: suspend (String) -> ImageBitmap?,
    pageOffsetProvider: () -> Float,
    staggerFactor: Float,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    var pressedScale by remember { mutableFloatStateOf(1f) }

    val animTiltX by animateFloatAsState(
        targetValue = tiltX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.65f),
        label = "rowTiltX"
    )
    val animTiltY by animateFloatAsState(
        targetValue = tiltY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.65f),
        label = "rowTiltY"
    )
    val animScale by animateFloatAsState(
        targetValue = pressedScale,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.7f),
        label = "rowScale"
    )

    val glyph = remember(app.packageName, app.label) {
        TileGlyphs.resolve(app.packageName, app.label)
    }
    val fallbackBitmap by produceState<ImageBitmap?>(initialValue = null, app.packageName, glyph) {
        if (glyph == null) {
            value = loadIcon(app.packageName)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = -pageOffsetProvider() * staggerFactor
                scaleX = animScale
                scaleY = animScale
                rotationX = animTiltX
                rotationY = animTiltY
                cameraDistance = 12f * density
            }
            .pointerInput(app.packageName) {
                detectTapGestures(
                    onPress = { offset ->
                        val nx = ((offset.x / size.width.coerceAtLeast(1)) - 0.5f) * 2f
                        val ny = ((offset.y / size.height.coerceAtLeast(1)) - 0.5f) * 2f
                        tiltY = nx * 5f
                        tiltX = -ny * 5f
                        pressedScale = 0.97f
                        tryAwaitRelease()
                        tiltX = 0f
                        tiltY = 0f
                        pressedScale = 1f
                    },
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            }
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(accentColor, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            if (glyph != null) {
                Icon(
                    imageVector = glyph,
                    contentDescription = app.label,
                    tint = MetroPalette.White,
                    modifier = Modifier.size(26.dp)
                )
            } else if (fallbackBitmap != null) {
                Image(
                    bitmap = fallbackBitmap!!,
                    contentDescription = app.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = app.label,
            fontFamily = MetroPalette.Selawik,
            fontWeight = FontWeight.Light,
            fontSize = 22.sp,
            color = MetroPalette.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ContextActionItem(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        fontFamily = MetroPalette.Selawik,
        fontWeight = FontWeight.Light,
        fontSize = 24.sp,
        color = MetroPalette.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    )
}
