package com.elementa.wallet.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.ui.state.HomeUiState
import com.elementa.wallet.ui.state.ChainMarketUi
import com.elementa.wallet.viewmodel.HomeViewModel
import com.elementa.wallet.ui.components.PulsarSparkline
import com.elementa.wallet.ui.components.RedesignedBottomNav
import kotlinx.coroutines.delay
import coil.compose.AsyncImage

@Composable
fun HomeScreen(
    onOpenAssets: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onSwap: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEcosystem: () -> Unit,
    onChainDetailClicked: (Chain, NetworkType) -> Unit = { _, _ -> },
    onMenuClick: () -> Unit = {},
    onLockWallet: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val homeVm = androidx.hilt.navigation.compose.hiltViewModel<HomeViewModel>()
    val homeState by homeVm.uiState.collectAsState()
    
    var isLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        isLoaded = true
        homeVm.refreshPortfolioWithPrices(isManualRefresh = false)
    }

    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { PulsarRedesignedTopBar(
                onMenuClick = onMenuClick,
                onLockClick = onLockWallet,
                onNotificationsClick = onNotificationsClick,
                onRefresh = { homeVm.refreshPortfolioWithPrices(isManualRefresh = true) }
            ) },
            bottomBar = {
                RedesignedBottomNav(
                    onDashboard = {},
                    onAssets = onOpenAssets,
                    onTransfers = onSend,
                    onSwap = onSwap,
                    onActivity = onOpenActivity,
                    currentRoute = "dashboard"
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AnimatedVisibility(visible = homeState.isManualRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = PulsarColors.PrimaryDark,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                }

                // Welcome Section
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        "VAULT COMMAND CENTER",
                        style = PulsarTypography.CyberLabel,
                        color = PulsarColors.ActiveTextPrimary.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Home",
                        style = PulsarTypography.Typography.displayMedium,
                        color = PulsarColors.ActiveTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = PulsarColors.ActivePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Secured Wallet",
                            style = PulsarTypography.Typography.labelSmall,
                            color = PulsarColors.ActiveTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Global Portfolio Card
                PremiumPortfolioCard(
                    state = homeState,
                    onSend = onSend,
                    onReceive = onReceive,
                    isRefreshing = homeState.isLoading,
                    onRefresh = { homeVm.refreshPortfolioWithPrices(isManualRefresh = true) }
                )

                // Chain Assets List
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val fallbackChains = listOf(
                        ChainMarketUi(Chain.BITCOIN, "Bitcoin", "BTC", "", 0.0, 0.0, "0", 0.0, 0.0, 0.0, 0L, "Normal", emptyList()),
                        ChainMarketUi(Chain.ETHEREUM, "Ethereum", "ETH", "", 0.0, 0.0, "0", 0.0, 0.0, 0.0, 0L, "Normal", emptyList()),
                        ChainMarketUi(Chain.SOLANA, "Solana", "SOL", "", 0.0, 0.0, "0", 0.0, 0.0, 0.0, 0L, "Normal", emptyList()),
                        ChainMarketUi(Chain.AVALANCHE, "Avalanche", "AVAX", "", 0.0, 0.0, "0", 0.0, 0.0, 0.0, 0L, "Normal", emptyList()),
                        ChainMarketUi(Chain.POLYGON, "Polygon", "POL", "", 0.0, 0.0, "0", 0.0, 0.0, 0.0, 0L, "Normal", emptyList()),
                        ChainMarketUi(Chain.BSC, "BNB Chain", "BNB", "", 0.0, 0.0, "0", 0.0, 0.0, 0.0, 0L, "Normal", emptyList())
                    )
                    val markets = if (homeState.chainMarkets.isNotEmpty()) homeState.chainMarkets else fallbackChains

                    markets.forEach { market ->
                        val statusColor = if (market.priceChangePct24h >= 0) PulsarColors.PrimaryDark else PulsarColors.DangerRed
                        ChainAssetCard(
                            name = market.name,
                            network = "Mainnet • ${market.networkSpeedLabel} (${market.networkLatencyMs}ms)",
                            balance = market.balanceAmount,
                            symbol = market.symbol,
                            usdValue = String.format("$%.2f", market.holdingsUsd),
                            statusColor = statusColor,
                            sparkline = if (market.sparkline.isNotEmpty()) market.sparkline else homeState.sparkline,
                            address = market.walletAddress,
                            logoUrl = market.logoUrl,
                            currentPrice = market.currentPriceUsd,
                            priceChange24h = market.priceChangePct24h,
                            alpha = 1f,
                            onClick = {
                                onChainDetailClicked(market.chain, NetworkType.MAINNET)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun PulsarRedesignedTopBar(
    onMenuClick: () -> Unit = {},
    onLockClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.clickable { onRefresh() }
        ) {
            Text(
                "PULSAR",
                style = PulsarTypography.CyberLabel,
                color = PulsarColors.ActivePrimary,
                letterSpacing = 4.sp,
                fontSize = 18.sp
            )
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusIconButton(Icons.Default.Lock, isPrimary = true, onClick = onLockClick)
            StatusIconButton(Icons.Default.Notifications, onClick = onNotificationsClick)
            StatusIconButton(Icons.Default.Menu, onClick = onMenuClick)
        }
    }
}

@Composable
private fun StatusIconButton(icon: ImageVector, isPrimary: Boolean = false, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isPrimary) PulsarColors.ActivePrimary.copy(alpha = 0.1f) else Color.Transparent,
        border = if (isPrimary) null else BorderStroke(1.dp, PulsarColors.ActiveTextPrimary.copy(alpha = 0.12f)),
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPrimary) PulsarColors.ActivePrimary else PulsarColors.ActiveTextPrimary.copy(alpha = 0.9f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PremiumPortfolioCard(
    state: HomeUiState,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PulsarColors.SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box {
            // Background Glow
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-30).dp)
                    .size(180.dp)
                    .blur(30.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PulsarColors.PrimaryDark.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

            Column(modifier = Modifier.padding(24.dp)) {
                val rotation by animateFloatAsState(
                    targetValue = if (isRefreshing) 360f else 0f,
                    animationSpec = tween(durationMillis = 900, easing = LinearEasing),
                    label = "portfolio_refresh_rotation"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "GLOBAL PORTFOLIO",
                        style = PulsarTypography.CyberLabel,
                        color = PulsarColors.TextSecondaryLight,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp
                    )

                    Surface(
                        onClick = onRefresh,
                        color = PulsarColors.SurfaceDark.copy(alpha = 0.5f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer { rotationZ = rotation }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = String.format("$%.2f", state.totalBalanceUsd),
                    style = PulsarTypography.Typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    letterSpacing = (-1).sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    val isPositive = state.balanceDeltaUsd24h >= 0
                    Icon(
                        imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = PulsarColors.PrimaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format("%.1f%%", kotlin.math.abs(state.balanceDeltaPct24h)),
                        style = PulsarTypography.Typography.labelSmall,
                        color = PulsarColors.PrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Vault PNL (24h)",
                        style = PulsarTypography.Typography.labelSmall,
                        color = PulsarColors.TextSecondaryLight,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onSend,
                        modifier = Modifier.weight(1f).height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PulsarColors.PrimaryDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.NorthEast, contentDescription = null, tint = Color.Black)
                            Text("Send Funds", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = onReceive,
                        modifier = Modifier.weight(1f).height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SouthWest, contentDescription = null, tint = Color.White)
                            Text("Receive Funds", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChainAssetCard(
    name: String,
    network: String,
    balance: String,
    symbol: String,
    usdValue: String,
    statusColor: Color,
    sparkline: List<Double>,
    address: String,
    logoUrl: String,
    currentPrice: Double = 0.0,
    priceChange24h: Double = 0.0,
    alpha: Float = 1f,
    onClick: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    var showCopied by remember { mutableStateOf(false) }
    
    // Format address for display (truncated)
    val displayAddress = if (address.length > 18) {
        "${address.take(10)}...${address.takeLast(6)}"
    } else if (address.isBlank()) {
        "Address not available"
    } else {
        address
    }
    
    // Reset copied state after delay
    LaunchedEffect(showCopied) {
        if (showCopied) {
            delay(2000)
            showCopied = false
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth().alpha(alpha),
        onClick = onClick,
        color = PulsarColors.SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.05f),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (logoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = logoUrl,
                                        contentDescription = name,
                                        modifier = Modifier.size(30.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Text(
                                        symbol.take(1),
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(14.dp)
                                .background(statusColor, CircleShape)
                                .border(2.dp, PulsarColors.SurfaceDark, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Text(network.uppercase(), style = PulsarTypography.CyberLabel, color = PulsarColors.TextSecondaryLight, fontSize = 9.sp)
                    }
                }
            }
            
            // Live Price Display
            if (currentPrice > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("LIVE PRICE", style = PulsarTypography.CyberLabel, color = PulsarColors.TextSecondaryLight, fontSize = 9.sp)
                        Text(
                            text = String.format("$%.2f", currentPrice),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isPositive = priceChange24h >= 0
                        Icon(
                            imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%+.2f%%", priceChange24h),
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (sparkline.isNotEmpty()) {
                PulsarSparkline(
                    data = sparkline,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    lineColor = PulsarColors.PrimaryDark
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Surface(
                color = Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.size(8.dp).background(
                            if (showCopied) PulsarColors.SuccessGreen else PulsarColors.PrimaryDark, 
                            CircleShape
                        ))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (showCopied) "Copied!" else displayAddress,
                            style = PulsarTypography.Typography.labelSmall,
                            color = if (showCopied) PulsarColors.SuccessGreen else PulsarColors.TextSecondaryLight,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            if (showCopied) Icons.Default.Check else Icons.Default.ContentCopy, 
                            contentDescription = "Copy Address", 
                            tint = if (showCopied) PulsarColors.SuccessGreen else PulsarColors.TextSecondaryLight, 
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { 
                                    if (address.isNotBlank()) {
                                        clipboardManager.setText(AnnotatedString(address))
                                        showCopied = true
                                    }
                                }
                        )
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = PulsarColors.TextSecondaryLight, modifier = Modifier.size(18.dp).clickable { })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                    Text("VIEW EXPLORER", style = PulsarTypography.CyberLabel, color = PulsarColors.TextSecondaryLight, fontSize = 9.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = PulsarColors.TextSecondaryLight, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}
