package com.elementa.wallet.ui.components.futuristic

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.elementa.wallet.ui.designsystem.PulsarColors

@Composable
fun AnimatedPriceText(
    price: Double,
    style: TextStyle,
    color: Color = Color.White,
    currencySymbol: String = "$"
) {
    var oldPrice by remember { mutableStateOf(price) }
    val isIncreased = price >= oldPrice
    
    LaunchedEffect(price) {
        oldPrice = price
    }

    Row {
        Text(currencySymbol, style = style, color = color)
        AnimatedContent(
            targetState = price,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInVertically { height -> height } + fadeIn()).togetherWith(
                        slideOutVertically { height -> -height } + fadeOut())
                } else {
                    (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                        slideOutVertically { height -> height } + fadeOut())
                }.using(
                    SizeTransform(clip = false)
                )
            },
            label = "PriceAnimation"
        ) { targetPrice ->
            Text(
                text = String.format("%.2f", targetPrice),
                style = style,
                color = if (price > oldPrice) PulsarColors.SuccessGreen else if (price < oldPrice) PulsarColors.ErrorRed else color
            )
        }
    }
}
