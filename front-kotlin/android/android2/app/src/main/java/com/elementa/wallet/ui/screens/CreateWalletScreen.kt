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

    PulsarBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Text(
                "PULSAR",
                style = PulsarTypography.CyberLabel,
                color = PulsarColors.PrimaryDark,
                letterSpacing = 4.sp,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Title and Description
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Create Your Wallet",
                    style = PulsarTypography.Typography.headlineLarge,
                    color = PulsarColors.TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Set up your wallet profile with a unique name and security password",
                    style = PulsarTypography.Typography.bodyMedium,
                    color = PulsarColors.TextSecondaryDark,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Form Card
            PulsarComponents.PulsarCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Wallet Name Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Wallet Name",
                            style = PulsarTypography.Typography.labelSmall,
                            color = PulsarColors.PrimaryDark,
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
                            style = PulsarTypography.Typography.labelSmall,
                            color = PulsarColors.PrimaryDark,
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
                        shape = RoundedCornerShape(12.dp),
                        color = PulsarColors.PrimaryDark.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, PulsarColors.PrimaryDark.copy(alpha = 0.3f)),
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
                                color = PulsarColors.TextSecondaryDark,
                                style = PulsarTypography.Typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Continue Button
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

                    // Back Button
                    PulsarComponents.PulsarOutlinedButton(
                        text = "Back",
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
