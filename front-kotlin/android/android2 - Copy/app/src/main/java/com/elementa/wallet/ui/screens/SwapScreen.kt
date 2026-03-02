package com.elementa.wallet.ui.screens

import android.widget.Toast

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.R
import com.elementa.wallet.ui.components.PulsarBottomNav
import androidx.compose.ui.platform.LocalContext
import com.elementa.wallet.ui.designsystem.*

@Composable
fun SwapScreen(
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onAssets: () -> Unit,
    onTransfers: () -> Unit,
    onSwap: () -> Unit,
    onActivity: () -> Unit,
    onHub: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    var payAmount by remember { mutableStateOf("0.00") }
    val payBalance = 1.45
    val receiveBalance = 0.05
    val mockRate = 0.045161 
    val networkFee = 1.25
    
    val payAmtDouble = payAmount.toDoubleOrNull() ?: 0.0
    val receiveAmount = if (payAmtDouble == 0.0) "0.00" else String.format("%.6f", payAmtDouble * mockRate)
    val payUsdValue = String.format("$%.2f", payAmtDouble * 2500.0)

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040A20))) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                PulsarBottomNav(
                    onHome = onDashboard,
                    onAssets = onAssets,
                    onHub = onHub,
                    onActivity = onActivity,
                    onSettings = onSettings,
                    currentRoute = "swap"
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // --- Top Title Bar ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Swap",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- Main Swap Container matching Image 1 ---
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0D1421).copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        
                        // Pay Section
                        Box(modifier = Modifier.fillMaxWidth()) {
                            SwapTokenEntry(
                                label = "Pay",
                                amount = payAmount,
                                onAmountChange = { 
                                    if (it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        val valDouble = it.toDoubleOrNull() ?: 0.0
                                        payAmount = if (valDouble > payBalance) payBalance.toString() else it
                                    }
                                },
                                balance = payBalance.toString(),
                                usdValue = payUsdValue,
                                symbol = "ETH",
                                iconRes = R.drawable.image_ethereum
                            )
                        }

                        // Arrow Divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1D293D),
                                modifier = Modifier
                                    .size(40.dp)
                                    .border(4.dp, Color(0xFF0D1421), RoundedCornerShape(12.dp))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = Color(0xFF00D3F2),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Receive Section
                        Box(modifier = Modifier.fillMaxWidth()) {
                            SwapTokenEntry(
                                label = "Receive",
                                amount = receiveAmount,
                                onAmountChange = {},
                                balance = receiveBalance.toString(),
                                usdValue = "", // Not shown in image 1 for receive
                                symbol = "BTC",
                                iconRes = R.drawable.image_bitcoin,
                                readOnly = true
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Details Area
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Rate", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                                Text("1 ETH = $mockRate BTC", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Network Fee", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                                Text("~$${String.format("%.2f", networkFee)}", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Button
                        Surface(
                            onClick = { 
                                Toast.makeText(context, "Swap Successful!", Toast.LENGTH_LONG).show()
                                onDashboard()
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF004F69), // Darker teal from image
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "Review Swap",
                                    color = Color(0xFF00D3F2),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun SwapTokenEntry(
    label: String,
    amount: String,
    onAmountChange: (String) -> Unit,
    balance: String,
    usdValue: String,
    symbol: String,
    iconRes: Int,
    readOnly: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF03080F).copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp)
                Text("Balance: $balance", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = amount,
                        onValueChange = onAmountChange,
                        readOnly = readOnly,
                        textStyle = TextStyle(
                            color = if (readOnly && (amount == "0.00" || amount == "0.000000")) Color.White.copy(alpha = 0.2f) else Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        cursorBrush = SolidColor(Color(0xFF00D3F2)),
                        decorationBox = { innerTextField ->
                            if (amount.isEmpty()) {
                                Text("0.00", color = Color.White.copy(alpha = 0.2f), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                            }
                            innerTextField()
                        }
                    )
                    if (usdValue.isNotEmpty()) {
                        Text(usdValue, color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    onClick = { /* Select Chain */ }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            }
        }
    }
}
