package com.elementa.wallet.ui.components.futuristic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.PulsarColors
import com.elementa.wallet.ui.designsystem.PulsarShapes
import com.elementa.wallet.ui.designsystem.PulsarTypography

@Composable
fun FuturisticAssetCard(
    symbol: String,
    name: String,
    balance: String,
    value: String,
    change: String,
    isPositive: Boolean = true
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Placeholder
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White.copy(alpha = 0.1f), PulsarShapes.Medium),
                contentAlignment = Alignment.Center
            ) {
                Text(symbol.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = PulsarTypography.Typography.titleMedium, color = Color.White)
                Text(symbol, style = PulsarTypography.Typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(value, style = PulsarTypography.Typography.titleMedium, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isPositive) "↗ $change" else "↘ $change",
                        color = if (isPositive) PulsarColors.SuccessGreen else PulsarColors.ErrorRed,
                        style = PulsarTypography.Typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
