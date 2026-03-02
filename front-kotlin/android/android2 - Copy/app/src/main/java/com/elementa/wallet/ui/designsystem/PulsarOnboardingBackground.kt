package com.elementa.wallet.ui.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.elementa.wallet.R

/**
 * Fullscreen cosmic background used for the new onboarding flow.
 * Renders the `bg_onboarding` drawable with a subtle radial glow overlay.
 */
@Composable
fun PulsarOnboardingBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_onboarding),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                    .blur(8.dp),
            contentScale = ContentScale.Crop
        )

        // Dark overlay + soft radial glow to match design
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xCC020816),
                            Color(0xE600050A)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PulsarColors.PrimaryDark.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    )
                ),
            content = content
        )
    }
}

