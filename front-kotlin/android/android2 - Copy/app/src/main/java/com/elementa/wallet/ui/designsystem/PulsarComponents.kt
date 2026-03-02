package com.elementa.wallet.ui.designsystem

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow

/**
 * Pulsar Design System Components
 * Fully dark-theme optimized with proper contrast ratios (WCAG AA standard)
 * All components use dark mode colors as primary
 */
object PulsarComponents {

    /**
     * Primary card component - Dark theme card with proper elevation and shadow
     * Default to dark surface with semi-transparent glass effect
     */
    @Composable
    fun PulsarCard(
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        containerColor: Color = PulsarColors.SurfaceDark,
        borderColor: Color = PulsarColors.BorderSubtleDark,
        content: @Composable ColumnScope.() -> Unit
    ) {
        val shape = RoundedCornerShape(32.dp)
        if (onClick != null) {
            Surface(
                onClick = onClick,
                shape = shape,
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = modifier.shadow(12.dp, shape, ambientColor = Color.Black.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(24.dp), content = content)
            }
        } else {
            Surface(
                shape = shape,
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = modifier.shadow(12.dp, shape, ambientColor = Color.Black.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(24.dp), content = content)
            }
        }
    }

    /**
     * Primary button with dark theme styling
     * High contrast cyan text on dark background
     */
    @Composable
    fun PulsarButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        glow: Boolean = false,
        backgroundColor: Color = PulsarColors.PrimaryDark,
        textColor: Color = Color.White
    ) {
        Button(
            onClick = onClick,
            modifier = modifier
                .height(56.dp)
                .then(
                    if (glow) Modifier.shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = PulsarColors.PrimaryDark.copy(alpha = 0.4f),
                        spotColor = PulsarColors.PrimaryDark.copy(alpha = 0.2f)
                    ) else Modifier
                ),
            enabled = enabled,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = textColor,
                disabledContainerColor = PulsarColors.TextMutedDark.copy(alpha = 0.3f),
                disabledContentColor = PulsarColors.TextMutedDark
            )
        ) {
            Text(text, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }

    /**
     * Secondary outlined button for dark theme
     * High contrast border and text
     */
    @Composable
    fun PulsarOutlinedButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        borderColor: Color = PulsarColors.BorderStrongDark,
        textColor: Color = PulsarColors.TextPrimaryDark
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(56.dp),
            enabled = enabled,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, borderColor),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = textColor,
                disabledContentColor = PulsarColors.TextMutedDark
            )
        ) {
            Text(text, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }

    /**
     * Glass morphic panel with dark glass effect
     * Semi-transparent dark surface with subtle border for depth
     */
    @Composable
    fun GlassmorphicPanel(
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit
    ) {
        Surface(
            modifier = modifier,
            color = PulsarColors.GlassBgDark,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, PulsarColors.GlassBorderDark)
        ) {
            Box(modifier = Modifier.padding(16.dp), content = content)
        }
    }

    /**
     * Input field with dark theme optimized styling
     * Proper contrast for text and hints
     */
    @Composable
    fun DarkThemedTextField(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        label: String = "",
        placeholder: String = "",
        isError: Boolean = false
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            label = if (label.isNotEmpty()) {{ Text(label) }} else null,
            placeholder = if (placeholder.isNotEmpty()) {{ Text(placeholder) }} else null,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PulsarColors.PrimaryDark,
                unfocusedBorderColor = PulsarColors.BorderSubtleDark,
                focusedLabelColor = PulsarColors.PrimaryDark,
                unfocusedLabelColor = PulsarColors.TextSecondaryDark,
                focusedTextColor = PulsarColors.TextPrimaryDark,
                unfocusedTextColor = PulsarColors.TextPrimaryDark,
                cursorColor = PulsarColors.PrimaryDark,
                errorBorderColor = PulsarColors.DangerRed,
                errorCursorColor = PulsarColors.DangerRed
            ),
            isError = isError,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 16.sp,
                color = PulsarColors.TextPrimaryDark
            )
        )
    }

    /**
     * PIN indicator dots for security screens
     */
    @Composable
    fun PinIndicator(
        length: Int = 4,
        filledCount: Int = 0,
        isError: Boolean = false
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(length) { index ->
                val isFilled = index < filledCount
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = when {
                                isError -> PulsarColors.DangerRed
                                isFilled -> PulsarColors.PrimaryDark
                                else -> PulsarColors.TextMutedDark.copy(alpha = 0.3f)
                            },
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }
    }

    /**
     * Elevated label for section headers
     */
    @Composable
    fun SectionLabel(text: String) {
        Text(
            text,
            style = PulsarTypography.CyberLabel,
            color = PulsarColors.PrimaryDark,
            letterSpacing = 2.sp,
            fontSize = 12.sp
        )
    }

    /**
     * Heading text with proper dark theme contrast
     */
    @Composable
    fun HeadingText(text: String) {
        Text(
            text,
            style = MaterialTheme.typography.headlineLarge,
            color = PulsarColors.TextPrimaryDark,
            fontWeight = FontWeight.Bold
        )
    }

    /**
     * Body text with optimized contrast
     */
    @Composable
    fun BodyText(text: String, dimmed: Boolean = false) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (dimmed) PulsarColors.TextSecondaryLight else PulsarColors.TextPrimaryDark
        )
    }

    /**
     * Premium Pulsar logo with subtle pulsing animation (starburst icon)
     */
    @Composable
    fun PulsarLogo(modifier: Modifier = Modifier, pulse: Boolean = true, size: Dp = 100.dp) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulsar_logo")
        
        val scale by if (pulse) {
            infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "logo_scale"
            )
        } else {
            remember { mutableStateOf(1f) }
        }

        val glowAlpha by if (pulse) {
            infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 0.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "logo_glow"
            )
        } else {
            remember { mutableStateOf(0.25f) }
        }

        val iconColor = Color(0xFF1E2530) // Dark icon color

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(scale)
            ) {
                // Outer Glow
                Box(
                    modifier = Modifier
                        .size(size + 20.dp)
                        .background(PulsarColors.PrimaryDark.copy(alpha = glowAlpha), androidx.compose.foundation.shape.CircleShape)
                )
                // Starburst logo using Canvas
                Canvas(modifier = Modifier.size(size)) {
                    val center = this.size.width / 2
                    val iconScale = this.size.width / 100f // Scale factor based on 100dp reference
                    
                    // Center circle
                    drawCircle(
                        color = iconColor,
                        radius = 12f * iconScale,
                        center = androidx.compose.ui.geometry.Offset(center, center)
                    )
                    
                    // Cardinal rays (top, bottom, left, right)
                    val rayWidth = 6f * iconScale
                    val rayInnerRadius = 17f * iconScale
                    val rayOuterRadius = 28f * iconScale
                    
                    // Top ray
                    drawRect(
                        color = iconColor,
                        topLeft = androidx.compose.ui.geometry.Offset(center - rayWidth/2, center - rayOuterRadius),
                        size = androidx.compose.ui.geometry.Size(rayWidth, rayOuterRadius - rayInnerRadius)
                    )
                    // Bottom ray
                    drawRect(
                        color = iconColor,
                        topLeft = androidx.compose.ui.geometry.Offset(center - rayWidth/2, center + rayInnerRadius),
                        size = androidx.compose.ui.geometry.Size(rayWidth, rayOuterRadius - rayInnerRadius)
                    )
                    // Right ray
                    drawRect(
                        color = iconColor,
                        topLeft = androidx.compose.ui.geometry.Offset(center + rayInnerRadius, center - rayWidth/2),
                        size = androidx.compose.ui.geometry.Size(rayOuterRadius - rayInnerRadius, rayWidth)
                    )
                    // Left ray
                    drawRect(
                        color = iconColor,
                        topLeft = androidx.compose.ui.geometry.Offset(center - rayOuterRadius, center - rayWidth/2),
                        size = androidx.compose.ui.geometry.Size(rayOuterRadius - rayInnerRadius, rayWidth)
                    )
                    
                    // Diagonal rays using rotated rectangles
                    val diagInner = 12f * iconScale
                    val diagOuter = 20f * iconScale
                    val diagWidth = 6f * iconScale
                    val diagAngleOffset = 0.707f // cos(45°) = sin(45°)
                    
                    // Top-right diagonal
                    rotate(45f, pivot = androidx.compose.ui.geometry.Offset(center, center)) {
                        drawRect(
                            color = iconColor,
                            topLeft = androidx.compose.ui.geometry.Offset(center - diagWidth/2, center - rayOuterRadius),
                            size = androidx.compose.ui.geometry.Size(diagWidth, rayOuterRadius - rayInnerRadius)
                        )
                    }
                    // Bottom-left diagonal
                    rotate(45f, pivot = androidx.compose.ui.geometry.Offset(center, center)) {
                        drawRect(
                            color = iconColor,
                            topLeft = androidx.compose.ui.geometry.Offset(center - diagWidth/2, center + rayInnerRadius),
                            size = androidx.compose.ui.geometry.Size(diagWidth, rayOuterRadius - rayInnerRadius)
                        )
                    }
                    // Top-left diagonal
                    rotate(-45f, pivot = androidx.compose.ui.geometry.Offset(center, center)) {
                        drawRect(
                            color = iconColor,
                            topLeft = androidx.compose.ui.geometry.Offset(center - diagWidth/2, center - rayOuterRadius),
                            size = androidx.compose.ui.geometry.Size(diagWidth, rayOuterRadius - rayInnerRadius)
                        )
                    }
                    // Bottom-right diagonal
                    rotate(-45f, pivot = androidx.compose.ui.geometry.Offset(center, center)) {
                        drawRect(
                            color = iconColor,
                            topLeft = androidx.compose.ui.geometry.Offset(center - diagWidth/2, center + rayInnerRadius),
                            size = androidx.compose.ui.geometry.Size(diagWidth, rayOuterRadius - rayInnerRadius)
                        )
                    }
                }
            }
            Text(
                "PULSAR",
                style = PulsarTypography.CyberLabel,
                color = PulsarColors.PrimaryDark,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 12.dp),
                letterSpacing = 4.sp
            )
        }
    /**
     * Slide to confirm component (Swipe to Send)
     * Secure and satisfying interaction for transaction confirmation
     */
    }
}

// Slide to confirm component (Swipe to Send)
// Secure and satisfying interaction for transaction confirmation
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulsarSlideToConfirm(
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    text: String = "Slide to Send",
    thumbColor: Color = Color(0xFF00D3F2),
    containerColor: Color = Color(0xFF1D293D).copy(alpha = 0.6f),
    textColor: Color = Color.White.copy(alpha = 0.6f)
) {
    val swipeState = rememberSwipeToDismissBoxState()
    
    LaunchedEffect(swipeState.currentValue) {
        if (swipeState.currentValue == SwipeToDismissBoxValue.EndToStart || swipeState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
            onConfirm()
            // Reset after some time if needed, but usually we navigate away
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(containerColor, RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        SwipeToDismissBox(
            state = swipeState,
            backgroundContent = { Box(Modifier.fillMaxSize()) },
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = false,
            modifier = Modifier.fillMaxSize()
        ) {
            val offset = swipeState.requireOffset()
            val progress = (offset / 1000f).coerceIn(0f, 1f) // heuristic for visuals
            
            Surface(
                modifier = Modifier
                    .size(64.dp)
                    .padding(4.dp)
                    .offset(x = 0.dp), // Fixed thumb handled by SwipeToDismissBox
                shape = RoundedCornerShape(16.dp),
                color = thumbColor,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

