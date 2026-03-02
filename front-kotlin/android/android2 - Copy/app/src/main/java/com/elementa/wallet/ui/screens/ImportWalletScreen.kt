package com.elementa.wallet.ui.screens

import android.widget.Toast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
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
import androidx.compose.ui.platform.LocalContext
import com.elementa.wallet.ui.designsystem.PulsarOnboardingBackground

@Composable
fun ImportWalletScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    val context = LocalContext.current
    var phraseText by remember { mutableStateOf("") }
    val wordCount = remember(phraseText) {
        phraseText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
    val canRestore = wordCount == 12 || wordCount == 24
    val pulsarBlue = Color(0xFF00D3F2)

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
            // --- Corrected Premium Wallet Logo matching welcome page style ---
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0D1421).copy(alpha = 0.8f),
                    modifier = Modifier.size(140.dp), // Reduced size
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Pulsar Logo",
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp)) // Reduced spacing

            Text(
                text = "Import Wallet",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp)) // Reduced spacing
            Text(
                text = "Enter your 12 or 24-word recovery phrase\nto restore your digital fortress.",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp)) // Reduced spacing

            // Text Field Area
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = phraseText,
                    onValueChange = { phraseText = it },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    placeholder = {
                        Text("word1 word2 word3 ...", color = Color.White.copy(alpha = 0.2f), fontSize = 16.sp)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = pulsarBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = pulsarBlue,
                        focusedContainerColor = Color(0xFF0D1421).copy(alpha = 0.6f),
                        unfocusedContainerColor = Color(0xFF0D1421).copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 26.sp)
                )
                Text(
                    text = "$wordCount words",
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp)) // Reduced spacing

            // Warning Notification
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFF9900).copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color(0xFFFF9900).copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9900), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Make sure you are in a safe place. Your recovery phrase grants full access to your funds.",
                        fontSize = 13.sp,
                        color = Color(0xFFFFBB66),
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp)) // Reduced spacing

            // Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = { 
                        if (canRestore) {
                            Toast.makeText(context, "Wallet Recovered Successfully!", Toast.LENGTH_LONG).show()
                            onContinue() 
                        }
                    },
                    enabled = canRestore,
                    modifier = Modifier.fillMaxWidth().height(64.dp).alpha(if (canRestore) 1f else 0.4f),
                    shape = RoundedCornerShape(20.dp),
                    color = pulsarBlue
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Restore Wallet", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                TextButton(onClick = onBack) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}