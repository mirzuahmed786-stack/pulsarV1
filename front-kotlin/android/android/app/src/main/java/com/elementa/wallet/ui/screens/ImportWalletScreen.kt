package com.elementa.wallet.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.QrCodeScanner
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
import com.elementa.wallet.ui.designsystem.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportWalletScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    val words = remember { mutableStateListOf(*Array(12) { "" }) }
    val filledCount = words.count { it.isNotBlank() }
    var selectedIndex by remember { mutableStateOf(-1) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = PulsarColors.PrimaryDark.copy(alpha = 0.1f),
                        modifier = Modifier.size(44.dp),
                        border = BorderStroke(1.dp, PulsarColors.PrimaryDark.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("←", color = PulsarColors.PrimaryDark, fontSize = 20.sp)
                        }
                    }
                    PulsarComponents.PulsarLogo()
                    Spacer(modifier = Modifier.width(44.dp))
                }
            },
            bottomBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding(),
                    color = Color.Transparent
                ) {
                    PulsarComponents.PulsarButton(
                        text = "Restore Vault",
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = filledCount == 12,
                        glow = filledCount == 12
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                // Title Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            "Secret Phrase",
                            style = PulsarTypography.Typography.headlineLarge,
                            color = PulsarColors.TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Enter your 12-word recovery phrase",
                            style = PulsarTypography.Typography.bodyMedium,
                            color = PulsarColors.TextSecondaryDark
                        )
                    }
                    Surface(
                        color = PulsarColors.PrimaryDark.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "$filledCount/12 words",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = PulsarTypography.Typography.labelSmall,
                            color = PulsarColors.PrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Word Grid
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (row in 0 until 4) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (col in 0 until 3) {
                                val index = row * 3 + col
                                WordSlot(
                                    index = index + 1,
                                    word = words[index],
                                    onClick = { selectedIndex = index },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Secondary Actions
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        onClick = { /* TODO: Paste from clipboard */ },
                        modifier = Modifier.weight(1f),
                        color = PulsarColors.SurfaceDark.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, PulsarColors.BorderSubtleDark)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = PulsarColors.TextPrimaryDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Paste", style = PulsarTypography.Typography.labelSmall, color = PulsarColors.TextPrimaryDark)
                        }
                    }
                    Surface(
                        onClick = { /* TODO: Scan QR */ },
                        modifier = Modifier.weight(1f),
                        color = PulsarColors.SurfaceDark.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, PulsarColors.BorderSubtleDark)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = PulsarColors.TextPrimaryDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan QR", style = PulsarTypography.Typography.labelSmall, color = PulsarColors.TextPrimaryDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (selectedIndex != -1) {
                ModalBottomSheet(
                    onDismissRequest = { selectedIndex = -1 },
                    sheetState = sheetState,
                    containerColor = PulsarColors.SurfaceDark,
                    contentColor = PulsarColors.TextPrimaryDark,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = PulsarColors.BorderSubtleDark) }
                ) {
                    WordInputSheet(
                        index = selectedIndex + 1,
                        currentWord = words[selectedIndex],
                        onWordConfirmed = { word ->
                            words[selectedIndex] = word
                            if (selectedIndex < 11) {
                                selectedIndex += 1
                            } else {
                                selectedIndex = -1
                                scope.launch { sheetState.hide() }
                            }
                        },
                        onClose = { selectedIndex = -1 }
                    )
                }
            }
        }
    }
}

@Composable
private fun WordSlot(
    index: Int,
    word: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFilled = word.isNotBlank()
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        color = if (isFilled) PulsarColors.PrimaryDark.copy(alpha = 0.1f) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isFilled) PulsarColors.PrimaryDark else PulsarColors.BorderSubtleDark)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                String.format("%02d", index),
                style = PulsarTypography.CyberLabel,
                color = if (isFilled) PulsarColors.PrimaryDark.copy(alpha = 0.6f) else PulsarColors.TextMutedDark,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isFilled) {
                Text(
                    word,
                    style = PulsarTypography.Typography.bodyMedium,
                    color = PulsarColors.PrimaryDark,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            } else {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = PulsarColors.TextMutedDark,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun WordInputSheet(
    index: Int,
    currentWord: String,
    onWordConfirmed: (String) -> Unit,
    onClose: () -> Unit
) {
    var text by remember { mutableStateOf(currentWord) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = PulsarColors.PrimaryDark.copy(alpha = 0.2f),
                    shape = CircleShape,
                     modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(String.format("%02d", index), color = PulsarColors.PrimaryDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Word Slot", style = PulsarTypography.Typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = PulsarColors.TextSecondaryDark)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Type word...", color = PulsarColors.TextSecondaryLight) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PulsarColors.PrimaryDark,
                unfocusedBorderColor = PulsarColors.BorderSubtleDark,
                focusedTextColor = PulsarColors.TextPrimaryDark,
                unfocusedTextColor = PulsarColors.TextPrimaryDark
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Suggestions (Simulated)
        val suggestions = listOf("Ocean", "Orchid", "Object", "Orange", "Online")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                Surface(
                    onClick = { onWordConfirmed(suggestion) },
                    shape = RoundedCornerShape(8.dp),
                    color = PulsarColors.SurfaceDark.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, PulsarColors.BorderSubtleDark)
                ) {
                    Text(
                        suggestion,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = PulsarTypography.Typography.labelSmall,
                        color = PulsarColors.TextSecondaryDark
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        PulsarComponents.PulsarButton(
            text = "Set Word",
            onClick = { if (text.isNotBlank()) onWordConfirmed(text) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
