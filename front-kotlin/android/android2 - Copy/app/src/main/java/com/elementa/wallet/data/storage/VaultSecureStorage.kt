package com.elementa.wallet.data.storage

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.elementa.wallet.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Singleton

private val Context.vaultDataStore: DataStore<Preferences> by preferencesDataStore(name = "vault_store")

class VaultSecureStorage constructor(
    private val context: Context,
    private val cryptoManager: CryptoManager
) {
    @Volatile
    private var cachedPayload: PinPayload? = null

    // Returns true if an encrypted PIN payload exists.
    suspend fun hasPin(): Boolean {
        val prefs = context.vaultDataStore.data.first()
        return !prefs[pinHashKey].isNullOrBlank()
    }

    // Saves a PBKDF2 hash payload encrypted with AES-GCM.
    suspend fun savePin(pin: String) {
        val salt = ByteArray(SALT_SIZE_BYTES)
        secureRandom.nextBytes(salt)
        val (payload, cache) = withContext(Dispatchers.Default) {
            val hash = derive(pin, salt, ITERATIONS)
            val payloadValue = buildString {
                append(ITERATIONS)
                append(":")
                append(Base64.encodeToString(salt, Base64.NO_WRAP))
                append(":")
                append(Base64.encodeToString(hash, Base64.NO_WRAP))
            }
            payloadValue to PinPayload(ITERATIONS, salt, hash)
        }
        val encrypted = withContext(Dispatchers.Default) {
            cryptoManager.encrypt(payload)
        }
        context.vaultDataStore.edit { prefs -> prefs[pinHashKey] = encrypted }
        cachedPayload = cache
    }

    // Verifies the PIN by decrypting and checking the stored hash.
    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.vaultDataStore.data.first()
        val encrypted = prefs[pinHashKey] ?: return false
        return withContext(Dispatchers.Default) {
            val payload = cachedPayload ?: run {
                val decrypted = runCatching { cryptoManager.decrypt(encrypted) }.getOrNull() ?: return@withContext false
                val segments = decrypted.split(":")
                if (segments.size != 3) return@withContext false
                val iterations = segments[0].toIntOrNull() ?: return@withContext false
                val salt = runCatching { Base64.decode(segments[1], Base64.NO_WRAP) }.getOrNull() ?: return@withContext false
                val expected = runCatching { Base64.decode(segments[2], Base64.NO_WRAP) }.getOrNull() ?: return@withContext false
                PinPayload(iterations, salt, expected)
            }
            val actual = derive(pin, payload.salt, payload.iterations)
            val isMatch = MessageDigest.isEqual(actual, payload.expected)
            if (isMatch && cachedPayload == null) {
                cachedPayload = payload
            }
            isMatch
        }
    }

    // Preloads and caches the decrypted PIN payload for faster unlock.
    suspend fun preloadPinCache() {
        if (cachedPayload != null) return
        val prefs = context.vaultDataStore.data.first()
        val encrypted = prefs[pinHashKey] ?: return
        cachedPayload = withContext(Dispatchers.Default) {
            val decrypted = runCatching { cryptoManager.decrypt(encrypted) }.getOrNull() ?: return@withContext null
            val segments = decrypted.split(":")
            if (segments.size != 3) return@withContext null
            val iterations = segments[0].toIntOrNull() ?: return@withContext null
            val salt = runCatching { Base64.decode(segments[1], Base64.NO_WRAP) }.getOrNull() ?: return@withContext null
            val expected = runCatching { Base64.decode(segments[2], Base64.NO_WRAP) }.getOrNull() ?: return@withContext null
            PinPayload(iterations, salt, expected)
        }
    }

    // Reads the persisted lock flag, defaulting to locked.
    suspend fun isLocked(): Boolean {
        val prefs = context.vaultDataStore.data.first()
        return prefs[lockedKey] ?: true
    }

    // Persists the lock flag.
    suspend fun setLocked(locked: Boolean) {
        context.vaultDataStore.edit { prefs -> prefs[lockedKey] = locked }
    }

    // Clears all persisted vault data.
    suspend fun clear() {
        context.vaultDataStore.edit { it.clear() }
        cachedPayload = null
    }

    // Derives a PBKDF2 hash for the provided PIN.
    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val factory = SecretKeyFactory.getInstance(KDF_ALGO)
        val keySpec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        return factory.generateSecret(keySpec).encoded
    }

    // Returns whether biometrics are enabled.
    suspend fun isBiometricsEnabled(): Boolean {
        val prefs = context.vaultDataStore.data.first()
        return prefs[biometricsKey] ?: false
    }

    // Persists biometrics enabled state.
    suspend fun setBiometricsEnabled(enabled: Boolean) {
        context.vaultDataStore.edit { prefs -> prefs[biometricsKey] = enabled }
    }

    private companion object {
        private const val KDF_ALGO = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 210_000
        private const val KEY_BITS = 256
        private const val SALT_SIZE_BYTES = 16
        private val secureRandom = SecureRandom()
        private val pinHashKey = stringPreferencesKey("vault_pin_hash")
        private val lockedKey = booleanPreferencesKey("vault_locked")
        private val biometricsKey = booleanPreferencesKey("biometrics_enabled")
    }

    private data class PinPayload(
        val iterations: Int,
        val salt: ByteArray,
        val expected: ByteArray
    )
}
