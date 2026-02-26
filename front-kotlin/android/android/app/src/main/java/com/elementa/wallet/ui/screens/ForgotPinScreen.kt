package com.elementa.wallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.viewmodel.VaultViewModel

/**
 * Forgot PIN Flow - Factory Reset Protocol
 * 
 * SECURITY CONTRACT:
 * - 2-step confirmation to prevent accidental wipes
 * - Step 1: Warning modal with clear consequences
 * - Step 2: User must type exact phrase "RESET MY WALLET"
 * - On confirm: Wipes all vault data (PIN hash, encrypted backups, failure trackers)
 * - User can restore from encrypted backup file OR create new wallet after reset
 * 
 * WORKFLOW:
 * UnlockScreen → "FORGOT PIN?" → ForgotPinScreen(step=1) → (confirm) → ForgotPinScreen(step=2) → Factory Reset → WelcomeScreen
 */
@Composable
fun ForgotPinScreen(
    viewModel: VaultViewModel,
    onReset: () -> Unit,  // Navigate back to Welcome/Onboarding
    onCancel: () -> Unit  // Return to UnlockScreen
) {
    var step by remember { mutableStateOf(1) }
    var confirmationInput by remember { mutableStateOf("") }
    
    val hapticFeedback = LocalHapticFeedback.current

    PulsarBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Back button (only on step 1)
            if (step == 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PulsarColors.SurfaceDark.copy(alpha = 0.4f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Text("←", color = Color.White, fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            when (step) {
                1 -> {
                    // WARNING STEP
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Warning Icon
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                                .border(2.dp, Color(0xFFEF4444).copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            "Reset Your Wallet?",
                            style = PulsarTypography.Typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = PulsarColors.SurfaceDark.copy(alpha = 0.6f)
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                WarningItem("❌ All wallet keys will be permanently deleted")
                                WarningItem("❌ Your PIN will be erased")
                                WarningItem("❌ Transaction history will be lost")
                                WarningItem("✓ This action CANNOT be undone")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            "You can restore your wallet ONLY if you have your encrypted backup file.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Buttons
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    step = 2
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444)
                                )
                            ) {
                                Text(
                                    "I Understand, Continue",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Button(
                                onClick = onCancel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PulsarColors.SurfaceDark
                                )
                            ) {
                                Text(
                                    "Cancel",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                
                2 -> {
                    // CONFIRMATION STEP
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Final Confirmation",
                            style = PulsarTypography.Typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            "Type the exact phrase below to confirm:",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = PulsarColors.PrimaryDark.copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(1.dp, PulsarColors.PrimaryDark.copy(alpha = 0.3f))
                        ) {
                            Text(
                                "RESET MY WALLET",
                                color = PulsarColors.PrimaryDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Text input
                        OutlinedTextField(
                            value = confirmationInput,
                            onValueChange = { confirmationInput = it.uppercase() },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Type here...", color = Color.White.copy(alpha = 0.4f))
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PulsarColors.PrimaryDark,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Execute button
                        Button(
                            enabled = confirmationInput == "RESET MY WALLET",
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.factoryReset()
                                onReset()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444),
                                disabledContainerColor = Color(0xFFEF4444).copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                "Execute Factory Reset",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                step = 1
                                confirmationInput = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PulsarColors.SurfaceDark
                            )
                        ) {
                            Text(
                                "Go Back",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WarningItem(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}
