// hassam dev: VaultError exception class - auto-generated from wallet.udl (Phase 2)
// Structured error type for all vault operations

package com.wallet_rust

// Abstract base exception for VaultError
sealed class VaultError(message: String) : Exception(message) {
    data class InvalidMnemonic(val details: String) : VaultError("Invalid mnemonic: $details")
    object InvalidPin : VaultError("PIN verification failed")
    object InvalidDerivationPath : VaultError("Invalid derivation path")
    data class CryptoError(val details: String) : VaultError("Cryptographic operation failed: $details")
    object InvalidKeyMaterial : VaultError("Invalid key material")
    data class SerializationError(val details: String) : VaultError("Serialization error: $details")
    object RecoveryBackupMismatch : VaultError("Recovery backup mismatch")
    data class UnknownError(val details: String) : VaultError("Unknown error: $details")
    
    companion object {
        fun fromRust(errorType: String, message: String): VaultError {
            return when (errorType) {
                "InvalidMnemonic" -> InvalidMnemonic(message)
                "InvalidPin" -> InvalidPin
                "InvalidDerivationPath" -> InvalidDerivationPath
                "CryptoError" -> CryptoError(message)
                "InvalidKeyMaterial" -> InvalidKeyMaterial
                "SerializationError" -> SerializationError(message)
                "RecoveryBackupMismatch" -> RecoveryBackupMismatch
                else -> UnknownError(message)
            }
        }
    }
}
