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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.elementa.wallet.R

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
    viewModel: AssetsViewModel = hiltViewModel(),
    liveDataViewModel: LiveDataViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onAssets: () -> Unit,
    onTransfers: () -> Unit,
    onSwap: () -> Unit,
    onActivity: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val tabs = listOf("Tokens", "NFTs", "Pools")
    var selectedTab by remember { mutableStateOf(0) }

    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                RedesignedBottomNav(
                    onDashboard = onDashboard,
                    onAssets = onAssets,
                    onTransfers = onTransfers,
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // --- Portfolio Header ---
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Portfolio Balance",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "$2,540.80",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF00FFA3).copy(alpha = 0.1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF00FFA3), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "+5.2% Today",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FFA3)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- Search Bar ---
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search tokens or NFTs", color = Color.White.copy(alpha = 0.2f), fontSize = 15.sp)
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Tabs ---
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    tabs.forEachIndexed { index, title ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = index },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                title,
                                fontSize = 16.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (selectedTab == index) {
                                Box(
                                    modifier = Modifier
                                        .height(3.dp)
                                        .width(20.dp)
                                        .background(Color(0xFF00C0FF), RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Assets List ---
                val mockAssets = listOf(
                    TokenAsset("Ethereum", "ETH", "1.4500", "$4,060.00", R.drawable.image_ethereum, Color(0xFF627EEA)),
                    TokenAsset("Bitcoin", "BTC", "0.0520", "$2,100.25", R.drawable.image_bitcoin, Color(0xFFF7931A)),
                    TokenAsset("Solana", "SOL", "12.00", "$1,440.00", R.drawable.image_solana, Color(0xFF14F195))
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    mockAssets.forEach { asset ->
                        AssetItemCard(asset)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

internal data class TokenAsset(
    val name: String,
    val symbol: String,
    val balance: String,
    val usdValue: String,
    val icon: Int,
    val accent: Color
)

@Composable
internal fun AssetItemCard(asset: TokenAsset) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0D1421).copy(alpha = 0.8f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = asset.accent.copy(alpha = 0.1f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = asset.icon),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(asset.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(asset.symbol, fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f))
            }

            // Trend
            Box(modifier = Modifier.size(width = 60.dp, height = 30.dp), contentAlignment = Alignment.Center) {
                // Mock chart line
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path()
                    path.moveTo(0f, size.height * 0.7f)
                    path.cubicTo(size.width * 0.3f, size.height * 0.8f, size.width * 0.6f, size.height * 0.2f, size.width, size.height * 0.4f)
                    drawPath(path, color = Color(0xFF00FFA3), style = Stroke(width = 2f, cap = StrokeCap.Round))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(asset.balance, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(asset.usdValue, fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
fun PulsarAssetsTopBar(
    onBack: () -> Unit,
    onNotifications: () -> Unit,
    onAddToken: () -> Unit
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onAddToken,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PulsarColors.SurfaceDark)
                    .border(1.dp, PulsarColors.BorderSubtleDark, CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Token",
                    tint = PulsarColors.TextSecondaryDark,
                    modifier = Modifier.size(20.dp)
                )
            }
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
}

/**
 * Displays a live wallet holding with current price and 24h change
 * Optimized for mobile view with proper spacing and no overflow
 */
@Composable
fun LiveHoldingCard(
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
