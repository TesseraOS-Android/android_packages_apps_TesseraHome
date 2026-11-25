package com.tesseraos.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesseraos.home.model.LauncherSettings
import com.tesseraos.home.theme.MetroOutlineButton
import com.tesseraos.home.theme.MetroPageHeader
import com.tesseraos.home.theme.MetroPalette
import com.tesseraos.home.theme.MetroRadioButton
import com.tesseraos.home.theme.MetroToggleSwitch

@Composable
fun MetroSettingsScreen(
    settings: LauncherSettings,
    notificationAccessGranted: Boolean,
    onUpdateSettings: (LauncherSettings) -> Unit,
    onPickWallpaper: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onResetDefaultTiles: () -> Unit,
    onClose: () -> Unit
) {
    val accent = Color(settings.accentColorHex)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroPalette.PureBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        MetroPageHeader(
            category = "TESSERAOS",
            title = "Tela Inicial"
        )

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Cor de destaque",
            fontFamily = MetroPalette.Selawik,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = MetroPalette.MutedLabel
        )
        Spacer(modifier = Modifier.height(10.dp))

        val rows = MetroPalette.Accents.chunked(5)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (swatch in row) {
                        val selected = settings.accentColorHex == swatch.hex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(swatch.color, RectangleShape)
                                .then(
                                    if (selected) Modifier.border(3.dp, MetroPalette.White, RectangleShape)
                                    else Modifier
                                )
                                .clickable {
                                    onUpdateSettings(settings.copy(accentColorHex = swatch.hex))
                                }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        MetroToggleSwitch(
            label = "Mostrar mais tiles (6 colunas)",
            checked = settings.gridColumns == 6,
            accentColor = accent,
            onCheckedChange = { more ->
                onUpdateSettings(settings.copy(gridColumns = if (more) 6 else 4))
            },
            helperText = "Alterna entre a grade classica de 4 colunas e a grade compacta de 6 colunas do WP 8.1."
        )

        MetroToggleSwitch(
            label = "Papel de parede parallax nos tiles",
            checked = settings.parallaxEnabled,
            accentColor = accent,
            onCheckedChange = { enabled ->
                onUpdateSettings(settings.copy(parallaxEnabled = enabled))
            },
            helperText = "Exibe a foto escolhida por tras dos tiles translucidos com deslocamento parallax."
        )

        if (settings.parallaxEnabled) {
            Spacer(modifier = Modifier.height(4.dp))
            MetroOutlineButton(
                text = if (settings.wallpaperUri == null) "Escolher imagem" else "Trocar imagem",
                onClick = onPickWallpaper
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Transparencia dos tiles",
                fontFamily = MetroPalette.Selawik,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = MetroPalette.MutedLabel
            )
            MetroRadioButton(
                label = "Suave",
                selected = settings.tileOpacity <= 0.55f,
                onClick = { onUpdateSettings(settings.copy(tileOpacity = 0.45f)) }
            )
            MetroRadioButton(
                label = "Equilibrado",
                selected = settings.tileOpacity in 0.56f..0.79f,
                onClick = { onUpdateSettings(settings.copy(tileOpacity = 0.72f)) }
            )
            MetroRadioButton(
                label = "Cristalino",
                selected = settings.tileOpacity >= 0.80f,
                onClick = { onUpdateSettings(settings.copy(tileOpacity = 0.88f)) }
            )
        }

        MetroToggleSwitch(
            label = "Animacao Turnstile 3D",
            checked = settings.turnstileEnabled,
            accentColor = accent,
            onCheckedChange = { enabled ->
                onUpdateSettings(settings.copy(turnstileEnabled = enabled))
            },
            helperText = "Rotacao 3D no eixo Y ao abrir aplicativos a partir da tela inicial."
        )

        MetroToggleSwitch(
            label = "Live Tiles com giro 3D",
            checked = settings.liveFlipEnabled,
            accentColor = accent,
            onCheckedChange = { enabled ->
                onUpdateSettings(settings.copy(liveFlipEnabled = enabled))
            },
            helperText = "Gira os tiles medios e largos para exibir o texto das notificacoes ativas."
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Integracao do sistema",
            fontFamily = MetroPalette.Selawik,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = MetroPalette.MutedLabel
        )
        Spacer(modifier = Modifier.height(10.dp))

        MetroOutlineButton(
            text = if (notificationAccessGranted) {
                "Live Tiles ativo (gerenciar acesso)"
            } else {
                "Ativar acesso a notificacoes"
            },
            onClick = onOpenNotificationSettings,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        MetroOutlineButton(
            text = "Definir TesseraHome como launcher padrao",
            onClick = onOpenHomeSettings,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        MetroOutlineButton(
            text = "Restaurar layout padrao de tiles",
            onClick = onResetDefaultTiles,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(22.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            MetroOutlineButton(
                text = "Voltar",
                onClick = onClose
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}
