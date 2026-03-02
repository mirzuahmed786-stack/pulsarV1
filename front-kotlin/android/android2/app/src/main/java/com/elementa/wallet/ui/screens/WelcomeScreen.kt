package com.elementa.wallet.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
    LaunchedEffect(Unit) {
        animateIn = true
    }

    val logoProgress by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "logoProgress"
    )
    val contentProgress by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 950, delayMillis = 180, easing = FastOutSlowInEasing),
        label = "contentProgress"
    )
    val googleProgress by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 900, delayMillis = 360, easing = FastOutSlowInEasing),
        label = "googleProgress"
    )

    val pulsarBlue = Color(0xFF0092B8)

    PulsarOnboardingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // ─────────────────────────────────────────────────────────────
            // Pulsar Logo Section
            // ─────────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(0.85f + 0.15f * logoProgress)
                    .alpha(logoProgress),
                contentAlignment = Alignment.Center
            ) {
                // Background Glow / Circle
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF030A19).copy(alpha = 0.85f),
                    modifier = Modifier.size(160.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Pulsar Logo",
                            modifier = Modifier.size(160.dp)
                        )
                    }
                }

                // Overlapping Rocket Icon
                Image(
                    painter = painterResource(id = R.drawable.ic_rocket_on_onboarding),
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-16).dp, y = (-16).dp)
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            // ─────────────────────────────────────────────────────────────
            // Title & Description
            // ─────────────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(contentProgress)
                    .offset(y = ((1f - contentProgress) * 20f).dp)
            ) {
                Text(
                    text = "Pulsar Wallet",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = pulsarBlue,
                    letterSpacing = (-0.5).sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Your gateway to the multi-chain\nuniverse. Secure, fast, and intuitive.",
                    fontSize = 17.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ─────────────────────────────────────────────────────────────
            // Primary Action Buttons
            // ─────────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentProgress)
                    .offset(y = ((1f - contentProgress) * 24f).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Create New Wallet Button
                Surface(
                    onClick = onCreateWallet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = pulsarBlue
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.createnew),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Create New Wallet",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Image(
                                painter = painterResource(id = R.drawable.createwalletarrow),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Import Existing Wallet Button
                Surface(
                    onClick = onImportWallet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1B2230).copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.import_wallet),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Import Existing Wallet",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Image(
                            painter = painterResource(id = R.drawable.import_button_arroow),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // ─────────────────────────────────────────────────────────────
            // Social Auth Section
            // ─────────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(googleProgress)
                    .offset(y = ((1f - googleProgress) * 24f).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Divider with text context
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        color = Color.White.copy(alpha = 0.12f),
                        thickness = 1.dp
                    )
                    Text(
                        text = "Or continue with",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .background(Color(0xFF030712)) // Match approximate background color to create gap
                            .padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Google Button
                Surface(
                    onClick = onGoogleLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.google),
                            contentDescription = "Google",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Google",
                            color = Color(0xFF1F2937),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}



