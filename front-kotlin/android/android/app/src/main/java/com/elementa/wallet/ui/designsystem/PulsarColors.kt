package com.elementa.wallet.ui.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Pulsar Color tokens - DARK MODE ONLY.
 * All light theme configurations have been removed per architectural requirements.
 * The wallet enforces dark mode regardless of system settings.
 */
object PulsarColors {
    // ─────────────────────────────────────────────────────────────
    // Primary Dark Theme Tokens
    // ─────────────────────────────────────────────────────────────
    
    val PrimaryDark = Color(0xFF13E1EC)
    val PrimaryStrongDark = Color(0xFF0CB8C2)
    val AccentDark = Color(0xFF13E1EC)
    val AccentStrongDark = Color(0xFF0EAFC0)

    val BackgroundDark = Color(0xFF000000)
    val SurfaceDark = Color(0xFF000000)
    val PanelDark = Color(0xFF121212)
    val ElevatedDark = Color(0xFF1E1E1E)

    val TextPrimaryDark = Color(0xFFFFFFFF)
    val TextSecondaryDark = Color(0xFFBFC9D4)
    val TextMutedDark = Color(0xFF9AA6B2)
    val TextDimDark = Color(0xFF6B7280)
    val TextSecondaryLight = TextSecondaryDark // Alias for compatibility

    // ─────────────────────────────────────────────────────────────
    // Status / Semantic Colors
    // ─────────────────────────────────────────────────────────────
    
    val SuccessGreen = Color(0xFF13E1EC) // Uses primary accent
    val WarningAmber = Color(0xFFD29922)
    val DangerRed = Color(0xFFF85149)
    val ErrorRed = Color(0xFFF85149)
    val InfoBlue = Color(0xFF58A6FF)

    // ─────────────────────────────────────────────────────────────
    // Glow / Gradients
    // ─────────────────────────────────────────────────────────────
    
    val PrimaryGlowDark = PrimaryDark.copy(alpha = 0.45f)
    val AccentGlowDark = AccentDark.copy(alpha = 0.3f)
    val OrbGlowDark = PrimaryDark.copy(alpha = 0.2f)

    val PrimaryGradientDark = listOf(PrimaryDark, AccentStrongDark)
    val PanelGradientDark = listOf(PanelDark, SurfaceDark)

    // ─────────────────────────────────────────────────────────────
    // Border Colors
    // ─────────────────────────────────────────────────────────────
    
    val BorderSubtleDark = Color(0xFF30363D)
    val BorderStrongDark = Color(0xFF484F58)
    val GlassBgDark = SurfaceDark.copy(alpha = 0.7f)
    val GlassBorderDark = Color(0xFF30363D).copy(alpha = 0.5f)

    // ─────────────────────────────────────────────────────────────
    // Legacy Aliases (for backwards compatibility)
    // ─────────────────────────────────────────────────────────────
    
    val ProfessionalTeal = PrimaryDark
    val GlowCyan = PrimaryGlowDark
    val BlackBackground = BackgroundDark

    // Text aliases
    val TextPrimary = TextPrimaryDark
    val TextSecondary = TextSecondaryDark
    val TextTertiary = TextMutedDark
    val TextInverse = Color(0xFFF0F6FC)
    val TextDarkBg = TextPrimaryDark

    // Gradient aliases
    val ActionGradient = PrimaryGradientDark
    val DarkActionGradient = PrimaryGradientDark
    val SolarGradient = PanelGradientDark

    // ─────────────────────────────────────────────────────────────
    // Active Theme Getters (always return dark values)
    // ─────────────────────────────────────────────────────────────
    
    val ActiveBackground: Color get() = BackgroundDark
    val ActiveSurface: Color get() = SurfaceDark
    val ActiveTextPrimary: Color get() = TextPrimaryDark
    val ActivePrimary: Color get() = PrimaryDark
    val ActiveAccent: Color get() = AccentDark
    val ActiveSuccess: Color get() = SuccessGreen
}

