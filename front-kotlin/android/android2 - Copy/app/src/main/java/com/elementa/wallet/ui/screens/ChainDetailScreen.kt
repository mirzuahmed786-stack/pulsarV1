package com.elementa.wallet.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.Transaction
import com.elementa.wallet.domain.model.TransactionType
import com.elementa.wallet.ui.components.ChainLogo
import com.elementa.wallet.ui.components.PulsarSparkline
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.viewmodel.ChainDetailViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChainDetailScreen(
    viewModel: ChainDetailViewModel,
    onAddToken: () -> Unit,
    onBack: () -> Unit,
    onSend: () -> Unit = {},
    onReceive: () -> Unit = {},
    onQrScan: () -> Unit = {},
    onActivity: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val pulsarBlue = Color(0xFF00D3F2)
    val cardBg = Color(0xFF1D293D)
    
    val symbol = nativeSymbolFor(state.chain)
    
    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    
                    Text(
                        text = "${state.chain.name} ($symbol)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    IconButton(onClick = { /* Sparkline overview */ }) {
                        Icon(Icons.Filled.TrendingUp, contentDescription = "Overview", tint = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Current Price Section
                item {
                    PriceHeaderSection(
                        chain = state.chain,
                        logoUrl = state.logoUrl,
                        price = state.currentPriceUsd,
                        changePct = state.priceChangePct24h,
                        cardBg = cardBg
                    )
                }

                // 2. Balance Section
                item {
                    val balanceValue = state.availableBalance.toDoubleOrNull() ?: 0.0
                    val totalUsd = balanceValue * state.currentPriceUsd
                    BalanceStatsSection(
                        balanceEth = state.availableBalance,
                        balanceUsd = String.format("%,.0f", totalUsd),
                        available = state.availableBalance,
                        staked = state.stakedBalance,
                        symbol = symbol,
                        cardBg = cardBg
                    )
                }

                // 3. Quick Actions
                item {
                    QuickActionRow(
                        onBuy = { },
                        onSend = onSend,
                        onSwap = { },
                        onReceive = onReceive,
                        accentColor = pulsarBlue
                    )
                }

                // 4. Tab Switcher
                item {
                    DetailTabSwitcher(
                        isActivitySelected = state.isActivityVisible,
                        onTabSelected = { viewModel.toggleActivityTab(it) },
                        cardBg = cardBg
                    )
                }

                if (!state.isActivityVisible) {
                    // Chart Content
                    item {
                        TimeframeFilters(accentColor = pulsarBlue)
                    }

                    item {
                        ChartSection(
                            title = "Price History (1W)",
                            data = state.sparkline.ifEmpty { listOf(0.2, 0.4, 0.3, 0.5, 0.4, 0.7, 1.0, 0.8) },
                            accentColor = pulsarBlue,
                            cardBg = cardBg
                        )
                    }

                    item {
                        VolumeSection(
                            title = "Volume",
                            data = List(20) { Math.random() },
                            accentColor = pulsarBlue,
                            cardBg = cardBg
                        )
                    }

                    item {
                        MarketStatsGrid(
                            marketCap = state.marketCapUsd,
                            volume24h = state.volume24hUsd,
                            circSupply = state.circulatingSupply,
                            ath = state.allTimeHigh,
                            cardBg = cardBg
                        )
                    }
                } else {
                    // Activity Content
                    if (state.transactions.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text("No recent transactions", color = Color.White.copy(alpha = 0.3f))
                            }
                        }
                    } else {
                        items(state.transactions) { tx ->
                            ActivityRow(tx, cardBg)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun PriceHeaderSection(chain: Chain, logoUrl: String?, price: Double, changePct: Double, cardBg: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ChainLogo(chain = chain, logoUrl = logoUrl, modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text("Current Price", fontSize = 16.sp, color = Color.White.copy(alpha = 0.4f))
            Text(text = String.format("$%,.2f", price), fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.offset(y = (-4).dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isPositive = changePct >= 0
                Icon(if (isPositive) Icons.Default.NorthEast else Icons.Default.SouthEast, null, tint = if (isPositive) Color(0xFF00FF88) else Color(0xFFFF4B4B), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = String.format("%.1f%% (24h)", changePct), fontSize = 16.sp, color = if (isPositive) Color(0xFF00FF88) else Color(0xFFFF4B4B), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BalanceStatsSection(balanceEth: String, balanceUsd: String, available: String, staked: String, symbol: String, cardBg: Color) {
    Surface(shape = RoundedCornerShape(24.dp), color = cardBg.copy(alpha = 0.5f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Your Balance", fontSize = 14.sp, color = Color.White.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "$balanceEth $symbol", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(text = "≈ $$balanceUsd", fontSize = 15.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.padding(top = 2.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Available / Staked", fontSize = 14.sp, color = Color.White.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(available, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Avl", fontSize = 14.sp, color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 2.dp))
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(staked, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Stk", fontSize = 14.sp, color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickActionRow(onBuy: () -> Unit, onSend: () -> Unit, onSwap: () -> Unit, onReceive: () -> Unit, accentColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ActionButtonItem(icon = Icons.Default.AccountBalanceWallet, label = "Buy", onClick = onBuy, accentColor = accentColor)
        ActionButtonItem(icon = Icons.Default.NorthEast, label = "Send", onClick = onSend, accentColor = accentColor)
        ActionButtonItem(icon = Icons.Default.Sync, label = "Swap", onClick = onSwap, accentColor = accentColor)
        ActionButtonItem(icon = Icons.Default.SouthWest, label = "Receive", onClick = onReceive, accentColor = accentColor)
    }
}

@Composable
private fun ActionButtonItem(icon: ImageVector, label: String, onClick: () -> Unit, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Surface(shape = CircleShape, color = Color(0xFF1E2631).copy(alpha = 0.8f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), modifier = Modifier.size(64.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(26.dp)) }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(label, fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DetailTabSwitcher(isActivitySelected: Boolean, onTabSelected: (Boolean) -> Unit, cardBg: Color) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF030712).copy(alpha = 0.6f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Row(modifier = Modifier.padding(4.dp)) {
            TabItem(label = "Chart", isSelected = !isActivitySelected, onClick = { onTabSelected(false) }, modifier = Modifier.weight(1f))
            TabItem(label = "Activity", isSelected = isActivitySelected, onClick = { onTabSelected(true) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TabItem(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = if (isSelected) Color(0xFF1E2631) else Color.Transparent, modifier = modifier.fillMaxHeight()) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun TimeframeFilters(accentColor: Color) {
    val filters = listOf("1D", "1W", "1M", "3M", "1Y", "ALL")
    var selectedFilter by remember { mutableStateOf("1W") }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        filters.forEach { filter ->
            Surface(onClick = { selectedFilter = filter }, shape = RoundedCornerShape(8.dp), color = if (selectedFilter == filter) Color(0xFF1E2631) else Color.Transparent) {
                Text(text = filter, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, color = if (selectedFilter == filter) accentColor else Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChartSection(title: String, data: List<Double>, accentColor: Color, cardBg: Color) {
    Surface(shape = RoundedCornerShape(24.dp), color = cardBg.copy(alpha = 0.4f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontSize = 14.sp, color = Color.White.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            PulsarSparkline(data = data, modifier = Modifier.fillMaxWidth().height(180.dp), lineColor = accentColor)
        }
    }
}

@Composable
private fun VolumeSection(title: String, data: List<Double>, accentColor: Color, cardBg: Color) {
    Surface(shape = RoundedCornerShape(24.dp), color = cardBg.copy(alpha = 0.4f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontSize = 14.sp, color = Color.White.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            VolumeChartMock(data = data, accentColor = accentColor)
        }
    }
}

@Composable
private fun VolumeChartMock(data: List<Double>, accentColor: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
        val width = size.width
        val height = size.height
        val barCount = data.size
        val barWidth = width / (barCount * 1.6f)
        val gap = barWidth * 0.6f
        data.indices.forEach { i ->
            val barHeight = (data[i] * height * 0.8f).toFloat().coerceAtLeast(4f)
            val x = i * (barWidth + gap)
            val y = height - barHeight
            drawRect(color = if (i == 12) accentColor else Color.White.copy(alpha = 0.1f), topLeft = androidx.compose.ui.geometry.Offset(x, y), size = androidx.compose.ui.geometry.Size(barWidth, barHeight))
        }
    }
}

@Composable
private fun MarketStatsGrid(marketCap: Double, volume24h: Double, circSupply: Double, ath: Double, cardBg: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBox(label = "Market Cap", value = "$${formatLargeNumber(marketCap)}", modifier = Modifier.weight(1f), cardBg = cardBg)
            StatBox(label = "24h Volume", value = "$${formatLargeNumber(volume24h)}", modifier = Modifier.weight(1f), cardBg = cardBg)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBox(label = "Circ. Supply", value = formatLargeNumber(circSupply), modifier = Modifier.weight(1f), cardBg = cardBg)
            StatBox(label = "All Time High", value = "$${formatLargeNumber(ath)}", modifier = Modifier.weight(1f), cardBg = cardBg)
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier, cardBg: Color) {
    Surface(shape = RoundedCornerShape(16.dp), color = cardBg.copy(alpha = 0.4f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(label, fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}

@Composable
private fun ActivityRow(tx: Transaction, cardBg: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = cardBg.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.05f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(when (tx.type) { TransactionType.SEND -> Icons.Default.ArrowOutward; TransactionType.RECEIVE -> Icons.Default.ArrowBack; else -> Icons.Default.Sync }, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.type.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(DateTimeFormatter.ofPattern("MMM dd, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(tx.timestamp)), fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f))
            }
            Text("${if (tx.isIncoming) "+" else "-"}${tx.amount} ${tx.symbol}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (tx.isIncoming) Color(0xFF00FF88) else Color.White)
        }
    }
}

private fun nativeSymbolFor(chain: Chain): String = when (chain) {
    Chain.BITCOIN   -> "BTC"
    Chain.ETHEREUM  -> "ETH"
    Chain.SOLANA    -> "SOL"
    Chain.BSC       -> "BNB"
    Chain.AVALANCHE -> "AVAX"
    Chain.POLYGON   -> "POL"
    Chain.LOCALHOST -> "ETH"
}

private fun formatLargeNumber(value: Double): String {
    if (value >= 1_000_000_000_000) return String.format("%.1fT", value / 1_000_000_000_000)
    if (value >= 1_000_000_000) return String.format("%.1fB", value / 1_000_000_000)
    if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000)
    return String.format("%,.0f", value)
}
