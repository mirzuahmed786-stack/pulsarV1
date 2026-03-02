package com.elementa.wallet.ui.designsystem

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

object PulsarMotion {
    // Equivalent to motionRules.ts
    
    val StandardEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val DecelerationEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val AccelerationEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    
    val FastDuration = 150
    val NormalDuration = 300
    val SlowDuration = 500
    
    @Composable
    fun FadeInUp(
        visible: Boolean,
        delay: Int = 0,
        content: @Composable () -> Unit
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(NormalDuration, delay)) +
                    slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(NormalDuration, delay, easing = DecelerationEasing)
                    ),
            exit = fadeOut(animationSpec = tween(NormalDuration))
        ) {
            content()
        }
    }

    @Composable
    fun StaggeredEntrance(
        visible: Boolean,
        index: Int,
        content: @Composable () -> Unit
    ) {
        FadeInUp(visible = visible, delay = index * 80, content = content)
    }

    @Composable
    fun ScaleFade(
        visible: Boolean,
        content: @Composable () -> Unit
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(initialScale = 0.9f, animationSpec = tween(NormalDuration)) + fadeIn(),
            exit = scaleOut(targetScale = 0.9f, animationSpec = tween(NormalDuration)) + fadeOut()
        ) {
            content()
        }
    }

    /**
     * Shake animation for error feedback
     */
    @Composable
    fun ShakeAnimation(
        trigger: Boolean,
        content: @Composable () -> Unit
    ) {
        val offset = remember { Animatable(0f) }

        LaunchedEffect(trigger) {
            if (trigger) {
                offset.snapTo(0f)
                repeat(3) {
                    offset.animateTo(8f, animationSpec = tween(60, easing = LinearEasing))
                    offset.animateTo(-8f, animationSpec = tween(60, easing = LinearEasing))
                }
                offset.animateTo(0f, animationSpec = tween(60, easing = LinearEasing))
            }
        }

        Box(modifier = Modifier.graphicsLayer(translationX = offset.value)) { content() }
    }
    
    fun <T> springLowStiffness() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
}
