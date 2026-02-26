package com.elementa.wallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.domain.model.BlockchainData
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.ui.components.PulsarSparkline
import com.elementa.wallet.ui.designsystem.*

@Composable
fun ChainsListScreen(
    onChainSelected: (Chain) -> Unit,
    onBack: () -> Unit
) {
    val blockchains = remember { BlockchainData.getData() }

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
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Text(
                        "ALL BLOCKCHAINS",
                        style = PulsarTypography.CyberLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp
                    )
                    Surface(
                        onClick = { },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "BLOCKCHAIN NETWORKS",
                            style = PulsarTypography.CyberLabel,
                            color = PulsarColors.TextSecondaryLight,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "Explore Active Networks",
                            style = PulsarTypography.Typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(blockchains) { blockchain ->
                    BlockchainCard(
                        blockchain = blockchain,
                        onClick = { onChainSelected(blockchain.chain) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun BlockchainCard(
    blockchain: BlockchainData,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = PulsarColors.SurfaceDark,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            // Header: Logo, Name, Price
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Blockchain Logo
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.size(56.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = blockchain.symbol.take(1),
                                style = PulsarTypography.Typography.titleMedium,
                                color = PulsarColors.PrimaryDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            blockchain.name,
                            style = PulsarTypography.Typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            blockchain.symbol,
                            style = PulsarTypography.Typography.labelSmall,
                            color = PulsarColors.TextSecondaryLight,
                            fontSize = 12.sp
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "$${String.format("%.2f", blockchain.currentPrice)}",
                        style = PulsarTypography.Typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (blockchain.priceChangePercent24h >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (blockchain.priceChangePercent24h >= 0) PulsarColors.SuccessGreen else PulsarColors.DangerRed,
                            modifier = Modifier.size(14.sp.value.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${String.format("%.2f", kotlin.math.abs(blockchain.priceChangePercent24h))}%",
                            style = PulsarTypography.Typography.labelSmall,
                            color = if (blockchain.priceChangePercent24h >= 0) PulsarColors.SuccessGreen else PulsarColors.DangerRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sparkline Chart
            if (blockchain.sparkline.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.02f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp)
                ) {
                    PulsarSparkline(
                        data = blockchain.sparkline,
                        lineColor = if (blockchain.priceChangePercent24h >= 0) PulsarColors.SuccessGreen else PulsarColors.DangerRed,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatsColumn(
                    label = "Market Cap",
                    value = formatLargeNumber(blockchain.marketCap)
                )
                StatsColumn(
                    label = "24h Volume",
                    value = formatLargeNumber(blockchain.volume24h)
                )
                StatsColumn(
                    label = "Network",
                    value = blockchain.networkType.name
                )
            }
        }
    }
}

@Composable
private fun StatsColumn(
    label: String,
    value: String
) {
    Column {
        Text(
            label,
            style = PulsarTypography.Typography.labelSmall,
            color = PulsarColors.TextSecondaryLight,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = PulsarTypography.Typography.labelMedium,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatLargeNumber(value: Double): String {
    return when {
        value >= 1_000_000_000 -> "${String.format("%.1f", value / 1_000_000_000)}B"
        value >= 1_000_000 -> "${String.format("%.1f", value / 1_000_000)}M"
        value >= 1_000 -> "${String.format("%.1f", value / 1_000)}K"
        else -> "${String.format("%.2f", value)}"
    }
}
