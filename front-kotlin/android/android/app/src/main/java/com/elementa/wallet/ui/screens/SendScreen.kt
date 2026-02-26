package com.elementa.wallet.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elementa.wallet.ui.components.RedesignedBottomNav
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.viewmodel.SendViewModel
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
// Token Data Model for Selection
// ─────────────────────────────────────────────────────────────

data class SelectableToken(
    val symbol: String,
    val name: String,
    val balance: String,
    val balanceUsd: Double,
    val iconLetter: String = symbol.firstOrNull()?.toString() ?: "?"
)

// ─────────────────────────────────────────────────────────────
// Address Validation Utilities
// ─────────────────────────────────────────────────────────────

private fun isValidEthereumAddress(address: String): Boolean {
    // Basic Ethereum address validation: 0x followed by 40 hex characters
    return address.matches(Regex("^0x[a-fA-F0-9]{40}$"))
}

private fun isValidSolanaAddress(address: String): Boolean {
    // Solana addresses are base58 encoded, typically 32-44 characters
    return address.matches(Regex("^[1-9A-HJ-NP-Za-km-z]{32,44}$"))
}

private fun isValidBitcoinAddress(address: String): Boolean {
    // Basic Bitcoin address validation (P2PKH, P2SH, Bech32)
    return address.matches(Regex("^(1|3|bc1)[a-zA-HJ-NP-Z0-9]{25,62}$"))
}

private fun validateRecipientAddress(address: String): AddressValidation {
    if (address.isBlank()) return AddressValidation.EMPTY
    
    return when {
        isValidEthereumAddress(address) -> AddressValidation.VALID_ETH
        isValidSolanaAddress(address) -> AddressValidation.VALID_SOL
        isValidBitcoinAddress(address) -> AddressValidation.VALID_BTC
        address.length < 10 -> AddressValidation.INCOMPLETE
        else -> AddressValidation.INVALID
    }
}

private enum class AddressValidation {
    EMPTY,
    INCOMPLETE,
    VALID_ETH,
    VALID_SOL,
    VALID_BTC,
    INVALID
}

// ─────────────────────────────────────────────────────────────
// Send Screen Composable
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onAssets: () -> Unit,
    onTransfers: () -> Unit,
    onSwap: () -> Unit,
    onActivity: () -> Unit,
    onContinue: (String, String) -> Unit,
    onQrScan: () -> Unit = {},
    initialRecipient: String = "",
    viewModel: SendViewModel = hiltViewModel()
) {
    var amount by remember { mutableStateOf("0.00") }
    var recipient by remember { mutableStateOf(initialRecipient) }
    var showTokenSheet by remember { mutableStateOf(false) }
    
    // Currently selected token
    var selectedToken by remember { 
        mutableStateOf(SelectableToken("ETH", "Ethereum", "1.45", 3_245.67)) 
    }
    
    // Mock held tokens - in production, fetch from AssetsViewModel
    val heldTokens = remember {
        listOf(
            SelectableToken("ETH", "Ethereum", "1.45", 3_245.67),
            SelectableToken("USDC", "USD Coin", "2,400.00", 2_400.00),
            SelectableToken("AVAX", "Avalanche", "25.5", 892.50),
            //SelectableToken("MATIC", "Polygon", "1,250.00", 1_125.00),
            SelectableToken("SOL", "Solana", "12.8", 2_560.00)
        )
    }
    
    val addressValidation = validateRecipientAddress(recipient)
    val isAddressValid = addressValidation in listOf(
        AddressValidation.VALID_ETH, 
        AddressValidation.VALID_SOL, 
        AddressValidation.VALID_BTC
    )
    
    val amountValue = amount.toDoubleOrNull()
    val canContinue = isAddressValid && amountValue != null && amountValue > 0
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                SendTopBar(onBack = onBack)
            },
            bottomBar = {
                RedesignedBottomNav(
                    onDashboard = onDashboard,
                    onAssets = onAssets,
                    onTransfers = onTransfers,
                    onSwap = onSwap,
                    onActivity = onActivity,
                    currentRoute = "transfers"
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Source Panel (From)
                SourceDestinationPanel(
                    label = "FROM",
                    value = "My Wallet",
                    icon = Icons.Default.AccountBalanceWallet,
                    isDestination = false
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Recipient Address Input
                RecipientAddressInput(
                    recipient = recipient,
                    onRecipientChange = { recipient = it },
                    validation = addressValidation,
                    onQrScan = onQrScan
                )

                // Amount Display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            amount,
                            style = PulsarTypography.Typography.displayLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 52.sp,
                            letterSpacing = (-2).sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            selectedToken.symbol,
                            style = PulsarTypography.Typography.headlineLarge,
                            color = PulsarColors.PrimaryDark,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                    
                    val amountDouble = amount.toDoubleOrNull() ?: 0.0
                    val balanceDouble = selectedToken.balance.replace(",", "").toDoubleOrNull() ?: 1.0
                    val pricePerToken = if (balanceDouble > 0.0) {
                        selectedToken.balanceUsd / balanceDouble
                    } else {
                        0.0
                    }
                    val usdValue = amountDouble * pricePerToken
                    Text(
                        "≈ $${String.format("%,.2f", usdValue)} USD",
                        style = PulsarTypography.Typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.padding(top = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // MAX Button
                        Surface(
                            onClick = { amount = selectedToken.balance.replace(",", "") },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, PulsarColors.PrimaryDark.copy(alpha = 0.3f))
                        ) {
                            Text(
                                "MAX",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                style = PulsarTypography.Typography.labelSmall,
                                color = PulsarColors.PrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Token Selector
                        Surface(
                            onClick = { showTokenSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape, 
                                    color = PulsarColors.PrimaryDark.copy(alpha = 0.2f), 
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            selectedToken.iconLetter, 
                                            color = PulsarColors.PrimaryDark, 
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    selectedToken.symbol, 
                                    style = PulsarTypography.Typography.labelSmall, 
                                    color = Color.White, 
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.ExpandMore, 
                                    contentDescription = null, 
                                    tint = Color.White.copy(alpha = 0.6f), 
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    // Balance Display
                    Text(
                        "Balance: ${selectedToken.balance} ${selectedToken.symbol}",
                        style = PulsarTypography.Typography.labelSmall,
                        color = PulsarColors.TextSecondaryDark,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                // Amount Input Field
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PulsarColors.SurfaceDark,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    BasicTextField(
                        value = amount,
                        onValueChange = { newValue ->
                            // Allow only numbers and one decimal point
                            if (newValue.isEmpty() || newValue == "0") {
                                amount = "0"
                            } else if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                amount = newValue
                            }
                        },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(PulsarColors.PrimaryDark),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                if (amount == "0" || amount.isEmpty()) {
                                    Text(
                                        "0.00",
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                PulsarComponents.PulsarButton(
                    text = "CONTINUE",
                    onClick = { onContinue(amount, recipient) },
                    enabled = canContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .navigationBarsPadding(),
                    glow = canContinue
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        // Token Selector Bottom Sheet
        if (showTokenSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTokenSheet = false },
                sheetState = sheetState,
                containerColor = PulsarColors.PanelDark,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(40.dp, 4.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                    )
                }
            ) {
                TokenSelectorSheet(
                    tokens = heldTokens,
                    selectedToken = selectedToken,
                    onTokenSelected = { token ->
                        selectedToken = token
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showTokenSheet = false
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Send Screen Components
// ─────────────────────────────────────────────────────────────

@Composable
private fun SendTopBar(onBack: () -> Unit) {
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
        Text(
            "TRANSFER FUNDS",
            style = PulsarTypography.CyberLabel,
            color = PulsarColors.PrimaryDark,
            letterSpacing = 4.sp,
            fontSize = 16.sp
        )
        IconButton(
            onClick = { /* QR Scanner */ },
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(PulsarColors.SurfaceDark.copy(alpha = 0.4f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = PulsarColors.PrimaryDark, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun RecipientAddressInput(
    recipient: String,
    onRecipientChange: (String) -> Unit,
    validation: AddressValidation,
    onQrScan: () -> Unit
) {
    val borderColor = when (validation) {
        AddressValidation.VALID_ETH, 
        AddressValidation.VALID_SOL, 
        AddressValidation.VALID_BTC -> PulsarColors.SuccessGreen.copy(alpha = 0.5f)
        AddressValidation.INVALID -> PulsarColors.DangerRed.copy(alpha = 0.5f)
        else -> Color.White.copy(alpha = 0.1f)
    }
    
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PulsarColors.SurfaceDark,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.size(44.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "TO",
                        style = PulsarTypography.CyberLabel,
                        color = PulsarColors.PrimaryDark.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    
                    BasicTextField(
                        value = recipient,
                        onValueChange = onRecipientChange,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(PulsarColors.PrimaryDark),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        decorationBox = { innerTextField ->
                            Box {
                                if (recipient.isEmpty()) {
                                    Text(
                                        "Enter wallet address",
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Validation indicator
                AnimatedVisibility(visible = recipient.isNotEmpty()) {
                    Icon(
                        imageVector = when (validation) {
                            AddressValidation.VALID_ETH, 
                            AddressValidation.VALID_SOL, 
                            AddressValidation.VALID_BTC -> Icons.Default.CheckCircle
                            AddressValidation.INVALID -> Icons.Default.Error
                            else -> Icons.Default.HourglassEmpty
                        },
                        contentDescription = null,
                        tint = when (validation) {
                            AddressValidation.VALID_ETH, 
                            AddressValidation.VALID_SOL, 
                            AddressValidation.VALID_BTC -> PulsarColors.SuccessGreen
                            AddressValidation.INVALID -> PulsarColors.DangerRed
                            else -> Color.White.copy(alpha = 0.3f)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            // Validation message
            AnimatedVisibility(visible = validation == AddressValidation.INVALID) {
                Text(
                    "Invalid wallet address format",
                    style = PulsarTypography.Typography.labelSmall,
                    color = PulsarColors.DangerRed,
                    modifier = Modifier.padding(top = 8.dp, start = 58.dp)
                )
            }
        }
    }
}

@Composable
private fun SourceDestinationPanel(
    label: String,
    value: String,
    icon: ImageVector,
    isDestination: Boolean,
    onAdd: (() -> Unit)? = null
) {
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
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDestination) Color.White.copy(alpha = 0.05f) else PulsarColors.PrimaryDark.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp),
                border = BorderStroke(1.dp, if (isDestination) Color.White.copy(alpha = 0.1f) else PulsarColors.PrimaryDark.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isDestination) Color.White.copy(alpha = 0.4f) else PulsarColors.PrimaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = PulsarTypography.CyberLabel,
                    color = PulsarColors.PrimaryDark.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    value,
                    style = PulsarTypography.Typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
            
            if (onAdd != null) {
                Surface(
                    onClick = onAdd,
                    shape = RoundedCornerShape(8.dp),
                    color = PulsarColors.PrimaryDark.copy(alpha = 0.05f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add", tint = PulsarColors.PrimaryDark, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenSelectorSheet(
    tokens: List<SelectableToken>,
    selectedToken: SelectableToken,
    onTokenSelected: (SelectableToken) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            "SELECT TOKEN",
            style = PulsarTypography.CyberLabel,
            color = PulsarColors.PrimaryDark,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            items(tokens) { token ->
                val isSelected = token.symbol == selectedToken.symbol
                Surface(
                    onClick = { onTokenSelected(token) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) PulsarColors.PrimaryDark.copy(alpha = 0.1f) else PulsarColors.SurfaceDark,
                    border = BorderStroke(
                        1.dp, 
                        if (isSelected) PulsarColors.PrimaryDark.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PulsarColors.PrimaryDark.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    token.iconLetter,
                                    color = PulsarColors.PrimaryDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(14.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                token.name,
                                style = PulsarTypography.Typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                token.symbol,
                                style = PulsarTypography.Typography.bodySmall,
                                color = PulsarColors.TextSecondaryDark
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                token.balance,
                                style = PulsarTypography.Typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                "$${String.format("%,.2f", token.balanceUsd)}",
                                style = PulsarTypography.Typography.bodySmall,
                                color = PulsarColors.TextSecondaryDark
                            )
                        }
                        
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PulsarColors.PrimaryDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(key: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = PulsarColors.SurfaceDark,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = modifier.height(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (key == "backspace") {
                Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = Color.White, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    key,
                    style = PulsarTypography.Typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
        }
    }
}
