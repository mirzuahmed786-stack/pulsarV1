package com.elementa.wallet.data.bindings

import com.squareup.moshi.Json

// KDF Parameters: Used in VaultRecord for key derivation configuration
data class KdfParams(
    val name: String,
    val salt: ByteArray,
    val memory_kib: Int,
    val iterations: Int,
    val parallelism: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KdfParams) return false
        return name == other.name &&
            salt.contentEquals(other.salt) &&
            memory_kib == other.memory_kib &&
            iterations == other.iterations &&
            parallelism == other.parallelism
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + memory_kib
        result = 31 * result + iterations
        result = 31 * result + parallelism
        return result
    }
}

// Cipher Blob: Encrypted secret storage structure
data class CipherBlob(
    val nonce: ByteArray,
    val ciphertext: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CipherBlob) return false
        return nonce.contentEquals(other.nonce) &&
            ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int {
        var result = nonce.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }
}

// Main Vault Record: Complete encrypted wallet structure
data class VaultRecord(
    val version: Int,
    val kdf: KdfParams,
    val cipher: CipherBlob,
    val public_address: String,
    @Json(name = "hdIndex")
    val hd_index: Int? = null,
    @Json(name = "isHd")
    val is_hd: Boolean? = null
)

// Access List Item: EIP-2930 access list entry for EVM transactions
data class AccessListItem(
    val address: String,
    val storage_keys: List<String>
)

// Legacy EVM Transaction: Pre-EIP1559 transaction format
data class UnsignedLegacyTx(
    val nonce: Long,
    val gas_price: String,
    val gas_limit: Long,
    val to: String,
    val value: String,
    val data: String,
    val chain_id: Long
)

// EIP-1559 Transaction: Dynamic fee transaction format (EVM)
data class UnsignedEip1559Tx(
    val chain_id: Long,
    val nonce: Long,
    val max_priority_fee_per_gas: String,
    val max_fee_per_gas: String,
    val gas_limit: Long,
    val to: String,
    val value: String,
    val data: String,
    val access_list: List<AccessListItem> = emptyList()
)

// Recovery Backup: Encrypted backup of vault for disaster recovery
data class RecoveryBackup(
    val version: Int,
    val kdf: KdfParams,
    val cipher: CipherBlob,
    @Json(name = "walletId")
    val wallet_id: String,
    @Json(name = "createdAt")
    val created_at: Long,
    @Json(name = "secretType")
    val secret_type: String,
    @Json(name = "hdIndex")
    val hd_index: Int
)

// Cloud Recovery Blob: Cloud-safe encrypted seed backup
data class CloudRecoveryBlob(
    val version: Int,
    @Json(name = "walletId")
    val wallet_id: String,
    @Json(name = "createdAt")
    val created_at: Long,
    @Json(name = "secretType")
    val secret_type: String,
    @Json(name = "hdIndex")
    val hd_index: Int,
    @Json(name = "encryptedSeedBlob")
    val encrypted_seed_blob: String
)

// Multi-chain addresses from a single mnemonic
data class MultichainAddresses(
    val eth: String,
    val btc: String,
    val sol: String
)

// Web3Auth integration result
data class Web3AuthWalletResult(
    val ethereum_address: String,
    val solana_address: String,
    val bitcoin_address: String,
    val encrypted_data: String
)
