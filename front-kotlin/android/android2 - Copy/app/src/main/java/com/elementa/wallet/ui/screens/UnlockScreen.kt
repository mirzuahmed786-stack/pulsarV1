package com.elementa.wallet.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.R
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.viewmodel.VaultUiState
import com.elementa.wallet.viewmodel.VaultViewModel
import kotlinx.coroutines.delay

@Composable
fun UnlockScreen(
    viewModel: VaultViewModel,
    onUnlocked: () -> Unit,
    onForgotPassword: () -> Unit = {}
) {
    var pin by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val bioMetricsEnabled by viewModel.bioMetricsEnabled.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.lock()
        viewModel.preloadPinCache()
        
        // Auto-trigger biometric if enabled
        if (bioMetricsEnabled) {
            viewModel.authenticateBiometrically()
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is VaultUiState.Unlocked -> onUnlocked()
            is VaultUiState.Error -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(800)
                pin = ""
                isSubmitting = false
            }
            is VaultUiState.FactoryResetRequired -> onForgotPassword()
            else -> Unit
        }
    }

    // Dashboard-style Background matching HomeScreen
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF030712))) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.wallet_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.4f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.15f))

            // Premium Logo from Pulsar design system
            PulsarComponents.PulsarLogo(pulse = true, size = 110.dp)
            
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Secure Vault",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter your 6-digit PIN to unlock",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // PIN Dots Area (Premium Look)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                repeat(6) { index ->
                    val isFilled = index < pin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(
                                color = if (isFilled) Color(0xFF00D3F2) else Color.White.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .border(
                                1.dp,
                                if (isFilled) Color(0xFF00D3F2) else Color.White.copy(alpha = 0.2f),
                                CircleShape
                            )
                    )
                }
            }

            // Numeric Keypad (Premium Glass Style)
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "BIO", "0", "DEL")
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keys.chunked(3).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        row.forEach { key ->
                            if (key == "BIO" && !bioMetricsEnabled) {
                                Spacer(modifier = Modifier.size(72.dp))
                            } else {
                                Surface(
                                    onClick = {
                                        when (key) {
                                            "DEL" -> {
                                                if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            }
                                            "BIO" -> {
                                                viewModel.authenticateBiometrically()
                                            }
                                            else -> {
                                                if (pin.length < 6 && !isSubmitting) {
                                                    pin += key
                                                    if (pin.length == 6) {
                                                        isSubmitting = true
                                                        viewModel.attemptUnlock(pin)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    shape = CircleShape,
                                    color = if (key == "BIO") Color(0xFF00D3F2).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        when (key) {
                                            "DEL" -> Icon(painterResource(R.drawable.back), null, tint = Color.White, modifier = Modifier.size(24.dp))
                                            "BIO" -> Icon(Icons.Default.Fingerprint, null, tint = Color(0xFF00D3F2), modifier = Modifier.size(32.dp))
                                            else -> Text(key, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            TextButton(
                onClick = onForgotPassword,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    "FORGOT PIN?",
                    style = PulsarTypography.CyberLabel,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 2.sp,
                    fontSize = 12.sp
                )
            }
        }
    }
}
