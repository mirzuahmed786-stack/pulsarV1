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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.viewmodel.VaultUiState
import com.elementa.wallet.viewmodel.VaultViewModel
import kotlinx.coroutines.delay

@Composable
// Displays the unlock flow for the vault.
fun UnlockScreen(
    viewModel: VaultViewModel,
    onUnlocked: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    // Prevent multiple rapid submissions / race that caused the 5-digit hang
    var isSubmitting by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val bioMetricsEnabled by viewModel.bioMetricsEnabled.collectAsState()
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val keypadKeySize = if (screenHeight < 700) 64.dp else 72.dp
    val keypadRowSpacing = if (screenHeight < 700) 14.dp else 20.dp

    LaunchedEffect(Unit) {
        viewModel.lock()
        viewModel.preloadPinCache()
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is VaultUiState.Unlocked -> onUnlocked()
            is VaultUiState.Error -> {
                isError = true
                // Allow re-entry after a short delay and clear submitting flag
                delay(800)
                pin = ""
                isError = false
                isSubmitting = false
            }
            else -> Unit
        }
    }

    PulsarBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo Header
            PulsarComponents.PulsarLogo(pulse = true)
            Text(
                "Secure Vault & Wallet",
                style = PulsarTypography.Typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "Welcome back",
                style = PulsarTypography.Typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp
            )
            Text(
                text = "Unlock your vault to continue",
                style = PulsarTypography.Typography.bodyLarge,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // PIN dots — always shown. Biometric is provided as the keypad bottom-left slot
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(100.dp)
            ) {
                repeat(6) { index ->
                    PinDot(
                        isFilled = index < pin.length,
                        isError = isError
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Keypad Section
                PulsarKeyboard(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    keySize = keypadKeySize,
                    rowSpacing = keypadRowSpacing,
                    onDigit = { digit ->
                        if (!isSubmitting && pin.length < 6) {
                            pin += digit
                            if (pin.length == 6) {
                                isSubmitting = true
                                viewModel.attemptUnlock(pin)
                            }
                        }
                    },
                    onDelete = { if (!isSubmitting && pin.isNotEmpty()) pin = pin.dropLast(1) },
                    bottomLeftSlot = if (bioMetricsEnabled) ({
                        Surface(
                            onClick = { viewModel.authenticateBiometrically() },
                            shape = RoundedCornerShape(18.dp),
                            color = PulsarColors.PanelDark.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, PulsarColors.BorderSubtleDark),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Biometric",
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }) else null
                )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = { },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, PulsarColors.BorderSubtleDark)
                ) {
                    Text(
                        "FORGOT PIN?",
                        style = PulsarTypography.CyberLabel,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
// Renders the biometric unlock button and rings.
private fun BiometricUnlockCircle(onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
        // Rings
        Box(modifier = Modifier.size(160.dp).border(1.dp, PulsarColors.PrimaryDark.copy(alpha = 0.1f), CircleShape))
        Box(modifier = Modifier.size(120.dp).border(2.dp, PulsarColors.PrimaryDark.copy(alpha = 0.2f), CircleShape))
        
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = PulsarColors.PrimaryDark.copy(alpha = 0.05f),
            border = androidx.compose.foundation.BorderStroke(1.dp, PulsarColors.PrimaryDark.copy(alpha = 0.3f)),
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = PulsarColors.PrimaryDark,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        Text(
            "TOUCH SENSOR TO UNLOCK",
            style = PulsarTypography.CyberLabel,
            color = PulsarColors.PrimaryDark,
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
        )
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
            .size(16.dp) // Fixed outer size to prevent layout jump
            .wrapContentSize(Alignment.Center)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(color, CircleShape)
                .then(
                    if (!isFilled) Modifier.border(1.dp, PulsarColors.BorderStrongDark, CircleShape)
                    else Modifier
                )
        )
    }
}

