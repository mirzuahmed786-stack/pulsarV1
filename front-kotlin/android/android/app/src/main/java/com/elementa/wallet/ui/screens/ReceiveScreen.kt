package com.elementa.wallet.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.components.RedesignedBottomNav
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.viewmodel.ReceiveViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun ReceiveScreen(
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onAssets: () -> Unit,
    onTransfers: () -> Unit,
    onSwap: () -> Unit,
    onActivity: () -> Unit,
    viewModel: ReceiveViewModel = hiltViewModel()
) {
    val receiveState by viewModel.uiState.collectAsState()
    val address = receiveState.receiveAddress.ifBlank { "" }
    val qrPayload = receiveState.qrPayload.ifBlank { address }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    // State for copy confirmation
    var showCopiedToast by remember { mutableStateOf(false) }
    
    // Generate QR Bitmap
    val qrBitmap = remember(qrPayload) {
        if (qrPayload.isNotBlank()) {
            generateQrBitmap(qrPayload, 512)
        } else null
    }
    
    // Chain info based on selected chain
    val chainName = when (receiveState.selectedChainCode.uppercase()) {
        "BTC" -> "Bitcoin"
        "ETH" -> "Ethereum"
        "SOL" -> "Solana"
        "AVAX" -> "Avalanche"
        "MATIC", "POL" -> "Polygon"
        "BNB" -> "BNB Chain"
        else -> "Wallet"
    }
    
    val chainSymbol = when (receiveState.selectedChainCode.uppercase()) {
        "BTC" -> "₿"
        "ETH" -> "Ξ"
        "SOL" -> "◎"
        "AVAX" -> "A"
        "MATIC", "POL" -> "P"
        "BNB" -> "B"
        else -> "W"
    }

    // Reset toast after delay
    LaunchedEffect(showCopiedToast) {
        if (showCopiedToast) {
            kotlinx.coroutines.delay(2000)
            showCopiedToast = false
        }
    }

    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Premium top bar with gradient accent
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
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
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "RECEIVE",
                                style = PulsarTypography.CyberLabel,
                                color = Color.White,
                                fontSize = 12.sp,
                                letterSpacing = 2.sp
                            )
                            Text(
                                chainName.uppercase(),
                                color = PulsarColors.PrimaryDark,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.size(44.dp))
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
                    currentRoute = "transfers"
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Premium QR Card with gradient border
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = PulsarColors.PrimaryDark.copy(alpha = 0.3f),
                            spotColor = PulsarColors.PrimaryDark.copy(alpha = 0.2f)
                        )
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    PulsarColors.PrimaryDark.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.1f)
                                )
                            ),
                            RoundedCornerShape(32.dp)
                        )
                        .padding(2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = Color.White,
                        modifier = Modifier.size(280.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (receiveState.isLoading || qrBitmap == null) {
                                // Loading state
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = PulsarColors.PrimaryDark,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Generating...",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                // Real QR Code
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "QR Code for $address",
                                        modifier = Modifier
                                            .size(200.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Chain indicator badge
                                    Surface(
                                        shape = CircleShape,
                                        color = PulsarColors.PrimaryDark,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                chainSymbol,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                // Address container with premium styling
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Label
                        Text(
                            "$chainName Address",
                            style = PulsarTypography.Typography.labelMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Address display with monospace style
                        if (address.isNotBlank()) {
                            Text(
                                formatAddressDisplay(address),
                                style = PulsarTypography.Typography.bodyMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        } else {
                            Text(
                                "Loading address...",
                                style = PulsarTypography.Typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Action buttons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Copy Button
                            ReceiveActionButton(
                                icon = Icons.Outlined.ContentCopy,
                                label = if (showCopiedToast) "Copied!" else "Copy",
                                isPrimary = true,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (address.isNotBlank()) {
                                        clipboardManager.setText(AnnotatedString(address))
                                        showCopiedToast = true
                                    }
                                }
                            )
                            
                            // Share Button
                            ReceiveActionButton(
                                icon = Icons.Outlined.Share,
                                label = "Share",
                                isPrimary = false,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (address.isNotBlank()) {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "My $chainName address:\n$address")
                                            putExtra(Intent.EXTRA_SUBJECT, "$chainName Wallet Address")
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, "Share Address")
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Security notice with premium styling
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.02f),
                    border = BorderStroke(1.dp, PulsarColors.WarningAmber.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = PulsarColors.WarningAmber.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Important",
                                style = PulsarTypography.Typography.labelMedium,
                                color = PulsarColors.WarningAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                getSecurityMessage(chainName),
                                style = PulsarTypography.Typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Network indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PulsarColors.SuccessGreen.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(PulsarColors.SuccessGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Mainnet",
                            style = PulsarTypography.Typography.labelSmall,
                            color = PulsarColors.SuccessGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun ReceiveActionButton(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = if (isPrimary) {
        PulsarColors.PrimaryDark
    } else {
        Color.White.copy(alpha = 0.05f)
    }
    
    val contentColor = if (isPrimary) {
        Color.White
    } else {
        Color.White.copy(alpha = 0.8f)
    }
    
    val borderColor = if (isPrimary) {
        Color.Transparent
    } else {
        Color.White.copy(alpha = 0.1f)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                style = PulsarTypography.Typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Generate QR code bitmap using ZXing
 */
private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.MARGIN, 1)
            put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H)
        }
        
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK 
                    else android.graphics.Color.WHITE
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

/**
 * Format address for better readability
 */
private fun formatAddressDisplay(address: String): String {
    if (address.length <= 20) return address
    
    // For long addresses, add line breaks for readability
    val chunks = address.chunked(14)
    return chunks.joinToString("\n")
}

/**
 * Get chain-specific security message
 */
private fun getSecurityMessage(chainName: String): String {
    return when (chainName) {
        "Bitcoin" -> "Only send Bitcoin (BTC) to this address. Sending other cryptocurrencies may result in permanent loss."
        "Solana" -> "Only send Solana (SOL) and SPL tokens to this address. Sending other assets may result in permanent loss."
        else -> "Only send $chainName compatible tokens to this address. Sending incompatible assets may result in permanent loss."
    }
}
