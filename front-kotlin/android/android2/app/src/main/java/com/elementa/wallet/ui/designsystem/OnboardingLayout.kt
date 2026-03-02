package com.elementa.wallet.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text

/**
 * Progress bar and header used only on welcome and onboarding screens.
 * Do not use on Dashboard, Chain Detail, or other app pages.
 */
@Composable
fun OnboardingProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val trackColor = Color.White.copy(alpha = 0.12f)
    val fillColor = PulsarColors.PrimaryDark
    
    // Animate progress changes
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "progress_animation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp) // Thinner like in image
            .clip(RoundedCornerShape(1.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(1.dp))
                .background(fillColor)
        )
    }
}

@Composable
fun OnboardingFlowHeader(modifier: Modifier = Modifier) {
    Text(
        text = "Pulsar Wallet Onboarding Flow",
        modifier = modifier.padding(vertical = 12.dp),
        fontSize = 12.sp,
        color = Color.White.copy(alpha = 0.4f),
        style = PulsarTypography.CyberLabel,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}
