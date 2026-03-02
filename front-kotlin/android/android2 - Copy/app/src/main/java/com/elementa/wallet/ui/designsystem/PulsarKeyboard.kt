package com.elementa.wallet.ui.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PulsarKeyboard — Stylish numeric keypad component for the Pulsar design system.
 *
 * Layout:
 *   1  2  3
 *   4  5  6
 *   7  8  9
 *   [bottomLeftSlot]  0  ⌫
 *
 * @param onDigit           Called with the pressed digit string ("0".."9").
 * @param onDelete          Called when the backspace key is pressed.
 * @param modifier          Optional layout modifier for the whole keypad.
 * @param keySize           Diameter of each key circle.
 * @param rowSpacing        Vertical gap between rows.
 * @param bottomLeftSlot    Optional composable drawn inside the bottom-left cell.
 *                          Pass `null` to leave it empty.
 */
@Composable
fun PulsarKeyboard(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    keySize: Dp = 72.dp,
    rowSpacing: Dp = 20.dp,
    bottomLeftSlot: @Composable (BoxScope.() -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(rowSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rows 1–3: digits 1-9
        for (row in 0..2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (col in 1..3) {
                    val digit = (row * 3 + col).toString()
                    PulsarKey(
                        label = digit,
                        size = keySize,
                        onClick = { onDigit(digit) }
                    )
                }
            }
        }

        // Bottom row: special left / 0 / backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bottom-left cell — optional slot
            Box(
                modifier = Modifier.size(keySize),
                contentAlignment = Alignment.Center
            ) {
                if (bottomLeftSlot != null) {
                    bottomLeftSlot()
                }
            }

            // 0
            PulsarKey(
                label = "0",
                size = keySize,
                onClick = { onDigit("0") }
            )

            // Backspace
            PulsarKey(
                label = "⌫",
                size = keySize,
                labelColor = PulsarColors.DangerRed,
                onClick = onDelete
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Individual key
// ──────────────────────────────────────────────────────────────────────────────

/**
 * A single circular key with press-scale animation and a subtle cyan glow on press.
 */
@Composable
fun PulsarKey(
    label: String,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color = PulsarColors.TextPrimaryDark
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Scale down slightly on press for tactile feel
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = tween(durationMillis = if (isPressed) 80 else 180),
        label = "key_scale"
    )

    // Glow intensity on press
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.55f else 0f,
        animationSpec = tween(durationMillis = if (isPressed) 80 else 300),
        label = "key_glow"
    )

    val glowColor = PulsarColors.PrimaryDark.copy(alpha = glowAlpha)

    val keyShape = RoundedCornerShape(18.dp)

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = keyShape,
        color = if (isPressed)
            PulsarColors.PrimaryDark.copy(alpha = 0.16f)
        else
            PulsarColors.PanelDark.copy(alpha = 0.65f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isPressed) PulsarColors.PrimaryDark.copy(alpha = 0.6f)
                    else PulsarColors.BorderSubtleDark
        ),
        modifier = modifier
            .size(size)
            .scale(scale)
            .cyanoGlow(glowColor, radius = (size.value * 0.35f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
                color = if (isPressed && labelColor == PulsarColors.TextPrimaryDark)
                    PulsarColors.PrimaryDark
                else
                    labelColor
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Glow draw modifier
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Draws a soft radial glow behind the composable, using Android BlurMaskFilter.
 * Used to create the neon-glow effect on key press.
 */
private fun Modifier.cyanoGlow(color: Color, radius: Float): Modifier =
    this.drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                asFrameworkPaint().apply {
                    isAntiAlias = true
                    this.color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(radius, 0f, 0f, color.toArgb())
                }
            }
            canvas.drawCircle(
                center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2),
                radius = size.minDimension / 2f,
                paint = paint
            )
        }
    }
