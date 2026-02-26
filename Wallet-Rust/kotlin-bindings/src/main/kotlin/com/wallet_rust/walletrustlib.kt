// hassam dev: Generated Kotlin FFI bindings from wallet.udl (Phase 2)
// Auto-generated wrapper functions for Kotlin-Rust bridge

package com.wallet_rust

/**
 * hassam dev: FFI wrapper functions - auto-generated from wallet.udl
 * These functions provide the bridge between Kotlin and the Rust backend
 * via JNI (Java Native Interface).
 * 
 * Total: 27 FFI functions across 5 categories
 */

// ========================================
// VAULT MANAGEMENT FUNCTIONS (7 total)
// ========================================

/**
 * hassam dev: Generate a new BIP39 mnemonic phrase
 * @return 12 or 24-word BIP39 mnemonic string
 * @throws VaultError.CryptoError if entropy generation fails
 */
external fun generateMnemonicFfi(): String

/**
 * hassam dev: Create a new vault with random mnemonic
 * @param pin User's PIN for vault encryption
 * @return Encrypted VaultRecord
 * @throws VaultError if vault creation fails
 */
external fun createVaultFfi(pin: String): String

/**
 * hassam dev: Import vault from existing mnemonic
 * @param pin User's PIN for vault encryption
 * @param mnemonic BIP39 mnemonic phrase
 * @param path Derivation path (e.g., "m/44'/60'/0'/0")
 * @return Encrypted VaultRecord
 * @throws VaultError.InvalidMnemonic if mnemonic is invalid
 */
external fun createVaultFromMnemonicFfi(pin: String, mnemonic: String, path: String): String

/**
 * hassam dev: Import vault from private key hex
 * @param pin User's PIN for vault encryption
 * @param privateKeyHex Private key in hex format
 * @return Encrypted VaultRecord
 * @throws VaultError.InvalidKeyMaterial if key is invalid
 */
external fun createVaultFromPrivateKeyFfi(pin: String, privateKeyHex: String): String

/**
 * hassam dev: Verify PIN and return vault address
 * @param pin User's PIN to verify
 * @param record VaultRecord to unlock
 * @return Public Ethereum address if PIN is correct
 * @throws VaultError.InvalidPin if PIN verification fails
 */
external fun verifyPinFfi(pin: String, record: String): String

/**
 * hassam dev: Rotate PIN (change old PIN to new PIN)
 * @param oldPin Current PIN
 * @param newPin New PIN
 * @param record VaultRecord to update
 * @return Updated VaultRecord with new PIN
 * @throws VaultError.InvalidPin if old PIN is incorrect
 */
external fun rotatePinFfi(oldPin: String, newPin: String, record: String): String

/**
 * hassam dev: Export ETH private key from vault
 * @param pin User's PIN for vault access
 * @param record VaultRecord serialized as JSON
 * @return Private key in hex format (0x...)
 * @throws VaultError if PIN is invalid or export fails
 */
external fun exportEthPrivateKeyFfi(pin: String, record: String): String


/**
 * hassam dev: Migrate vault to new format/structure
 * @param pin User's PIN for vault access
 * @param record VaultRecord to migrate
 * @return Migrated VaultRecord
 * @throws VaultError if migration fails
 */
external fun migrateVaultFfi(pin: String, record: String): String

// ========================================
// TRANSACTION SIGNING FUNCTIONS (6 total)
// ========================================

/**
 * hassam dev: Sign legacy EVM transaction
 * @param pin User's PIN
 * @param record VaultRecord with private key
 * @param tx Unsigned legacy transaction
 * @return Signed transaction (hex-encoded)
 * @throws VaultError if signing fails
 */
external fun signTransactionFfi(pin: String, record: String, tx: String): String

/**
 * hassam dev: Sign EIP-1559 dynamic fee transaction
 * @param pin User's PIN
 * @param record VaultRecord with private key
 * @param tx Unsigned EIP-1559 transaction
 * @return Signed transaction (hex-encoded)
 * @throws VaultError if signing fails
 */
external fun signTransactionEip1559Ffi(pin: String, record: String, tx: String): String

/**
 * hassam dev: Sign legacy EVM with explicit chain ID validation
 * @param pin User's PIN
 * @param record VaultRecord with private key
 * @param tx Unsigned legacy transaction
 * @param expectedChainId Chain ID to validate against
 * @return Signed transaction (hex-encoded)
 * @throws VaultError if signing fails or chain ID mismatch
 */
external fun signTransactionWithChainFfi(
    pin: String,
    record: String,
    tx: String,
    expectedChainId: Long
): String

/**
 * hassam dev: Sign EIP-1559 with explicit chain ID validation
 * @param pin User's PIN
 * @param record VaultRecord with private key
 * @param tx Unsigned EIP-1559 transaction
 * @param expectedChainId Chain ID to validate against
 * @return Signed transaction (hex-encoded)
 * @throws VaultError if signing fails or chain ID mismatch
 */
external fun signTransactionEip1559WithChainFfi(
    pin: String,
    record: String,
    tx: String,
    expectedChainId: Long
): String

/**
 * hassam dev: Sign Solana transaction message
 * @param pin User's PIN
 * @param record VaultRecord with private key
 * @param message Solana transaction message (bytes)
 * @return Signed message (bytes)
 * @throws VaultError if signing fails
 */
external fun signSolanaTransactionFfi(pin: String, record: String, message: ByteArray): ByteArray

/**
 * hassam dev: Sign Bitcoin transaction sighash
 * @param pin User's PIN
 * @param record VaultRecord with private key
 * @param sighashHex Transaction sighash in hex format
 * @param testnet True for testnet, false for mainnet
 * @return Signature (hex-encoded)
 * @throws VaultError if signing fails
 */
external fun signBitcoinTransactionFfi(
    pin: String,
    record: String,
    sighashHex: String,
    testnet: Boolean
): String

// ========================================
// KEY DERIVATION & ADDRESS FUNCTIONS (5 total)
// ========================================

/**
 * hassam dev: Derive Bitcoin address from mnemonic
 * @param mnemonic BIP39 mnemonic phrase
 * @param testnet True for testnet, false for mainnet
 * @return Bitcoin address (P2PKH or P2WPKH format)
 * @throws VaultError.InvalidMnemonic if mnemonic is invalid
 */
external fun deriveBtcAddressFfi(mnemonic: String, testnet: Boolean): String

/**
 * hassam dev: Derive Solana address from mnemonic
 * @param mnemonic BIP39 mnemonic phrase
 * @return Solana address (base58-encoded)
 * @throws VaultError.InvalidMnemonic if mnemonic is invalid
 */
external fun deriveSolAddressFfi(mnemonic: String): String

/**
 * hassam dev: Get Bitcoin public key for signing
 * @param pin User's PIN
 * @param record VaultRecord with private key
 * @param testnet True for testnet, false for mainnet
 * @return Public key (hex-encoded)
 * @throws VaultError if retrieval fails
 */
external fun getBtcPublicKeyFfi(pin: String, record: String, testnet: Boolean): String

/**
 * hassam dev: Get all chain addresses (ETH, BTC, SOL) from vault
 * @param pin User's PIN
 * @param record VaultRecord with private key
 * @param testnet True for testnet, false for mainnet
 * @return MultichainAddresses with ETH, BTC, and SOL addresses
 * @throws VaultError if derivation fails
 */
external fun getMultichainAddressesFfi(
    pin: String,
    record: String,
    testnet: Boolean
): String

/**
 * hassam dev: Get all chain addresses at specific HD index
 * @param pin User's PIN
 * @param record VaultRecord with private key
 * @param testnet True for testnet, false for mainnet
 * @param index HD wallet index
 * @return MultichainAddresses at specified index
 * @throws VaultError if derivation fails
 */
external fun getMultichainAddressesByIndexFfi(
    pin: String,
    record: String,
    testnet: Boolean,
    index: Int
): String

// ========================================
// RECOVERY & BACKUP FUNCTIONS (6 total)
// ========================================

/**
 * hassam dev: Create encrypted recovery backup with passphrase
 * @param pin User's PIN for vault access
 * @param record VaultRecord to backup
 * @param backupPassphrase Passphrase for backup encryption
 * @return Encrypted RecoveryBackup
 * @throws VaultError if backup creation fails
 */
external fun createRecoveryBackupFfi(
    pin: String,
    record: String,
    backupPassphrase: String
): String

/**
 * hassam dev: Restore vault from recovery backup
 * @param backupPassphrase Passphrase for backup decryption
 * @param backup RecoveryBackup to restore from
 * @param newPin PIN for new vault
 * @return Restored VaultRecord
 * @throws VaultError if restoration fails
 */
external fun restoreVaultFromRecoveryBackupFfi(
    backupPassphrase: String,
    backup: String,
    newPin: String
): String

/**
 * hassam dev: Verify recovery backup integrity
 * @param backupPassphrase Passphrase for verification
 * @param backup RecoveryBackup to verify
 * @throws VaultError.RecoveryBackupMismatch if verification fails
 */
external fun verifyRecoveryBackupFfi(
    backupPassphrase: String,
    backup: String
): Unit

/**
 * hassam dev: Create cloud-safe recovery blob
 * @param pin User's PIN for vault access
 * @param record VaultRecord to backup
 * @param oauthKek OAuth-derived key encryption key
 * @return Encrypted CloudRecoveryBlob
 * @throws VaultError if blob creation fails
 */
external fun createCloudRecoveryBlobFfi(
    pin: String,
    record: String,
    oauthKek: String
): String

/**
 * hassam dev: Restore vault from cloud blob
 * @param oauthKek OAuth-derived key encryption key
 * @param blob CloudRecoveryBlob to restore from
 * @param newPin PIN for new vault
 * @return Restored VaultRecord
 * @throws VaultError if restoration fails
 */
external fun restoreVaultFromCloudRecoveryBlobFfi(
    oauthKek: String,
    blob: String,
    newPin: String
): String

/**
 * hassam dev: Verify cloud recovery blob integrity
 * @param oauthKek OAuth-derived key encryption key
 * @param blob CloudRecoveryBlob to verify
 * @throws VaultError if verification fails
 */
external fun verifyCloudRecoveryBlobFfi(
    oauthKek: String,
    blob: String
): Unit

// ========================================
// WEB3AUTH INTEGRATION FUNCTIONS (2 total)
// ========================================

/**
 * hassam dev: Create wallet from Web3Auth OAuth private key
 * @param web3authPrivateKey Private key from Web3Auth
 * @param testnet True for testnet, false for mainnet
 * @return Wallet with vault and address
 * @throws VaultError if wallet creation fails
 */
external fun createWalletFromWeb3authKeyFfi(
    web3authPrivateKey: String,
    testnet: Boolean
): String

/**
 * hassam dev: Restore wallet from Web3Auth key + encrypted data
 * @param web3authPrivateKey Private key from Web3Auth
 * @param encryptedData Encrypted wallet data
 * @param testnet True for testnet, false for mainnet
 * @return Restored wallet with vault and address
 * @throws VaultError if restoration fails
 */
external fun restoreWalletFromWeb3authKeyFfi(
    web3authPrivateKey: String,
    encryptedData: String,
    testnet: Boolean
): String

// ========================================
// OPTIONAL: Key Export (1 total - gated feature)
// ========================================

