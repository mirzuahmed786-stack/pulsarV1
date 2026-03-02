package com.elementa.wallet.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elementa.wallet.R
import com.elementa.wallet.ui.components.PulsarBottomNav
import com.elementa.wallet.ui.designsystem.*
// PulsarSlideToConfirm is a top-level function in designsystem
import com.elementa.wallet.viewmodel.SendViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onAssets: () -> Unit,
    onTransfers: () -> Unit,
    onSwap: () -> Unit,
    onActivity: () -> Unit,
    onHub: () -> Unit,
    onSettings: () -> Unit,
    onContinue: (String, String) -> Unit,
    onQrScan: () -> Unit = {},
    initialRecipient: String = "",
    viewModel: SendViewModel = hiltViewModel()
) {
    var amount by remember { mutableStateOf("0.00") }
    var recipient by remember { mutableStateOf(initialRecipient) }
    var selectedFee by remember { mutableStateOf("Standard") }
    var showAssetSelector by remember { mutableStateOf(false) }
    var showPinSheet by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Default asset
    var selectedAsset by remember { mutableStateOf(AssetItem("Ethereum", "ETH", 1.45, 4060.00, R.drawable.image_ethereum)) }
    
    val sheetState = rememberModalBottomSheetState()
    
    // Update recipient when initialRecipient changes
    LaunchedEffect(initialRecipient) {
        if (initialRecipient.isNotEmpty()) {
            recipient = initialRecipient
            clipboardManager.setText(AnnotatedString(initialRecipient))
        }
    }
    
    val pulsarBlue = Color(0xFF00D3F2)

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
                    currentRoute = "transfers"
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
                Spacer(modifier = Modifier.height(20.dp))
                
                // --- Header matching Image 2 ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = Color(0xFF0D1421).copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.send_page_arrow),
                                contentDescription = "Back",
                                tint = pulsarBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
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
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp)) // Reduced spacing

                // --- Main Card matching Image 2 ---
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0D1421).copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        
                        // Asset Selector
                        Text(
                            "Asset",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF030712).copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showAssetSelector = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = selectedAsset.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    selectedAsset.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White.copy(alpha = 0.4f))
                            }
                        }
                        
                        Row(
                            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Balance: ${selectedAsset.balance} ${selectedAsset.symbol}",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "≈ $${selectedAsset.usdValue}",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 14.sp
                            )
                        }

                        // Amount Input
                        Text(
                            "Amount",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF030712).copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    cursorBrush = SolidColor(pulsarBlue),
                                    decorationBox = { innerTextField ->
                                        if (amount.isEmpty() || amount == "0.00") {
                                            Text("0.00", color = Color.White.copy(alpha = 0.3f), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                        }
                                        innerTextField()
                                    }
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1D293D).copy(alpha = 0.9f),
                                    onClick = { amount = selectedAsset.balance.toString() }
                                ) {
                                    Text(
                                        "MAX",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = pulsarBlue
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Recipient Address
                        Text(
                            "Recipient Address",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF030712).copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
                                                "Enter ${selectedAsset.symbol} address",
                                                color = Color.White.copy(alpha = 0.3f),
                                                fontSize = 15.sp
                                            )
                                        }
                                        innerTextField()
                                    },
                                    cursorBrush = SolidColor(pulsarBlue)
                                )
                                IconButton(onClick = onQrScan, modifier = Modifier.size(28.dp)) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.scan_qr_icon),
                                        contentDescription = "Scan",
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Network Fee
                        Text(
                            "Network Fee",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            SendFeeCard(
                                title = "Standard",
                                time = "~5 mins",
                                isSelected = selectedFee == "Standard",
                                onClick = { selectedFee = "Standard" },
                                modifier = Modifier.weight(1f)
                            )
                            SendFeeCard(
                                title = "Priority",
                                time = "~1 min",
                                isSelected = selectedFee == "Priority",
                                onClick = { selectedFee = "Priority" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp)) // Reduced spacing

                // Updated Swipe to Send as requested
                PulsarSlideToConfirm(
                    onConfirm = {
                        if (amount.toDoubleOrNull() ?: 0.0 > 0 && recipient.isNotEmpty()) {
                            showPinSheet = true
                        } else {
                            // Reset swipe or show error toast
                            Toast.makeText(context, "Enter valid amount and address", Toast.LENGTH_SHORT).show()
                        }
                    },
                    text = "Slide to Send ${selectedAsset.symbol}",
                    thumbColor = pulsarBlue
                )
                
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        if (showAssetSelector) {
            ModalBottomSheet(
                onDismissRequest = { showAssetSelector = false },
                sheetState = sheetState,
                containerColor = Color(0xFF030712),
                contentColor = Color.White
            ) {
                AssetSelectorContent(
                    selectedAsset = selectedAsset,
                    onAssetSelected = { 
                        selectedAsset = it
                        showAssetSelector = false
                    }
                )
            }
        }

        if (showPinSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPinSheet = false },
                containerColor = Color(0xFF0D1421),
                contentColor = Color.White
            ) {
                PinEntrySheetContent(
                    onPinSuccess = {
                        showPinSheet = false
                        Toast.makeText(context, "Transaction Sent Successfully!", Toast.LENGTH_LONG).show()
                        scope.launch {
                            delay(500)
                            onDashboard()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PinEntrySheetContent(onPinSuccess: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    val maxPinLength = 6
    
    Column(
        modifier = Modifier.padding(24.dp).padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Enter Security PIN", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Please enter your 6-digit PIN to authorize the transaction", color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // PIN Dots
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(maxPinLength) { index ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            if (index < pin.length) Color(0xFF00D3F2) else Color.White.copy(alpha = 0.15f),
                            CircleShape
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Simple numeric keypad
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "DEL")
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            keys.chunked(3).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    row.forEach { key ->
                        if (key.isNotEmpty()) {
                            Surface(
                                onClick = {
                                    if (key == "DEL") {
                                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                    } else {
                                        if (pin.length < maxPinLength) {
                                            pin += key
                                            if (pin.length == maxPinLength) {
                                                onPinSuccess()
                                            }
                                        }
                                    }
                                },
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.05f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key == "DEL") {
                                        Icon(Icons.Default.Backspace, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Text(key, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(72.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetSelectorContent(
    selectedAsset: AssetItem,
    onAssetSelected: (AssetItem) -> Unit
) {
    val assets = listOf(
        AssetItem("Ethereum", "ETH", 1.45, 4060.00, R.drawable.image_ethereum),
        AssetItem("Bitcoin", "BTC", 0.05, 3100.00, R.drawable.image_bitcoin),
        AssetItem("Solana", "SOL", 145.2, 20328.00, R.drawable.image_solana),
        AssetItem("BSC", "BNB", 2.5, 1500.00, R.drawable.image_ethereum), // Reusing ETH icon as placeholder if BNB missing
        AssetItem("Polygon", "POL", 500.0, 450.00, R.drawable.image_ethereum),
        AssetItem("Avalanche", "AVAX", 15.0, 600.00, R.drawable.image_ethereum)
    )

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Text("Select Asset", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(assets) { asset ->
                Surface(
                    onClick = { onAssetSelected(asset) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (asset.symbol == selectedAsset.symbol) Color(0xFF1D293D) else Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(painter = painterResource(id = asset.icon), contentDescription = null, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(asset.name, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(asset.symbol, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SendFeeCard(
    title: String,
    time: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulsarBlue = Color(0xFF00D3F2)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF030712).copy(alpha = 0.8f),
        border = BorderStroke(
            1.dp, 
            if (isSelected) pulsarBlue else Color.White.copy(alpha = 0.1f)
        ),
        modifier = modifier.height(95.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) pulsarBlue else Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                time,
                fontSize = 13.sp,
                color = if (isSelected) pulsarBlue.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
