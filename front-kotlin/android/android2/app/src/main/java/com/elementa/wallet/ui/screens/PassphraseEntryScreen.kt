package com.elementa.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.R
import com.elementa.wallet.ui.designsystem.OnboardingProgressBar
import com.elementa.wallet.ui.designsystem.PulsarOnboardingBackground

@Composable
fun PassphraseEntryScreen(
    onBack: () -> Unit,
    onConfirmed: () -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    
    val ONBOARDING_STEP = 0.9f
    val pulsarBlue = Color(0xFF0092B8)

    PulsarOnboardingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            OnboardingProgressBar(
                progress = ONBOARDING_STEP,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Spacer(modifier = Modifier.weight(0.12f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Add Passphrase",
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "An optional 13th word for\nextra security (BIP-39).",
                    fontSize = 17.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Input Field
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Passphrase (Optional)",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Secret word...", color = Color.White.copy(alpha = 0.3f)) },
                        visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = pulsarBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = pulsarBlue,
                            focusedContainerColor = Color(0xFF0D1421).copy(alpha = 0.5f),
                            unfocusedContainerColor = Color(0xFF0D1421).copy(alpha = 0.5f)
                        ),
                        trailingIcon = {
                            IconButton(onClick = { pinVisible = !pinVisible }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.eye__button),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Leave blank if you don't want a passphrase.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.18f))

            Surface(
                onClick = onConfirmed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(18.dp),
                color = pulsarBlue
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Finish Setup",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
