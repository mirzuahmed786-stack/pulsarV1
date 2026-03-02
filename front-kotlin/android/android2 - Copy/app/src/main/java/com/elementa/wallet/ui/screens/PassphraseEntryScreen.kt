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
    val pulsarBlue = Color(0xFF00D3F2)

    PulsarOnboardingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OnboardingProgressBar(
                progress = ONBOARDING_STEP,
                modifier = Modifier.padding(vertical = 12.dp).width(200.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Add Passphrase",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "A mandatory passpharse for extra security. Think of it as your 13th word.",
                fontSize = 17.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Input Field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Passphrase",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    placeholder = { Text("Secret word...", color = Color.White.copy(alpha = 0.2f)) },
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = pulsarBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = pulsarBlue,
                        focusedContainerColor = Color(0xFF03080F).copy(alpha = 0.6f),
                        unfocusedContainerColor = Color(0xFF03080F).copy(alpha = 0.6f)
                    ),
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(
                                painter = painterResource(id = R.drawable.eye__button),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "This word cannot be recovered if lost.",
                    fontSize = 12.sp,
                    color = Color(0xFFFF3333).copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            Surface(
                onClick = { if (passphrase.isNotEmpty()) onConfirmed() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                enabled = passphrase.isNotEmpty(),
                shape = RoundedCornerShape(20.dp),
                color = if (passphrase.isNotEmpty()) pulsarBlue else Color(0xFF1D293D).copy(alpha = 0.4f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Finish Setup",
                        color = if (passphrase.isNotEmpty()) Color.Black else Color.White.copy(alpha = 0.3f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            TextButton(onClick = onBack) {
                Text("Back", color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
