package com.elementa.wallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.*

@Composable
fun CreateWalletScreen(onBack: () -> Unit, onContinue: (String) -> Unit) {
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nicknameError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    PulsarOnboardingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Premium Logo from Pulsar design system
            PulsarComponents.PulsarLogo(pulse = true, size = 100.dp)
            
            Spacer(modifier = Modifier.height(32.dp))

            // Title and Description
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Create Your Wallet",
                    fontSize = 32.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Set up your wallet profile with a unique\nname and security password",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Form Card
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFF0D1421).copy(alpha = 0.6f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Wallet Name Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Wallet Name",
                            fontSize = 14.sp,
                            color = Color(0xFF00D3F2),
                            fontWeight = FontWeight.SemiBold
                        )
                        PulsarComponents.DarkThemedTextField(
                            value = nickname,
                            onValueChange = {
                                nickname = it
                                nicknameError = false
                            },
                            placeholder = "e.g., My Main Vault",
                            isError = nicknameError,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Security Password Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Security Password (min 6 chars)",
                            fontSize = 14.sp,
                            color = Color(0xFF00D3F2),
                            fontWeight = FontWeight.SemiBold
                        )
                        PulsarComponents.DarkThemedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = false
                            },
                            placeholder = "Enter security password",
                            isError = passwordError,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Info Box
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF00D3F2).copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color(0xFF00D3F2).copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🛡️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Your password secures your keys locally on this device.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Buttons
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PulsarComponents.PulsarButton(
                            text = "Continue",
                            onClick = {
                                if (nickname.isBlank()) nicknameError = true
                                else if (password.length < 6) passwordError = true
                                else onContinue(password)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = nickname.isNotBlank() && password.length >= 6,
                            glow = true
                        )

                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
