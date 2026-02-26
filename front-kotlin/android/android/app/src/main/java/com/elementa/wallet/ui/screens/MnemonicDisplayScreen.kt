package com.elementa.wallet.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.PulsarColors
import com.elementa.wallet.ui.designsystem.PulsarComponents
import com.elementa.wallet.ui.designsystem.PulsarTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MnemonicDisplayScreen(
    onMnemonicSaved: () -> Unit,
    onBack: () -> Unit
) {
    // In production, generate or retrieve from secure storage
    val mnemonic = listOf(
        "abandon", "ability", "able", "about", "above", "absent",
        "absorb", "abstract", "absurd", "abuse", "access", "accident"
    )
    
    val bg = Color(0xFF051414)
    val cardBg = Color(0xFF0B1E1E)
    
    var showCopyWarning by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
            // Back button row
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

            Spacer(modifier = Modifier.height(16.dp))

            // Shield icon with radial glow
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
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
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = PulsarColors.PrimaryDark,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "RECOVERY PHRASE",
                style = PulsarTypography.CyberLabel,
                color = PulsarColors.PrimaryDark,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Backup Your Vault",
                style = PulsarTypography.Typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Write down these 12 words in order and store them somewhere very safe.",
                style = PulsarTypography.Typography.bodyMedium,
                color = PulsarColors.TextSecondaryLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Copy to clipboard button with warning
            Surface(
                onClick = { showCopyWarning = true },
                shape = RoundedCornerShape(12.dp),
                color = PulsarColors.WarningAmber.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    PulsarColors.WarningAmber.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = PulsarColors.WarningAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Copy to Clipboard",
                        style = PulsarTypography.Typography.labelMedium,
                        color = PulsarColors.WarningAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3-column staggered grid for seed words - fixed height
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                userScrollEnabled = false
            ) {
                itemsIndexed(mnemonic) { index, word ->
                    // Staggered effect: odd columns have slight offset
                    val topPadding = if (index % 3 == 1) 6.dp else 0.dp
                    Box(modifier = Modifier.padding(top = topPadding)) {
                        SeedWordCell(index + 1, word, cardBg)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Warning box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PulsarColors.DangerRed.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, PulsarColors.DangerRed.copy(alpha = 0.22f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Never share your recovery phrase. Anyone with these words can access your assets.",
                        style = PulsarTypography.Typography.labelSmall,
                        color = PulsarColors.DangerRed,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            PulsarComponents.PulsarButton(
                text = "I've Secured My Phrase",
                onClick = onMnemonicSaved,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                glow = true
            )
        }
        
        // Copy Warning Dialog
        if (showCopyWarning) {
            AlertDialog(
                onDismissRequest = { showCopyWarning = false },
                containerColor = PulsarColors.PanelDark,
                titleContentColor = Color.White,
                textContentColor = PulsarColors.TextSecondaryDark,
                title = {
                    Text(
                        "Security Warning",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            "Copying your seed phrase to the clipboard is risky:",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("• Other apps may access clipboard data")
                        Text("• The phrase may be synced to cloud services")
                        Text("• Screenshots may capture sensitive data")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Only proceed if you understand these risks and will immediately paste it to a secure location.",
                            color = PulsarColors.WarningAmber
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Copy to clipboard
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Seed Phrase", mnemonic.joinToString(" "))
                            clipboardManager.setPrimaryClip(clip)
                            showCopyWarning = false
                            
                            // Clear clipboard after 60 seconds for security
                            scope.launch {
                                delay(60_000)
                                clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
                            }
                        }
                    ) {
                        Text("Copy Anyway", color = PulsarColors.WarningAmber)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCopyWarning = false }) {
                        Text("Cancel", color = PulsarColors.TextSecondaryDark)
                    }
                }
            )
        }
    }
}

@Composable
private fun SeedWordCell(index: Int, word: String, cardBg: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Text(
                text = index.toString().padStart(2, '0'),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = PulsarColors.PrimaryDark.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = word,
                style = PulsarTypography.Typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

