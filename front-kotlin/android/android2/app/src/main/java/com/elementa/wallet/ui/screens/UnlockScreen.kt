package com.elementa.wallet.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.PulsarBackground
import com.elementa.wallet.ui.designsystem.PulsarColors
import com.elementa.wallet.ui.designsystem.PulsarComponents
import com.elementa.wallet.ui.designsystem.PulsarTypography
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

    PulsarBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.2f))

            // Logo
            PulsarComponents.PulsarLogo(pulse = true, size = 120.dp)
            
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Welcome back",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Enter your PIN to unlock vault",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // PIN Dots or Numeric Input
            // For pixel perfect, let's use a themed numeric field
            OutlinedTextField(
                value = pin,
                onValueChange = { input ->
                    if (input.length <= 6 && input.all { it.isDigit() } && !isSubmitting) {
                        pin = input
                        if (input.length == 6) {
                            isSubmitting = true
                            viewModel.attemptUnlock(input)
                        }
                    }
                },
                modifier = Modifier.width(240.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    letterSpacing = 8.sp,
                    color = Color.White
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PulsarColors.PrimaryDark,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = Color(0xFF0D1421).copy(alpha = 0.5f),
                    unfocusedContainerColor = Color(0xFF0D1421).copy(alpha = 0.5f),
                    cursorColor = PulsarColors.PrimaryDark
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (bioMetricsEnabled) {
                IconButton(
                    onClick = { viewModel.authenticateBiometrically() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            TextButton(onClick = onForgotPassword) {
                Text(
                    "FORGOT PIN?",
                    style = PulsarTypography.CyberLabel,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 2.sp
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
// Renders a single PIN dot with animated state.
private fun PinDot(isFilled: Boolean, isError: Boolean) {
    val size by animateDpAsState(
        targetValue = if (isFilled) 16.dp else 12.dp,
        animationSpec = tween(200)
    )
    val color by animateColorAsState(
        targetValue = when {
            isError -> PulsarColors.DangerRed
            isFilled -> PulsarColors.PrimaryDark
            else -> PulsarColors.BorderSubtleDark
        },
        animationSpec = tween(200)
    )

    Box(
        modifier = Modifier
            .size(16.dp)
            .wrapContentSize(Alignment.Center)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(color, CircleShape)
        )
    }
}

