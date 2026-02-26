// mod bitcoin;
mod crypto;
mod evm;
mod jni;
mod keys;
mod solana;
mod types;

// hassam dev: JNI bridge for Android/Kotlin integration
pub use types::{AccessListItem, CloudRecoveryBlob, RecoveryBackup, UnsignedEip1559Tx, UnsignedLegacyTx, VaultRecord, VaultError};

const MAX_HD_INDEX: u32 = 0x7FFF_FFFF;

fn eth_path_for_index(index: u32) -> Result<String, String> {
    if index > MAX_HD_INDEX {
        return Err("Invalid HD index".to_string());
    }
    Ok(format!("m/44'/60'/0'/0/{}", index))
}

fn parse_eth_index_from_path(path: &str) -> Result<u32, String> {
    // Allow only standard non-hardened account indices: m/44'/60'/0'/0/<index>
    const PREFIX: &str = "m/44'/60'/0'/0/";
    if !path.starts_with(PREFIX) {
        return Err("Unsupported derivation path".to_string());
    }
    let suffix = &path[PREFIX.len()..];
    if suffix.is_empty() || !suffix.chars().all(|c| c.is_ascii_digit()) {
        return Err("Unsupported derivation path".to_string());
    }
    let index: u32 = suffix.parse().map_err(|_| "Unsupported derivation path".to_string())?;
    if index > MAX_HD_INDEX {
        return Err("Invalid HD index".to_string());
    }
    Ok(index)
}

pub fn derive_btc_address(mnemonic: &str, testnet: bool) -> Result<String, String> {
    let key = keys::derive_btc_from_mnemonic(mnemonic, testnet)?;
    Ok(key.address)
}

pub fn derive_sol_address(mnemonic: &str) -> Result<String, String> {
    let key = keys::derive_sol_from_mnemonic(mnemonic)?;
    Ok(key.address)
}

// Helper to get the ETH signing key from either a raw private key (Legacy) or Mnemonic (V2)
fn get_eth_key_from_secret(secret: &[u8]) -> Result<keys::KeyMaterial, String> {
    if secret.len() == 32 {
        // Legacy: Raw Private Key
        let hex_key = hex::encode(secret);
        keys::key_from_private_key_hex(&hex_key)
    } else {
        // V2: Mnemonic string
        let mnemonic = std::str::from_utf8(secret).map_err(|_| "Invalid mnemonic utf8".to_string())?;
        keys::derive_secp256k1_from_mnemonic(mnemonic, "m/44'/60'/0'/0/0")
    }
}

fn get_eth_key_from_mnemonic_index(mnemonic: &str, index: u32) -> Result<keys::KeyMaterial, String> {
    let path = eth_path_for_index(index)?;
    keys::derive_secp256k1_from_mnemonic(mnemonic, &path)
}

// (legacy helper removed; we now derive SOL/BTC using `hdIndex` when present)

pub fn create_vault(pin: &str) -> Result<VaultRecord, String> {
    // V2: Generate unique mnemonic
    let mnemonic = keys::generate_mnemonic()?;
    
    // Derive ETH address to use as the public identifier of this vault
    let eth_key = keys::derive_secp256k1_from_mnemonic(&mnemonic, "m/44'/60'/0'/0/0")?;
    
    // Encrypt the *mnemonic* itself
    let mut record = crypto::encrypt_secret(mnemonic.as_bytes(), pin, eth_key.address)?;
    record.hd_index = Some(0);
    record.is_hd = Some(true);
    
    // Self-verification
    match verify_pin(pin, &record) {
        Ok(_) => Ok(record),
        Err(e) => Err(format!("Self-verification failed during vault creation: {}", e)),
    }
}

pub fn create_vault_from_mnemonic(pin: &str, mnemonic: &str, path: &str) -> Result<VaultRecord, String> {
    // For import, we store the Mnemonic directly.
    // The 'path' argument is used to derive the primary ID (address) but we store the whole seed.
    let index = parse_eth_index_from_path(path)?;
    let eth_key = keys::derive_secp256k1_from_mnemonic(mnemonic, path)?;
    let mut record = crypto::encrypt_secret(mnemonic.as_bytes(), pin, eth_key.address)?;
    record.hd_index = Some(index);
    record.is_hd = Some(true);
    Ok(record)
}

pub fn create_vault_from_private_key(pin: &str, private_key_hex: &str) -> Result<VaultRecord, String> {
    // Legacy support: Store raw private key bytes
    let key_material = keys::key_from_private_key_hex(private_key_hex)?;
    let mut record = crypto::encrypt_secret(&key_material.secret_key[..], pin, key_material.address)?;
    record.hd_index = Some(0);
    record.is_hd = Some(false);
    Ok(record)
}

pub fn generate_mnemonic() -> Result<String, String> {
    keys::generate_mnemonic()
}

pub fn verify_pin(pin: &str, record: &VaultRecord) -> Result<String, String> {
    let secret = crypto::decrypt_secret(pin, record)?;
    
    // Verify we can derive the key/mnemonic for the active index (default 0).
    let key = if secret.len() == 32 {
        let idx = record.hd_index.unwrap_or(0);
        if idx != 0 {
            return Err("Legacy vault does not support HD derivation".to_string());
        }
        get_eth_key_from_secret(&secret)?
    } else {
        let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic utf8".to_string())?;
        let idx = record.hd_index.unwrap_or(0);
        get_eth_key_from_mnemonic_index(mnemonic, idx)?
    };
    
    // Verify the derived address matches the record ID
    if key.address != record.public_address {
        return Err("Derived address does not match record ID".to_string());
    }
    
    Ok(record.public_address.clone())
}

#[cfg(feature = "dangerous-key-export")]
pub fn export_eth_private_key(pin: &str, record: &VaultRecord) -> Result<String, String> {
    let secret = crypto::decrypt_secret(pin, record)?;
    let key = if secret.len() == 32 {
        let idx = record.hd_index.unwrap_or(0);
        if idx != 0 {
            return Err("Legacy vault does not support HD derivation".to_string());
        }
        get_eth_key_from_secret(&secret)?
    } else {
        let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic utf8".to_string())?;
        let idx = record.hd_index.unwrap_or(0);
        get_eth_key_from_mnemonic_index(mnemonic, idx)?
    };
    Ok(format!("0x{}", hex::encode(key.secret_key.as_ref())))
}

#[cfg(not(feature = "dangerous-key-export"))]
pub fn export_eth_private_key(_pin: &str, _record: &VaultRecord) -> Result<String, String> {
    Err("Key export disabled in this build".to_string())
}

#[derive(serde::Serialize)]
pub struct MultichainAddresses {
    pub eth: String,
    pub sol: String,
    pub btc: String,
}

pub fn get_multichain_addresses(pin: &str, record: &VaultRecord, testnet: bool) -> Result<MultichainAddresses, String> {
    let secret = crypto::decrypt_secret(pin, record)?;
    let idx = record.hd_index.unwrap_or(0);
    
    if secret.len() == 32 {
        // Legacy vault: Can only return ETH 
        if idx != 0 {
            return Err("Legacy vault does not support HD derivation".to_string());
        }
        let key = get_eth_key_from_secret(&secret)?;
        return Ok(MultichainAddresses {
            eth: key.address,
            sol: String::new(),
            btc: String::new(),
        });
    }
    
    // V2: Mnemonic
    let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic".to_string())?;

    let eth_key = get_eth_key_from_mnemonic_index(mnemonic, idx)?;
    let sol_key = keys::derive_sol_from_mnemonic_with_index(mnemonic, idx)?;
    let btc_key = keys::derive_btc_from_mnemonic_with_index(mnemonic, testnet, idx)?;
    let btc_key_addr = btc_key.address;

    Ok(MultichainAddresses {
        eth: eth_key.address,
        sol: sol_key.address,
        btc: btc_key_addr,
    })
}

pub fn get_multichain_addresses_by_index(
    pin: &str,
    record: &VaultRecord,
    testnet: bool,
    index: u32
) -> Result<MultichainAddresses, String> {
    let secret = crypto::decrypt_secret(pin, record)?;

    if secret.len() == 32 {
        return Err("Legacy vault does not support HD derivation".to_string());
    }

    let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic".to_string())?;
    let eth_path = format!("m/44'/60'/0'/0/{}", index);
    let eth_key = keys::derive_secp256k1_from_mnemonic(mnemonic, &eth_path)?;
    let sol_key = keys::derive_sol_from_mnemonic_with_index(mnemonic, index)?;
    let btc_key = keys::derive_btc_from_mnemonic_with_index(mnemonic, testnet, index)?;

    Ok(MultichainAddresses {
        eth: eth_key.address,
        sol: sol_key.address,
        btc: btc_key.address,
    })
}

pub fn get_btc_public_key(pin: &str, record: &VaultRecord, testnet: bool) -> Result<String, String> {
    use k256::ecdsa::SigningKey;

    let secret = crypto::decrypt_secret(pin, record)?;
    if secret.len() == 32 {
        return Err("Legacy vault does not support Bitcoin".to_string());
    }
    let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic".to_string())?;
    let idx = record.hd_index.unwrap_or(0);
    let btc_key = keys::derive_btc_from_mnemonic_with_index(mnemonic, testnet, idx)?;

    let signing_key = SigningKey::from_bytes(btc_key.secret_key.as_ref().into())
        .map_err(|e| format!("Invalid secret key: {}", e))?;

    let public_key = signing_key.verifying_key().to_encoded_point(true);
    Ok(hex::encode(public_key.as_bytes()))
}

pub fn sign_transaction(pin: &str, record: &VaultRecord, tx: &UnsignedLegacyTx) -> Result<String, String> {
    let secret = crypto::decrypt_secret(pin, record)?;
    let key = if secret.len() == 32 {
        let idx = record.hd_index.unwrap_or(0);
        if idx != 0 {
            return Err("Legacy vault does not support HD derivation".to_string());
        }
        get_eth_key_from_secret(&secret)?
    } else {
        let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic utf8".to_string())?;
        let idx = record.hd_index.unwrap_or(0);
        get_eth_key_from_mnemonic_index(mnemonic, idx)?
    };
    evm::sign_legacy_tx(key.secret_key.as_ref(), tx)
}

pub fn sign_transaction_eip1559(pin: &str, record: &VaultRecord, tx: &UnsignedEip1559Tx) -> Result<String, String> {
    let secret = crypto::decrypt_secret(pin, record)?;
    let key = if secret.len() == 32 {
        let idx = record.hd_index.unwrap_or(0);
        if idx != 0 {
            return Err("Legacy vault does not support HD derivation".to_string());
        }
        get_eth_key_from_secret(&secret)?
    } else {
        let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic utf8".to_string())?;
        let idx = record.hd_index.unwrap_or(0);
        get_eth_key_from_mnemonic_index(mnemonic, idx)?
    };
    evm::sign_eip1559_tx(key.secret_key.as_ref(), tx)
}

pub fn sign_transaction_with_chain(pin: &str, record: &VaultRecord, tx: &UnsignedLegacyTx, expected_chain_id: u64) -> Result<String, String> {
    if tx.chain_id != expected_chain_id {
        return Err("Invalid chain id".to_string());
    }
    let secret = crypto::decrypt_secret(pin, record)?;
    let key = if secret.len() == 32 {
        let idx = record.hd_index.unwrap_or(0);
        if idx != 0 {
            return Err("Legacy vault does not support HD derivation".to_string());
        }
        get_eth_key_from_secret(&secret)?
    } else {
        let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic utf8".to_string())?;
        let idx = record.hd_index.unwrap_or(0);
        get_eth_key_from_mnemonic_index(mnemonic, idx)?
    };
    evm::sign_legacy_tx(key.secret_key.as_ref(), tx)
}

pub fn sign_transaction_eip1559_with_chain(
    pin: &str,
    record: &VaultRecord,
    tx: &UnsignedEip1559Tx,
    expected_chain_id: u64
) -> Result<String, String> {
    if tx.chain_id != expected_chain_id {
        return Err("Invalid chain id".to_string());
    }
    let secret = crypto::decrypt_secret(pin, record)?;
    let key = if secret.len() == 32 {
        let idx = record.hd_index.unwrap_or(0);
        if idx != 0 {
            return Err("Legacy vault does not support HD derivation".to_string());
        }
        get_eth_key_from_secret(&secret)?
    } else {
        let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic utf8".to_string())?;
        let idx = record.hd_index.unwrap_or(0);
        get_eth_key_from_mnemonic_index(mnemonic, idx)?
    };
    evm::sign_eip1559_tx(key.secret_key.as_ref(), tx)
}

pub fn sign_solana_transaction(pin: &str, record: &VaultRecord, message: &[u8]) -> Result<Vec<u8>, String> {
    let secret = crypto::decrypt_secret(pin, record)?;
    if secret.len() == 32 {
        return Err("Legacy vault does not support Solana".to_string());
    }
    let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic".to_string())?;
    let idx = record.hd_index.unwrap_or(0);
    let key = keys::derive_sol_from_mnemonic_with_index(mnemonic, idx)?;
    solana::sign_solana_message(key.secret_key.as_ref(), message)
}

pub fn sign_bitcoin_transaction(
    pin: &str,
    record: &VaultRecord,
    sighash_hex: &str,
    testnet: bool,
) -> Result<String, String> {
    use k256::ecdsa::{SigningKey, signature::hazmat::PrehashSigner};

    let secret = crypto::decrypt_secret(pin, record)?;
    if secret.len() == 32 {
        return Err("Legacy vault does not support Bitcoin".to_string());
    }
    let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic".to_string())?;
    let idx = record.hd_index.unwrap_or(0);
    let btc_key = keys::derive_btc_from_mnemonic_with_index(mnemonic, testnet, idx)?;

    let signing_key = SigningKey::from_bytes(btc_key.secret_key.as_ref().into())
        .map_err(|e| format!("Invalid secret key: {}", e))?;

    let sighash_bytes = hex::decode(sighash_hex)
        .map_err(|e| format!("Invalid sighash hex: {} (length: {}, value: '{}')", e, sighash_hex.len(), sighash_hex))?;
    
    // Sign the 32-byte hash
    let signature: k256::ecdsa::Signature = signing_key.sign_prehash(&sighash_bytes)
        .map_err(|e| format!("Signing failed: {}", e))?;

    // Return raw 64-byte signature (bitcoinjs PSBT signer expects raw or DER depending on internal logic, 
    // but the "Expected 64" error implies raw 64-byte (r,s) is required here).
    Ok(hex::encode(signature.to_bytes()))
}

pub fn rotate_pin(old_pin: &str, new_pin: &str, record: &VaultRecord) -> Result<VaultRecord, String> {
    let secret = crypto::decrypt_secret(old_pin, record)?;
    // Re-encrypt the secret (whether it's bytes or mnemonic string) with new PIN
    let mut updated = crypto::encrypt_secret(&secret, new_pin, record.public_address.clone())?;
    updated.hd_index = record.hd_index;
    updated.is_hd = record.is_hd;
    Ok(updated)
}

pub fn migrate_vault(pin: &str, record: &VaultRecord) -> Result<VaultRecord, String> {
    let secret = crypto::decrypt_secret(pin, record)?;
    let mut updated = crypto::encrypt_secret(&secret, pin, record.public_address.clone())?;
    updated.hd_index = record.hd_index;
    updated.is_hd = record.is_hd;
    Ok(updated)
}

#[cfg(target_arch = "wasm32")]
fn now_ms() -> u64 {
    // `std::time::SystemTime::now()` panics on wasm32-unknown-unknown.
    // Use JS time (ms since epoch) instead.
    js_sys::Date::now() as u64
}

#[cfg(not(target_arch = "wasm32"))]
fn now_ms() -> u64 {
    use std::time::{SystemTime, UNIX_EPOCH};
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis() as u64)
        .unwrap_or(0)
}

pub fn create_recovery_backup(pin: &str, record: &VaultRecord, backup_passphrase: &str) -> Result<RecoveryBackup, String> {
    // Decrypt the vault (PIN) then re-encrypt the underlying secret with a separate backup passphrase.
    let secret = crypto::decrypt_secret(pin, record)?;
    let secret_type = if secret.len() == 32 { "raw32" } else { "mnemonic" }.to_string();
    let active_index = record.hd_index.unwrap_or(0);

    // Stable wallet id: eth address at index 0 (for HD), else record id for legacy.
    let wallet_id = if secret.len() == 32 {
        record.public_address.clone()
    } else {
        let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic utf8".to_string())?;
        get_eth_key_from_mnemonic_index(mnemonic, 0)?.address
    };

    let (version, kdf, cipher) = crypto::encrypt_recovery_backup_secret(&secret, backup_passphrase)?;
    Ok(RecoveryBackup {
        version,
        kdf,
        cipher,
        wallet_id,
        created_at: now_ms(),
        secret_type,
        hd_index: active_index,
    })
}

pub fn restore_vault_from_recovery_backup(
    backup_passphrase: &str,
    backup: &RecoveryBackup,
    new_pin: &str
) -> Result<VaultRecord, String> {
    if backup.version != 1 {
        return Err("Unsupported recovery backup version".to_string());
    }
    let secret = crypto::decrypt_recovery_backup_secret(backup_passphrase, &backup.kdf, &backup.cipher)?;
    if backup.secret_type == "raw32" {
        if secret.len() != 32 {
            return Err("Invalid recovery backup secret".to_string());
        }
        let pk_hex = hex::encode(secret.as_slice());
        return create_vault_from_private_key(new_pin, &pk_hex);
    }
    if backup.secret_type != "mnemonic" {
        return Err("Unsupported recovery backup secret type".to_string());
    }
    let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic utf8".to_string())?;
    let path = eth_path_for_index(backup.hd_index)?;
    create_vault_from_mnemonic(new_pin, mnemonic, &path)
}

pub fn verify_recovery_backup(
    backup_passphrase: &str,
    backup: &RecoveryBackup,
) -> Result<(), String> {
    if backup.version != 1 {
        return Err("Unsupported recovery backup version".to_string());
    }

    let secret = crypto::decrypt_recovery_backup_secret(backup_passphrase, &backup.kdf, &backup.cipher)?;

    // Verify the backup's walletId matches the recovered secret's index-0 ETH address.
    let expected_wallet_id = if backup.secret_type == "raw32" {
        if secret.len() != 32 {
            return Err("Invalid recovery backup secret".to_string());
        }
        get_eth_key_from_secret(&secret)?.address
    } else if backup.secret_type == "mnemonic" {
        let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic utf8".to_string())?;
        get_eth_key_from_mnemonic_index(mnemonic, 0)?.address
    } else {
        return Err("Unsupported recovery backup secret type".to_string());
    };

    if expected_wallet_id != backup.wallet_id {
        return Err("Recovery backup walletId mismatch".to_string());
    }

    Ok(())
}

pub fn create_cloud_recovery_blob(pin: &str, record: &VaultRecord, oauth_kek: &str) -> Result<CloudRecoveryBlob, String> {
    // Decrypt the vault (PIN) then encrypt the underlying secret with an OAuth-derived KEK.
    let secret = crypto::decrypt_secret(pin, record)?;
    let secret_type = if secret.len() == 32 { "raw32" } else { "mnemonic" }.to_string();
    let active_index = record.hd_index.unwrap_or(0);

    // Stable wallet id: eth address at index 0 (for HD), else record id for legacy.
    let wallet_id = if secret.len() == 32 {
        record.public_address.clone()
    } else {
        let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic utf8".to_string())?;
        get_eth_key_from_mnemonic_index(mnemonic, 0)?.address
    };

    let encrypted_seed_blob = crypto::encrypt_with_key(secret.as_slice(), oauth_kek)?;

    Ok(CloudRecoveryBlob {
        version: 1,
        wallet_id,
        created_at: now_ms(),
        secret_type,
        hd_index: active_index,
        encrypted_seed_blob,
    })
}

pub fn restore_vault_from_cloud_recovery_blob(
    oauth_kek: &str,
    blob: &CloudRecoveryBlob,
    new_pin: &str
) -> Result<VaultRecord, String> {
    if blob.version != 1 {
        return Err("Unsupported cloud recovery blob version".to_string());
    }
    let secret = crypto::decrypt_with_key(&blob.encrypted_seed_blob, oauth_kek)?;
    if blob.secret_type == "raw32" {
        if secret.len() != 32 {
            return Err("Invalid cloud recovery secret".to_string());
        }
        let pk_hex = hex::encode(secret.as_slice());
        return create_vault_from_private_key(new_pin, &pk_hex);
    }
    if blob.secret_type != "mnemonic" {
        return Err("Unsupported cloud recovery secret type".to_string());
    }
    let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic utf8".to_string())?;
    let path = eth_path_for_index(blob.hd_index)?;
    create_vault_from_mnemonic(new_pin, mnemonic, &path)
}

pub fn verify_cloud_recovery_blob(
    oauth_kek: &str,
    blob: &CloudRecoveryBlob,
) -> Result<(), String> {
    if blob.version != 1 {
        return Err("Unsupported cloud recovery blob version".to_string());
    }
    let secret = crypto::decrypt_with_key(&blob.encrypted_seed_blob, oauth_kek)?;
    let expected_wallet_id = if blob.secret_type == "raw32" {
        if secret.len() != 32 {
            return Err("Invalid cloud recovery secret".to_string());
        }
        get_eth_key_from_secret(&secret)?.address
    } else if blob.secret_type == "mnemonic" {
        let mnemonic = std::str::from_utf8(&secret).map_err(|_| "Invalid mnemonic utf8".to_string())?;
        get_eth_key_from_mnemonic_index(mnemonic, 0)?.address
    } else {
        return Err("Unsupported cloud recovery secret type".to_string());
    };

    if expected_wallet_id != blob.wallet_id {
        return Err("Cloud recovery walletId mismatch".to_string());
    }
    Ok(())
}

/// Web3Auth Integration: Create wallet from Web3Auth OAuth private key
/// This is used for FIRST-TIME login via Google/Apple OAuth
#[derive(serde::Serialize)]
pub struct Web3AuthWalletResult {
    pub ethereum_address: String,
    pub solana_address: String,
    pub bitcoin_address: String,
    pub encrypted_data: String, // For storage
}

pub fn create_wallet_from_web3auth_key(
    web3auth_private_key: &str,
    testnet: bool
) -> Result<Web3AuthWalletResult, String> {
    // Derive all multi-chain wallets deterministically
    let wallets = keys::derive_multichain_from_web3auth(web3auth_private_key, testnet)?;
    
    // Serialize the wallet data for encryption
    use serde_json::json;
    let wallet_data = json!({
        "eth_key": hex::encode(wallets.ethereum.secret_key.as_ref()),
        "sol_key": hex::encode(wallets.solana.secret_key.as_ref()),
        "btc_key": hex::encode(wallets.bitcoin.secret_key.as_ref()),
        "version": "web3auth_v1"
    });
    
    let wallet_json = serde_json::to_string(&wallet_data)
        .map_err(|_| "Failed to serialize wallet data".to_string())?;
    
    // Encrypt using the Web3Auth key itself as the encryption key
    // This ensures only the user with the Web3Auth key can decrypt
    let encrypted = crypto::encrypt_with_key(
        wallet_json.as_bytes(),
        web3auth_private_key
    )?;
    
    Ok(Web3AuthWalletResult {
        ethereum_address: wallets.ethereum.address,
        solana_address: wallets.solana.address,
        bitcoin_address: wallets.bitcoin.address,
        encrypted_data: encrypted,
    })
}

/// Web3Auth Integration: Restore wallet from Web3Auth OAuth private key
/// This is used for RETURNING USER login via Google/Apple OAuth
pub fn restore_wallet_from_web3auth_key(
    web3auth_private_key: &str,
    encrypted_data: &str,
    testnet: bool
) -> Result<Web3AuthWalletResult, String> {
    // Decrypt the stored wallet data
    let decrypted = crypto::decrypt_with_key(encrypted_data, web3auth_private_key)?;
    
    let wallet_json = String::from_utf8(decrypted)
        .map_err(|_| "Invalid wallet data encoding".to_string())?;
    
    // Parse the wallet data and validate version
    let wallet_data: serde_json::Value = serde_json::from_str(&wallet_json)
        .map_err(|_| "Invalid wallet data format".to_string())?;
    let version = wallet_data
        .get("version")
        .and_then(|val| val.as_str())
        .ok_or_else(|| "Missing wallet data version".to_string())?;
    if version != "web3auth_v1" {
        return Err("Unsupported wallet data version".to_string());
    }
    
    // Extract addresses (we could also re-derive them for validation)
    let wallets = keys::derive_multichain_from_web3auth(web3auth_private_key, testnet)?;
    
    Ok(Web3AuthWalletResult {
        ethereum_address: wallets.ethereum.address,
        solana_address: wallets.solana.address,
        bitcoin_address: wallets.bitcoin.address,
        encrypted_data: encrypted_data.to_string(),
    })
}

// ============================================================================
// hassam dev: FFI EXPORT WRAPPERS for Kotlin/JVM Bridge (Phase 1)
// These functions are marked with  for auto-generated Kotlin bindings
// They wrap the internal functions (which use &str) to accept String for FFI
// and convert Result<T, String> to Result<T, VaultError> for proper error handling in Kotlin
// ============================================================================

// === Vault Management Functions (7) ===


pub fn generate_mnemonic_ffi() -> Result<String, VaultError> {
    generate_mnemonic()
        .map_err(|e| VaultError::CryptoError(e))
}


pub fn create_vault_ffi(pin: String) -> Result<VaultRecord, VaultError> {
    create_vault(&pin)
        .map_err(|e| VaultError::CryptoError(e))
}


pub fn create_vault_from_mnemonic_ffi(pin: String, mnemonic: String, path: String) -> Result<VaultRecord, VaultError> {
    create_vault_from_mnemonic(&pin, &mnemonic, &path)
        .map_err(|e| match e.as_str() {
            err if err.contains("Invalid mnemonic") => VaultError::InvalidMnemonic(e),
            err if err.contains("Unsupported derivation") => VaultError::InvalidDerivationPath,
            _ => VaultError::CryptoError(e),
        })
}


pub fn create_vault_from_private_key_ffi(pin: String, private_key_hex: String) -> Result<VaultRecord, VaultError> {
    create_vault_from_private_key(&pin, &private_key_hex)
        .map_err(|e| match e.as_str() {
            err if err.contains("Invalid private key") => VaultError::InvalidKeyMaterial,
            _ => VaultError::CryptoError(e),
        })
}


pub fn verify_pin_ffi(pin: String, record: VaultRecord) -> Result<String, VaultError> {
    verify_pin(&pin, &record)
        .map_err(|_| VaultError::InvalidPin)
}


pub fn rotate_pin_ffi(old_pin: String, new_pin: String, record: VaultRecord) -> Result<VaultRecord, VaultError> {
    rotate_pin(&old_pin, &new_pin, &record)
        .map_err(|e| VaultError::CryptoError(e))
}


pub fn migrate_vault_ffi(pin: String, record: VaultRecord) -> Result<VaultRecord, VaultError> {
    migrate_vault(&pin, &record)
        .map_err(|e| VaultError::CryptoError(e))
}

// === Transaction Signing Functions (6) ===


pub fn sign_transaction_ffi(pin: String, record: VaultRecord, tx: UnsignedLegacyTx) -> Result<String, VaultError> {
    sign_transaction(&pin, &record, &tx)
        .map_err(|e| VaultError::CryptoError(e))
}


pub fn sign_transaction_eip1559_ffi(pin: String, record: VaultRecord, tx: UnsignedEip1559Tx) -> Result<String, VaultError> {
    sign_transaction_eip1559(&pin, &record, &tx)
        .map_err(|e| VaultError::CryptoError(e))
}


pub fn sign_transaction_with_chain_ffi(pin: String, record: VaultRecord, tx: UnsignedLegacyTx, expected_chain_id: u64) -> Result<String, VaultError> {
    sign_transaction_with_chain(&pin, &record, &tx, expected_chain_id)
        .map_err(|e| VaultError::CryptoError(e))
}


pub fn sign_transaction_eip1559_with_chain_ffi(pin: String, record: VaultRecord, tx: UnsignedEip1559Tx, expected_chain_id: u64) -> Result<String, VaultError> {
    sign_transaction_eip1559_with_chain(&pin, &record, &tx, expected_chain_id)
        .map_err(|e| VaultError::CryptoError(e))
}


pub fn sign_solana_transaction_ffi(pin: String, record: VaultRecord, message: Vec<u8>) -> Result<Vec<u8>, VaultError> {
    sign_solana_transaction(&pin, &record, &message)
        .map_err(|e| VaultError::CryptoError(e))
}


pub fn sign_bitcoin_transaction_ffi(pin: String, record: VaultRecord, sighash_hex: String, testnet: bool) -> Result<String, VaultError> {
    sign_bitcoin_transaction(&pin, &record, &sighash_hex, testnet)
        .map_err(|e| VaultError::CryptoError(e))
}

// === Key Derivation & Address Functions (5) ===


pub fn derive_btc_address_ffi(mnemonic: String, testnet: bool) -> Result<String, VaultError> {
    derive_btc_address(&mnemonic, testnet)
        .map_err(|e| match e.as_str() {
            err if err.contains("Invalid mnemonic") => VaultError::InvalidMnemonic(e),
            _ => VaultError::CryptoError(e),
        })
}


pub fn derive_sol_address_ffi(mnemonic: String) -> Result<String, VaultError> {
    derive_sol_address(&mnemonic)
        .map_err(|e| match e.as_str() {
            err if err.contains("Invalid mnemonic") => VaultError::InvalidMnemonic(e),
            _ => VaultError::CryptoError(e),
        })
}


pub fn get_btc_public_key_ffi(pin: String, record: VaultRecord, testnet: bool) -> Result<String, VaultError> {
    get_btc_public_key(&pin, &record, testnet)
        .map_err(|e| VaultError::CryptoError(e))
}


pub fn get_multichain_addresses_ffi(pin: String, record: VaultRecord, testnet: bool) -> Result<MultichainAddresses, VaultError> {
    get_multichain_addresses(&pin, &record, testnet)
        .map_err(|e| VaultError::CryptoError(e))
}


pub fn get_multichain_addresses_by_index_ffi(pin: String, record: VaultRecord, testnet: bool, index: u32) -> Result<MultichainAddresses, VaultError> {
    get_multichain_addresses_by_index(&pin, &record, testnet, index)
        .map_err(|e| VaultError::CryptoError(e))
}

// === Recovery & Backup Functions (6) ===


pub fn create_recovery_backup_ffi(pin: String, record: VaultRecord, backup_passphrase: String) -> Result<RecoveryBackup, VaultError> {
    create_recovery_backup(&pin, &record, &backup_passphrase)
        .map_err(|e| VaultError::CryptoError(e))
}


pub fn restore_vault_from_recovery_backup_ffi(backup_passphrase: String, backup: RecoveryBackup, new_pin: String) -> Result<VaultRecord, VaultError> {
    restore_vault_from_recovery_backup(&backup_passphrase, &backup, &new_pin)
        .map_err(|e| VaultError::CryptoError(e))
}


pub fn verify_recovery_backup_ffi(backup_passphrase: String, backup: RecoveryBackup) -> Result<(), VaultError> {
    verify_recovery_backup(&backup_passphrase, &backup)
        .map_err(|e| VaultError::RecoveryBackupMismatch)
}


pub fn create_cloud_recovery_blob_ffi(pin: String, record: VaultRecord, oauth_kek: String) -> Result<CloudRecoveryBlob, VaultError> {
    create_cloud_recovery_blob(&pin, &record, &oauth_kek)
        .map_err(|e| VaultError::CryptoError(e))
}


pub fn restore_vault_from_cloud_recovery_blob_ffi(oauth_kek: String, blob: CloudRecoveryBlob, new_pin: String) -> Result<VaultRecord, VaultError> {
    restore_vault_from_cloud_recovery_blob(&oauth_kek, &blob, &new_pin)
        .map_err(|e| VaultError::CryptoError(e))
}


pub fn verify_cloud_recovery_blob_ffi(oauth_kek: String, blob: CloudRecoveryBlob) -> Result<(), VaultError> {
    verify_cloud_recovery_blob(&oauth_kek, &blob)
        .map_err(|e| VaultError::CryptoError(e))
}

// === Web3Auth Integration (2) ===


pub fn create_wallet_from_web3auth_key_ffi(web3auth_private_key: String, testnet: bool) -> Result<Web3AuthWalletResult, VaultError> {
    create_wallet_from_web3auth_key(&web3auth_private_key, testnet)
        .map_err(|e| VaultError::CryptoError(e))
}


pub fn restore_wallet_from_web3auth_key_ffi(web3auth_private_key: String, encrypted_data: String, testnet: bool) -> Result<Web3AuthWalletResult, VaultError> {
    restore_wallet_from_web3auth_key(&web3auth_private_key, &encrypted_data, testnet)
        .map_err(|e| VaultError::CryptoError(e))
}

// === Optional: Key Export (gated feature) ===

#[cfg(feature = "dangerous-key-export")]

pub fn export_eth_private_key_ffi(pin: String, record: VaultRecord) -> Result<String, VaultError> {
    export_eth_private_key(&pin, &record)
        .map_err(|e| VaultError::CryptoError(e))
}

#[cfg(not(feature = "dangerous-key-export"))]

pub fn export_eth_private_key_ffi(_pin: String, _record: VaultRecord) -> Result<String, VaultError> {
    Err(VaultError::UnknownError("Key export is disabled in this build".to_string()))
}

// ============================================================================
// end hassam dev: FFI export section
// ============================================================================

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_create_and_verify_hd_vault() {
        let pin = "testpin";
        let record = create_vault(pin).unwrap();
        let address = verify_pin(pin, &record).unwrap();
        assert_eq!(address, record.public_address);
        
        let addresses = get_multichain_addresses(pin, &record, true).unwrap();
        assert_eq!(addresses.eth, record.public_address);
        assert!(!addresses.sol.is_empty());
    }

    #[test]
    fn test_sign_transaction_hd() {
        let pin = "testpin";
        let record = create_vault(pin).unwrap();
        let tx = UnsignedLegacyTx {
            nonce: 0,
            gas_price: "20000000000".to_string(),
            gas_limit: 21000,
            to: "0x0000000000000000000000000000000000000000".to_string(),
            value: "1000000000000000000".to_string(),
            data: "".to_string(),
            chain_id: 1,
        };
        let signed = sign_transaction(pin, &record, &tx).unwrap();
        assert!(signed.starts_with("0x"));
    }
}
