package com.elementa.wallet.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun WalletCreatedScreen(onGetStarted: () -> Unit) {
    val PROGRESS_FINAL = 1f
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
                progress = PROGRESS_FINAL,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Confirm",
                fontSize = 32.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Let's make sure you're ready.",
                fontSize = 17.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Shield Icon Section
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(Color(0xFF13322D).copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(Color(0xFF1B4E44), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.elementa.wallet.R.drawable.wallet_created),
                        contentDescription = "Success",
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Wallet Created!",
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your wallet is now ready securely\non this device.",
                fontSize = 17.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1.2f))

            Surface(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(18.dp),
                color = pulsarBlue
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Get Started",
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

