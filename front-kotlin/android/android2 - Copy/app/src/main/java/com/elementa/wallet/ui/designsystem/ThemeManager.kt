package com.elementa.wallet.ui.designsystem

/**
 * Theme Manager - DARK MODE ONLY.
 * The wallet enforces dark mode regardless of system settings.
 * Light theme toggle functionality has been removed per architectural requirements.
 */
object ThemeManager {
    // Always dark mode - value is immutable
    const val isDark: Boolean = true
    
    // Toggle is disabled - no-op for backwards compatibility
    @Suppress("UNUSED_PARAMETER")
    fun toggle() {
        // Dark mode is enforced - toggle does nothing
    }
}
