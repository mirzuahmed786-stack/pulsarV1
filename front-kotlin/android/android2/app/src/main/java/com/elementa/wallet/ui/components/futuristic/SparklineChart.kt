package com.elementa.wallet.ui.components.futuristic

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.elementa.wallet.ui.designsystem.PulsarColors

@Composable
fun SparklineChart(
    data: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = PulsarColors.ProfessionalTeal,
    showGradient: Boolean = true
) {
    if (data.isEmpty()) return

    val animationProgress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        animationProgress.animateTo(
            1f,
            animationSpec = tween(1500, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
        )
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val maxPrice = data.maxOrNull() ?: 1.0
        val minPrice = data.minOrNull() ?: 0.0
        val priceRange = (maxPrice - minPrice).coerceAtLeast(0.0001)

        val stepX = width / (data.size - 1)
        val path = Path()
        
        data.indices.forEach { i ->
            val x = i * stepX
            val y = height - ((data[i] - minPrice) / priceRange * height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Animated draw using path measure or simple clip
        // For premium feel, we use a glow effect
        drawPath(
            path = path,
            color = lineColor.copy(alpha = 0.3f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        if (showGradient) {
            val fillPath = Path().apply {
                addPath(path)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.2f * animationProgress.value), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )
        }
    }
}
