package com.elementa.wallet.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.*
import kotlin.math.roundToInt

@Composable
fun SendConfirmationScreen(
    amount: String,
    recipient: String,
    onBack: () -> Unit,
    onConfirmed: () -> Unit
) {
    PulsarBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            // Semi-transparent overlay to simulate bottom sheet over content
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                    .background(PulsarColors.BackgroundDark.copy(alpha = 0.95f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                    .padding(horizontal = 24.dp)
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .width(48.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .align(Alignment.CenterHorizontally)
                )

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Confirm Transaction",
                        style = PulsarTypography.Typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f))
                    }
                }

                // Transaction Amount
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Ξ", color = Color.White, fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "$amount ETH",
                            style = PulsarTypography.Typography.displayLarge,
                            color = PulsarColors.PrimaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp,
                            letterSpacing = (-1).sp
                        )
                    }
                    Text(
                        "≈ $1,240.50 USD",
                        style = PulsarTypography.Typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Details Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.03f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PulsarColors.PrimaryDark.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PulsarColors.PrimaryDark, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("FROM", style = PulsarTypography.CyberLabel, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                Text("Pulsar Vault · 0x82...12", style = PulsarTypography.Typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Box(modifier = Modifier.padding(start = 20.dp).height(24.dp).width(1.dp).background(Color.White.copy(alpha = 0.1f)))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("TO", style = PulsarTypography.CyberLabel, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                Text("External Wallet · $recipient", style = PulsarTypography.Typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fees
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.02f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Network Fee (Gas)", style = PulsarTypography.Typography.bodySmall, color = Color.White.copy(alpha = 0.4f))
                            Text("0.002 ETH (~$5.00)", style = PulsarTypography.Typography.bodySmall, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                        Divider(color = Color.White.copy(alpha = 0.05f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Total Amount", style = PulsarTypography.Typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("0.502 ETH", style = PulsarTypography.Typography.titleLarge, color = PulsarColors.PrimaryDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Security Note
                Box(
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "THIS ACTION IS IRREVERSIBLE",
                            style = PulsarTypography.CyberLabel,
                            color = Color(0xFFF59E0B),
                            fontSize = 9.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Slide to Send
                SlideToSendButton(onConfirmed = onConfirmed)

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun SlideToSendButton(onConfirmed: () -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxOffset = 280f // Adjust based on width
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(CircleShape)
            .background(PulsarColors.PrimaryDark.copy(alpha = 0.05f))
            .border(1.dp, PulsarColors.PrimaryDark.copy(alpha = 0.2f), CircleShape)
            .padding(4.dp)
    ) {
        // Track Text
        Text(
            "SLIDE TO SEND FUNDS",
            modifier = Modifier.align(Alignment.Center),
            style = PulsarTypography.CyberLabel,
            color = PulsarColors.PrimaryDark,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold
        )

        // Handle
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .size(64.dp)
                .clip(CircleShape)
                .background(PulsarColors.PrimaryDark)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > maxOffset * 0.8f) {
                                offsetX = maxOffset
                                onConfirmed()
                            } else {
                                offsetX = 0f
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(0f, maxOffset)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.KeyboardDoubleArrowRight, contentDescription = null, tint = PulsarColors.BackgroundDark, modifier = Modifier.size(28.dp))
        }
    }
}
