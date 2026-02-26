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

@Composable
fun PinConfirmScreen(
    originalPin: String,
    onConfirmed: (String) -> Unit,
    onBack: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val bg = Color(0xFF051414)

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

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

            // Step indicators — step 2 active
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .width(if (i == 1) 24.dp else 8.dp)
                            .height(8.dp)
                            .background(
                                if (i <= 1) PulsarColors.PrimaryDark else PulsarColors.PrimaryDark.copy(alpha = 0.25f),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "STEP 2 OF 3",
                style = PulsarTypography.CyberLabel,
                color = PulsarColors.PrimaryDark,
                fontSize = 10.sp,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "CONFIRM PIN",
                style = PulsarTypography.CyberLabel,
                color = PulsarColors.PrimaryDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Verify Your PIN",
                style = PulsarTypography.Typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Re-enter your 6-digit PIN to confirm.",
                style = PulsarTypography.Typography.bodyMedium,
                color = PulsarColors.TextSecondaryLight,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            PulsarMotion.ShakeAnimation(trigger = isError) {
                PulsarComponents.PinIndicator(
                    length = 6,
                    filledCount = pin.length,
                    isError = isError
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            PulsarKeyboard(
                modifier = Modifier.padding(bottom = 24.dp),
                onDigit = { digit ->
                    if (pin.length < 6) {
                        pin += digit
                        if (pin.length == 6) {
                            if (pin == originalPin) onConfirmed(pin)
                            else isError = true
                        }
                    }
                },
                onDelete = {
                    if (pin.isNotEmpty()) pin = pin.dropLast(1)
                    isError = false
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



