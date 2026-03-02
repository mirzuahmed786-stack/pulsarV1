package com.elementa.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.OnboardingProgressBar
import com.elementa.wallet.ui.designsystem.PulsarOnboardingBackground

@Composable
fun MnemonicDisplayScreen(
    onMnemonicSaved: () -> Unit,
    onBack: () -> Unit
) {
    val mnemonic = listOf(
        "abandon", "ability", "able", "about", "above", "absent",
        "absorb", "abstract", "absurd", "abuse", "access", "accident"
    )
    
    val ONBOARDING_STEP = 0.66f
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

            Spacer(modifier = Modifier.weight(0.1f))

            Text(
                text = "Recovery Phrase",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Write down these 12 words in order\nand store them somewhere very safe.",
                fontSize = 17.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Grid of words
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().height(240.dp),
                userScrollEnabled = false
            ) {
                itemsIndexed(mnemonic) { index, word ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0D1421).copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                fontSize = 10.sp,
                                color = pulsarBlue,
                                fontWeight = FontWeight.Bold
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

            Spacer(modifier = Modifier.weight(0.2f))

            Surface(
                onClick = onMnemonicSaved,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(18.dp),
                color = pulsarBlue
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "I've Secured My Phrase",
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
