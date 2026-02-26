package com.elementa.wallet.ui.components.futuristic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.PulsarColors
import com.elementa.wallet.ui.designsystem.PulsarShapes

@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = PulsarShapes.ActionPill,
                spotColor = PulsarColors.ProfessionalTeal
            )
            .clip(PulsarShapes.ActionPill)
            .background(
                if (enabled) Brush.horizontalGradient(PulsarColors.ActionGradient)
                else Brush.horizontalGradient(listOf(Color.Gray, Color.LightGray))
            )
            .clickable(enabled = enabled, onClick = onClick)
            .height(56.dp)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = if (enabled) Color.White else Color.DarkGray,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            fontSize = 14.sp
        )
    }
}
