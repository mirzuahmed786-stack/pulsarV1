package com.elementa.wallet.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.R
import com.elementa.wallet.ui.designsystem.PulsarOnboardingBackground

@Composable
fun WelcomeScreen(
    onCreateWallet: () -> Unit,
    onImportWallet: () -> Unit,
    onGoogleLogin: () -> Unit = {},
    onAppleLogin: () -> Unit = {}
) {
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }

    val logoProgress by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "logoProgress"
    )
    
    val pulsarBlue = Color(0xFF00D3F2)

    PulsarOnboardingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- Corrected Premium Logo Section ---
            Box(
                modifier = Modifier
                    .size(150.dp) // Reduced size
                    .scale(0.8f + 0.2f * logoProgress)
                    .alpha(logoProgress),
                contentAlignment = Alignment.Center
            ) {
                // Outer Glow Circle
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0D1421).copy(alpha = 0.8f),
                    modifier = Modifier.size(140.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Pulsar Logo",
                            modifier = Modifier.size(90.dp)
                        )
                    }
                }

                // Mini Rocket Badge
                Image(
                    painter = painterResource(id = R.drawable.ic_rocket_on_onboarding),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp)) // Reduced spacing

            // Title & Description
            Text(
                text = "Pulsar Wallet",
                fontSize = 38.sp, // Slightly reduced
                fontWeight = FontWeight.Black,
                color = pulsarBlue,
                letterSpacing = (-1).sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp)) // Reduced spacing
            
            Text(
                text = "Secure, fast, and intuitive gateway to\nthe multi-chain universe.",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(40.dp)) // Reduced spacing

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp) // Reduced spacing
            ) {
                // Create New Wallet
                Surface(
                    onClick = onCreateWallet,
                    modifier = Modifier.fillMaxWidth().height(60.dp), // Reduced height
                    shape = RoundedCornerShape(18.dp),
                    color = pulsarBlue
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AddCircleOutline, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Create New Wallet", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                // Import Wallet
                Surface(
                    onClick = onImportWallet,
                    modifier = Modifier.fillMaxWidth().height(60.dp), // Reduced height
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1D293D).copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FileDownload, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Import Existing Wallet", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowForward, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp)) // Reduced spacing

            // Divider
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                HorizontalDivider(modifier = Modifier.fillMaxWidth(0.8f), color = Color.White.copy(alpha = 0.1f))
                Text(
                    "Or continue with",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp,
                    modifier = Modifier.background(Color(0xFF03080F).copy(alpha = 0.9f)).padding(horizontal = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp)) // Reduced spacing

            // Google Button
            Surface(
                onClick = onGoogleLogin,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(painter = painterResource(id = R.drawable.google), contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Google", color = Color(0xFF03080F), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                }
            }
        }
    }
}
