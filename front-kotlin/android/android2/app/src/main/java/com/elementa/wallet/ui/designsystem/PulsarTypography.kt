package com.elementa.wallet.ui.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object PulsarTypography {
    private val DefaultFontFamily = FontFamily.SansSerif
    
    val Typography = Typography(
        displayLarge = TextStyle(
            fontFamily = DefaultFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 42.sp,
            lineHeight = 52.sp,
            letterSpacing = (-1.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = DefaultFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.5).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = DefaultFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = DefaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = DefaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = DefaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.25.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = DefaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        labelSmall = TextStyle(
            fontFamily = DefaultFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.sp
        )
    )
    
    val CyberLabel = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 11.sp,
        letterSpacing = 5.sp,
        lineHeight = 16.sp
    )

    val CryptoAmount = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        letterSpacing = 0.sp
    )
}

