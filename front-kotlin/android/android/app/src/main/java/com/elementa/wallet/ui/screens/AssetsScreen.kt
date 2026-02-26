package com.elementa.wallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.ui.components.PulsarSparkline
import com.elementa.wallet.ui.components.RedesignedBottomNav
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.viewmodel.AssetsViewModel
import com.elementa.wallet.viewmodel.LiveDataViewModel

// Accent colours per coin — matches image 2
private fun coinAccent(symbol: String): Color = when (symbol.uppercase()) {
    "BTC"          -> Color(0xFFF7931A)
    "ETH"          -> Color(0xFF627EEA)
    "SOL"          -> Color(0xFF00FFA3)
    "MATIC", "POL" -> Color(0xFF8247E5)
    "AVAX"         -> Color(0xFFE84142)
    "BNB"          -> Color(0xFFF3BA2F)
    else           -> PulsarColors.PrimaryDark
}

@Composable
fun AssetsScreen(
    viewModel: AssetsViewModel,
    liveDataViewModel: LiveDataViewModel = hiltViewModel(),
    onAddToken: () -> Unit,
    onChainDetail: (Chain, NetworkType) -> Unit,
    onAssetDetail: (String, String) -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onBack: () -> Unit,
    onSwap: () -> Unit = {},
    onActivity: () -> Unit = {},
    onNotifications: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val holdings by liveDataViewModel.walletHoldings.collectAsState()
    val isLoading by liveDataViewModel.isLoading.collectAsState()
    
    val cardBg = PulsarColors.SurfaceDark
    val cyan = PulsarColors.PrimaryDark
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Tokens", "NFTs", "Staking")
    var searchQuery by remember { mutableStateOf("") }

    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { PulsarAssetsTopBar(onBack = onBack, onNotifications = onNotifications) },
            bottomBar = {
                RedesignedBottomNav(
                    onDashboard = onBack,
                    onAssets = {},
                    onTransfers = onSend,
                    onSwap = onSwap,
                    onActivity = onActivity,
                    currentRoute = "assets"
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Refresh indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = cyan,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                }

                // Refresh indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = cyan,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Welcome Section ───────────────────────────────────────
                    Column {
                        Text(
                            "PORTFOLIO OVERVIEW",
                            style = PulsarTypography.CyberLabel,
                            color = PulsarColors.TextSecondaryDark.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "Assets",
                            style = PulsarTypography.Typography.displayMedium,
                            color = PulsarColors.TextPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Search bar ────────────────────────────────────────────
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PulsarColors.SurfaceDark, RoundedCornerShape(24.dp))
                            .border(1.dp, PulsarColors.BorderSubtleDark, RoundedCornerShape(24.dp)),
                        placeholder = {
                            Text(
                                "Search assets...", 
                                color = PulsarColors.TextMutedDark, 
                                style = PulsarTypography.Typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = PulsarColors.TextMutedDark, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search", tint = PulsarColors.TextMutedDark, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        textStyle = PulsarTypography.Typography.bodyMedium.copy(
                            color = PulsarColors.TextPrimaryDark
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = PulsarColors.PrimaryDark
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Tabs ──────────────────────────────────────────────────────────
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        tabs.forEachIndexed { idx, label ->
                            val active = idx == selectedTab
                            Surface(
                                onClick = { selectedTab = idx },
                                shape = RoundedCornerShape(24.dp),
                                color = if (active) cyan else PulsarColors.SurfaceDark,
                                border = if (active) null else BorderStroke(1.dp, PulsarColors.BorderSubtleDark)
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = if (active) Color.Black else PulsarColors.TextSecondaryDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Content ───────────────────────────────────────────────────────────
                    if (selectedTab == 0) {
                        Text(
                            "YOUR PORTFOLIO",
                            style = PulsarTypography.CyberLabel,
                            color = PulsarColors.TextMutedDark,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Total value section
                        holdings?.let { summary ->
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = PulsarColors.SurfaceDark,
                                border = BorderStroke(1.dp, PulsarColors.BorderSubtleDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "Total Balance",
                                            style = PulsarTypography.Typography.labelSmall,
                                            color = PulsarColors.TextSecondaryDark,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            String.format("$%.2f", summary.totalValueUSD),
                                            style = PulsarTypography.Typography.headlineLarge,
                                            color = PulsarColors.TextPrimaryDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 32.sp,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 12.dp)
                                        ) {
                                            val change24h = summary.change24hPercent
                                            val changeColor = when {
                                                change24h > 0 -> cyan
                                                change24h < 0 -> PulsarColors.ErrorRed
                                                else -> PulsarColors.TextMutedDark
                                            }
                                            Icon(
                                                if (change24h >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                contentDescription = null,
                                                tint = changeColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                String.format("%.2f%%", kotlin.math.abs(change24h)),
                                                color = changeColor,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                " 24h",
                                                color = PulsarColors.TextMutedDark,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(40.dp),
                                            color = cyan,
                                            strokeWidth = 3.dp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Show wallet holdings
                        holdings?.let { summary ->
                            // Filter holdings by search query
                            val filteredHoldings = if (searchQuery.isBlank()) {
                                summary.holdings
                            } else {
                                summary.holdings.filter { holding ->
                                    holding.name.contains(searchQuery, ignoreCase = true) ||
                                    holding.symbol.contains(searchQuery, ignoreCase = true) ||
                                    holding.chain.name.contains(searchQuery, ignoreCase = true)
                                }
                            }
                            
                            if (filteredHoldings.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            if (searchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.Default.Wallet,
                                            contentDescription = null,
                                            modifier = Modifier.size(56.dp),
                                            tint = cyan.copy(alpha = 0.4f)
                                        )
                                        Text(
                                            if (searchQuery.isNotBlank()) "No assets found for \"$searchQuery\"" else "No holdings yet",
                                            color = PulsarColors.TextSecondaryDark,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(top = 16.dp)
                                        )
                                        Text(
                                            if (searchQuery.isNotBlank()) "Try a different search" else "Start receiving assets",
                                            color = PulsarColors.TextMutedDark,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    filteredHoldings.forEach { holding ->
                                        LiveHoldingCard(
                                            holding = holding,
                                            cardBg = cardBg,
                                            onTrade = { onAssetDetail(holding.chain.name.lowercase(), holding.symbol.lowercase()) },
                                            onSend = onSend
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${tabs[selectedTab]} coming soon",
                                color = PulsarColors.TextMutedDark,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun PulsarAssetsTopBar(
    onBack: () -> Unit,
    onNotifications: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(PulsarColors.SurfaceDark)
                .border(1.dp, PulsarColors.BorderSubtleDark, CircleShape)
        ) {
            Icon(
                Icons.Default.GridView,
                contentDescription = "Back to Dashboard",
                tint = PulsarColors.PrimaryDark,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            "Asset Portfolio",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = PulsarColors.TextPrimaryDark,
            style = PulsarTypography.Typography.titleMedium
        )
        IconButton(
            onClick = onNotifications,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(PulsarColors.SurfaceDark)
                .border(1.dp, PulsarColors.BorderSubtleDark, CircleShape)
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = PulsarColors.TextSecondaryDark,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Displays a live wallet holding with current price and 24h change
 * Optimized for mobile view with proper spacing and no overflow
 */
@Composable
private fun LiveHoldingCard(
    holding: com.elementa.wallet.domain.model.TokenHolding,
    cardBg: Color,
    onTrade: () -> Unit,
    onSend: () -> Unit
) {
    val accent = coinAccent(holding.symbol)
    val changeColor = if (holding.change24h >= 0) PulsarColors.PrimaryDark else PulsarColors.ErrorRed

    Surface(
        onClick = onTrade,
        shape = RoundedCornerShape(24.dp),
        color = cardBg,
        border = BorderStroke(1.dp, PulsarColors.BorderSubtleDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // ── Top Section: Icon + Name ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Icon + Name + Network
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box {
                        // Coin icon
                        Surface(
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.15f),
                            border = BorderStroke(2.dp, accent.copy(alpha = 0.4f)),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    holding.symbol.take(1),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    color = accent
                                )
                            }
                        }
                        // Status indicator (online dot)
                        Surface(
                            shape = CircleShape,
                            color = PulsarColors.PrimaryDark,
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.BottomEnd)
                        ) {}
                    }
                    
                    Spacer(modifier = Modifier.width(14.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            holding.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = PulsarColors.TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${holding.chain.name.uppercase()} - FAST (0MS)",
                            fontSize = 10.sp,
                            color = PulsarColors.TextMutedDark,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Balance (moved below name to avoid overflow) ─────────────
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "BALANCE",
                    fontSize = 9.sp,
                    color = PulsarColors.TextMutedDark,
                    letterSpacing = 1.sp
                )
                Text(
                    "${String.format("%.1f", holding.balance.toDoubleOrNull() ?: 0.0)} ${holding.symbol}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = PulsarColors.TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    holding.getFormattedValue(),
                    fontSize = 13.sp,
                    color = PulsarColors.PrimaryDark,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Live Price Section ──────────────────────────────────────
            Column {
                Text(
                    "LIVE PRICE",
                    fontSize = 9.sp,
                    color = PulsarColors.TextMutedDark,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        String.format("$%.2f", holding.priceUSD),
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = PulsarColors.TextPrimaryDark
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                changeColor.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            if (holding.change24h >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = changeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${if (holding.change24h >= 0) "+" else ""}${String.format("%.2f%%", holding.change24h)}",
                            fontSize = 13.sp,
                            color = changeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Chart Section ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PulsarColors.PanelDark.copy(alpha = 0.3f))
                    .padding(8.dp)
            ) {
                // Use live sparkline data from API, with fallback to normalized values
                val sparklineData = if (holding.sparkline.isNotEmpty()) {
                    // Normalize sparkline values to 0-1 range for consistent chart display
                    val minVal = holding.sparkline.minOrNull() ?: 0.0
                    val maxVal = holding.sparkline.maxOrNull() ?: 1.0
                    val range = maxVal - minVal
                    if (range > 0) {
                        holding.sparkline.map { (it - minVal) / range }
                    } else {
                        holding.sparkline
                    }
                } else {
                    // Fallback when no data available
                    listOf(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5)
                }
                
                PulsarSparkline(
                    data = sparklineData,
                    modifier = Modifier.fillMaxSize(),
                    lineColor = accent,
                    animate = false
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Bottom Section: Actions ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tap for details hint - with flexible space
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(PulsarColors.PrimaryDark, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Tap card for details",
                        fontSize = 11.sp,
                        color = PulsarColors.TextMutedDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Action icons - fixed width
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = PulsarColors.TextMutedDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = "Explorer",
                            tint = PulsarColors.TextMutedDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── View Explorer Link ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "VIEW EXPLORER",
                    fontSize = 9.sp,
                    color = PulsarColors.TextMutedDark,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ArrowOutward,
                    contentDescription = null,
                    tint = PulsarColors.TextMutedDark,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}