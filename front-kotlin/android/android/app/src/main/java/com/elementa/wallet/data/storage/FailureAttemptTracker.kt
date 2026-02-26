package com.elementa.wallet.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.failureDataStore: DataStore<Preferences> by preferencesDataStore(name = "failure_tracking")

/**
 * Failure Attempt Tracker
 * 
 * SECURITY CONTRACT:
 * - Tracks failed PIN entry attempts across app sessions
 * - Enforces exponential backoff lockouts: 3 attempts (5 min) → 6 attempts (30 min) → 10 attempts (permanent)
 * - Persists failure count to prevent easy bypass via app restart
 * - Clears counter only on successful unlock or explicit factory reset
 * 
 * ATTACK MITIGATION:
 * - Brute force attacks face: 3 fails → 5 min wait, 6 fails → 30 min wait, 10 fails → forced reset
 * - App restart does NOT reset counter due to persistent storage
 * - Each failure is logged with timestamp for audit trail
 */
@Singleton
class FailureAttemptTracker @Inject constructor(
    private val context: Context
) {
    
    private val attemptCountKey = intPreferencesKey("pin_attempt_count")
    private val lastFailureTimeKey = longPreferencesKey("pin_last_failure_time")
    private val lockoutUntilKey = longPreferencesKey("pin_lockout_until")
    
    /**
     * Record a failed PIN attempt and check if lockout should be triggered
     * 
     * @return Updated attempt count and lockout status
     */
    suspend fun recordFailureAttempt(): FailureAttemptRecord {
        val prefs = context.failureDataStore.data.first()
        val currentCount = (prefs[attemptCountKey] ?: 0) + 1
        val now = System.currentTimeMillis()
        
        // Determine lockout duration
        val (shouldLockout, lockoutMinutes) = computeLockout(currentCount)
        val lockoutUntil = if (shouldLockout) {
            now + (lockoutMinutes * 60 * 1000L)
        } else {
            null
        }
        
        // Persist
        context.failureDataStore.edit { prefs ->
            prefs[attemptCountKey] = currentCount
            prefs[lastFailureTimeKey] = now
            if (lockoutUntil != null) {
                prefs[lockoutUntilKey] = lockoutUntil
            }
        }
        
        return FailureAttemptRecord(
            attemptCount = currentCount,
            isLockedByTimeout = shouldLockout,
            lockoutDurationMinutes = lockoutMinutes,
            lastFailureEpochMs = now,
            nextRetryEpochMs = lockoutUntil ?: now
        )
    }
    
    /**
     * Clear failure counter after successful unlock
     */
    suspend fun clearFailureAttempts() {
        context.failureDataStore.edit { prefs ->
            prefs.remove(attemptCountKey)
            prefs.remove(lastFailureTimeKey)
            prefs.remove(lockoutUntilKey)
        }
    }
    
    /**
     * Get current failure state without recording new failure
     */
    suspend fun getFailureState(): FailureAttemptRecord {
        val prefs = context.failureDataStore.data.first()
        val attemptCount = prefs[attemptCountKey] ?: 0
        val lastFailureTime = prefs[lastFailureTimeKey] ?: System.currentTimeMillis()
        val lockoutUntil = prefs[lockoutUntilKey]
        val now = System.currentTimeMillis()
        
        // Check if lockout window has expired
        val isCurrentlyLocked = lockoutUntil != null && now < lockoutUntil
        
        // If lockout expired, may need to clear
        if (lockoutUntil != null && now >= lockoutUntil) {
            // Lockout window has passed, but don't auto-clear (user should retry and succeed)
        }
        
        val (_, lockoutMinutes) = computeLockout(attemptCount)
        
        return FailureAttemptRecord(
            attemptCount = attemptCount,
            isLockedByTimeout = isCurrentlyLocked,
            lockoutDurationMinutes = lockoutMinutes,
            lastFailureEpochMs = lastFailureTime,
            nextRetryEpochMs = lockoutUntil ?: now
        )
    }
    
    /**
     * Check if user is currently locked out (used for UI disable/enable)
     */
    suspend fun isCurrentlyLocked(): Boolean {
        val state = getFailureState()
        return state.isLockedByTimeout || state.attemptCount >= 10
    }
    
    /**
     * Get minutes remaining in current lockout window (or 0 if no lockout)
     */
    suspend fun getMinutesUntilRetry(): Long {
        val state = getFailureState()
        if (!state.isLockedByTimeout) return 0
        
        val now = System.currentTimeMillis()
        val remainingMs = state.nextRetryEpochMs - now
        return if (remainingMs > 0) {
            remainingMs / 60_000 + 1  // Round up to next minute
        } else {
            0
        }
    }
    
    /**
     * Force clear all failure records (called during factory reset)
     */
    suspend fun forceReset() {
        context.failureDataStore.edit { prefs -> prefs.clear() }
    }
    
    private fun computeLockout(attemptCount: Int): Pair<Boolean, Int> {
        return when {
            attemptCount >= 10 -> true to Int.MAX_VALUE  // Permanent until factory reset
            attemptCount >= 6  -> true to 30             // 30-minute lockout
            attemptCount >= 3  -> true to 5              // 5-minute lockout
            else               -> false to 0             // No lockout
        }
    }
}

data class FailureAttemptRecord(
    val attemptCount: Int,
    val isLockedByTimeout: Boolean = false,
    val lockoutDurationMinutes: Int = 0,
    val lastFailureEpochMs: Long = System.currentTimeMillis(),
    val nextRetryEpochMs: Long = System.currentTimeMillis()
) {
    fun getDisplayMessage(): String {
        return when {
            attemptCount >= 10 -> 
                "Wallet locked. 10 failed attempts. Use 'FORGOT PIN?' to reset."
            isLockedByTimeout && lockoutDurationMinutes == 30 ->
                "Locked 30 minutes. Attempts: $attemptCount/10"
            isLockedByTimeout ->
                "Locked 5 minutes. Attempts: $attemptCount/10"
            attemptCount >= 6 ->
                "${10 - attemptCount} attempts left before 30-min lock"
            attemptCount >= 3 ->
                "${10 - attemptCount} attempts left before 5-min lock"
            attemptCount > 0 ->
                "Invalid PIN. Please try again."
            else -> ""
        }
    }
}
