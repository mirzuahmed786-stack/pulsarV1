package com.elementa.wallet.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.TokenAsset
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.viewmodel.ChainDetailViewModel

@Composable
fun ChainDetailScreen(
    viewModel: ChainDetailViewModel,
    onAddToken: () -> Unit,
    onBack: () -> Unit,
    onSend: () -> Unit = {},
    onReceive: () -> Unit = {},
    onQrScan: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val accentColor = PulsarColors.PrimaryDark // Unified professional accent color
    val rotation by animateFloatAsState(
        targetValue = if (state.isLoading) 360f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "chain_refresh_rotation"
    )

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
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Color.White)
                        }
                    }
                    Text(
                        "${state.chain.name.uppercase()} NETWORK",
                        style = PulsarTypography.CyberLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp
                    )
                    Surface(
                        onClick = { viewModel.refresh() },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = rotation }
                            )
                        }
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
                item {
                    AnimatedVisibility(visible = state.isManualRefreshing) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = accentColor,
                            trackColor = Color.White.copy(alpha = 0.08f)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    val nativeSymbol = nativeSymbolFor(state.chain)
                    val nativeBalance = state.tokens
                        .firstOrNull { it.symbol.equals(nativeSymbol, ignoreCase = true) }
                        ?.balance
                        ?: "0"
                    val holdingsUsd = state.tokens.sumOf { it.balanceInUsd }
                    ChainHeroCard(
                        chainName = state.chain.name.replaceFirstChar { it.uppercase() },
                        networkType = state.network.name,
                        balance = nativeBalance,
                        symbol = nativeSymbol,
                        usdValue = "$${String.format("%.2f", holdingsUsd)}",
                        displayAddress = formatWalletAddress(state.walletAddress, state.chain),
                        fullAddress = state.walletAddress,
                        logoUrl = state.logoUrl,
                        accentColor = accentColor
                    )
                }

                // Action Buttons Section (Send, Receive, QR Scanner)
                item {
                    ChainActionButtons(
                        accentColor = accentColor,
                        onSend = onSend,
                        onReceive = onReceive,
                        onQrScan = onQrScan
                    )
                }

                if (state.sparkline.isNotEmpty()) {
                    item {
                        MarketStatsSection(
                            price = state.currentPriceUsd,
                            priceChange24h = state.priceChangePct24h,
                            marketCap = state.marketCapUsd,
                            volume24h = state.volume24hUsd,
                            networkSpeed = state.networkSpeedLabel,
                            networkLatencyMs = state.networkLatencyMs,
                            sparkline = state.sparkline,
                            accentColor = accentColor
                        )
                    }
                }

                // Only show Chain Assets section for EVM chains (not Bitcoin, Solana)
                if (state.chain.isEvm) {
                    item {
                        HoldingsInChainSection(
                            tokens = state.tokens,
                            isEvmChain = state.chain.isEvm,
                            onAddToken = onAddToken,
                            onRemoveToken = { viewModel.removeToken(it) }
                        )
                    }
                }
                
                // Network Info Section for all chains
                item {
                    NetworkInfoSection(
                        chain = state.chain,
                        networkSpeed = state.networkSpeedLabel,
                        networkLatencyMs = state.networkLatencyMs,
                        accentColor = accentColor
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun nativeSymbolFor(chain: Chain): String = when (chain) {
    Chain.BITCOIN -> "BTC"
    Chain.ETHEREUM -> "ETH"
    Chain.SOLANA -> "SOL"
    Chain.BSC -> "BNB"
    Chain.AVALANCHE -> "AVAX"
    Chain.POLYGON -> "POL"
    Chain.LOCALHOST -> "ETH"
}

/**
 * Format wallet address for display, showing truncated version with ellipsis.
 * Shows appropriate placeholder if address is empty for the given chain.
 */
private fun formatWalletAddress(address: String, chain: Chain): String {
    if (address.isBlank()) {
        return when (chain) {
            Chain.BITCOIN -> "Bitcoin address not configured"
            Chain.SOLANA -> "Solana address not configured"
            else -> "Wallet address not configured"
        }
    }
    
    // Truncate address for display: show first 10 and last 8 characters
    return if (address.length > 20) {
        "${address.take(10)}...${address.takeLast(8)}"
    } else {
        address
    }
}

@Composable
private fun ChainHeroCard(
    chainName: String,
    networkType: String,
    balance: String,
    symbol: String,
    usdValue: String,
    displayAddress: String,
    fullAddress: String,
    logoUrl: String?,
    accentColor: Color
) {
    val clipboardManager = LocalClipboardManager.current
    var showCopied by remember { mutableStateOf(false) }
    
    // Gradient background for premium feel
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            accentColor.copy(alpha = 0.15f),
            accentColor.copy(alpha = 0.05f),
            Color.Transparent
        )
    )
    
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = accentColor.copy(alpha = 0.3f),
                spotColor = accentColor.copy(alpha = 0.2f)
            )
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1B23),
                            Color(0xFF12131A)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            // Subtle gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(gradientBrush)
            )
            
            Column(modifier = Modifier.padding(24.dp)) {
                // Header with Logo and Chain Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Chain Logo with glow effect
                    Box(contentAlignment = Alignment.Center) {
                        // Glow background
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .blur(16.dp)
                                .background(accentColor.copy(alpha = 0.4f), CircleShape)
                        )
                        // Logo container
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E1F28),
                            border = BorderStroke(2.dp, accentColor.copy(alpha = 0.5f)),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (!logoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = logoUrl,
                                        contentDescription = chainName,
                                        modifier = Modifier.size(36.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Text(
                                        symbol.take(1),
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                            }
                        }
                        // Status indicator
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = (-2).dp, y = (-2).dp)
                                .background(PulsarColors.SuccessGreen, CircleShape)
                                .border(2.dp, Color(0xFF1E1F28), CircleShape)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            chainName,
                            style = PulsarTypography.Typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(PulsarColors.SuccessGreen, CircleShape)
                            )
                            Text(
                                networkType.uppercase(),
                                style = PulsarTypography.CyberLabel,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    
                    // Quick stats badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            symbol,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = PulsarTypography.Typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Balance Section with enhanced typography
                Column {
                    Text(
                        "AVAILABLE BALANCE",
                        style = PulsarTypography.CyberLabel,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = balance,
                                style = PulsarTypography.Typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-1).sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = symbol,
                                style = PulsarTypography.Typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "USD VALUE",
                                style = PulsarTypography.CyberLabel,
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 9.sp
                            )
                            Text(
                                text = usdValue,
                                style = PulsarTypography.Typography.titleLarge,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Address Section with improved design
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.03f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Address icon
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.1f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            displayAddress,
                            style = PulsarTypography.Typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Copy button - copies FULL address
                        Surface(
                            onClick = {
                                if (fullAddress.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(fullAddress))
                                    showCopied = true
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (showCopied) PulsarColors.SuccessGreen.copy(alpha = 0.2f) 
                                   else Color.White.copy(alpha = 0.05f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    if (showCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = if (showCopied) PulsarColors.SuccessGreen else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    if (showCopied) "Copied" else "Copy",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (showCopied) PulsarColors.SuccessGreen else Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
                
                // Reset copied state
                LaunchedEffect(showCopied) {
                    if (showCopied) {
                        kotlinx.coroutines.delay(2000)
                        showCopied = false
                    }
                }
            }
        }
    }
}

@Composable
private fun ChainActionButtons(
    accentColor: Color,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onQrScan: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Send Button
        EnhancedActionButton(
            icon = Icons.Default.ArrowUpward,
            label = "Send",
            accentColor = accentColor,
            onClick = onSend,
            modifier = Modifier.weight(1f)
        )
        
        // Receive Button
        EnhancedActionButton(
            icon = Icons.Default.ArrowDownward,
            label = "Receive",
            accentColor = PulsarColors.SuccessGreen,
            onClick = onReceive,
            modifier = Modifier.weight(1f)
        )
        
        // QR Scanner Button
        EnhancedActionButton(
            icon = Icons.Default.QrCodeScanner,
            label = "Scan",
            accentColor = Color(0xFF00FFFF),
            onClick = onQrScan,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EnhancedActionButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = modifier
            .height(80.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = accentColor.copy(alpha = 0.2f),
                spotColor = accentColor.copy(alpha = 0.1f)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.2f),
                            accentColor.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.4f),
                            accentColor.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon with glow effect
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .blur(8.dp)
                            .background(accentColor.copy(alpha = 0.3f), CircleShape)
                    )
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    label,
                    style = PulsarTypography.Typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = 0.1f),
        modifier = Modifier.size(width = 100.dp, height = 72.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                label,
                style = PulsarTypography.Typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun HoldingsInChainSection(
    tokens: List<TokenAsset>,
    isEvmChain: Boolean,
    onAddToken: () -> Unit,
    onRemoveToken: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1B23),
                            Color(0xFF14151C)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Token,
                            contentDescription = null,
                            tint = PulsarColors.PrimaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "TOKENS",
                            style = PulsarTypography.CyberLabel,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        
                        // Token count badge
                        if (tokens.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PulsarColors.PrimaryDark.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "${tokens.size}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PulsarColors.PrimaryDark
                                )
                            }
                        }
                    }
                    
                    // Import button - only for EVM chains
                    if (isEvmChain) {
                        Surface(
                            onClick = onAddToken,
                            shape = RoundedCornerShape(10.dp),
                            color = PulsarColors.PrimaryDark.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, PulsarColors.PrimaryDark.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = PulsarColors.PrimaryDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "Import",
                                    style = PulsarTypography.Typography.labelSmall,
                                    color = PulsarColors.PrimaryDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (tokens.isEmpty()) {
                    // Empty state with better design
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.02f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No tokens found",
                                style = PulsarTypography.Typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Import custom tokens to track them here",
                                style = PulsarTypography.Typography.bodySmall,
                                color = Color.White.copy(alpha = 0.25f)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tokens.forEach { token ->
                            ChainTokenRow(
                                token = token,
                                onRemove = { onRemoveToken(token.address) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChainTokenRow(token: TokenAsset, onRemove: () -> Unit) {
    var showRemoveConfirm by remember { mutableStateOf(false) }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Token icon
            Surface(
                shape = CircleShape,
                color = PulsarColors.PrimaryDark.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        token.symbol.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = PulsarColors.PrimaryDark,
                        fontSize = 18.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    token.symbol,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = PulsarTypography.Typography.bodyLarge
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (token.isCustom) Color(0xFFFFAA00).copy(alpha = 0.15f)
                               else PulsarColors.SuccessGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            if (token.isCustom) "Custom" else "Native",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (token.isCustom) Color(0xFFFFAA00)
                                   else PulsarColors.SuccessGreen
                        )
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    token.balance,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = PulsarTypography.Typography.bodyLarge
                )
                
                if (token.balanceInUsd > 0) {
                    Text(
                        "$${String.format("%.2f", token.balanceInUsd)}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
            
            // Remove button (only for custom tokens)
            if (token.isCustom) {
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    onClick = { 
                        if (showRemoveConfirm) {
                            onRemove()
                        } else {
                            showRemoveConfirm = true
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (showRemoveConfirm) PulsarColors.DangerRed.copy(alpha = 0.2f)
                           else Color.White.copy(alpha = 0.05f)
                ) {
                    Icon(
                        if (showRemoveConfirm) Icons.Default.DeleteForever else Icons.Outlined.Delete,
                        contentDescription = "Remove",
                        tint = if (showRemoveConfirm) PulsarColors.DangerRed else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketStatsSection(
    price: Double,
    priceChange24h: Double,
    marketCap: Double,
    volume24h: Double,
    networkSpeed: String,
    networkLatencyMs: Long,
    sparkline: List<Double>,
    accentColor: Color
) {
    val isPositive = priceChange24h >= 0
    val changeColor = if (isPositive) PulsarColors.SuccessGreen else PulsarColors.DangerRed
    
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1B23),
                            Color(0xFF14151C)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.ShowChart,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "MARKET DATA",
                        style = PulsarTypography.CyberLabel,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Live indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(PulsarColors.SuccessGreen, CircleShape)
                        )
                        Text(
                            "LIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = PulsarColors.SuccessGreen,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Price Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = accentColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "CURRENT PRICE",
                                style = PulsarTypography.CyberLabel,
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 9.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$${String.format("%,.2f", price)}",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 28.sp,
                                letterSpacing = (-0.5).sp
                            )
                        }
                        
                        // Change indicator
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = changeColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, changeColor.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = changeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    String.format("%+.2f%%", priceChange24h),
                                    fontWeight = FontWeight.Bold,
                                    color = changeColor,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Market Cap",
                        value = "$${formatLargeNumber(marketCap)}",
                        icon = Icons.Outlined.PieChart,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "24h Volume",
                        value = "$${formatLargeNumber(volume24h)}",
                        icon = Icons.Outlined.BarChart,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Chart Section
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Timeline,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "7-DAY TREND",
                            style = PulsarTypography.CyberLabel,
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.02f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            com.elementa.wallet.ui.components.PulsarSparkline(
                                data = sparkline,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                lineColor = accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    label.uppercase(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 0.5.sp
                )
                Text(
                    value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun NetworkInfoSection(
    chain: Chain,
    networkSpeed: String,
    networkLatencyMs: Long,
    accentColor: Color
) {
    val speedColor = when (networkSpeed.lowercase()) {
        "fast" -> PulsarColors.SuccessGreen
        "normal" -> Color(0xFFFFAA00)
        else -> PulsarColors.DangerRed
    }
    
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            speedColor.copy(alpha = 0.08f),
                            Color(0xFF1A1B23)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            speedColor.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Network status indicator
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .blur(6.dp)
                                .background(speedColor.copy(alpha = 0.4f), CircleShape)
                        )
                        Surface(
                            shape = CircleShape,
                            color = speedColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, speedColor.copy(alpha = 0.5f)),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.NetworkCheck,
                                    contentDescription = null,
                                    tint = speedColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    
                    Column {
                        Text(
                            "Network Status",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Text(
                            networkSpeed.uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = speedColor
                        )
                    }
                }
                
                // Latency display
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.05f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Speed,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "${networkLatencyMs}ms",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}


private fun formatLargeNumber(value: Double): String {
    if (value >= 1_000_000_000_000) return String.format("%.2fT", value / 1_000_000_000_000)
    if (value >= 1_000_000_000) return String.format("%.2fB", value / 1_000_000_000)
    if (value >= 1_000_000) return String.format("%.2fM", value / 1_000_000)
    if (value >= 1_000) return String.format("%.2fK", value / 1_000)
    return String.format("%.2f", value)
}
