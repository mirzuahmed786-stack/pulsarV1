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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.painterResource
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.R

@Composable
fun SwapScreen(
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onAssets: () -> Unit,
    onTransfers: () -> Unit,
    onSwap: () -> Unit,
    onActivity: () -> Unit
) {
    var payAmount by remember { mutableStateOf("1.45") }
    // Mock balances and rate
    val payBalance = 1.45
    val mockRate = 0.045161 // 1 ETH = 0.045161 BTC
    val networkFeeEstimate = 1.25

    // Automatically calculate receive amount
    val payAmtDouble = payAmount.toDoubleOrNull() ?: 0.0
    val receiveAmount = String.format("%.6f", payAmtDouble * mockRate)
    val payUsdValue = String.format("$%,.2f", payAmtDouble * 2800.0) // Mock price

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
                    currentRoute = "swap"
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // --- Header Section ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF031627),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SwapCalls,
                                    contentDescription = null,
                                    tint = Color(0xFF00C0FF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Swap",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Swap Cards ---
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Pay Card
                        SwapAssetCard(
                            label = "You Pay",
                            amount = payAmount,
                            onAmountChange = { 
                                if (it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    val valDouble = it.toDoubleOrNull() ?: 0.0
                                    payAmount = if (valDouble > payBalance) payBalance.toString() else it
                                }
                            },
                            symbol = "ETH",
                            balance = "1.45 ETH",
                            usdValue = payUsdValue,
                            onMaxClick = { payAmount = payBalance.toString() }
                        )

                        // Receive Card
                        SwapAssetCard(
                            label = "You Receive",
                            amount = receiveAmount,
                            onAmountChange = {},
                            symbol = "BTC",
                            balance = "0.05 BTC",
                            usdValue = "≈ $4,060.00", // Example
                            readOnly = true
                        )
                    }

                    // Middle Swap Icon
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00C0FF),
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.Center)
                            .border(4.dp, Color(0xFF020914), CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Details ---
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Rate", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                        Text("1 ETH = ${mockRate} BTC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estimated Network Fee", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                        Text("~$${String.format("%.2f", networkFeeEstimate)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // --- Action Button ---
                Surface(
                    onClick = { },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF00A7C4), Color(0xFF0094AB))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Review Swap",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun SwapAssetCard(
    label: String,
    amount: String,
    onAmountChange: (String) -> Unit,
    symbol: String,
    balance: String,
    usdValue: String,
    readOnly: Boolean = false,
    onMaxClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0D1421).copy(alpha = 0.8f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                Text("Balance: $balance", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f))
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = amount,
                        onValueChange = onAmountChange,
                        readOnly = readOnly,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Text(usdValue, fontSize = 14.sp, color = Color.White.copy(alpha = 0.4f))
                }
                
                Surface(
                    onClick = { },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF03080F),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val iconRes = when(symbol) {
                            "ETH" -> R.drawable.image_ethereum
                            "BTC" -> R.drawable.image_bitcoin
                            else -> R.drawable.image_ethereum
                        }
                        Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Icon(Icons.Default.ExpandMore, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            if (!readOnly) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    onClick = onMaxClick,
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0B1B2B)
                ) {
                    Text(
                        "MAX",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00C0FF)
                    )
                }
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
