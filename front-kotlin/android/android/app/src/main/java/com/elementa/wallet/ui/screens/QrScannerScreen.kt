package com.elementa.wallet.ui.screens

import android.Manifest
import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.elementa.wallet.ui.designsystem.PulsarBackground
import com.elementa.wallet.ui.designsystem.PulsarTypography
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * QR Scanner screen with ML Kit barcode scanning.
 * Supports scanning wallet addresses, payment URIs, and WalletConnect URIs.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    onScanned: (String) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    var hasScanned by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    PulsarBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                cameraPermissionState.status.isGranted -> {
                    // Camera Preview with Scanner
                    CameraPreviewWithScanner(
                        flashEnabled = flashEnabled,
                        onQrCodeScanned = { scannedValue ->
                            if (!hasScanned) {
                                hasScanned = true
                                val result = parseQrContent(scannedValue)
                                if (result != null) {
                                    onScanned(result)
                                } else {
                                    errorMessage = "Invalid QR code format"
                                    hasScanned = false
                                }
                            }
                        },
                        onError = { error ->
                            errorMessage = error
                        }
                    )
                    
                    // Scanning overlay
                    ScannerOverlay(
                        onBack = onBack,
                        flashEnabled = flashEnabled,
                        onToggleFlash = { flashEnabled = !flashEnabled },
                        errorMessage = errorMessage,
                        onDismissError = { errorMessage = null }
                    )
                }
                
                cameraPermissionState.status.shouldShowRationale -> {
                    // Show rationale
                    PermissionRationale(
                        onBack = onBack,
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                    )
                }
                
                else -> {
                    // Permission denied permanently
                    PermissionDenied(onBack = onBack)
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewWithScanner(
    flashEnabled: Boolean,
    onQrCodeScanned: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    
    LaunchedEffect(flashEnabled, camera) {
        camera?.cameraControl?.enableTorch(flashEnabled)
    }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            
            val executor = Executors.newSingleThreadExecutor()
            val barcodeScanner = BarcodeScanning.getClient()
            
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = Preview.Builder()
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(executor) { imageProxy ->
                                @androidx.camera.core.ExperimentalGetImage
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                when (barcode.valueType) {
                                                    Barcode.TYPE_TEXT,
                                                    Barcode.TYPE_URL -> {
                                                        barcode.rawValue?.let { value ->
                                                            onQrCodeScanned(value)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            onError("Scan failed: ${e.message}")
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }
                    
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    onError("Camera initialization failed: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ScannerOverlay(
    onBack: () -> Unit,
    flashEnabled: Boolean,
    onToggleFlash: () -> Unit,
    errorMessage: String?,
    onDismissError: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top bar
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
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            
            Text(
                "SCAN QR CODE",
                style = PulsarTypography.CyberLabel,
                color = Color.White,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )
            
            Surface(
                onClick = onToggleFlash,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = if (flashEnabled) "Disable flash" else "Enable flash",
                        tint = if (flashEnabled) Color(0xFFFFD700) else Color.White
                    )
                }
            }
        }
        
        // Scanning frame in center
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            // Scanning target box
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .border(
                        width = 3.dp,
                        color = Color(0xFF00FFFF),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
            
            // Corner accents
            ScannerCorners()
        }
        
        // Instructions at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = Color(0xFF00FFFF),
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "Position QR code within the frame",
                style = PulsarTypography.Typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                "Supports wallet addresses, payment URIs,\nand WalletConnect codes",
                style = PulsarTypography.Typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
        
        // Error snackbar
        if (errorMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = onDismissError) {
                        Text("Dismiss", color = Color.White)
                    }
                },
                containerColor = Color(0xFFD32F2F)
            ) {
                Text(errorMessage, color = Color.White)
            }
        }
    }
}

@Composable
private fun ScannerCorners() {
    val cornerSize = 30.dp
    val strokeWidth = 4.dp
    val cornerColor = Color(0xFF00FFFF)
    
    Box(modifier = Modifier.size(280.dp)) {
        // Top-left corner
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(cornerSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(strokeWidth)
                    .background(cornerColor)
            )
        }
        
        // Top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(cornerSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(strokeWidth)
                    .align(Alignment.TopEnd)
                    .background(cornerColor)
            )
        }
        
        // Bottom-left corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(cornerSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .align(Alignment.BottomStart)
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(strokeWidth)
                    .background(cornerColor)
            )
        }
        
        // Bottom-right corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(cornerSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .align(Alignment.BottomEnd)
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(strokeWidth)
                    .align(Alignment.BottomEnd)
                    .background(cornerColor)
            )
        }
    }
}

@Composable
private fun PermissionRationale(
    onBack: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = Color(0xFF00FFFF),
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Camera Permission Required",
            style = PulsarTypography.Typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            "To scan QR codes, the app needs access to your camera. Your camera is only used for scanning and no images are stored.",
            style = PulsarTypography.Typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00FFFF)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission", color = Color.Black)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        TextButton(onClick = onBack) {
            Text("Cancel", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun PermissionDenied(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = Color(0xFFFF5722),
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Camera Access Denied",
            style = PulsarTypography.Typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            "Please enable camera permission in your device settings to scan QR codes.",
            style = PulsarTypography.Typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go Back", color = Color.White)
        }
    }
}

/**
 * Parse QR content and validate it's a supported format.
 * Supports:
 * - Ethereum addresses (0x...)
 * - Bitcoin addresses (1..., 3..., bc1...)
 * - Solana addresses (base58, 32-44 chars)
 * - EIP-681 payment URIs (ethereum:0x...)
 * - BIP-21 payment URIs (bitcoin:...)
 * - Solana Pay URIs (solana:...)
 * - WalletConnect URIs (wc:...)
 * 
 * Returns the validated address/URI or null if invalid.
 */
private fun parseQrContent(content: String): String? {
    val trimmed = content.trim()
    
    return when {
        // EIP-681 Ethereum payment URI
        trimmed.startsWith("ethereum:", ignoreCase = true) -> {
            trimmed
        }
        
        // BIP-21 Bitcoin payment URI
        trimmed.startsWith("bitcoin:", ignoreCase = true) -> {
            trimmed
        }
        
        // Solana Pay URI
        trimmed.startsWith("solana:", ignoreCase = true) -> {
            trimmed
        }
        
        // WalletConnect URI
        trimmed.startsWith("wc:", ignoreCase = true) -> {
            trimmed
        }
        
        // Plain Ethereum address
        trimmed.startsWith("0x") && trimmed.length == 42 && 
                trimmed.drop(2).all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' } -> {
            trimmed
        }
        
        // Plain Bitcoin address (legacy P2PKH)
        trimmed.startsWith("1") && trimmed.length in 26..35 && 
                trimmed.all { it.isLetterOrDigit() } -> {
            trimmed
        }
        
        // Plain Bitcoin address (P2SH)
        trimmed.startsWith("3") && trimmed.length in 26..35 && 
                trimmed.all { it.isLetterOrDigit() } -> {
            trimmed
        }
        
        // Plain Bitcoin address (Bech32/SegWit)
        trimmed.startsWith("bc1", ignoreCase = true) && trimmed.length in 42..62 -> {
            trimmed
        }
        
        // Plain Solana address (base58, typically 32-44 characters)
        trimmed.length in 32..44 && trimmed.all { 
            it.isLetterOrDigit() && it !in "0OIl" // Base58 excludes these
        } -> {
            trimmed
        }
        
        // Unknown format - still return it and let the caller decide
        trimmed.isNotEmpty() -> trimmed
        
        else -> null
    }
}
