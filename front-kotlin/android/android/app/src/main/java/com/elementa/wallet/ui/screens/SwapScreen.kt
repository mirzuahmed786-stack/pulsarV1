package com.elementa.wallet.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.components.RedesignedBottomNav
import com.elementa.wallet.ui.designsystem.*

@Composable
fun SwapScreen(
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onAssets: () -> Unit,
    onTransfers: () -> Unit,
    onSwap: () -> Unit,
    onActivity: () -> Unit
) {
    var payAmount by remember { mutableStateOf("0.5") }
    var receiveAmount by remember { mutableStateOf("1245.56") }

    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { RedesignedSwapTopBar(onBack) },
            bottomBar = {
                RedesignedBottomNav(
                    onDashboard = onDashboard,
                    onAssets = onAssets,
                    onTransfers = onTransfers,
                    onSwap = onSwap,
                    onActivity = onActivity,
                    currentRoute = "swap"
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
                Spacer(modifier = Modifier.height(8.dp))

                // Swap Interface
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RedesignedSwapAssetCard(
                            label = "You Pay",
                            amount = payAmount,
                            onAmountChange = { payAmount = it },
                            asset = "ETH",
                            balance = "1.45 ETH",
                            usdValue = "≈ $1,245.56",
                            isReceive = false
                        )
                        
                        RedesignedSwapAssetCard(
                            label = "You Receive",
                            amount = receiveAmount,
                            onAmountChange = {},
                            asset = "USDC",
                            balance = "420.00 USDC",
                            usdValue = "1 ETH = 2,491.12 USDC",
                            isReceive = true,
                            readOnly = true
                        )
                    }
                    
                    // Central Swap Icon
                    Surface(
                        shape = CircleShape,
                        color = PulsarColors.PrimaryDark,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(52.dp)
                            .border(6.dp, PulsarColors.BackgroundDark, CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SwapVert, contentDescription = "Swap", tint = Color.Black, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                // Details Area
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SwapDetailRow(
                        label = "Slippage Tolerance",
                        value = "0.5%",
                        icon = Icons.Default.Info
                    )
                    SwapDetailRow(
                        label = "Network Fee",
                        value = "~$4.20 (22 Gwei)",
                        icon = Icons.Default.LocalGasStation
                    )
                    SwapDetailRow(
                        label = "Pulsar Route",
                        value = "Best Price",
                        icon = Icons.Default.AutoAwesome,
                        isHighlight = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                PulsarComponents.PulsarButton(
                    text = "Swap Now",
                    onClick = { /* Execute Swap */ },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    glow = true
                )

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun RedesignedSwapTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
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
            "SWAP",
            style = PulsarTypography.CyberLabel,
            color = PulsarColors.PrimaryDark,
            letterSpacing = 4.sp,
            fontSize = 18.sp
        )
        
        Surface(
            onClick = { },
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.05f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun RedesignedSwapAssetCard(
    label: String,
    amount: String,
    onAmountChange: (String) -> Unit,
    asset: String,
    balance: String,
    usdValue: String,
    isReceive: Boolean,
    readOnly: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = PulsarTypography.Typography.labelSmall,
                    color = PulsarColors.TextSecondaryLight,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Balance: $balance",
                        style = PulsarTypography.Typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                    if (!isReceive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PulsarColors.PrimaryDark.copy(alpha = 0.1f),
                            modifier = Modifier.clickable { onAmountChange("1.45") }
                        ) {
                            Text(
                                "MAX",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = PulsarTypography.Typography.labelSmall,
                                color = PulsarColors.PrimaryDark,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    readOnly = readOnly,
                    textStyle = PulsarTypography.Typography.displayMedium.copy(
                        color = if (isReceive) PulsarColors.PrimaryDark else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                Surface(
                    onClick = { },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(asset.take(1), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(asset, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                usdValue,
                style = PulsarTypography.Typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SwapDetailRow(
    label: String,
    value: String,
    icon: ImageVector,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = PulsarTypography.Typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
        }
        Text(
            value,
            style = PulsarTypography.Typography.bodyMedium,
            color = if (isHighlight) PulsarColors.PrimaryDark else Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold
        )
    }
}
