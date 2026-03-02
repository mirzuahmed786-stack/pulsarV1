package com.elementa.wallet.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ResponsiveSpec(
    val isCompact: Boolean,
    val isMedium: Boolean,
    val isExpanded: Boolean,
    val horizontalPadding: Dp,
    val sectionSpacing: Dp,
    val cardRadius: Dp
)

@Composable
fun rememberResponsiveSpec(width: Dp): ResponsiveSpec {
    return when {
        width >= 1000.dp -> ResponsiveSpec(
            isCompact = false,
            isMedium = false,
            isExpanded = true,
            horizontalPadding = 24.dp,
            sectionSpacing = 18.dp,
            cardRadius = 26.dp
        )
        width >= 700.dp -> ResponsiveSpec(
            isCompact = false,
            isMedium = true,
            isExpanded = false,
            horizontalPadding = 20.dp,
            sectionSpacing = 16.dp,
            cardRadius = 24.dp
        )
        else -> ResponsiveSpec(
            isCompact = true,
            isMedium = false,
            isExpanded = false,
            horizontalPadding = 12.dp,
            sectionSpacing = 14.dp,
            cardRadius = 22.dp
        )
    }
}