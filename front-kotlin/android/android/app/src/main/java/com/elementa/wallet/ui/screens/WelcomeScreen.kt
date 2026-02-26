package com.elementa.wallet.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.*

@Composable
fun WelcomeScreen(
    onCreateWallet: () -> Unit,
    onImportWallet: () -> Unit,
    onGoogleLogin: () -> Unit = {},
    onAppleLogin: () -> Unit = {}
) {
    // Deep dark teal background matching the image
    val bgColor = Color(0xFF051414)
    val cyanColor = PulsarColors.PrimaryDark // #13E1EC

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Wave canvas in upper portion
        WelcomeWaveBackground(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.TopCenter),
            cyanColor = cyanColor
        )

        // Radial centre glow
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 80.dp)
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            cyanColor.copy(alpha = 0.18f),
                            cyanColor.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(52.dp))

            // ── Inline logo: heart-pulse icon + PULSAR text ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Heart icon with pulse bar overlay
                Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = CircleShape,
                        color = cyanColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {}
                    Icon(
                        imageVector = Icons.Default.MonitorHeart,
                        contentDescription = null,
                        tint = cyanColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "PULSAR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = Color.White,
                    letterSpacing = 3.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Text content ──
            Column(
                modifier = Modifier.padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Digital Future",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 40.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "The most secure vault for your\ndigital assets and identity.",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── Buttons ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Primary: Create New Vault
                Button(
                    onClick = onCreateWallet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cyanColor,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "Create New Vault",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                // Google login
                WelcomeSocialButton(
                    text = "Connect with Google",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    onClick = onGoogleLogin
                )

                // Apple login
                WelcomeSocialButton(
                    text = "Continue with Apple",
                    leadingIcon = {
                        Text(
                            text = "iOS",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    },
                    onClick = onAppleLogin
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Import link
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .clickable { onImportWallet() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = cyanColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "IMPORT USING RECOVERY PHRASE",
                    color = cyanColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WelcomeSocialButton(
    text: String,
    leadingIcon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(50.dp),
        color = Color(0xFF0A1E1E),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            leadingIcon()
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

/** Draws flowing cyan wave lines to match the image background. */
@Composable
private fun WelcomeWaveBackground(modifier: Modifier = Modifier, cyanColor: Color) {
    val wave1 = cyanColor.copy(alpha = 0.28f)
    val wave2 = cyanColor.copy(alpha = 0.14f)
    val wave3 = cyanColor.copy(alpha = 0.08f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Wave 1 — main arc sweeping from left to right across mid-screen
        val p1 = Path().apply {
            moveTo(0f, h * 0.62f)
            cubicTo(w * 0.18f, h * 0.28f, w * 0.52f, h * 0.72f, w * 0.72f, h * 0.36f)
            cubicTo(w * 0.82f, h * 0.18f, w * 0.92f, h * 0.42f, w * 1.05f, h * 0.30f)
        }
        drawPath(p1, wave1, style = Stroke(width = 2.8f, cap = StrokeCap.Round))

        // Wave 2 — tighter curve above
        val p2 = Path().apply {
            moveTo(0f, h * 0.50f)
            cubicTo(w * 0.20f, h * 0.20f, w * 0.48f, h * 0.58f, w * 0.68f, h * 0.28f)
            cubicTo(w * 0.80f, h * 0.10f, w * 0.90f, h * 0.36f, w * 1.05f, h * 0.22f)
        }
        drawPath(p2, wave2, style = Stroke(width = 1.8f, cap = StrokeCap.Round))

        // Wave 3 — broad sweep below
        val p3 = Path().apply {
            moveTo(-w * 0.05f, h * 0.72f)
            cubicTo(w * 0.22f, h * 0.42f, w * 0.55f, h * 0.82f, w * 0.80f, h * 0.48f)
            cubicTo(w * 0.90f, h * 0.36f, w * 0.95f, h * 0.56f, w * 1.05f, h * 0.46f)
        }
        drawPath(p3, wave3, style = Stroke(width = 3.5f, cap = StrokeCap.Round))

        // Extra glowing arc on top
        val p4 = Path().apply {
            moveTo(w * 0.10f, h * 0.38f)
            cubicTo(w * 0.30f, h * 0.08f, w * 0.60f, h * 0.50f, w * 0.85f, h * 0.18f)
            cubicTo(w * 0.92f, h * 0.08f, w * 0.98f, h * 0.24f, w * 1.05f, h * 0.16f)
        }
        drawPath(p4, wave1.copy(alpha = 0.18f), style = Stroke(width = 1.4f, cap = StrokeCap.Round))
    }
}
