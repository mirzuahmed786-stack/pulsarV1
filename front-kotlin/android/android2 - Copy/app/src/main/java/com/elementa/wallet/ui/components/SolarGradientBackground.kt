package com.elementa.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.elementa.wallet.ui.theme.LightBackground

@Composable
fun SolarGradientBackground(content: @Composable () -> Unit) {
    val lightGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFFE0F2F1).copy(alpha = 0.5f), // Very soft teal/mint center
            LightBackground
        ),
        radius = 2000f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .background(lightGradient)
    ) {
        content()
    }
}
