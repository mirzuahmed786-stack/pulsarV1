use serde::{Deserialize, Serialize};

// hassam dev: VaultError enum for structured error handling in FFI (Phase 1)
// This allows Kotlin to catch specific error types instead of generic strings
#[derive(Debug, Clone, Serialize, Deserialize, thiserror::Error)]
pub enum VaultError {
    #[error("Invalid mnemonic: {0}")]
    InvalidMnemonic(String),

    #[error("PIN verification failed")]
    InvalidPin,

    #[error("Invalid derivation path")]
    InvalidDerivationPath,

    #[error("Cryptographic operation failed: {0}")]
    CryptoError(String),

    #[error("Invalid key material")]
    InvalidKeyMaterial,

    #[error("Serialization error: {0}")]
    SerializationError(String),

    #[error("Recovery backup mismatch")]
    RecoveryBackupMismatch,

    #[error("Unknown error: {0}")]
    UnknownError(String),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct VaultRecord {
    pub version: u32,
    pub kdf: KdfParams,
    pub cipher: CipherBlob,
    pub public_address: String,
    // Optional metadata persisted by the frontend (camelCase).
    // When present, Rust uses `hdIndex` as the active account selection for derivation/signing.
    #[serde(default, rename = "hdIndex")]
    pub hd_index: Option<u32>,
    #[serde(default, rename = "isHd")]
    pub is_hd: Option<bool>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RecoveryBackup {
    pub version: u32,
    pub kdf: KdfParams,
    pub cipher: CipherBlob,
    // Stable wallet identifier for the seed (ETH addr at index 0).
    #[serde(rename = "walletId")]
    pub wallet_id: String,
    #[serde(rename = "createdAt")]
    pub created_at: u64,
    // "mnemonic" or "raw32"
    #[serde(rename = "secretType")]
    pub secret_type: String,
    // The active EVM account index this backup was created from.
    // This is metadata only; restore can choose a different index later.
    #[serde(rename = "hdIndex")]
    pub hd_index: u32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CloudRecoveryBlob {
    pub version: u32,
    #[serde(rename = "walletId")]
    pub wallet_id: String,
    #[serde(rename = "createdAt")]
    pub created_at: u64,
    #[serde(rename = "secretType")]
    pub secret_type: String,
    #[serde(rename = "hdIndex")]
    pub hd_index: u32,
    // Hex string: nonce (24 bytes) + ciphertext.
    #[serde(rename = "encryptedSeedBlob")]
    pub encrypted_seed_blob: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct KdfParams {
    pub name: String,
    pub salt: Vec<u8>,
    pub memory_kib: u32,
    pub iterations: u32,
    pub parallelism: u32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CipherBlob {
    pub nonce: Vec<u8>,
    pub ciphertext: Vec<u8>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UnsignedLegacyTx {
    pub nonce: u64,
    pub gas_price: String,
    pub gas_limit: u64,
    pub to: String,
    pub value: String,
    pub data: String,
    pub chain_id: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AccessListItem {
    pub address: String,
    pub storage_keys: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UnsignedEip1559Tx {
    pub chain_id: u64,
    pub nonce: u64,
    pub max_priority_fee_per_gas: String,
    pub max_fee_per_gas: String,
    pub gas_limit: u64,
    pub to: String,
    pub value: String,
    pub data: String,
    pub access_list: Vec<AccessListItem>,
}
