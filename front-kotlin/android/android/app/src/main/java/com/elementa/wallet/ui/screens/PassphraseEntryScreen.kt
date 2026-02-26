package com.elementa.wallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.*

@Composable
fun PassphraseEntryScreen(
    onBack: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val bg = Color(0xFF051414)
    val cardBg = Color(0xFF0B1E1E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
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

            Spacer(modifier = Modifier.height(20.dp))

            // Lock icon with glow
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    PulsarColors.PrimaryDark.copy(alpha = 0.20f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PulsarColors.PrimaryDark.copy(alpha = 0.12f),
                    modifier = Modifier.size(62.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PulsarColors.PrimaryDark,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "BIP-39 PASSPHRASE",
                style = PulsarTypography.CyberLabel,
                color = PulsarColors.PrimaryDark,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "13th Word Security",
                style = PulsarTypography.Typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "An optional passphrase creates a completely separate vault for enhanced security.",
                style = PulsarTypography.Typography.bodyMedium,
                color = PulsarColors.TextSecondaryLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Info box
            Surface(
                color = PulsarColors.PrimaryDark.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, PulsarColors.PrimaryDark.copy(alpha = 0.18f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PulsarColors.PrimaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Entering a different passphrase opens a different wallet. Keep this private — Pulsar cannot recover it if lost.",
                        style = PulsarTypography.Typography.bodySmall,
                        color = PulsarColors.TextSecondaryDark,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Input area
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Passphrase",
                    style = PulsarTypography.Typography.labelSmall,
                    color = PulsarColors.TextSecondaryDark,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    placeholder = {
                        Text(
                            "Secret 13th word…",
                            color = PulsarColors.PrimaryDark.copy(alpha = 0.28f)
                        )
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Hide" else "Show",
                                tint = PulsarColors.PrimaryDark.copy(alpha = 0.6f)
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PulsarColors.PrimaryDark.copy(alpha = 0.05f),
                        unfocusedContainerColor = PulsarColors.PrimaryDark.copy(alpha = 0.05f),
                        focusedBorderColor = PulsarColors.PrimaryDark,
                        unfocusedBorderColor = PulsarColors.PrimaryDark.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = PulsarColors.PrimaryDark
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Case-sensitive  •  Leave blank to skip",
                    style = PulsarTypography.Typography.labelSmall,
                    color = PulsarColors.TextSecondaryDark.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Divider line
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(PulsarColors.PrimaryDark.copy(alpha = 0.1f))
                    )
                    Text(
                        text = "END-TO-END ENCRYPTED",
                        style = PulsarTypography.CyberLabel,
                        color = PulsarColors.TextSecondaryDark,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(PulsarColors.PrimaryDark.copy(alpha = 0.1f))
                    )
                }

                PulsarComponents.PulsarButton(
                    text = "Confirm & Open Vault",
                    onClick = { onConfirm(passphrase) },
                    modifier = Modifier.fillMaxWidth(),
                    glow = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "By continuing, you acknowledge that Pulsar cannot recover this passphrase if lost.",
                    style = PulsarTypography.Typography.bodySmall,
                    color = PulsarColors.TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 11.sp
                )
            }
        }
    }
}
