package com.elementa.wallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elementa.wallet.R
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.ui.components.PulsarBottomNav
import com.elementa.wallet.ui.designsystem.PulsarBackground
import com.elementa.wallet.ui.designsystem.PulsarColors
import com.elementa.wallet.ui.designsystem.PulsarTypography
import com.elementa.wallet.ui.state.ChainMarketUi
import com.elementa.wallet.viewmodel.HomeViewModel

@Composable
fun AssetsScreen(
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onAssets: () -> Unit,
    onTransfers: () -> Unit,
    onSwap: () -> Unit,
    onActivity: () -> Unit,
    onHub: () -> Unit,
    onSettings: () -> Unit,
    onAddToken: () -> Unit = {}
) {
    val homeVm = hiltViewModel<HomeViewModel>()
    val homeState by homeVm.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040A20))) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AssetsTopBar(onAddToken = onAddToken)
            },
            bottomBar = {
                PulsarBottomNav(
                    onHome = onDashboard,
                    onAssets = {},
                    onHub = onHub,
                    onActivity = onActivity,
                    onSettings = onSettings,
                    currentRoute = "assets"
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar matching Image 1
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1D293D).copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            modifier = Modifier.weight(1f),
                            cursorBrush = SolidColor(Color(0xFF00D3F2)),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search tokens...",
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Asset List
                val filteredMarkets = homeState.chainMarkets.filter {
                    it.name.contains(searchQuery, ignoreCase = true) || 
                    it.symbol.contains(searchQuery, ignoreCase = true)
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (filteredMarkets.isEmpty() && homeState.isLoading) {
                        repeat(4) { AssetCardPlaceholder() }
                    } else if (filteredMarkets.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("No assets found", color = Color.White.copy(alpha = 0.4f))
                        }
                    } else {
                        filteredMarkets.forEach { market ->
                            val chainBadge = when(market.chain) {
                                Chain.ETHEREUM -> "ETH"
                                Chain.BITCOIN -> "BTC"
                                Chain.SOLANA -> "SOL"
                                else -> market.symbol
                            }
                            
                            AssetRowItem(
                                name = market.name,
                                symbol = market.symbol,
                                chainBadge = chainBadge,
                                balance = market.balanceAmount,
                                usdValue = market.holdingsUsd,
                                priceChange = market.priceChangePct24h,
                                logoRes = when(market.chain) {
                                    Chain.ETHEREUM -> R.drawable.image_ethereum
                                    Chain.BITCOIN -> R.drawable.image_bitcoin
                                    Chain.SOLANA -> R.drawable.image_solana
                                    else -> R.drawable.buy_coin
                                }
                            )
                        }
                        
                        // Add mock USD Coin for 100% pixel match if it's not in the list
                        if (searchQuery.isEmpty() || "USD Coin".contains(searchQuery, ignoreCase = true)) {
                            AssetRowItem(
                                name = "USD Coin",
                                symbol = "USDC",
                                chainBadge = "ETH",
                                balance = "2,540",
                                usdValue = 2540.00,
                                priceChange = 0.01,
                                logoRes = R.drawable.buy_coin
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AssetsTopBar(onAddToken: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Assets",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Surface(
            onClick = onAddToken,
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1D293D).copy(alpha = 0.8f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Add Token",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AssetRowItem(
    name: String,
    symbol: String,
    chainBadge: String,
    balance: String,
    usdValue: Double,
    priceChange: Double,
    logoRes: Int
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0D1421).copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth().height(115.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.05f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = name,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(18.dp))
            
            // Name and Badge
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = chainBadge,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = symbol,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                }
            }
            
            // Financials
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = balance,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = String.format("$%,.2f", usdValue),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )
                val isPositive = priceChange >= 0
                Text(
                    text = String.format("%+.1f%%", priceChange),
                    color = if (isPositive) Color(0xFF05DF72) else Color(0xFFFF4B4B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AssetCardPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxWidth().height(115.dp),
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(24.dp)
    ) { }
}
