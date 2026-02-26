// hassam dev: Vault data classes - auto-generated from wallet.udl (Phase 2)
// Core data structures for vault operations

package com.wallet_rust

import com.squareup.moshi.Json

// KDF Parameters: Used in VaultRecord for key derivation configuration
data class KdfParams(
    val name: String,                    // Algorithm name: "argon2id"
    val salt: ByteArray,                 // KDF salt for key derivation
    val memory_kib: Int,                 // Memory cost in KiB
    val iterations: Int,                 // Time cost: number of iterations
    val parallelism: Int                 // Parallelism factor
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
        return arrayOf(name, salt.contentHashCode(), memory_kib, iterations, parallelism).hashCode()
    }
}

// Cipher Blob: Encrypted secret storage structure
data class CipherBlob(
    val nonce: ByteArray,                // Encryption nonce (24 bytes for XChaCha20)
    val ciphertext: ByteArray            // Encrypted payload
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CipherBlob) return false
        return nonce.contentEquals(other.nonce) &&
            ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int {
        return arrayOf(nonce.contentHashCode(), ciphertext.contentHashCode()).hashCode()
    }
}

// Main Vault Record: Complete encrypted wallet structure
data class VaultRecord(
    val version: Int,                    // Structure version
    val kdf: KdfParams,                  // Key derivation parameters
    val cipher: CipherBlob,              // Encrypted secret blob
    val public_address: String,          // EVM address (primary wallet ID)
    @Json(name = "hdIndex")
    val hd_index: Int? = null,           // Optional HD derivation index
    @Json(name = "isHd")
    val is_hd: Boolean? = null           // Optional flag: mnemonic vs raw key
)

// Access List Item: EIP-2930 access list entry for EVM transactions
data class AccessListItem(
    val address: String,                 // Contract address
    val storage_keys: List<String>       // Storage keys
)

// Legacy EVM Transaction: Pre-EIP1559 transaction format
data class UnsignedLegacyTx(
    val nonce: Long,                     // Transaction nonce
    val gas_price: String,               // Wei as string (big integer)
    val gas_limit: Long,                 // Gas limit
    val to: String,                      // Recipient address (0x-prefixed)
    val value: String,                   // Wei as string (big integer)
    val data: String,                    // Hex-encoded call data
    val chain_id: Long                   // EVM chain ID
)

// EIP-1559 Transaction: Dynamic fee transaction format (EVM)
data class UnsignedEip1559Tx(
    val chain_id: Long,                  // EVM chain ID
    val nonce: Long,                     // Transaction nonce
    val max_priority_fee_per_gas: String,// Priority fee in Wei (string)
    val max_fee_per_gas: String,         // Maximum fee in Wei (string)
    val gas_limit: Long,                 // Gas limit
    val to: String,                      // Recipient address
    val value: String,                   // Wei as string
    val data: String,                    // Hex-encoded call data
    val access_list: List<AccessListItem> = emptyList() // Optional access list
)

// Recovery Backup: Encrypted backup of vault for disaster recovery
data class RecoveryBackup(
    val version: Int,                    // Structure version
    val kdf: KdfParams,                  // Key derivation parameters
    val cipher: CipherBlob,              // Encrypted secret blob
    @Json(name = "walletId")
    val wallet_id: String,               // ETH address at index 0 (stable ID)
    @Json(name = "createdAt")
    val created_at: Long,                // Creation timestamp
    @Json(name = "secretType")
    val secret_type: String,             // "mnemonic" or "raw32"
    @Json(name = "hdIndex")
    val hd_index: Int                    // Active account index at creation
)

// Cloud Recovery Blob: Cloud-safe encrypted seed backup
data class CloudRecoveryBlob(
    val version: Int,                    // Structure version
    @Json(name = "walletId")
    val wallet_id: String,               // ETH address at index 0
    @Json(name = "createdAt")
    val created_at: Long,                // Creation timestamp
    @Json(name = "secretType")
    val secret_type: String,             // "mnemonic" or "raw32"
    @Json(name = "hdIndex")
    val hd_index: Int,                   // Active account index
    @Json(name = "encryptedSeedBlob")
    val encrypted_seed_blob: String      // Hex: nonce (24 bytes) + ciphertext
)

// Multi-chain addresses from a single mnemonic
data class MultichainAddresses(
    val eth: String,                     // Ethereum address
    val btc: String,                     // Bitcoin address
    val sol: String                      // Solana address
)

// Web3Auth integration result
data class Web3AuthWalletResult(
    val ethereum_address: String,        // Ethereum address
    val solana_address: String,          // Solana address
    val bitcoin_address: String,         // Bitcoin address
    val encrypted_data: String           // Encrypted wallet data
)
