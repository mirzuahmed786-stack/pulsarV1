package com.elementa.wallet.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.ui.components.PulsarBottomNav
import com.elementa.wallet.ui.components.PulsarSparkline
import com.elementa.wallet.ui.designsystem.PulsarBackground
import com.elementa.wallet.ui.designsystem.PulsarColors
import com.elementa.wallet.ui.designsystem.PulsarOnboardingBackground
import com.elementa.wallet.ui.designsystem.PulsarTypography
import com.elementa.wallet.ui.state.ChainMarketUi
import com.elementa.wallet.ui.state.HomeUiState
import com.elementa.wallet.viewmodel.HomeViewModel
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
    onBuy: () -> Unit = {},
    onChainDetailClicked: (Chain, NetworkType) -> Unit = { _, _ -> },
    onLockWallet: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val homeVm = androidx.hilt.navigation.compose.hiltViewModel<HomeViewModel>()
    val homeState by homeVm.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        homeVm.refreshPortfolioWithPrices(isManualRefresh = false)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF030712))) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = com.elementa.wallet.R.drawable.wallet_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.4f
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = { 
                DashboardTopBar(
                    onRefresh = { homeVm.refreshPortfolioWithPrices(isManualRefresh = true) }
                ) 
            },
            bottomBar = {
                PulsarBottomNav(
                    onHome = {},
                    onAssets = onOpenAssets,
                    onHub = onOpenEcosystem,
                    onActivity = onOpenActivity,
                    onSettings = onOpenSettings,
                    currentRoute = "home"
                )
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
                Spacer(modifier = Modifier.height(4.dp))

                // Premium Portfolio Card 100% pixel perfect
                PortfolioBalanceCard(
                    state = homeState,
                    onSend = onSend,
                    onReceive = onReceive,
                    onSwap = onSwap,
                    onBuy = onBuy
                )

                // Assets Header matching image 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Assets",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                    Text(
                        text = "View All",
                        color = Color(0xFF00D3F2),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        modifier = Modifier.clickable { onOpenAssets() }
                    )
                }

                // Asset List matching image 1
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val markets = homeState.chainMarkets
                    if (markets.isEmpty() && homeState.isLoading) {
                        repeat(3) { AssetCardPlaceholder() }
                    } else if (markets.isEmpty()) {
                        EmptyAssetState(onRefresh = { homeVm.refreshPortfolioWithPrices() })
                    } else {
                        markets.forEach { market ->
                            DashboardAssetRow(
                                market = market,
                                onClick = { onChainDetailClicked(market.chain, NetworkType.MAINNET) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun DashboardTopBar(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Dashboard",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )
            Text(
                text = "Welcome back, Traveler",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 15.sp
            )
        }

        Surface(
            onClick = onRefresh,
            shape = CircleShape,
            color = Color(0xFF00D3F2).copy(alpha = 0.1f),
            modifier = Modifier.size(44.dp),
            border = BorderStroke(1.dp, Color(0xFF00D3F2).copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Color(0xFF00D3F2),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun PortfolioBalanceCard(
    state: HomeUiState,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onSwap: () -> Unit,
    onBuy: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(440.dp),
        color = Color(0xFF0D1421).copy(alpha = 0.7f),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Large Sparkline filling the middle area
            if (state.sparkline.isNotEmpty()) {
                PulsarSparkline(
                    data = state.sparkline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = (-100).dp)
                        .alpha(0.9f),
                    lineColor = Color(0xFF05DF72), // Vibrant green from image
                    animate = true
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Balance",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$30,028.00", // matching image 1 exactly
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF05DF72).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.NorthEast, null, tint = Color(0xFF05DF72), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+5.2%", 
                                    color = Color(0xFF05DF72), 
                                    fontSize = 14.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "vs last week",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 14.sp
                        )
                    }
                }

                // Action Buttons at bottom - larger circles with blue icons matching image
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DashboardActionItem("Send", painterResource(id = com.elementa.wallet.R.drawable.send_page_arrow), onSend)
                    DashboardActionItem("Receive", painterResource(id = com.elementa.wallet.R.drawable.receive_page_arrooe), onReceive)
                    DashboardActionItem("Swap", painterResource(id = com.elementa.wallet.R.drawable.hub_nav), onSwap)
                    DashboardActionItem("Buy", painterResource(id = com.elementa.wallet.R.drawable.buy), onBuy)
                }
            }
        }
    }
}

@Composable
private fun DashboardActionItem(label: String, icon: androidx.compose.ui.graphics.painter.Painter, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = Color(0xFF1D293D).copy(alpha = 0.8f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = icon,
                    contentDescription = label,
                    tint = Color(0xFF00D3F2), // Accent blue from image
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DashboardAssetRow(market: ChainMarketUi, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() },
        color = Color(0xFF0D1421).copy(alpha = 0.5f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val logoRes = when (market.chain) {
                Chain.ETHEREUM -> com.elementa.wallet.R.drawable.image_ethereum
                Chain.BITCOIN -> com.elementa.wallet.R.drawable.image_bitcoin
                Chain.SOLANA -> com.elementa.wallet.R.drawable.image_solana
                else -> null
            }
            
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.05f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (logoRes != null) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = logoRes),
                            contentDescription = market.name,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        AsyncImage(
                            model = market.logoUrl,
                            contentDescription = market.name,
                            modifier = Modifier.size(36.dp),
                            fallback = painterResource(id = com.elementa.wallet.R.drawable.logo)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(18.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = market.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
                Text(
                    text = "${market.balanceAmount} ${market.symbol}",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("$%,.0f", market.holdingsUsd),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
                val isPositive = market.priceChangePct24h >= 0
                Text(
                    text = String.format("%+.1f%%", market.priceChangePct24h),
                    color = if (isPositive) Color(0xFF05DF72) else Color(0xFFFF4B4B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AssetCardPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxWidth().height(110.dp),
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(24.dp)
    ) { }
}

@Composable
private fun EmptyAssetState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Sync, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("No assets found", color = Color.White.copy(alpha = 0.4f))
        TextButton(onClick = onRefresh) {
            Text("Retry Refresh", color = Color(0xFF00D1FF))
        }
    }
}
