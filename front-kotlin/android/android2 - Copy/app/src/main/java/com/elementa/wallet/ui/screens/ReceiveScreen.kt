package com.elementa.wallet.ui.screens

import android.widget.Toast

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elementa.wallet.R
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.viewmodel.ReceiveViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onAssets: () -> Unit,
    onTransfers: () -> Unit,
    onSwap: () -> Unit,
    onActivity: () -> Unit,
    onHub: () -> Unit,
    onSettings: () -> Unit,
    viewModel: ReceiveViewModel = hiltViewModel()
) {
    val receiveState by viewModel.uiState.collectAsState()
    val address = receiveState.receiveAddress.ifBlank { "0x71C7656EC7ab88b098defB751B7401B5f6d8976F" } 
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    val qrBitmap = remember(address) {
        if (address.isNotBlank()) {
            generateQrBitmap(address, 512)
        } else null
    }

    var showChainSelector by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040A20))) {
        Scaffold(
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // --- Header ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = Color(0xFF001F1A), 
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF00D3F2), 
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(
                            "Receive",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.offset(y = (-2).dp)
                        )
                        Text(
                            "Scan to deposit funds",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- Main Receiver Card matching Image 1 ---
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF0D1421).copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Network Selector
                        val selectedChainName = getChainNameByCode(receiveState.selectedChainCode)
                        val selectedChainIcon = getChainIconByCode(receiveState.selectedChainCode)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF030712).copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showChainSelector = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = selectedChainIcon),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    selectedChainName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ExpandMore, null, tint = Color.White.copy(alpha = 0.3f))
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // QR Code Container
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            modifier = Modifier.size(260.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(20.dp)) {
                                qrBitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "QR Code",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } ?: CircularProgressIndicator(color = Color.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Address Box
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF03080F),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = address,
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                fontFamily = PulsarTypography.CyberLabel.fontFamily
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                onClick = { 
                                    clipboardManager.setText(AnnotatedString(address))
                                    Toast.makeText(context, "Address Copied!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1D293D).copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                modifier = Modifier.weight(1f).height(64.dp)
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
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Copy", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1D293D).copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                modifier = Modifier.weight(1f).height(64.dp)
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
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Share", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Footer Warning Box
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF332B14).copy(alpha = 0.4f), 
                            border = BorderStroke(1.dp, Color(0xFFFF9900).copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Only send ${getChainNameByCode(receiveState.selectedChainCode)} (${getSymbolByCode(receiveState.selectedChainCode)}) to this address.",
                                    color = Color(0xFFFFBB66),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
            }

            if (showChainSelector) {
                ModalBottomSheet(
                    onDismissRequest = { showChainSelector = false },
                    sheetState = sheetState,
                    containerColor = Color(0xFF0D1421),
                    contentColor = Color.White
                ) {
                    val chains = listOf("ETH", "BTC", "SOL", "AVAX", "POL", "BNB")
                    Column(modifier = Modifier.padding(24.dp).padding(bottom = 40.dp)) {
                        Text("Select Network", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(24.dp))
                        chains.forEach { chainCode ->
                            Surface(
                                onClick = {
                                    viewModel.selectChain(chainCode)
                                    showChainSelector = false
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (receiveState.selectedChainCode == chainCode) Color(0xFF1D293D) else Color.Transparent,
                                modifier = Modifier.fillMaxWidth().height(64.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(painter = painterResource(id = getChainIconByCode(chainCode)), null, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(getChainNameByCode(chainCode), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (receiveState.selectedChainCode == chainCode) {
                                        Icon(Icons.Default.Check, null, tint = Color(0xFF00D3F2))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getChainNameByCode(code: String): String = when (code.uppercase()) {
    "BTC" -> "Bitcoin"
    "ETH" -> "Ethereum"
    "SOL" -> "Solana"
    "BNB" -> "BSC"
    "POL" -> "Polygon"
    "AVAX" -> "Avalanche"
    else -> "Ethereum"
}

private fun getSymbolByCode(code: String): String = when (code.uppercase()) {
    "BTC" -> "BTC"
    "ETH" -> "ETH"
    "SOL" -> "SOL"
    "BNB" -> "BNB"
    "POL" -> "POL"
    "AVAX" -> "AVAX"
    else -> "ETH"
}

private fun getChainIconByCode(code: String): Int = when (code.uppercase()) {
    "BTC" -> R.drawable.image_bitcoin
    "ETH" -> R.drawable.image_ethereum
    "SOL" -> R.drawable.image_solana
    "BNB" -> R.drawable.image_ethereum
    "POL" -> R.drawable.image_ethereum
    "AVAX" -> R.drawable.image_ethereum
    else -> R.drawable.image_ethereum
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.MARGIN, 0)
            put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H)
        }
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) { null }
}
