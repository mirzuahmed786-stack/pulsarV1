// hassam dev: VaultApi convenience wrapper for Kotlin/JVM clients (Phase 2)
// Provides a user-friendly API that wraps the generated FFI functions

package com.wallet_rust

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * hassam dev: VaultApi - Convenience wrapper for vault operations
 * 
 * This singleton object provides a clean, Kotlin-idiomatic API for all 27
 * vault operations. It wraps the raw FFI functions from walletrustlib.kt
 * and handles common patterns like error conversion and default parameters.
 * 
 * All methods transparently handle JSON serialization/deserialization
 * for communication with the Rust FFI layer (Phase 4).
 * 
 * Example usage:
 * ```kotlin
 * try {
 *     val vault = VaultApi.createVault("1234")
 *     val address = VaultApi.verifyPin("1234", vault)
 *     println("Vault address: $address")
 * } catch (e: VaultError.InvalidPin) {
 *     println("Wrong PIN!")
 * }
 * ```
 * 
 * All methods are @JvmStatic for easy access from Java code.
 */
object VaultApi {
    
    // hassam dev: Custom ByteArray adapter for Moshi - converts to/from JSON arrays
    // Rust serializes Vec<u8> as [1, 2, 3, ...] so we need to match that format
    class ByteArrayAdapter {
        @com.squareup.moshi.ToJson
        fun toJson(value: ByteArray): List<Int> {
            return value.map { it.toInt() and 0xFF }
        }
        
        @com.squareup.moshi.FromJson
        fun fromJson(value: List<Int>): ByteArray {
            return value.map { it.toByte() }.toByteArray()
        }
    }
    
    // hassam dev: Moshi JSON serializer for FFI communication (Phase 4)
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(ByteArrayAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
    }
    
    // ========================================
    // JSON SERIALIZATION HELPERS (Phase 4)
    // ========================================
    
    /**
     * Serialize VaultRecord to JSON string for FFI communication
     */
    internal fun vaultToJson(vault: VaultRecord): String {
        return moshi.adapter(VaultRecord::class.java).toJson(vault)
    }
    
    /**
     * Check if FFI result contains an error and throw exception if it does
     */
    internal fun checkErrorResponse(result: String) {
        if (result.contains("\"error\":")) {
            val errorJson = moshi.adapter<Map<String, String>>(Map::class.java).fromJson(result)
            val errorMsg = errorJson?.get("error") ?: "Unknown error"
            throw VaultError.UnknownError(errorMsg)
        }
    }
    
    /**
     * Deserialize JSON string to VaultRecord from FFI result
     * Checks for error responses from Rust and throws appropriate VaultError
     */
    internal fun jsonToVault(json: String): VaultRecord {
        // Check if response contains an error
        checkErrorResponse(json)
        return moshi.adapter(VaultRecord::class.java).fromJson(json)
            ?: throw VaultError.SerializationError("Failed to deserialize VaultRecord")
    }
    
    /**
     * Serialize RecoveryBackup to JSON string for FFI communication
     */
    internal fun backupToJson(backup: RecoveryBackup): String {
        return moshi.adapter(RecoveryBackup::class.java).toJson(backup)
    }
    
    /**
     * Deserialize JSON string to RecoveryBackup from FFI result
     */
    internal fun jsonToBackup(json: String): RecoveryBackup {
        checkErrorResponse(json)
        return moshi.adapter(RecoveryBackup::class.java).fromJson(json)
            ?: throw VaultError.SerializationError("Failed to deserialize RecoveryBackup")
    }
    
    /**
     * Serialize CloudRecoveryBlob to JSON string for FFI communication
     */
    internal fun blobToJson(blob: CloudRecoveryBlob): String {
        return moshi.adapter(CloudRecoveryBlob::class.java).toJson(blob)
    }
    
    /**
     * Deserialize JSON string to CloudRecoveryBlob from FFI result
     */
    internal fun jsonToBlob(json: String): CloudRecoveryBlob {
        checkErrorResponse(json)
        return moshi.adapter(CloudRecoveryBlob::class.java).fromJson(json)
            ?: throw VaultError.SerializationError("Failed to deserialize CloudRecoveryBlob")
    }
    
    /**
     * Serialize MultichainAddresses to JSON string for FFI communication
     */
    internal fun addressesToJson(addresses: MultichainAddresses): String {
        return moshi.adapter(MultichainAddresses::class.java).toJson(addresses)
    }
    
    /**
     * Deserialize JSON string to MultichainAddresses from FFI result
     */
    internal fun jsonToAddresses(json: String): MultichainAddresses {
        return moshi.adapter(MultichainAddresses::class.java).fromJson(json)
            ?: throw VaultError.SerializationError("Failed to deserialize MultichainAddresses")
    }
    
    /**
     * Serialize UnsignedLegacyTx to JSON string for FFI communication
     */
    internal fun legacyTxToJson(tx: UnsignedLegacyTx): String {
        return moshi.adapter(UnsignedLegacyTx::class.java).toJson(tx)
    }
    
    /**
     * Serialize UnsignedEip1559Tx to JSON string for FFI communication
     */
    internal fun eip1559TxToJson(tx: UnsignedEip1559Tx): String {
        return moshi.adapter(UnsignedEip1559Tx::class.java).toJson(tx)
    }
    
    /**
     * Deserialize JSON string to Web3AuthWalletResult from FFI result
     */
    internal fun jsonToWeb3AuthResult(json: String): Web3AuthWalletResult {
        checkErrorResponse(json)
        return moshi.adapter(Web3AuthWalletResult::class.java).fromJson(json)
            ?: throw VaultError.SerializationError("Failed to deserialize Web3AuthWalletResult")
    }
    
    // ========================================
    // VAULT MANAGEMENT (7 METHODS)
    // ========================================
    
    /**
     * hassam dev: Generate a new BIP39 mnemonic phrase
     * 
     * Creates 24 random words from the BIP39 wordlist.
     * 
     * @return 24-word mnemonic string (space-separated)
     * @throws VaultError.CryptoError if entropy generation fails
     * 
     * Example:
     * ```kotlin
     * val mnemonic = VaultApi.generateMnemonic()
     * ```
     */
    @JvmStatic
    fun generateMnemonic(): String {
        val result = generateMnemonicFfi()
        checkErrorResponse(result)
        return result
    }
    
    /**
     * hassam dev: Create a new vault with random mnemonic
     * 
     * Generates a random BIP39 mnemonic and creates an encrypted vault.
     * The vault can be unlocked later with the same PIN.
     * 
     * @param pin User's PIN (any non-empty string)
     * @return Encrypted VaultRecord
     * @throws VaultError if vault creation fails
     * 
     * Example:
     * ```kotlin
     * val vault = VaultApi.createVault("1234")
     * ```
     */
    @JvmStatic
    fun createVault(pin: String): VaultRecord {
        val jsonResult = createVaultFfi(pin)
        return jsonToVault(jsonResult)
    }
    
    /**
     * hassam dev: Import vault from existing mnemonic
     * 
     * Creates a vault from a known BIP39 mnemonic phrase.
     * Default derivation path is "m/44'/60'/0'/0" (Ethereum standard).
     * 
     * @param pin User's PIN for vault encryption
     * @param mnemonic 12 or 24-word BIP39 mnemonic phrase
     * @param path BIP32 derivation path (default: "m/44'/60'/0'/0")
     * @return Encrypted VaultRecord
     * @throws VaultError.InvalidMnemonic if mnemonic is invalid
     * 
     * Example:
     * ```kotlin
     * val vault = VaultApi.createVaultFromMnemonic("1234", mnemonic)
     * val vaultCustomPath = VaultApi.createVaultFromMnemonic(
     *     "1234", mnemonic, "m/44'/0'/0'/0"  // Bitcoin path
     * )
     * ```
     */
    @JvmStatic
    fun createVaultFromMnemonic(
        pin: String,
        mnemonic: String,
        path: String = "m/44'/60'/0'/0/0"
    ): VaultRecord {
        val jsonResult = createVaultFromMnemonicFfi(pin, mnemonic, path)
        return jsonToVault(jsonResult)
    }
    
    /**
     * hassam dev: Import vault from private key hex
     * 
     * Creates a vault from a raw 32-byte private key in hex format.
     * Note: This won't have mnemonic backup capabilities.
     * 
     * @param pin User's PIN for vault encryption
     * @param privateKeyHex 32-byte private key as hex string (66 chars: "0x" + 64 hex chars)
     * @return Encrypted VaultRecord
     * @throws VaultError.InvalidKeyMaterial if key is invalid or wrong length
     * 
     * Example:
     * ```kotlin
     * val vault = VaultApi.createVaultFromPrivateKey(
     *     "1234", 
     *     "0x1234567890abcdef..." // 32 bytes in hex
     * )
     * ```
     */
    @JvmStatic
    fun createVaultFromPrivateKey(pin: String, privateKeyHex: String): VaultRecord {
        val jsonResult = createVaultFromPrivateKeyFfi(pin, privateKeyHex)
        return jsonToVault(jsonResult)
    }
    
    /**
     * hassam dev: Verify PIN and return vault address
     * 
     * Unlocks the vault with the provided PIN and returns the associated address.
     * This is a quick way to verify the PIN is correct.
     * 
     * @param pin User's PIN to verify
     * @param record VaultRecord to unlock
     * @return Ethereum address if PIN is correct
     * @throws VaultError.InvalidPin if PIN verification fails
     * 
     * Example:
     * ```kotlin
     * try {
     *     val address = VaultApi.verifyPin("1234", vault)
     * } catch (e: VaultError.InvalidPin) {
     *     println("Wrong PIN!")
     * }
     * ```
     */
    @JvmStatic
    fun verifyPin(pin: String, record: VaultRecord): String {
        val vaultJson = vaultToJson(record)
        val result = verifyPinFfi(pin, vaultJson)
        checkErrorResponse(result)
        return result
    }
    
    /**
     * hassam dev: Rotate PIN (change old PIN to new PIN)
     * 
     * Changes the vault's PIN without affecting the underlying secret.
     * Both old and new PINs must be non-empty.
     * 
     * @param oldPin Current PIN
     * @param newPin New PIN
     * @param record VaultRecord to update
     * @return Updated VaultRecord with new PIN
     * @throws VaultError.InvalidPin if old PIN is incorrect
     * 
     * Example:
     * ```kotlin
     * val updated = VaultApi.rotatePin("1234", "5678", vault)
     * ```
     */
    @JvmStatic
    fun rotatePin(oldPin: String, newPin: String, record: VaultRecord): VaultRecord {
        val vaultJson = vaultToJson(record)
        val resultJson = rotatePinFfi(oldPin, newPin, vaultJson)
        return jsonToVault(resultJson)
    }
    
    /**
     * hassam dev: Export the ETH private key from a vault
     * 
     * Decrypts the vault with the PIN and returns the Ethereum private key in hex format.
     * Used for wallet backup or external signing operations.
     * 
     * @param pin User's PIN for vault access
     * @param record VaultRecord to export key from
     * @return ETH private key as hex string (format: 0x...)
     * @throws VaultError if PIN is invalid or export fails
     * 
     * Example:
     * ```kotlin
     * val privateKeyHex = VaultApi.exportEthPrivateKey("1234", vault)
     * // Returns: "0xabcd1234..."
     * ```
     */
    @JvmStatic
    fun exportEthPrivateKey(pin: String, record: VaultRecord): String {
        val vaultJson = vaultToJson(record)
        val resultJson = exportEthPrivateKeyFfi(pin, vaultJson)
        // Check for error response and return private key directly
        checkErrorResponse(resultJson)
        return resultJson
    }
    
    /**
     * hassam dev: Migrate vault to new format/structure
     * 
     * Updates vault to latest format/structure if needed.
     * Safe to call multiple times (no-op if already migrated).
     * 
     * @param pin User's PIN for vault access
     * @param record VaultRecord to migrate
     * @return Migrated VaultRecord (may be same as input if already migrated)
     * @throws VaultError if migration fails
     * 
     * Example:
     * ```kotlin
     * val migrated = VaultApi.migrateVault("1234", vault)
     * ```
     */
    @JvmStatic
    fun migrateVault(pin: String, record: VaultRecord): VaultRecord {
        val vaultJson = vaultToJson(record)
        val resultJson = migrateVaultFfi(pin, vaultJson)
        return jsonToVault(resultJson)
    }
    
    // ========================================
    // TRANSACTION SIGNING (6 METHODS)
    // ========================================
    
    /**
     * hassam dev: Sign legacy EVM transaction
     * 
     * Signs a pre-EIP1559 Ethereum transaction. The transaction must match
     * the vault's chain ID, or use signTransactionWithChain() for validation.
     * 
     * @param pin User's PIN
     * @param record VaultRecord with private key
     * @param tx Unsigned legacy transaction
     * @return Signed transaction in RLP-encoded hex format (0x-prefixed)
     * @throws VaultError if signing fails
     * 
     * Example:
     * ```kotlin
     * val tx = UnsignedLegacyTx(
     *     nonce = 0u,
     *     gas_price = "20000000000",  // 20 Gwei
     *     gas_limit = 21000u,
     *     to = "0xRecipient...",
     *     value = "1000000000000000000",  // 1 ETH in Wei
     *     data = "",
     *     chain_id = 1u
     * )
     * val signature = VaultApi.signTransaction("1234", vault, tx)
     * ```
     */
    @JvmStatic
    fun signTransaction(pin: String, record: VaultRecord, tx: UnsignedLegacyTx): String {
        val vaultJson = vaultToJson(record)
        val txJson = legacyTxToJson(tx)
        return signTransactionFfi(pin, vaultJson, txJson)
    }
    
    /**
     * hassam dev: Sign EIP-1559 dynamic fee transaction
     * 
     * Signs a modern Ethereum transaction with dynamic gas fees (post-London fork).
     * More efficient than legacy transactions.
     * 
     * @param pin User's PIN
     * @param record VaultRecord with private key
     * @param tx Unsigned EIP-1559 transaction
     * @return Signed transaction in RLP-encoded hex format (0x-prefixed)
     * @throws VaultError if signing fails
     * 
     * Example:
     * ```kotlin
     * val tx = UnsignedEip1559Tx(
     *     chain_id = 1u,
     *     nonce = 0u,
     *     max_priority_fee_per_gas = "2000000000",  // 2 Gwei
     *     max_fee_per_gas = "50000000000",          // 50 Gwei
     *     gas_limit = 21000u,
     *     to = "0xRecipient...",
     *     value = "1000000000000000000",
     *     data = "",
     *     access_list = emptyList()
     * )
     * val signature = VaultApi.signTransactionEip1559("1234", vault, tx)
     * ```
     */
    @JvmStatic
    fun signTransactionEip1559(pin: String, record: VaultRecord, tx: UnsignedEip1559Tx): String {
        val vaultJson = vaultToJson(record)
        val txJson = eip1559TxToJson(tx)
        return signTransactionEip1559Ffi(pin, vaultJson, txJson)
    }
    
    /**
     * hassam dev: Sign legacy EVM transaction with explicit chain ID validation
     * 
     * Secure variant that validates the transaction chain ID matches expected.
     * Prevents accidentally signing transaction for wrong chain.
     * 
     * @param pin User's PIN
     * @param record VaultRecord with private key
     * @param tx Unsigned legacy transaction
     * @param expectedChainId Expected chain ID (1=Mainnet, 5=Goerli, etc.)
     * @return Signed transaction in RLP-encoded hex format
     * @throws VaultError if signing fails or chain ID mismatch
     * 
     * Example:
     * ```kotlin
     * val signature = VaultApi.signTransactionWithChain(
     *     "1234", vault, tx,
    *     expectedChainId = 1L  // Mainnet only
     * )
     * ```
     */
    @JvmStatic
    fun signTransactionWithChain(
        pin: String,
        record: VaultRecord,
        tx: UnsignedLegacyTx,
        expectedChainId: Long
    ): String {
        val vaultJson = vaultToJson(record)
        val txJson = legacyTxToJson(tx)
        return signTransactionWithChainFfi(pin, vaultJson, txJson, expectedChainId)
    }
    
    /**
     * hassam dev: Sign EIP-1559 transaction with explicit chain ID validation
     * 
     * Secure variant for EIP-1559 that validates chain ID.
     * 
     * @param pin User's PIN
     * @param record VaultRecord with private key
     * @param tx Unsigned EIP-1559 transaction
     * @param expectedChainId Expected chain ID
     * @return Signed transaction in RLP-encoded hex format
     * @throws VaultError if signing fails or chain ID mismatch
     */
    @JvmStatic
    fun signTransactionEip1559WithChain(
        pin: String,
        record: VaultRecord,
        tx: UnsignedEip1559Tx,
        expectedChainId: Long
    ): String {
        val vaultJson = vaultToJson(record)
        val txJson = eip1559TxToJson(tx)
        return signTransactionEip1559WithChainFfi(pin, vaultJson, txJson, expectedChainId)
    }
    
    /**
     * hassam dev: Sign Solana transaction message
     * 
     * Signs a Solana transaction message for blockchain submission.
     * 
     * @param pin User's PIN
     * @param record VaultRecord with Solana private key
     * @param message Solana transaction message (raw bytes)
     * @return Signature (64 bytes for Ed25519)
     * @throws VaultError if signing fails
     * 
     * Example:
     * ```kotlin
     * val message = /* transaction message bytes */
     * val signature = VaultApi.signSolanaTransaction("1234", vault, message)
     * ```
     */
    @JvmStatic
    fun signSolanaTransaction(pin: String, record: VaultRecord, message: ByteArray): ByteArray {
        val vaultJson = vaultToJson(record)
        return signSolanaTransactionFfi(pin, vaultJson, message)
    }
    
    /**
     * hassam dev: Sign Bitcoin transaction sighash
     * 
     * Signs a Bitcoin transaction sighash for UTXO submission.
     * 
     * @param pin User's PIN
     * @param record VaultRecord with Bitcoin private key
     * @param sighashHex Transaction sighash in hex format (0x-prefixed)
     * @param testnet True for testnet, false for mainnet
     * @return DER-encoded signature in hex format
     * @throws VaultError if signing fails
     * 
     * Example:
     * ```kotlin
     * val signature = VaultApi.signBitcoinTransaction(
     *     "1234", vault, sighashHex,
     *     testnet = false  // Mainnet
     * )
     * ```
     */
    @JvmStatic
    fun signBitcoinTransaction(
        pin: String,
        record: VaultRecord,
        sighashHex: String,
        testnet: Boolean
    ): String {
        val vaultJson = vaultToJson(record)
        return signBitcoinTransactionFfi(pin, vaultJson, sighashHex, testnet)
    }
    
    // ========================================
    // KEY DERIVATION & ADDRESSES (5 METHODS)
    // ========================================
    
    /**
     * hassam dev: Derive Bitcoin address from mnemonic
     * 
     * Derives a Bitcoin address from a BIP39 mnemonic using standard path.
     * No PIN required (mnemonic is public for address derivation).
     * 
     * @param mnemonic BIP39 mnemonic phrase
     * @param testnet True for testnet addresses, false for mainnet
     * @return Bitcoin address (P2WPKH format)
     * @throws VaultError.InvalidMnemonic if mnemonic is invalid
     * 
     * Example:
     * ```kotlin
     * val btcAddr = VaultApi.deriveBtcAddress(mnemonic, testnet = false)
     * ```
     */
    @JvmStatic
    fun deriveBtcAddress(mnemonic: String, testnet: Boolean): String {
        return deriveBtcAddressFfi(mnemonic, testnet)
    }
    
    /**
     * hassam dev: Derive Solana address from mnemonic
     * 
     * Derives a Solana address from a BIP39 mnemonic.
     * Uses the standard Solana derivation path.
     * 
     * @param mnemonic BIP39 mnemonic phrase
     * @return Solana address (base58-encoded)
     * @throws VaultError.InvalidMnemonic if mnemonic is invalid
     * 
     * Example:
     * ```kotlin
     * val solAddr = VaultApi.deriveSolAddress(mnemonic)
     * ```
     */
    @JvmStatic
    fun deriveSolAddress(mnemonic: String): String {
        return deriveSolAddressFfi(mnemonic)
    }
    
    /**
     * hassam dev: Get Bitcoin public key for signing
     * 
     * Extracts the Bitcoin public key from vault for transaction verification.
     * 
     * @param pin User's PIN
     * @param record VaultRecord with private key
     * @param testnet True for testnet, false for mainnet
     * @return Public key in hex format
     * @throws VaultError if retrieval fails
     */
    @JvmStatic
    fun getBtcPublicKey(pin: String, record: VaultRecord, testnet: Boolean): String {
        val vaultJson = vaultToJson(record)
        return getBtcPublicKeyFfi(pin, vaultJson, testnet)
    }
    
    /**
     * hassam dev: Get all chain addresses (ETH, BTC, SOL) from vault
     * 
     * Derives addresses for all supported chains in one call.
     * Convenience method for multi-chain wallet setup.
     * 
     * @param pin User's PIN
     * @param record VaultRecord with private key
     * @param testnet True for testnet, false for mainnet
     * @return MultichainAddresses with ETH, BTC, and SOL addresses
     * @throws VaultError if derivation fails
     * 
     * Example:
     * ```kotlin
     * val addresses = VaultApi.getMultichainAddresses("1234", vault, testnet = false)
     * println("ETH: ${addresses.eth}")
     * println("BTC: ${addresses.btc}")
     * println("SOL: ${addresses.sol}")
     * ```
     */
    @JvmStatic
    fun getMultichainAddresses(
        pin: String,
        record: VaultRecord,
        testnet: Boolean
    ): MultichainAddresses {
        val vaultJson = vaultToJson(record)
        val resultJson = getMultichainAddressesFfi(pin, vaultJson, testnet)
        return jsonToAddresses(resultJson)
    }
    
    /**
     * hassam dev: Get all chain addresses at specific HD index
     * 
     * For HD wallets, derives addresses at a specific account index.
     * Useful for multi-account wallets (index 0, 1, 2, etc.).
     * 
     * @param pin User's PIN
     * @param record VaultRecord with private key
     * @param testnet True for testnet, false for mainnet
     * @param index HD wallet index (0, 1, 2...)
     * @return MultichainAddresses at the specified index
     * @throws VaultError if derivation fails
     * 
     * Example:
     * ```kotlin
     * val addr1 = VaultApi.getMultichainAddressesByIndex("1234", vault, false, 0u)
     * val addr2 = VaultApi.getMultichainAddressesByIndex("1234", vault, false, 1u)
     * ```
     */
    @JvmStatic
    fun getMultichainAddressesByIndex(
        pin: String,
        record: VaultRecord,
        testnet: Boolean,
        index: Int
    ): MultichainAddresses {
        val vaultJson = vaultToJson(record)
        val resultJson = getMultichainAddressesByIndexFfi(pin, vaultJson, testnet, index)
        return jsonToAddresses(resultJson)
    }
    
    // ========================================
    // RECOVERY & BACKUP (6 METHODS)
    // ========================================
    
    /**
     * hassam dev: Create encrypted recovery backup with passphrase
     * 
     * Creates a portable backup that can be stored securely offline.
     * Can be restored with the backup passphrase (not the original PIN).
     * 
     * @param pin User's PIN for vault access
     * @param record VaultRecord to backup
     * @param backupPassphrase Passphrase for backup encryption (different from PIN)
     * @return Encrypted RecoveryBackup
     * @throws VaultError if backup creation fails
     * 
     * Example:
     * ```kotlin
     * val backup = VaultApi.createRecoveryBackup(
     *     "1234",
     *     vault,
     *     "my-backup-passphrase"
     * )
     * // Store backup.toString() securely
     * ```
     */
    @JvmStatic
    fun createRecoveryBackup(
        pin: String,
        record: VaultRecord,
        backupPassphrase: String
    ): RecoveryBackup {
        val vaultJson = vaultToJson(record)
        val resultJson = createRecoveryBackupFfi(pin, vaultJson, backupPassphrase)
        return jsonToBackup(resultJson)
    }
    
    /**
     * hassam dev: Restore vault from recovery backup
     * 
     * Restores a vault from a previously created backup.
     * The backup is unlocked with the backup passphrase, then re-encrypted
     * with a new PIN.
     * 
     * @param backupPassphrase Passphrase used when creating the backup
     * @param backup RecoveryBackup to restore from
     * @param newPin PIN for the restored vault
     * @return Restored VaultRecord
     * @throws VaultError if restoration fails
     * 
     * Example:
     * ```kotlin
     * val vault = VaultApi.restoreVaultFromRecoveryBackup(
     *     "my-backup-passphrase",
     *     backup,
     *     "new-pin"
     * )
     * ```
     */
    @JvmStatic
    fun restoreVaultFromRecoveryBackup(
        backupPassphrase: String,
        backup: RecoveryBackup,
        newPin: String
    ): VaultRecord {
        val backupJson = backupToJson(backup)
        val resultJson = restoreVaultFromRecoveryBackupFfi(backupPassphrase, backupJson, newPin)
        return jsonToVault(resultJson)
    }
    
    /**
     * hassam dev: Verify recovery backup integrity
     * 
     * Quick check that a backup is valid and uncorrupted.
     * Useful before attempting full restoration.
     * 
     * @param backupPassphrase Passphrase used when creating the backup
     * @param backup RecoveryBackup to verify
     * @throws VaultError.RecoveryBackupMismatch if backup is corrupted
     * @throws VaultError if verification fails
     * 
     * Example:
     * ```kotlin
     * try {
     *     VaultApi.verifyRecoveryBackup(passphrase, backup)
     *     println("Backup is valid!")
     * } catch (e: VaultError.RecoveryBackupMismatch) {
     *     println("Backup is corrupted!")
     * }
     * ```
     */
    @JvmStatic
    fun verifyRecoveryBackup(backupPassphrase: String, backup: RecoveryBackup) {
        val backupJson = backupToJson(backup)
        verifyRecoveryBackupFfi(backupPassphrase, backupJson)
    }
    
    /**
     * hassam dev: Create cloud-safe recovery blob
     * 
     * Creates a minimal recovery blob optimized for cloud storage.
     * Encrypted with OAuth-derived key for secure cloud backup.
     * 
     * @param pin User's PIN for vault access
     * @param record VaultRecord to backup
     * @param oauthKek OAuth-derived key encryption key (from OAuth provider)
     * @return Encrypted CloudRecoveryBlob
     * @throws VaultError if blob creation fails
     * 
     * Example:
     * ```kotlin
     * val blob = VaultApi.createCloudRecoveryBlob(
     *     "1234",
     *     vault,
     *     oauthKek = "oauth-derived-key-from-google-or-apple"
     * )
     * // Upload blob.encrypted_seed_blob to cloud storage
     * ```
     */
    @JvmStatic
    fun createCloudRecoveryBlob(
        pin: String,
        record: VaultRecord,
        oauthKek: String
    ): CloudRecoveryBlob {
        val vaultJson = vaultToJson(record)
        val resultJson = createCloudRecoveryBlobFfi(pin, vaultJson, oauthKek)
        return jsonToBlob(resultJson)
    }
    
    /**
     * hassam dev: Restore vault from cloud blob
     * 
     * Restores a vault from a cloud recovery blob using the same OAuth key.
     * 
     * @param oauthKek OAuth-derived key encryption key
     * @param blob CloudRecoveryBlob to restore from
     * @param newPin PIN for the restored vault
     * @return Restored VaultRecord
     * @throws VaultError if restoration fails
     * 
     * Example:
     * ```kotlin
     * val vault = VaultApi.restoreVaultFromCloudRecoveryBlob(
     *     oauthKek = "same-oauth-key",
     *     blob,
     *     newPin = "new-pin"
     * )
     * ```
     */
    @JvmStatic
    fun restoreVaultFromCloudRecoveryBlob(
        oauthKek: String,
        blob: CloudRecoveryBlob,
        newPin: String
    ): VaultRecord {
        val blobJson = blobToJson(blob)
        val resultJson = restoreVaultFromCloudRecoveryBlobFfi(oauthKek, blobJson, newPin)
        return jsonToVault(resultJson)
    }
    
    /**
     * hassam dev: Verify cloud recovery blob integrity
     * 
     * Quick verification that a cloud blob is valid before restoration.
     * 
     * @param oauthKek OAuth-derived key encryption key
     * @param blob CloudRecoveryBlob to verify
     * @throws VaultError if verification fails
     * 
     * Example:
     * ```kotlin
     * VaultApi.verifyCloudRecoveryBlob(oauthKek, blob)
     * ```
     */
    @JvmStatic
    fun verifyCloudRecoveryBlob(oauthKek: String, blob: CloudRecoveryBlob) {
        val blobJson = blobToJson(blob)
        verifyCloudRecoveryBlobFfi(oauthKek, blobJson)
    }
    
    // ========================================
    // WEB3AUTH INTEGRATION (2 METHODS)
    // ========================================
    
    /**
     * hassam dev: Create wallet from Web3Auth OAuth private key
     * 
     * Integrates with Web3Auth for OAuth-based wallet creation.
     * Creates vault using Web3Auth-provided private key.
     * 
     * @param web3authPrivateKey Private key received from Web3Auth
     * @param testnet True for testnet, false for mainnet
     * @return Web3AuthWalletResult with vault and address
     * @throws VaultError if wallet creation fails
     * 
     * Example:
     * ```kotlin
     * val result = VaultApi.createWalletFromWeb3authKey(
     *     web3authPrivateKey,
     *     testnet = false
     * )
     * println("Wallet address: ${result.address}")
     * ```
     */
    @JvmStatic
    fun createWalletFromWeb3authKey(
        web3authPrivateKey: String,
        testnet: Boolean
    ): Web3AuthWalletResult {
        val resultJson = createWalletFromWeb3authKeyFfi(web3authPrivateKey, testnet)
        return jsonToWeb3AuthResult(resultJson)
    }
    
    /**
     * hassam dev: Restore wallet from Web3Auth key + encrypted data
     * 
     * Recovers an existing wallet using Web3Auth key and encrypted wallet data.
     * 
     * @param web3authPrivateKey Private key from Web3Auth
     * @param encryptedData Previously encrypted wallet data
     * @param testnet True for testnet, false for mainnet
     * @return Web3AuthWalletResult with restored vault and address
     * @throws VaultError if restoration fails
     * 
     * Example:
     * ```kotlin
     * val result = VaultApi.restoreWalletFromWeb3authKey(
     *     web3authPrivateKey,
     *     encryptedDataFromStorage,
     *     testnet = false
     * )
     * ```
     */
    @JvmStatic
    fun restoreWalletFromWeb3authKey(
        web3authPrivateKey: String,
        encryptedData: String,
        testnet: Boolean
    ): Web3AuthWalletResult {
        val resultJson = restoreWalletFromWeb3authKeyFfi(web3authPrivateKey, encryptedData, testnet)
        return jsonToWeb3AuthResult(resultJson)
    }
    
    // ========================================
    // OPTIONAL: KEY EXPORT (1 METHOD - Gated Feature)
    // ========================================
    
}
