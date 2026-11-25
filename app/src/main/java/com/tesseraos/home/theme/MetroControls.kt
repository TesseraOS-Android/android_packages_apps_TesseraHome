package com.tesseraos.home.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MetroPageHeader(
    category: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = category.uppercase(),
            fontFamily = MetroPalette.Selawik,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MetroPalette.White,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontFamily = MetroPalette.Selawik,
            fontWeight = FontWeight.Light,
            fontSize = 44.sp,
            lineHeight = 48.sp,
            color = MetroPalette.White
        )
    }
}

@Composable
fun MetroToggleSwitch(
    label: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    helperText: String? = null,
    onText: String = "Ativado",
    offText: String = "Desativado"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 42.dp else 0.dp,
        animationSpec = tween(durationMillis = 160),
        label = "metroThumb"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = label,
            fontFamily = MetroPalette.Selawik,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = MetroPalette.MutedLabel
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (checked) onText else offText,
                fontFamily = MetroPalette.Selawik,
                fontWeight = FontWeight.Light,
                fontSize = 24.sp,
                color = MetroPalette.White
            )
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(28.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(22.dp)
                        .border(2.dp, MetroPalette.White, RectangleShape)
                        .padding(3.dp)
                ) {
                    if (checked) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .background(accentColor, RectangleShape)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset)
                        .width(14.dp)
                        .height(28.dp)
                        .background(MetroPalette.White, RectangleShape)
                )
            }
        }
        if (!helperText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = helperText,
                fontFamily = MetroPalette.Selawik,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MetroPalette.HelperGray
            )
        }
    }
}

@Composable
fun MetroRadioButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(2.dp, MetroPalette.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(MetroPalette.White, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            fontFamily = MetroPalette.Selawik,
            fontWeight = FontWeight.Light,
            fontSize = 22.sp,
            color = MetroPalette.White
        )
    }
}

@Composable
fun MetroOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(2.dp, MetroPalette.White, RectangleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = MetroPalette.Selawik,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MetroPalette.White
        )
    }
}
