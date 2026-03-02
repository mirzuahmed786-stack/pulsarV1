package com.elementa.wallet.data.repository

import com.elementa.wallet.data.bindings.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WalletOperationsRepository - Handles all 27 wallet functions via Rust JNI backend
 * 
 * Functions:
 * - Vault Management: 7 functions
 * - Transaction Signing: 6 functions
 * - Key Derivation & Addresses: 5 functions
 * - Recovery & Backup: 6 functions
 * - Web3Auth Integration: 2 functions
 */
@Singleton
class WalletOperationsRepository @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ========================================
    // VAULT MANAGEMENT (7 FUNCTIONS)
    // ========================================

    suspend fun generateMnemonic(): String = withContext(Dispatchers.Default) {
        VaultApi.generateMnemonic()
    }

    suspend fun createVault(pin: String): VaultRecord = withContext(Dispatchers.Default) {
        VaultApi.createVault(pin)
    }

    suspend fun createVaultFromMnemonic(
        pin: String,
        mnemonic: String,
        path: String = "m/44'/60'/0'/0/0"
    ): VaultRecord = withContext(Dispatchers.Default) {
        VaultApi.createVaultFromMnemonic(pin, mnemonic, path)
    }

    suspend fun createVaultFromPrivateKey(pin: String, privateKeyHex: String): VaultRecord =
        withContext(Dispatchers.Default) {
            VaultApi.createVaultFromPrivateKey(pin, privateKeyHex)
        }

    suspend fun verifyPin(pin: String, record: VaultRecord): String = withContext(Dispatchers.Default) {
        VaultApi.verifyPin(pin, record)
    }

    suspend fun rotatePin(oldPin: String, newPin: String, record: VaultRecord): VaultRecord =
        withContext(Dispatchers.Default) {
            VaultApi.rotatePin(oldPin, newPin, record)
        }

    suspend fun exportEthPrivateKey(pin: String, record: VaultRecord): String =
        withContext(Dispatchers.Default) {
            VaultApi.exportEthPrivateKey(pin, record)
        }

    suspend fun migrateVault(pin: String, record: VaultRecord): VaultRecord =
        withContext(Dispatchers.Default) {
            VaultApi.migrateVault(pin, record)
        }

    // ========================================
    // TRANSACTION SIGNING (6 FUNCTIONS)
    // ========================================

    suspend fun signTransaction(
        pin: String,
        record: VaultRecord,
        tx: UnsignedLegacyTx
    ): String = withContext(Dispatchers.Default) {
        VaultApi.signTransaction(pin, record, tx)
    }

    suspend fun signTransactionEip1559(
        pin: String,
        record: VaultRecord,
        tx: UnsignedEip1559Tx
    ): String = withContext(Dispatchers.Default) {
        VaultApi.signTransactionEip1559(pin, record, tx)
    }

    suspend fun signTransactionWithChain(
        pin: String,
        record: VaultRecord,
        tx: UnsignedLegacyTx,
        expectedChainId: Long
    ): String = withContext(Dispatchers.Default) {
        VaultApi.signTransactionWithChain(pin, record, tx, expectedChainId)
    }

    suspend fun signTransactionEip1559WithChain(
        pin: String,
        record: VaultRecord,
        tx: UnsignedEip1559Tx,
        expectedChainId: Long
    ): String = withContext(Dispatchers.Default) {
        VaultApi.signTransactionEip1559WithChain(pin, record, tx, expectedChainId)
    }

    suspend fun signSolanaTransaction(
        pin: String,
        record: VaultRecord,
        message: ByteArray
    ): ByteArray = withContext(Dispatchers.Default) {
        VaultApi.signSolanaTransaction(pin, record, message)
    }

    suspend fun signBitcoinTransaction(
        pin: String,
        record: VaultRecord,
        sighashHex: String,
        testnet: Boolean
    ): String = withContext(Dispatchers.Default) {
        VaultApi.signBitcoinTransaction(pin, record, sighashHex, testnet)
    }

    // ========================================
    // KEY DERIVATION & ADDRESSES (5 FUNCTIONS)
    // ========================================

    suspend fun deriveBtcAddress(mnemonic: String, testnet: Boolean): String =
        withContext(Dispatchers.Default) {
            VaultApi.deriveBtcAddress(mnemonic, testnet)
        }

    suspend fun deriveSolAddress(mnemonic: String): String = withContext(Dispatchers.Default) {
        VaultApi.deriveSolAddress(mnemonic)
    }

    suspend fun getBtcPublicKey(pin: String, record: VaultRecord, testnet: Boolean): String =
        withContext(Dispatchers.Default) {
            VaultApi.getBtcPublicKey(pin, record, testnet)
        }

    suspend fun getMultichainAddresses(
        pin: String,
        record: VaultRecord,
        testnet: Boolean
    ): MultichainAddresses = withContext(Dispatchers.Default) {
        VaultApi.getMultichainAddresses(pin, record, testnet)
    }

    suspend fun getMultichainAddressesByIndex(
        pin: String,
        record: VaultRecord,
        testnet: Boolean,
        index: Int
    ): MultichainAddresses = withContext(Dispatchers.Default) {
        VaultApi.getMultichainAddressesByIndex(pin, record, testnet, index)
    }

    // ========================================
    // RECOVERY & BACKUP (6 FUNCTIONS)
    // ========================================

    suspend fun createRecoveryBackup(
        pin: String,
        record: VaultRecord,
        backupPassphrase: String
    ): RecoveryBackup = withContext(Dispatchers.Default) {
        VaultApi.createRecoveryBackup(pin, record, backupPassphrase)
    }

    suspend fun restoreVaultFromRecoveryBackup(
        backupPassphrase: String,
        backup: RecoveryBackup,
        newPin: String
    ): VaultRecord = withContext(Dispatchers.Default) {
        VaultApi.restoreVaultFromRecoveryBackup(backupPassphrase, backup, newPin)
    }

    suspend fun verifyRecoveryBackup(
        backupPassphrase: String,
        backup: RecoveryBackup
    ) = withContext(Dispatchers.Default) {
        VaultApi.verifyRecoveryBackup(backupPassphrase, backup)
    }

    suspend fun createCloudRecoveryBlob(
        pin: String,
        record: VaultRecord,
        oauthKek: String
    ): CloudRecoveryBlob = withContext(Dispatchers.Default) {
        VaultApi.createCloudRecoveryBlob(pin, record, oauthKek)
    }

    suspend fun restoreVaultFromCloudRecoveryBlob(
        oauthKek: String,
        blob: CloudRecoveryBlob,
        newPin: String
    ): VaultRecord = withContext(Dispatchers.Default) {
        VaultApi.restoreVaultFromCloudRecoveryBlob(oauthKek, blob, newPin)
    }

    suspend fun verifyCloudRecoveryBlob(oauthKek: String, blob: CloudRecoveryBlob) =
        withContext(Dispatchers.Default) {
            VaultApi.verifyCloudRecoveryBlob(oauthKek, blob)
        }

    // ========================================
    // WEB3AUTH INTEGRATION (2 FUNCTIONS)
    // ========================================

    suspend fun createWalletFromWeb3authKey(
        web3authPrivateKey: String,
        testnet: Boolean
    ): Web3AuthWalletResult = withContext(Dispatchers.Default) {
        VaultApi.createWalletFromWeb3authKey(web3authPrivateKey, testnet)
    }

    suspend fun restoreWalletFromWeb3authKey(
        web3authPrivateKey: String,
        encryptedData: String,
        testnet: Boolean
    ): Web3AuthWalletResult = withContext(Dispatchers.Default) {
        VaultApi.restoreWalletFromWeb3authKey(web3authPrivateKey, encryptedData, testnet)
    }
}
