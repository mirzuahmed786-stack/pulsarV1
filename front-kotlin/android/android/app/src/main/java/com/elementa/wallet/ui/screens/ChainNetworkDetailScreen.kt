package com.elementa.wallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elementa.wallet.config.BlockchainApiConfig
import com.elementa.wallet.config.getEthereumApiKey
import com.elementa.wallet.config.getPolygonApiKey
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.ChainData
import com.elementa.wallet.ui.components.PulsarSparkline
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.util.WalletLogger
import com.elementa.wallet.viewmodel.LiveDataViewModel

/**
 * Display live chain data for Avalanche and Polygon blockchains
 * Shows price charts, transactions, and network statistics
 */
@Composable
fun ChainNetworkDetailScreen(
    chain: Chain,
    viewModel: LiveDataViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onTransactionClick: (String) -> Unit = {},
    onSend: () -> Unit = {},
    onReceive: () -> Unit = {},
    onQrScan: () -> Unit = {},
    onAddToken: () -> Unit = {}
) {
    val chainDataMap by viewModel.chainData.collectAsState()
    val chainData = chainDataMap[chain]
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Get API config
    val apiConfig: BlockchainApiConfig = remember { BlockchainApiConfig() }
    
    // Remember state for loading error
    var loadingError by remember { mutableStateOf<String?>(null) }
    var attempted by remember { mutableStateOf(false) }
    
    val accentColor = when (chain) {
        Chain.AVALANCHE -> Color(0xFFE84142)
        Chain.POLYGON -> Color(0xFF8247E5)
        else -> PulsarColors.PrimaryDark
    }
    
    val chainName = when (chain) {
        Chain.AVALANCHE -> "Avalanche"
        Chain.POLYGON -> "Polygon"
        else -> "Network"
    }
    
    // ✅ CRITICAL FIX: Initialize data loading when screen appears with wallet addresses from session
    LaunchedEffect(chain) {
        if (!attempted) {
            attempted = true
            try {
                WalletLogger.logInfo("ChainNetworkDetailScreen", "Initializing $chainName data loading")
                
                // Determine API key based on chain
                val apiKey = when (chain) {
                    Chain.AVALANCHE -> apiConfig.getEthereumApiKey()  // Avalanche uses Snowtrace (Etherscan-compatible)
                    Chain.POLYGON -> apiConfig.getPolygonApiKey()      // Polygon uses PolygonScan
                    else -> ""
                }
                
                // Get wallet addresses from session
                // Both Avalanche and Polygon use Ethereum address format (same EVM address)
                val walletAddresses = viewModel.getWalletAddressesFromSession()
                
                WalletLogger.logInfo("ChainNetworkDetailScreen", "Loading live data for $chainName with addresses: ${walletAddresses.keys.joinToString()}")
                
                viewModel.loadLiveData(
                    walletAddresses = walletAddresses,
                    apiKey = apiKey
                )
                
                loadingError = null
            } catch (e: Exception) {
                WalletLogger.logError("ChainNetworkDetailScreen", "Data loading failed", e)
                loadingError = "Failed to initialize data loading: ${e.message}"
            }
        }
    }
    
    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            chainName.uppercase(),
                            style = PulsarTypography.CyberLabel,
                            color = accentColor,
                            letterSpacing = 2.sp,
                            fontSize = 14.sp
                        )
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = accentColor,
                                strokeWidth = 1.5.dp
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            // Refresh data with wallet addresses from session
                            val apiKey = when (chain) {
                                Chain.AVALANCHE -> apiConfig.getEthereumApiKey()
                                Chain.POLYGON -> apiConfig.getPolygonApiKey()
                                else -> ""
                            }
                            val walletAddresses = viewModel.getWalletAddressesFromSession()
                            viewModel.refresh(walletAddresses, apiKey)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PulsarColors.SurfaceDark.copy(alpha = 0.4f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        ) { padding ->
            if (chainData == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (loadingError != null) {
                        // Show error state with helpful message
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Failed to load data",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Text(
                            loadingError ?: "Unknown error",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                            textAlign = TextAlign.Center
                        )
                        // Retry button
                        Button(
                            onClick = {
                                attempted = false
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Retry")
                        }
                    } else {
                        // Show loading state
                        CircularProgressIndicator(
                            color = accentColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Loading $chainName data...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Text(
                            "Fetching prices and network data",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    // Price card
                    item {
                        ChainPriceCard(
                            chain = chain,
                            price = chainData.nativePrice,
                            change24h = chainData.nativeChange24h,
                            balanceUSD = chainData.totalBalanceUSD,
                            accentColor = accentColor
                        )
                    }
                    
                    // Action Buttons Section (Send, Receive, QR Scanner, Import Token)
                    item {
                        ChainNetworkActionButtons(
                            chain = chain,
                            accentColor = accentColor,
                            onSend = onSend,
                            onReceive = onReceive,
                            onQrScan = onQrScan,
                            onAddToken = onAddToken
                        )
                    }
                    
                    // Price chart / sparkline - ALWAYS show for live data
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1A1B23)
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    "7 Day Price Chart",
                                    style = PulsarTypography.Typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                if (chainData.sparkline.isNotEmpty()) {
                                    PulsarSparkline(
                                        data = chainData.sparkline,
                                        lineColor = accentColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp),
                                        animate = true
                                    )
                                } else {
                                    // Loading state for chart
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                color = accentColor,
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Text(
                                                "Loading chart data...",
                                                color = Color.White.copy(alpha = 0.4f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Holdings balance
                    item {
                        ChainHoldingsCard(
                            chain = chain,
                            balance = chainData.nativeBalance,
                            symbol = chainData.nativeSymbol,
                            balanceUSD = chainData.totalBalanceUSD,
                            accentColor = accentColor
                        )
                    }
                    
                    // Network stats
                    item {
                        NetworkStatsCard(
                            networkStatus = chainData.networkStatus,
                            gasPrice = chainData.gasPrice ?: "N/A",
                            blockNumber = chainData.blockNumber ?: 0,
                            accentColor = accentColor
                        )
                    }
                    
                    // Recent transactions
                    if (chainData.transactions.isNotEmpty()) {
                        item {
                            Text(
                                "RECENT TRANSACTIONS",
                                style = PulsarTypography.CyberLabel,
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 11.sp,
                                letterSpacing = 2.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        
                        items(chainData.transactions.take(10)) { tx ->
                            LiveTransactionCard(tx)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChainPriceCard(
    chain: Chain,
    price: Double,
    change24h: Double,
    balanceUSD: Double,
    accentColor: Color
) {
    val changeColor = if (change24h >= 0) PulsarColors.PrimaryDark else Color(0xFFEF4444)
    val symbol = when (chain) {
        Chain.AVALANCHE -> "AVAX"
        Chain.POLYGON -> "Polygon"  // Match reference image style
        else -> "TOKEN"
    }
    
    val chainFullName = when (chain) {
        Chain.AVALANCHE -> "Avalanche"
        Chain.POLYGON -> "Polygon"
        else -> "Network"
    }
    
    // Pixel-perfect card matching reference Bitcoin design
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),  // Fixed height matching reference
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1B23)  // Dark background matching reference
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Chain icon circle
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .border(2.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            symbol.take(1),
                            color = accentColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            chainFullName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "MAINNET - FAST (0MS)",  // Matching reference style
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                
                // Balance section
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "BALANCE",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        String.format("%.1f", balanceUSD / price) + " ${if (chain == Chain.AVALANCHE) "AVAX" else "MATIC"}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        String.format("$%.2f", balanceUSD),
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Price section
            Column {
                Text(
                    "LIVE PRICE",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        String.format("$%.2f", price),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            if (change24h >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = changeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            String.format("%+.2f%%", change24h),
                            color = changeColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChainHoldingsCard(
    chain: Chain,
    balance: String,
    symbol: String,
    balanceUSD: Double,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = PulsarColors.SurfaceDark.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Your Balance",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        style = PulsarTypography.Typography.labelSmall
                    )
                    Text(
                        balance.toDoubleOrNull()?.let { String.format("%.4f", it) } ?: "0.0000",
                        color = accentColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        symbol,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        String.format("$%.2f", balanceUSD),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "USD Value",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkStatsCard(
    networkStatus: com.elementa.wallet.domain.model.NetworkStatus,
    gasPrice: String,
    blockNumber: Long,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = PulsarColors.SurfaceDark.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NetworkStatItem(
                label = "Network Status",
                value = networkStatus.name,
                color = when (networkStatus) {
                    com.elementa.wallet.domain.model.NetworkStatus.HEALTHY -> PulsarColors.PrimaryDark
                    com.elementa.wallet.domain.model.NetworkStatus.SLOW -> Color(0xFFFACC15)
                    com.elementa.wallet.domain.model.NetworkStatus.CONGESTED -> Color(0xFFEF4444)
                    com.elementa.wallet.domain.model.NetworkStatus.OFFLINE -> Color(0xFF6B7280)
                }
            )
            NetworkStatItem(
                label = "Gas Price",
                value = gasPrice,
                color = accentColor
            )
            NetworkStatItem(
                label = "Block #",
                value = blockNumber.toString().takeLast(5),
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun NetworkStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            style = PulsarTypography.Typography.labelSmall
        )
        Text(
            value,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ChainNetworkActionButtons(
    chain: Chain,
    accentColor: Color,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onQrScan: () -> Unit,
    onAddToken: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = PulsarColors.SurfaceDark.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ChainNetworkActionButton(
                icon = Icons.Default.ArrowUpward,
                label = "Send",
                accentColor = accentColor,
                onClick = onSend
            )
            ChainNetworkActionButton(
                icon = Icons.Default.ArrowDownward,
                label = "Receive",
                accentColor = PulsarColors.SuccessGreen,
                onClick = onReceive
            )
            ChainNetworkActionButton(
                icon = Icons.Default.QrCodeScanner,
                label = "Scan QR",
                accentColor = Color(0xFF00FFFF),
                onClick = onQrScan
            )
            // Only show Import Token for EVM chains (Avalanche and Polygon are both EVM)
            if (chain.isEvm) {
                ChainNetworkActionButton(
                    icon = Icons.Default.Add,
                    label = "Import",
                    accentColor = PulsarColors.PrimaryDark,
                    onClick = onAddToken
                )
            }
        }
    }
}

@Composable
private fun ChainNetworkActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.1f),
        modifier = Modifier.size(width = 72.dp, height = 64.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                style = PulsarTypography.Typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        }
    }
}
