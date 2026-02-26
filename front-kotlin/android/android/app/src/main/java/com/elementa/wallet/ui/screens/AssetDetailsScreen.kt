package com.elementa.wallet.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.ui.components.PulsarSparkline

@Composable
fun AssetDetailsScreen(
    chainId: String,
    assetId: String,
    onBack: () -> Unit
) {
    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PulsarColors.SurfaceDark.copy(alpha = 0.4f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        "${assetId.uppercase()} ASSET",
                        style = PulsarTypography.CyberLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp
                    )
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PulsarColors.SurfaceDark.copy(alpha = 0.4f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.StarBorder, contentDescription = "Favorite", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Price Hero Section
                AssetPriceHero(assetId)

                // Chart
                Box(modifier = Modifier.fillMaxWidth().height(220.dp).padding(vertical = 12.dp)) {
                    PulsarSparkline(
                        data = listOf(140.0, 141.2, 139.8, 142.5, 141.0, 143.8, 142.52),
                        modifier = Modifier.fillMaxSize(),
                        lineColor = PulsarColors.PrimaryDark
                    )
                }

                // Balance Card
                AssetBalanceCard(assetId)

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PulsarComponents.PulsarButton(
                        text = "Send",
                        onClick = {},
                        modifier = Modifier.weight(1f).height(56.dp),
                        glow = true
                    )
                    PulsarComponents.PulsarOutlinedButton(
                        text = "Receive",
                        onClick = {},
                        modifier = Modifier.weight(1f).height(56.dp)
                    )
                }

                // Stats Section
                SettingsSection(title = "Market Statistics") {
                    StatRow("Market Cap", "$89.2B")
                    StatRow("24H Volume", "$2.1B")
                    StatRow("Circulating Supply", "441.2M")
                    StatRow("All Time High", "$260.06")
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun AssetPriceHero(assetId: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            "$142.52", 
            style = PulsarTypography.Typography.displayLarge, 
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 56.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = PulsarColors.PrimaryDark, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("+4.25%", color = PulsarColors.PrimaryDark, style = PulsarTypography.Typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.White.copy(alpha = 0.05f),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    "24H", 
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = Color.White.copy(alpha = 0.4f), 
                    style = PulsarTypography.Typography.labelSmall,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun AssetBalanceCard(assetId: String) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("YOUR BALANCE", style = PulsarTypography.CyberLabel, color = PulsarColors.PrimaryDark, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("12.450 ${assetId.uppercase()}", style = PulsarTypography.Typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("$1,774.37", style = PulsarTypography.Typography.titleLarge, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            title.uppercase(),
            style = PulsarTypography.CyberLabel,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = PulsarColors.SurfaceDark,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = PulsarTypography.Typography.bodyMedium, color = Color.White.copy(alpha = 0.4f))
        Text(value, style = PulsarTypography.Typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
