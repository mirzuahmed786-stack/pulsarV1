package com.elementa.wallet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.components.SolarGradientBackground
import com.elementa.wallet.ui.designsystem.PulsarColors
import com.elementa.wallet.ui.designsystem.PulsarComponents
import com.elementa.wallet.ui.designsystem.PulsarTypography

@Composable
fun SwapOrchestrationScreen(onBack: () -> Unit) {
    var fromAmount by remember { mutableStateOf("") }
    var toAmount by remember { mutableStateOf("") }
    
    SolarGradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { PulsarTopBarWithBack(onBack) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    "S W A P   H U B", 
                    style = PulsarTypography.CyberLabel,
                    color = PulsarColors.TextTertiary,
                    letterSpacing = 4.sp
                )
                
                Text(
                    "Exchange Assets", 
                    style = PulsarTypography.Typography.displayLarge,
                    color = PulsarColors.TextPrimary
                )

                // From Card
                SwapAssetInputCard(
                    label = "FROM",
                    amount = fromAmount,
                    onAmountChange = { fromAmount = it },
                    assetSymbol = "ETH",
                    assetName = "Ethereum",
                    balance = "1.24"
                )

                // Swap Icon
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = PulsarColors.ProfessionalTeal,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("⇅", color = Color.White, fontSize = 24.sp)
                        }
                    }
                }

                // To Card
                SwapAssetInputCard(
                    label = "TO (ESTIMATED)",
                    amount = toAmount,
                    onAmountChange = { toAmount = it },
                    assetSymbol = "USDC",
                    assetName = "USD Coin",
                    balance = "420.00"
                )

                PulsarComponents.PulsarCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Rate", color = PulsarColors.TextSecondary)
                        Text("1 ETH = 2,450.12 USDC", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Slippage Tolerance", color = PulsarColors.TextSecondary)
                        Text("0.5%", color = PulsarColors.ProfessionalTeal, fontWeight = FontWeight.Bold)
                    }
                }

                PulsarComponents.PulsarButton(
                    text = "Review Swap",
                    onClick = { /* Orchestrate Review Flow */ },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun SwapAssetInputCard(
    label: String,
    amount: String,
    onAmountChange: (String) -> Unit,
    assetSymbol: String,
    assetName: String,
    balance: String
) {
    PulsarComponents.PulsarCard {
        Text(label, style = PulsarTypography.CyberLabel, color = PulsarColors.TextTertiary)
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                TextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    placeholder = { Text("0.00", fontSize = 28.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = PulsarTypography.Typography.displayLarge.copy(fontSize = 28.sp)
                )
                Text("≈ $0.00", color = PulsarColors.TextSecondary, modifier = Modifier.padding(start = 16.dp))
            }
            Surface(
                onClick = { /* Open Token Picker */ },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = PulsarColors.ElevatedDark
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(assetSymbol, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("▾")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Balance: $balance $assetSymbol", color = PulsarColors.TextTertiary, style = PulsarTypography.CyberLabel)
    }
}
