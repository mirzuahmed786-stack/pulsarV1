package com.elementa.wallet.data.bindings

sealed class VaultError(message: String) : Exception(message) {
    data class InvalidPin(override val message: String = "Invalid PIN") : VaultError(message)
    data class InvalidMnemonic(override val message: String = "Invalid mnemonic") : VaultError(message)
    data class InvalidKeyMaterial(override val message: String = "Invalid key material") : VaultError(message)
    data class CryptoError(override val message: String = "Cryptographic error") : VaultError(message)
    data class InvalidDerivationPath(override val message: String = "Invalid derivation path") : VaultError(message)
    data class SerializationError(override val message: String = "Serialization error") : VaultError(message)
    data class RecoveryBackupMismatch(override val message: String = "Recovery backup verification failed") : VaultError(message)
    data class UnknownError(override val message: String = "Unknown error") : VaultError(message)

    companion object {
        fun fromMessage(message: String): VaultError {
            return when {
                message.contains("InvalidPin", ignoreCase = true) -> InvalidPin(message)
                message.contains("InvalidMnemonic", ignoreCase = true) -> InvalidMnemonic(message)
                message.contains("InvalidKeyMaterial", ignoreCase = true) -> InvalidKeyMaterial(message)
                message.contains("CryptoError", ignoreCase = true) -> CryptoError(message)
                message.contains("InvalidDerivationPath", ignoreCase = true) -> InvalidDerivationPath(message)
                message.contains("SerializationError", ignoreCase = true) -> SerializationError(message)
                message.contains("RecoveryBackupMismatch", ignoreCase = true) -> RecoveryBackupMismatch(message)
                else -> UnknownError(message)
            }
        }
    }
}
