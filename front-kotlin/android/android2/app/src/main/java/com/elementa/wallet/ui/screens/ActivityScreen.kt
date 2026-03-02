package com.elementa.wallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elementa.wallet.ui.components.RedesignedBottomNav
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.Transaction
import com.elementa.wallet.domain.model.TransactionStatus
import com.elementa.wallet.domain.model.TransactionType
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.viewmodel.LiveDataViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class ActivityType { ALL, SENDS, RECEIVES, SWAPS }

@Composable
fun ActivityScreen(
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onAssets: () -> Unit,
    onTransfers: () -> Unit,
    onSwap: () -> Unit,
    onActivity: () -> Unit,
    viewModel: LiveDataViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val holdings by viewModel.walletHoldings.collectAsState()
    
    var selectedFilter by remember { mutableStateOf(ActivityType.ALL) }
    
    // Filter transactions based on selected type
    val filteredTransactions = remember(transactions, selectedFilter) {
        transactions.filter { tx ->
            when (selectedFilter) {
                ActivityType.ALL -> true
                ActivityType.SENDS -> !tx.isIncoming
                ActivityType.RECEIVES -> tx.isIncoming
                ActivityType.SWAPS -> tx.type == TransactionType.SWAP
            }
        }.sortedByDescending { it.timestamp }
    }

    // Group transactions by date
    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { tx ->
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                .withZone(ZoneId.systemDefault())
            formatter.format(Instant.ofEpochSecond(tx.timestamp))
        }
    }

    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(modifier = Modifier.background(PulsarColors.BackgroundDark.copy(alpha = 0.8f)).statusBarsPadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
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
                                "ACTIVITY",
                                style = PulsarTypography.CyberLabel,
                                color = PulsarColors.PrimaryDark,
                                letterSpacing = 4.sp,
                                fontSize = 18.sp
                            )
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = PulsarColors.PrimaryDark,
                                    strokeWidth = 1.5.dp
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.refresh(emptyMap()) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PulsarColors.SurfaceDark.copy(alpha = 0.4f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        items(ActivityType.entries) { type ->
                            val isSelected = type == selectedFilter
                            Surface(
                                onClick = { selectedFilter = type },
                                shape = CircleShape,
                                color = if (isSelected) PulsarColors.PrimaryDark else PulsarColors.SurfaceDark,
                                contentColor = if (isSelected) Color.Black else Color.White.copy(alpha = 0.6f),
                                border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Text(
                                    type.name.lowercase().replaceFirstChar { it.uppercase() },
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                    style = PulsarTypography.Typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                RedesignedBottomNav(
                    onDashboard = onDashboard,
                    onAssets = onAssets,
                    onTransfers = onTransfers,
                    onSwap = onSwap,
                    onActivity = onActivity,
                    currentRoute = "activity"
                )
            }
        ) { padding ->
            if (transactions.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.SyncAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = PulsarColors.PrimaryDark.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No transactions yet",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Connect your wallet to see transaction history",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = padding,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)
                ) {
                    groupedTransactions.forEach { (date, txs) ->
                        item {
                            Text(
                                date.uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                                style = PulsarTypography.CyberLabel,
                                color = PulsarColors.TextSecondaryLight,
                                fontSize = 11.sp,
                                letterSpacing = 2.sp
                            )
                        }
                        items(txs) { tx ->
                            LiveTransactionCard(tx)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ActivityTopIconButton(icon: ImageVector) {
    Surface(
        onClick = { },
        shape = CircleShape,
        color = PulsarColors.SurfaceDark,
        modifier = Modifier.size(42.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ActivityItemCard(tx: Transaction) {
    Surface(
        onClick = { },
        shape = RoundedCornerShape(20.dp),
        color = PulsarColors.SurfaceDark,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusColor = when (tx.status) {
                TransactionStatus.COMPLETED -> if (tx.isIncoming) PulsarColors.PrimaryDark else Color.White
                TransactionStatus.PENDING -> Color(0xFFFACC15) // Amber
                TransactionStatus.FAILED -> PulsarColors.DangerRed
            }
            
            val iconBg = when (tx.status) {
                TransactionStatus.COMPLETED -> if (tx.isIncoming) PulsarColors.PrimaryDark.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)
                TransactionStatus.PENDING -> Color(0xFFFACC15).copy(alpha = 0.1f)
                TransactionStatus.FAILED -> PulsarColors.DangerRed.copy(alpha = 0.1f)
            }
            
            val typeIcon = when (tx.type) {
                TransactionType.SEND -> Icons.Default.ArrowOutward
                TransactionType.RECEIVE -> Icons.Default.ArrowDownward
                TransactionType.SWAP -> Icons.Default.SyncAlt
                else -> Icons.Default.AccountBalanceWallet
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconBg,
                modifier = Modifier.size(48.dp),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(typeIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.type.name, style = PulsarTypography.Typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        tx.status.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = PulsarTypography.Typography.labelSmall,
                        color = PulsarColors.TextSecondaryLight,
                        fontSize = 11.sp
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (tx.isIncoming) "+" else "-"}${tx.amount} ${tx.symbol}",
                    style = PulsarTypography.Typography.bodyLarge,
                    color = if (tx.status == TransactionStatus.FAILED) Color.White.copy(alpha = 0.4f) else if (tx.isIncoming) PulsarColors.PrimaryDark else Color.White,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (tx.status == TransactionStatus.FAILED) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
                val usdValue = tx.amountUSD?.let { "$${String.format("%.2f", it)}" } ?: "—"
                Text(
                    usdValue,
                    style = PulsarTypography.Typography.labelSmall,
                    color = PulsarColors.TextSecondaryLight,
                    fontSize = 11.sp
                )
            }
        }
    }
}
/**
 * Displays a live transaction from blockchain data
 */
@Composable
internal fun LiveTransactionCard(tx: Transaction) {
    val statusColor = when (tx.status) {
        TransactionStatus.COMPLETED -> if (tx.isIncoming) PulsarColors.PrimaryDark else Color.White
        TransactionStatus.PENDING -> Color(0xFFFACC15)
        TransactionStatus.FAILED -> Color(0xFFEF4444)
    }
    
    val iconBg = when (tx.status) {
        TransactionStatus.COMPLETED -> if (tx.isIncoming) PulsarColors.PrimaryDark.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)
        TransactionStatus.PENDING -> Color(0xFFFACC15).copy(alpha = 0.1f)
        TransactionStatus.FAILED -> Color(0xFFEF4444).copy(alpha = 0.1f)
    }
    
    val icon = when {
        tx.isIncoming -> Icons.Default.SouthWest
        tx.type == TransactionType.SWAP -> Icons.Default.SyncAlt
        else -> Icons.Default.NorthEast
    }

    Surface(
        onClick = { },
        shape = RoundedCornerShape(20.dp),
        color = PulsarColors.SurfaceDark,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconBg,
                modifier = Modifier.size(48.dp),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Transaction details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${tx.type.name.lowercase().replaceFirstChar { it.uppercase() }} ${tx.symbol}",
                    style = PulsarTypography.Typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        tx.status.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = PulsarTypography.Typography.labelSmall,
                        color = PulsarColors.TextSecondaryLight,
                        fontSize = 11.sp
                    )
                    // Chain indicator
                    Text(
                        "• ${tx.chain.name}",
                        style = PulsarTypography.Typography.labelSmall,
                        color = PulsarColors.TextSecondaryLight,
                        fontSize = 10.sp,
                        modifier = Modifier.alpha(0.7f)
                    )
                }
            }
            
            // Amount and USD value
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (tx.isIncoming) "+" else "-"} ${
                        tx.amount.toDoubleOrNull()?.let { String.format("%.4f", it) } ?: tx.amount
                    } ${tx.symbol}",
                    style = PulsarTypography.Typography.bodyLarge,
                    color = if (tx.status == TransactionStatus.FAILED) {
                        Color.White.copy(alpha = 0.4f)
                    } else {
                        if (tx.isIncoming) PulsarColors.PrimaryDark else Color.White
                    },
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (tx.status == TransactionStatus.FAILED) {
                        androidx.compose.ui.text.style.TextDecoration.LineThrough
                    } else {
                        null
                    }
                )
                tx.amountUSD?.let {
                    Text(
                        String.format("$%.2f", it),
                        style = PulsarTypography.Typography.labelSmall,
                        color = PulsarColors.TextSecondaryLight,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}