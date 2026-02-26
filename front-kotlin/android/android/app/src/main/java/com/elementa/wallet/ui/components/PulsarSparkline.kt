package com.elementa.wallet.ui.components

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
fun PulsarSparkline(
    data: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = PulsarColors.ProfessionalTeal,
    animate: Boolean = true
) {
    if (data.isEmpty()) return

    val animationProgress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        if (animate) {
            animationProgress.animateTo(
                1f,
                animationSpec = tween(1000, easing = LinearOutSlowInEasing)
            )
        } else {
            animationProgress.snapTo(1f)
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        if (data.size < 2) return@Canvas

        val maxPrice = data.maxOrNull() ?: 1.0
        val minPrice = data.minOrNull() ?: 0.0
        val priceRange = (maxPrice - minPrice).coerceAtLeast(0.0001)

        val linePath = Path()
        val stepX = width / (data.size - 1)
        
        data.indices.forEach { i ->
            val x = i * stepX
            val y = height - ((data[i] - minPrice) / priceRange * height).toFloat()
            
            if (i == 0) linePath.moveTo(x, y)
            else linePath.lineTo(x, y)
        }

        // Draw the line
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Use a separate path for filling to avoid mutating linePath
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )
    }
}
