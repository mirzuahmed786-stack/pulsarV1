package com.elementa.wallet.data.bindings

// FFI declarations for native Rust wallet functions via JNI
// Each function corresponds to a JNI wrapper in the Rust jni.rs

object WallettrustlibKt {
    init {
        try {
            System.loadLibrary("wallet_core")
        } catch (e: Exception) {
            throw RuntimeException("Failed to load wallet_core native library", e)
        }
    }

    // Vault Management (7 functions)
    external fun generateMnemonicFfi(): String
    external fun createVaultFfi(pin: String): String
    external fun createVaultFromMnemonicFfi(pin: String, mnemonic: String, path: String): String
    external fun createVaultFromPrivateKeyFfi(pin: String, privateKeyHex: String): String
    external fun verifyPinFfi(pin: String, vaultJson: String): String
    external fun rotatePinFfi(oldPin: String, newPin: String, vaultJson: String): String
    external fun exportEthPrivateKeyFfi(pin: String, vaultJson: String): String
    external fun migrateVaultFfi(pin: String, vaultJson: String): String

    // Transaction Signing (6 functions)
    external fun signTransactionFfi(pin: String, vaultJson: String, txJson: String): String
    external fun signTransactionEip1559Ffi(pin: String, vaultJson: String, txJson: String): String
    external fun signTransactionWithChainFfi(pin: String, vaultJson: String, txJson: String, expectedChainId: Long): String
    external fun signTransactionEip1559WithChainFfi(pin: String, vaultJson: String, txJson: String, expectedChainId: Long): String
    external fun signSolanaTransactionFfi(pin: String, vaultJson: String, message: ByteArray): ByteArray
    external fun signBitcoinTransactionFfi(pin: String, vaultJson: String, sighashHex: String, testnet: Boolean): String

    // Key Derivation & Addresses (5 functions)
    external fun deriveBtcAddressFfi(mnemonic: String, testnet: Boolean): String
    external fun deriveSolAddressFfi(mnemonic: String): String
    external fun getBtcPublicKeyFfi(pin: String, vaultJson: String, testnet: Boolean): String
    external fun getMultichainAddressesFfi(pin: String, vaultJson: String, testnet: Boolean): String
    external fun getMultichainAddressesByIndexFfi(pin: String, vaultJson: String, testnet: Boolean, index: Int): String

    // Recovery & Backup (6 functions)
    external fun createRecoveryBackupFfi(pin: String, vaultJson: String, backupPassphrase: String): String
    external fun restoreVaultFromRecoveryBackupFfi(backupPassphrase: String, backupJson: String, newPin: String): String
    external fun verifyRecoveryBackupFfi(backupPassphrase: String, backupJson: String)
    external fun createCloudRecoveryBlobFfi(pin: String, vaultJson: String, oauthKek: String): String
    external fun restoreVaultFromCloudRecoveryBlobFfi(oauthKek: String, blobJson: String, newPin: String): String
    external fun verifyCloudRecoveryBlobFfi(oauthKek: String, blobJson: String)

    // Web3Auth Integration (2 functions)
    external fun createWalletFromWeb3authKeyFfi(web3authPrivateKey: String, testnet: Boolean): String
    external fun restoreWalletFromWeb3authKeyFfi(web3authPrivateKey: String, encryptedData: String, testnet: Boolean): String
}
