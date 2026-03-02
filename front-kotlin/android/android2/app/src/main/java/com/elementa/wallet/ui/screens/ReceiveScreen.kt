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
import androidx.compose.ui.res.painterResource
import com.elementa.wallet.R
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
    val address = receiveState.receiveAddress.ifBlank { "0x1234...5678" } // Mock if blank
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    val qrBitmap = remember(address) {
        if (address.isNotBlank()) {
            generateQrBitmap(address, 512)
        } else null
    }

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
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // --- Header Section ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF031627), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Receive",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Scan to deposit funds",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Network Selector Card ---
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0D1421).copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Network",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF03080F),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { /* Open network selector */ }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.image_ethereum),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Ethereum",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        "ETH Network",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.4f)
                                    )
                                }
                                Icon(
                                    Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- QR Code Card ---
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White,
                    modifier = Modifier.size(260.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
                        qrBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        } ?: CircularProgressIndicator(color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Address Box ---
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF03080F),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        address,
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 20.dp),
                        fontSize = 13.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- Action Buttons ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = { clipboardManager.setText(AnnotatedString(address)) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0D1421).copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.copy_icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, address)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Address"))
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0D1421).copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.share_icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Important Notice ---
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF2B1F03).copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, Color(0xFF7A5C00).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.warning),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Important",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB800)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Only send Ethereum (ETH) to this address. Sending other cryptocurrencies may result in permanent loss.",
                                fontSize = 12.sp,
                                color = Color(0xFFFFB800).copy(alpha = 0.8f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
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
