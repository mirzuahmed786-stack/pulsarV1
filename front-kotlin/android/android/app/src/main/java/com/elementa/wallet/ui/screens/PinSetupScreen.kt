package com.elementa.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.*
import kotlinx.coroutines.delay

@Composable
fun PinSetupScreen(
    onPinEntered: (String) -> Unit,
    onBack: () -> Unit
) {
    // Use String list to match PulsarKeyboard's onDigit type
    var pinDigits by remember { mutableStateOf(listOf<String>()) }
    val pin by remember { derivedStateOf { pinDigits.joinToString("") } }
    
    // Flag to prevent multiple submissions
    var isSubmitting by remember { mutableStateOf(false) }
    
    val bg = Color(0xFF051414)

    // Handle PIN completion with debounce to prevent the 5-digit hang bug
    LaunchedEffect(pin) {
        if (pin.length == 6 && !isSubmitting) {
            isSubmitting = true
            delay(100) // Small delay for UI feedback
            onPinEntered(pin)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = PulsarColors.PrimaryDark.copy(alpha = 0.08f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("←", color = PulsarColors.PrimaryDark, fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Lock icon with glow
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    PulsarColors.PrimaryDark.copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PulsarColors.PrimaryDark.copy(alpha = 0.12f),
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PulsarColors.PrimaryDark,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                "SET PIN",
                style = PulsarTypography.CyberLabel,
                color = PulsarColors.PrimaryDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Secure Your Vault",
                style = PulsarTypography.Typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Create a 6-digit PIN to protect access to your wallet.",
                style = PulsarTypography.Typography.bodyMedium,
                color = PulsarColors.TextSecondaryLight,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            PulsarComponents.PinIndicator(
                length = 6,
                filledCount = pinDigits.size
            )

            Spacer(modifier = Modifier.weight(1f))

            PulsarKeyboard(
                modifier = Modifier.padding(bottom = 24.dp),
                onDigit = { digit ->
                    // Fix for the 5-digit hang bug: use list-based state updates
                    // and prevent input when already submitting
                    if (pinDigits.size < 6 && !isSubmitting) {
                        pinDigits = pinDigits + digit
                    }
                },
                onDelete = { 
                    if (pinDigits.isNotEmpty() && !isSubmitting) {
                        pinDigits = pinDigits.dropLast(1)
                    }
                },
                bottomLeftSlot = {
                    PulsarKey(
                        label = "Back",
                        size = 72.dp,
                        labelColor = PulsarColors.TextSecondaryDark,
                        onClick = onBack,
                        modifier = Modifier
                    )
                }
            )
        }
    }
}



