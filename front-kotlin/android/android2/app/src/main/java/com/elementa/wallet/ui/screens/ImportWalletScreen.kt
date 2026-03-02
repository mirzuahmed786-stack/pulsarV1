package com.elementa.wallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.R
import com.elementa.wallet.ui.designsystem.PulsarOnboardingBackground

@Composable
fun ImportWalletScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    var phraseText by remember { mutableStateOf("") }
    val wordCount = remember(phraseText) {
        phraseText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
    val canRestore = wordCount == 12 || wordCount == 24
    val pulsarBlue = Color(0xFF0092B8)

    PulsarOnboardingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Logo Header - Blue Theme
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(pulsarBlue.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(pulsarBlue.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.import_wallet),
                        contentDescription = null,
                        tint = pulsarBlue,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Import Wallet",
                fontSize = 32.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Enter your 12 or 24-word recovery phrase to\nrestore your wallet.",
                fontSize = 17.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Text Field for Phrase
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = phraseText,
                    onValueChange = { phraseText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    placeholder = {
                        Text(
                            "word1 word2 word3 ...",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = pulsarBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = pulsarBlue,
                        focusedContainerColor = Color(0xFF0D1421).copy(alpha = 0.5f),
                        unfocusedContainerColor = Color(0xFF0D1421).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 26.sp)
                )
                Text(
                    text = "$wordCount words",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Warning Box - Amber Theme
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF3B2E16).copy(alpha = 0.7f),
                border = BorderStroke(1.dp, Color(0xFF755118).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Make sure you are in a safe place and no one is watching. Your recovery phrase grants full access to your funds.",
                        fontSize = 15.sp,
                        color = Color(0xFFFDE68A),
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(
                        "Cancel",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Surface(
                    onClick = { if (canRestore) onContinue() },
                    enabled = canRestore,
                    modifier = Modifier
                        .width(200.dp)
                        .height(64.dp)
                        .alpha(if (canRestore) 1f else 0.4f),
                    shape = RoundedCornerShape(18.dp),
                    color = pulsarBlue
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Restore Wallet",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}