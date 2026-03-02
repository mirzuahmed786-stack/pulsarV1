package com.elementa.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.OnboardingProgressBar
import com.elementa.wallet.ui.designsystem.PulsarOnboardingBackground
import kotlinx.coroutines.launch

@Composable
fun MnemonicDisplayScreen(
    onMnemonicSaved: () -> Unit,
    onBack: () -> Unit
) {
    val mnemonic = listOf(
        "witch", "collapse", "practice", "feed", "shame", "open",
        "despair", "creek", "road", "again", "ice", "least"
    )
    
    val ONBOARDING_STEP = 0.66f
    val pulsarBlue = Color(0xFF00D3F2)
    var isChecked by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    PulsarOnboardingBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                OnboardingProgressBar(
                    progress = ONBOARDING_STEP,
                    modifier = Modifier.padding(vertical = 12.dp).width(200.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "Seed Phrase",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Write down your secret recovery phrase in order.\nThis is the ONLY way to recover your wallet.",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Danger warning box
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFF3333).copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF3333).copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF4B4B),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Never share your recovery phrase with anyone. Pulsar Support will never ask for it.",
                            fontSize = 14.sp,
                            color = Color(0xFFFFB8B8),
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Grid of words
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0D1421).copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(20.dp),
                        userScrollEnabled = false
                    ) {
                        itemsIndexed(mnemonic) { index, word ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF03080F),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = (index + 1).toString(),
                                        fontSize = 11.sp,
                                        color = pulsarBlue.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.width(18.dp)
                                    )
                                    Text(
                                        text = word,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Checkbox - Larger and centered
                Surface(
                    onClick = { isChecked = !isChecked },
                    shape = RoundedCornerShape(20.dp),
                    color = if(isChecked) pulsarBlue.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.03f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if(isChecked) pulsarBlue else Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (isChecked) pulsarBlue else Color.Transparent)
                                .border(2.dp, if (isChecked) pulsarBlue else Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "I have saved my recovery phrase securely.",
                            color = if(isChecked) Color.White else Color.White.copy(alpha = 0.5f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Copy Button
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    
                    Surface(
                        onClick = {
                            val text = mnemonic.joinToString(" ")
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
                            android.widget.Toast.makeText(context, "Seed phrase copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentCopy, null, tint = pulsarBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Copy to Clipboard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        onClick = {
                            if (isChecked) onMnemonicSaved()
                            else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please confirm you've saved your phrase!")
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isChecked) pulsarBlue else Color(0xFF1D293D).copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().height(68.dp).alpha(if (isChecked) 1f else 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "Continue", 
                                color = if (isChecked) Color.Black else Color.White.copy(alpha = 0.4f), 
                                fontWeight = FontWeight.ExtraBold, 
                                fontSize = 18.sp
                            )
                        }
                    }
                    
                    TextButton(onClick = onBack) {
                        Text("Back", color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
