package com.elementa.wallet.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elementa.wallet.R

/**
 * Wallet logo component using the cyan loading spinner design
 * Can be used throughout the app for branding
 */
@Composable
fun WalletLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showBorder: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF00D9E1))
            .then(
                if (showBorder) {
                    Modifier.border(2.dp, Color(0xFF00D9E1).copy(alpha = 0.3f), CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Elementa Wallet Logo",
            modifier = Modifier.size(size * 0.9f)
        )
    }
}
