package com.elementa.wallet.viewmodel

/**
 * Enhanced Security State Machine for Vault Authentication
 * Implements "Failure-First" lockout protocol with exponential backoff
 * 
 * STATE TRANSITIONS:
 * Locked → Authenticating → Unlocked (success) / FailedAttempt (fail)
 * FailedAttempt(1-2) → Authenticating (allow retry)
 * FailedAttempt(3) → LockedByCooldown(5 min)
 * FailedAttempt(6) → LockedByCooldown(30 min)
 * FailedAttempt(10+) → FactoryResetRequired
 */
sealed class EnhancedVaultUiState {
    object Idle : EnhancedVaultUiState()
    object Locked : EnhancedVaultUiState()
    object Authenticating : EnhancedVaultUiState()
    object Unlocked : EnhancedVaultUiState()
    
    /**
     * User provided incorrect PIN. Track attempt count and determine lockout.
     * 
     * @param attemptCount 1-based count of failed attempts in current session
     * @param maxAttempts Hard limit of 10 before factory reset
     * @param isLockedByTimeout True if lockout window is active
     * @param lockoutDurationMinutes How long to wait before retry allowed (0 = immediate retry)
     * @param nextRetryEpochMs Unix timestamp when next retry becomes available
     * @param lastFailureEpochMs Unix timestamp of most recent failed attempt
     */
    data class FailedAttempt(
        val attemptCount: Int,
        val maxAttempts: Int = 10,
        val isLockedByTimeout: Boolean = false,
        val lockoutDurationMinutes: Int = 0,
        val nextRetryEpochMs: Long = System.currentTimeMillis(),
        val lastFailureEpochMs: Long = System.currentTimeMillis()
    ) : EnhancedVaultUiState() {
        
        companion object {
            fun computeLockout(attemptCount: Int): Pair<Boolean, Int> {
                return when {
                    attemptCount >= 10 -> true to Int.MAX_VALUE  // Permanent until factory reset
                    attemptCount >= 6  -> true to 30             // 30-minute lockout
                    attemptCount >= 3  -> true to 5              // 5-minute lockout
                    else               -> false to 0             // No lockout, allow immediate retry
                }
            }
            
            /**
             * User-visible message for current failure state
             */
            fun getErrorMessage(state: FailedAttempt): String {
                return when {
                    state.attemptCount >= 10 -> 
                        "Wallet locked permanently. Factory reset required. Use 'FORGOT PIN?' to recover."
                    state.isLockedByTimeout && state.lockoutDurationMinutes == 30 ->
                        "Wallet locked for 30 minutes. Please try again later. Pattern: 6+ failed attempts."
                    state.isLockedByTimeout && state.lockoutDurationMinutes == 5 ->
                        "Wallet locked for 5 minutes. Please try again later. Pattern: 3+ failed attempts."
                    state.attemptCount >= 6 ->
                        "${10 - state.attemptCount} attempts remaining before permanent lock."
                    state.attemptCount >= 3 ->
                        "${10 - state.attemptCount} attempts before 5-minute lockout."
                    else -> 
                        "Invalid PIN. Please try again."
                }
            }
        }
        
        fun getMinutesUntilUnlock(): Long {
            val now = System.currentTimeMillis()
            return if (now < nextRetryEpochMs) {
                (nextRetryEpochMs - now) / 60_000 + 1
            } else {
                0
            }
        }
    }
    
    /**
     * Generic authentication error (network, system, etc)
     */
    data class Error(val message: String, val throwable: Throwable? = null) : EnhancedVaultUiState()
    
    /**
     * Display warning and require explicit confirmation before factory reset
     * This is a destructive action that cannot be undone without backup file
     */
    object FactoryResetRequired : EnhancedVaultUiState()
}
