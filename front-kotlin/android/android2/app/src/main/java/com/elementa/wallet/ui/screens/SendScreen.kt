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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elementa.wallet.ui.components.RedesignedBottomNav
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.viewmodel.SendViewModel
import com.elementa.wallet.R
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
    var selectedTab by remember { mutableStateOf("Standard") }
    
    // Update recipient when initialRecipient changes (e.g. from QR scan result)
    LaunchedEffect(initialRecipient) {
        if (initialRecipient.isNotEmpty()) {
            recipient = initialRecipient
        }
    }
    
    // Default selected token: Ethereum
    var selectedToken by remember { 
        mutableStateOf(SelectableToken("ETH", "Ethereum", "1.45", 4060.00)) 
    }
    
    val balanceDouble = selectedToken.balance.replace(",", "").toDoubleOrNull() ?: 1.45
    val usdValue = (amount.toDoubleOrNull() ?: 0.0) * (selectedToken.balanceUsd / balanceDouble)

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
                    currentRoute = "transfers"
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
                
                // --- Header Section ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF031627),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ArrowOutward,
                                contentDescription = null,
                                tint = Color(0xFF0092B8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Send",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Transfer assets to another wallet",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Main Send Card ---
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0D1421).copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        
                        // Asset Selector
                        Text(
                            "Asset",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF030712),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { /* Open selector */ }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.image_ethereum),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    selectedToken.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.padding(top = 10.dp, bottom = 28.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Balance: ${selectedToken.balance} ${selectedToken.symbol}",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.35f)
                            )
                            Text(
                                "≈ $${String.format("%,.2f", selectedToken.balanceUsd)}",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.35f)
                            )
                        }

                        // Amount Input
                        Text(
                            "Amount",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF030712),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = amount,
                                    onValueChange = { 
                                        if (it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                            amount = it
                                        }
                                    },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    cursorBrush = SolidColor(Color(0xFF0092B8))
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0B1B2B),
                                    onClick = { amount = balanceDouble.toString() }
                                ) {
                                    Text(
                                        "MAX",
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0092B8)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Recipient Address
                        Text(
                            "Recipient Address",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF030712),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = recipient,
                                    onValueChange = { recipient = it },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 15.sp
                                    ),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        if (recipient.isEmpty()) {
                                            Text(
                                                "Enter ETH address",
                                                color = Color.White.copy(alpha = 0.3f),
                                                fontSize = 15.sp
                                            )
                                        }
                                        innerTextField()
                                    },
                                    cursorBrush = SolidColor(Color(0xFF0092B8))
                                )
                                IconButton(onClick = onQrScan, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.scan_qr_icon),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Network Fee
                        Text(
                            "Network Fee",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            FeeOptionCard(
                                title = "Standard",
                                time = "~5 mins",
                                isSelected = selectedTab == "Standard",
                                onClick = { selectedTab = "Standard" },
                                modifier = Modifier.weight(1f)
                            )
                            FeeOptionCard(
                                title = "Priority",
                                time = "~1 min",
                                isSelected = selectedTab == "Priority",
                                onClick = { selectedTab = "Priority" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Review Button
                Surface(
                    onClick = { onContinue(amount, recipient) },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0092B8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Review Transaction",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun FeeOptionCard(
    title: String,
    time: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF030712),
        border = BorderStroke(
            1.dp, 
            if (isSelected) Color(0xFF0092B8).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.05f)
        ),
        modifier = modifier.height(84.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF0092B8) else Color.White
            )
            Text(
                time,
                fontSize = 13.sp,
                color = if (isSelected) Color(0xFF0092B8).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Send Screen Components
// ─────────────────────────────────────────────────────────────

@Composable
private fun SendTopBar(onBack: () -> Unit, onQrScan: () -> Unit) {
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
            onClick = { onQrScan() },
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
