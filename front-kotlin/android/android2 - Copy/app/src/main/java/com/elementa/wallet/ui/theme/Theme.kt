package com.elementa.wallet.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.elementa.wallet.ui.designsystem.PulsarColors
import com.elementa.wallet.ui.designsystem.PulsarTypography

/**
 * Dark-mode only theme: ignores system light/dark setting entirely.
 * Hardcoded to the Pulsar dark palette derived from the welcome screen assets.
 * All light-theme configurations have been purged per architectural requirements.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PulsarColors.PrimaryDark,
    onPrimary = Color.Black,
    secondary = PulsarColors.AccentDark,
    onSecondary = Color.Black,
    tertiary = PulsarColors.PrimaryStrongDark,
    onTertiary = Color.Black,
    background = PulsarColors.BackgroundDark,
    onBackground = Color.White,
    surface = PulsarColors.SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = PulsarColors.PanelDark,
    onSurfaceVariant = PulsarColors.TextSecondaryDark,
    error = PulsarColors.ErrorRed,
    onError = Color.White,
    outline = PulsarColors.BorderSubtleDark,
    outlineVariant = PulsarColors.BorderStrongDark
)

@Composable
fun WalletTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true, // Ignored - always dark
    content: @Composable () -> Unit
) {
    // Force dark status bar appearance
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PulsarColors.BackgroundDark.toArgb()
            window.navigationBarColor = PulsarColors.BackgroundDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = PulsarTypography.Typography,
        content = content
    )
}
