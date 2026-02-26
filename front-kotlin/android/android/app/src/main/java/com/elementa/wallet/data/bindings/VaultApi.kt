package com.elementa.wallet.data.bindings

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * VaultApi - Convenience wrapper for all 27 vault operations
 * 
 * This singleton provides a clean Kotlin-idiomatic API for wallet operations.
 * It wraps raw FFI functions and handles JSON serialization/deserialization.
 */
object VaultApi {
    
    // Custom ByteArray adapter for Moshi
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
    
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(ByteArrayAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
    }
    
    // ========================================
    // JSON SERIALIZATION HELPERS
    // ========================================
    
    internal fun vaultToJson(vault: VaultRecord): String {
        return moshi.adapter(VaultRecord::class.java).toJson(vault)
    }
    
    internal fun checkErrorResponse(result: String) {
        if (result.contains("\"error\":")) {
            try {
                // Try to parse as JSON to extract error message
                if (result.startsWith("{") && result.contains("error")) {
                    val errorStart = result.indexOf("\"error\"")
                    val errorValueStart = result.indexOf("\"", errorStart + 8)
                    val errorValueEnd = result.indexOf("\"", errorValueStart + 1)
                    if (errorStart != -1 && errorValueStart != -1 && errorValueEnd != -1) {
                        val errorMsg = result.substring(errorValueStart + 1, errorValueEnd)
                        throw VaultError.UnknownError(errorMsg)
                    }
                }
            } catch (e: Exception) {
                // Fall through
            }
            throw VaultError.UnknownError(result)
        }
    }
    
    internal fun jsonToVault(json: String): VaultRecord {
        checkErrorResponse(json)
        return moshi.adapter(VaultRecord::class.java).fromJson(json)
            ?: throw VaultError.SerializationError("Failed to deserialize VaultRecord")
    }
    
    internal fun backupToJson(backup: RecoveryBackup): String {
        return moshi.adapter(RecoveryBackup::class.java).toJson(backup)
    }
    
    internal fun jsonToBackup(json: String): RecoveryBackup {
        checkErrorResponse(json)
        return moshi.adapter(RecoveryBackup::class.java).fromJson(json)
            ?: throw VaultError.SerializationError("Failed to deserialize RecoveryBackup")
    }
    
    internal fun blobToJson(blob: CloudRecoveryBlob): String {
        return moshi.adapter(CloudRecoveryBlob::class.java).toJson(blob)
    }
    
    internal fun jsonToBlob(json: String): CloudRecoveryBlob {
        checkErrorResponse(json)
        return moshi.adapter(CloudRecoveryBlob::class.java).fromJson(json)
            ?: throw VaultError.SerializationError("Failed to deserialize CloudRecoveryBlob")
    }
    
    internal fun addressesToJson(addresses: MultichainAddresses): String {
        return moshi.adapter(MultichainAddresses::class.java).toJson(addresses)
    }
    
    internal fun jsonToAddresses(json: String): MultichainAddresses {
        return moshi.adapter(MultichainAddresses::class.java).fromJson(json)
            ?: throw VaultError.SerializationError("Failed to deserialize MultichainAddresses")
    }
    
    internal fun legacyTxToJson(tx: UnsignedLegacyTx): String {
        return moshi.adapter(UnsignedLegacyTx::class.java).toJson(tx)
    }
    
    internal fun eip1559TxToJson(tx: UnsignedEip1559Tx): String {
        return moshi.adapter(UnsignedEip1559Tx::class.java).toJson(tx)
    }
    
    internal fun jsonToWeb3AuthResult(json: String): Web3AuthWalletResult {
        checkErrorResponse(json)
        return moshi.adapter(Web3AuthWalletResult::class.java).fromJson(json)
            ?: throw VaultError.SerializationError("Failed to deserialize Web3AuthWalletResult")
    }
    
    // ========================================
    // VAULT MANAGEMENT (7 METHODS)
    // ========================================
    
    @JvmStatic
    fun generateMnemonic(): String {
        val result = WallettrustlibKt.generateMnemonicFfi()
        checkErrorResponse(result)
        return result
    }
    
    @JvmStatic
    fun createVault(pin: String): VaultRecord {
        val jsonResult = WallettrustlibKt.createVaultFfi(pin)
        return jsonToVault(jsonResult)
    }
    
    @JvmStatic
    fun createVaultFromMnemonic(
        pin: String,
        mnemonic: String,
        path: String = "m/44'/60'/0'/0/0"
    ): VaultRecord {
        val jsonResult = WallettrustlibKt.createVaultFromMnemonicFfi(pin, mnemonic, path)
        return jsonToVault(jsonResult)
    }
    
    @JvmStatic
    fun createVaultFromPrivateKey(pin: String, privateKeyHex: String): VaultRecord {
        val jsonResult = WallettrustlibKt.createVaultFromPrivateKeyFfi(pin, privateKeyHex)
        return jsonToVault(jsonResult)
    }
    
    @JvmStatic
    fun verifyPin(pin: String, record: VaultRecord): String {
        val vaultJson = vaultToJson(record)
        val result = WallettrustlibKt.verifyPinFfi(pin, vaultJson)
        checkErrorResponse(result)
        return result
    }
    
    @JvmStatic
    fun rotatePin(oldPin: String, newPin: String, record: VaultRecord): VaultRecord {
        val vaultJson = vaultToJson(record)
        val resultJson = WallettrustlibKt.rotatePinFfi(oldPin, newPin, vaultJson)
        return jsonToVault(resultJson)
    }
    
    @JvmStatic
    fun exportEthPrivateKey(pin: String, record: VaultRecord): String {
        val vaultJson = vaultToJson(record)
        val resultJson = WallettrustlibKt.exportEthPrivateKeyFfi(pin, vaultJson)
        checkErrorResponse(resultJson)
        return resultJson
    }
    
    @JvmStatic
    fun migrateVault(pin: String, record: VaultRecord): VaultRecord {
        val vaultJson = vaultToJson(record)
        val resultJson = WallettrustlibKt.migrateVaultFfi(pin, vaultJson)
        return jsonToVault(resultJson)
    }
    
    // ========================================
    // TRANSACTION SIGNING (6 METHODS)
    // ========================================
    
    @JvmStatic
    fun signTransaction(pin: String, record: VaultRecord, tx: UnsignedLegacyTx): String {
        val vaultJson = vaultToJson(record)
        val txJson = legacyTxToJson(tx)
        return WallettrustlibKt.signTransactionFfi(pin, vaultJson, txJson)
    }
    
    @JvmStatic
    fun signTransactionEip1559(pin: String, record: VaultRecord, tx: UnsignedEip1559Tx): String {
        val vaultJson = vaultToJson(record)
        val txJson = eip1559TxToJson(tx)
        return WallettrustlibKt.signTransactionEip1559Ffi(pin, vaultJson, txJson)
    }
    
    @JvmStatic
    fun signTransactionWithChain(
        pin: String,
        record: VaultRecord,
        tx: UnsignedLegacyTx,
        expectedChainId: Long
    ): String {
        val vaultJson = vaultToJson(record)
        val txJson = legacyTxToJson(tx)
        return WallettrustlibKt.signTransactionWithChainFfi(pin, vaultJson, txJson, expectedChainId)
    }
    
    @JvmStatic
    fun signTransactionEip1559WithChain(
        pin: String,
        record: VaultRecord,
        tx: UnsignedEip1559Tx,
        expectedChainId: Long
    ): String {
        val vaultJson = vaultToJson(record)
        val txJson = eip1559TxToJson(tx)
        return WallettrustlibKt.signTransactionEip1559WithChainFfi(pin, vaultJson, txJson, expectedChainId)
    }
    
    @JvmStatic
    fun signSolanaTransaction(pin: String, record: VaultRecord, message: ByteArray): ByteArray {
        val vaultJson = vaultToJson(record)
        return WallettrustlibKt.signSolanaTransactionFfi(pin, vaultJson, message)
    }
    
    @JvmStatic
    fun signBitcoinTransaction(
        pin: String,
        record: VaultRecord,
        sighashHex: String,
        testnet: Boolean
    ): String {
        val vaultJson = vaultToJson(record)
        return WallettrustlibKt.signBitcoinTransactionFfi(pin, vaultJson, sighashHex, testnet)
    }
    
    // ========================================
    // KEY DERIVATION & ADDRESSES (5 METHODS)
    // ========================================
    
    @JvmStatic
    fun deriveBtcAddress(mnemonic: String, testnet: Boolean): String {
        return WallettrustlibKt.deriveBtcAddressFfi(mnemonic, testnet)
    }
    
    @JvmStatic
    fun deriveSolAddress(mnemonic: String): String {
        return WallettrustlibKt.deriveSolAddressFfi(mnemonic)
    }
    
    @JvmStatic
    fun getBtcPublicKey(pin: String, record: VaultRecord, testnet: Boolean): String {
        val vaultJson = vaultToJson(record)
        return WallettrustlibKt.getBtcPublicKeyFfi(pin, vaultJson, testnet)
    }
    
    @JvmStatic
    fun getMultichainAddresses(
        pin: String,
        record: VaultRecord,
        testnet: Boolean
    ): MultichainAddresses {
        val vaultJson = vaultToJson(record)
        val resultJson = WallettrustlibKt.getMultichainAddressesFfi(pin, vaultJson, testnet)
        return jsonToAddresses(resultJson)
    }
    
    @JvmStatic
    fun getMultichainAddressesByIndex(
        pin: String,
        record: VaultRecord,
        testnet: Boolean,
        index: Int
    ): MultichainAddresses {
        val vaultJson = vaultToJson(record)
        val resultJson = WallettrustlibKt.getMultichainAddressesByIndexFfi(pin, vaultJson, testnet, index)
        return jsonToAddresses(resultJson)
    }
    
    // ========================================
    // RECOVERY & BACKUP (6 METHODS)
    // ========================================
    
    @JvmStatic
    fun createRecoveryBackup(
        pin: String,
        record: VaultRecord,
        backupPassphrase: String
    ): RecoveryBackup {
        val vaultJson = vaultToJson(record)
        val resultJson = WallettrustlibKt.createRecoveryBackupFfi(pin, vaultJson, backupPassphrase)
        return jsonToBackup(resultJson)
    }
    
    @JvmStatic
    fun restoreVaultFromRecoveryBackup(
        backupPassphrase: String,
        backup: RecoveryBackup,
        newPin: String
    ): VaultRecord {
        val backupJson = backupToJson(backup)
        val resultJson = WallettrustlibKt.restoreVaultFromRecoveryBackupFfi(backupPassphrase, backupJson, newPin)
        return jsonToVault(resultJson)
    }
    
    @JvmStatic
    fun verifyRecoveryBackup(backupPassphrase: String, backup: RecoveryBackup) {
        val backupJson = backupToJson(backup)
        WallettrustlibKt.verifyRecoveryBackupFfi(backupPassphrase, backupJson)
    }
    
    @JvmStatic
    fun createCloudRecoveryBlob(
        pin: String,
        record: VaultRecord,
        oauthKek: String
    ): CloudRecoveryBlob {
        val vaultJson = vaultToJson(record)
        val resultJson = WallettrustlibKt.createCloudRecoveryBlobFfi(pin, vaultJson, oauthKek)
        return jsonToBlob(resultJson)
    }
    
    @JvmStatic
    fun restoreVaultFromCloudRecoveryBlob(
        oauthKek: String,
        blob: CloudRecoveryBlob,
        newPin: String
    ): VaultRecord {
        val blobJson = blobToJson(blob)
        val resultJson = WallettrustlibKt.restoreVaultFromCloudRecoveryBlobFfi(oauthKek, blobJson, newPin)
        return jsonToVault(resultJson)
    }
    
    @JvmStatic
    fun verifyCloudRecoveryBlob(oauthKek: String, blob: CloudRecoveryBlob) {
        val blobJson = blobToJson(blob)
        WallettrustlibKt.verifyCloudRecoveryBlobFfi(oauthKek, blobJson)
    }
    
    // ========================================
    // WEB3AUTH INTEGRATION (2 METHODS)
    // ========================================
    
    @JvmStatic
    fun createWalletFromWeb3authKey(
        web3authPrivateKey: String,
        testnet: Boolean
    ): Web3AuthWalletResult {
        val resultJson = WallettrustlibKt.createWalletFromWeb3authKeyFfi(web3authPrivateKey, testnet)
        return jsonToWeb3AuthResult(resultJson)
    }
    
    @JvmStatic
    fun restoreWalletFromWeb3authKey(
        web3authPrivateKey: String,
        encryptedData: String,
        testnet: Boolean
    ): Web3AuthWalletResult {
        val resultJson = WallettrustlibKt.restoreWalletFromWeb3authKeyFfi(web3authPrivateKey, encryptedData, testnet)
        return jsonToWeb3AuthResult(resultJson)
    }
}
