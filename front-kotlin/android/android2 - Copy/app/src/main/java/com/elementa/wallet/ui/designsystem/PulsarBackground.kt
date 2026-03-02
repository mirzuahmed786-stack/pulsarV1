package com.elementa.wallet.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun PulsarBackground(content: @Composable () -> Unit) {
    val bg = PulsarColors.ActiveBackground
    val surface = PulsarColors.ActiveSurface
    val glowBase = PulsarColors.ActivePrimary.copy(alpha = 0.08f)

    val gradient = Brush.verticalGradient(
        colors = listOf(
            bg,
            surface.copy(alpha = 0.8f),
            bg
        )
    )

    val glowGradient = Brush.radialGradient(
        colors = listOf(
            glowBase,
            Color.Transparent
        ),
        center = androidx.compose.ui.geometry.Offset(x = 100f, y = 100f),
        radius = 1200f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .background(gradient)
            .background(glowGradient)
    ) {
        content()
    }
}
