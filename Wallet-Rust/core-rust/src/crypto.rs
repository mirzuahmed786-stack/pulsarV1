use argon2::{Algorithm, Argon2, Params, Version};
use chacha20poly1305::aead::{Aead, KeyInit};
use chacha20poly1305::{ChaCha20Poly1305, Nonce, XChaCha20Poly1305, XNonce};
use rand::rngs::OsRng;
use rand::RngCore;
use zeroize::{Zeroize, Zeroizing};

use crate::types::{CipherBlob, KdfParams, VaultRecord};

const VAULT_VERSION: u32 = 1;
const RECOVERY_BACKUP_VERSION: u32 = 1;

#[allow(deprecated)]
#[allow(deprecated)]
pub fn encrypt_secret(secret_data: &[u8], pin: &str, address: String) -> Result<VaultRecord, String> {
    let mut salt = [0u8; 16];
    OsRng.fill_bytes(&mut salt);

    let params = Params::new(128 * 1024, 4, 1, Some(32))
        .map_err(|_| "Failed to set Argon2 params".to_string())?;
    let argon2 = Argon2::new(Algorithm::Argon2id, Version::V0x13, params);

    let mut key_bytes = [0u8; 32];
    argon2
        .hash_password_into(pin.as_bytes(), &salt, &mut key_bytes)
        .map_err(|_| "Failed to derive key".to_string())?;

    let cipher = XChaCha20Poly1305::new((&key_bytes).into());
    let mut nonce = [0u8; 24];
    OsRng.fill_bytes(&mut nonce);

    let nonce_ref = XNonce::from(nonce);
    let ciphertext = cipher
        .encrypt(&nonce_ref, secret_data)
        .map_err(|_| "Failed to encrypt secret".to_string())?;

    key_bytes.zeroize();

    Ok(VaultRecord {
        version: VAULT_VERSION,
        kdf: KdfParams {
            name: "argon2id".to_string(),
            salt: salt.to_vec(),
            memory_kib: 128 * 1024,
            iterations: 4,
            parallelism: 1,
        },
        cipher: CipherBlob {
            nonce: nonce.to_vec(),
            ciphertext,
        },
        public_address: address,
        hd_index: None,
        is_hd: None,
    })
}

#[allow(deprecated)]
pub fn decrypt_secret(pin: &str, record: &VaultRecord) -> Result<Zeroizing<Vec<u8>>, String> {
    if record.kdf.name != "argon2id" {
        return Err("Unsupported KDF".to_string());
    }

    let params = Params::new(
        record.kdf.memory_kib,
        record.kdf.iterations,
        record.kdf.parallelism,
        Some(32),
    )
    .map_err(|_| "Invalid KDF params".to_string())?;
    let argon2 = Argon2::new(Algorithm::Argon2id, Version::V0x13, params);

    let mut key_bytes = [0u8; 32];
    argon2
        .hash_password_into(pin.as_bytes(), &record.kdf.salt, &mut key_bytes)
        .map_err(|_| "Failed to derive key".to_string())?;

    let plaintext = match record.cipher.nonce.len() {
        24 => {
            let cipher = XChaCha20Poly1305::new((&key_bytes).into());
            let nonce = <[u8; 24]>::try_from(record.cipher.nonce.as_slice())
                .map_err(|_| "Invalid XChaCha20 nonce length".to_string())?;
            let nonce = XNonce::from(nonce);
            cipher
                .decrypt(&nonce, record.cipher.ciphertext.as_slice())
                .map_err(|e| format!(
                    "Decryption failed (XChaCha20): {}. Nonce len: {}, Cipher len: {}",
                    e,
                    record.cipher.nonce.len(),
                    record.cipher.ciphertext.len()
                ))?
        }
        12 => {
            let cipher = ChaCha20Poly1305::new((&key_bytes).into());
            let nonce = <[u8; 12]>::try_from(record.cipher.nonce.as_slice())
                .map_err(|_| "Invalid ChaCha20 nonce length".to_string())?;
            let nonce = Nonce::from(nonce);
            cipher
                .decrypt(&nonce, record.cipher.ciphertext.as_slice())
                .map_err(|e| format!(
                    "Decryption failed (ChaCha20): {}. Nonce len: {}, Cipher len: {}",
                    e,
                    record.cipher.nonce.len(),
                    record.cipher.ciphertext.len()
                ))?
        }
        _ => return Err(format!("Invalid nonce length: {}", record.cipher.nonce.len())),
    };
    key_bytes.zeroize();
    Ok(Zeroizing::new(plaintext))
}

pub fn encrypt_recovery_backup_secret(secret_data: &[u8], passphrase: &str) -> Result<(u32, KdfParams, CipherBlob), String> {
    if passphrase.trim().is_empty() {
        return Err("Backup passphrase is required".to_string());
    }

    let mut salt = [0u8; 16];
    OsRng.fill_bytes(&mut salt);

    // WASM memory is much tighter than native. A 128MiB Argon2id setting can OOM/trap
    // in the browser (seen as `RuntimeError: unreachable`). Use a smaller, still
    // meaningful memory cost in wasm builds.
    let (memory_kib, iterations, parallelism) = if cfg!(target_arch = "wasm32") {
        (32 * 1024, 3, 1) // 32MiB
    } else {
        (128 * 1024, 4, 1) // 128MiB
    };

    let params = Params::new(memory_kib, iterations, parallelism, Some(32))
        .map_err(|_| "Failed to set Argon2 params".to_string())?;
    let argon2 = Argon2::new(Algorithm::Argon2id, Version::V0x13, params);

    let mut key_bytes = [0u8; 32];
    argon2
        .hash_password_into(passphrase.as_bytes(), &salt, &mut key_bytes)
        .map_err(|_| "Failed to derive key".to_string())?;

    let cipher = XChaCha20Poly1305::new((&key_bytes).into());
    let mut nonce = [0u8; 24];
    OsRng.fill_bytes(&mut nonce);

    let nonce_ref = XNonce::from(nonce);
    let ciphertext = cipher
        .encrypt(&nonce_ref, secret_data)
        .map_err(|_| "Failed to encrypt secret".to_string())?;

    key_bytes.zeroize();

    Ok((
        RECOVERY_BACKUP_VERSION,
        KdfParams {
            name: "argon2id".to_string(),
            salt: salt.to_vec(),
            memory_kib,
            iterations,
            parallelism,
        },
        CipherBlob {
            nonce: nonce.to_vec(),
            ciphertext,
        },
    ))
}

pub fn decrypt_recovery_backup_secret(passphrase: &str, kdf: &KdfParams, cipher: &CipherBlob) -> Result<Zeroizing<Vec<u8>>, String> {
    if kdf.name != "argon2id" {
        return Err("Unsupported KDF".to_string());
    }

    let params = Params::new(
        kdf.memory_kib,
        kdf.iterations,
        kdf.parallelism,
        Some(32),
    )
    .map_err(|_| "Invalid KDF params".to_string())?;
    let argon2 = Argon2::new(Algorithm::Argon2id, Version::V0x13, params);

    let mut key_bytes = [0u8; 32];
    argon2
        .hash_password_into(passphrase.as_bytes(), &kdf.salt, &mut key_bytes)
        .map_err(|_| "Failed to derive key".to_string())?;

    if cipher.nonce.len() != 24 {
        key_bytes.zeroize();
        return Err("Invalid nonce length".to_string());
    }

    let xchacha = XChaCha20Poly1305::new((&key_bytes).into());
    let nonce = <[u8; 24]>::try_from(cipher.nonce.as_slice())
        .map_err(|_| "Invalid XChaCha20 nonce length".to_string())?;
    let nonce = XNonce::from(nonce);
    let plaintext = xchacha
        .decrypt(&nonce, cipher.ciphertext.as_slice())
        .map_err(|_| "Decryption failed".to_string())?;

    key_bytes.zeroize();
    Ok(Zeroizing::new(plaintext))
}

/// Web3Auth Integration: Encrypt data using Web3Auth private key
/// This is simpler than PIN-based encryption since we use the key directly
pub fn encrypt_with_key(data: &[u8], web3auth_key: &str) -> Result<String, String> {
    use sha2::{Sha256, Digest};
    
    // Derive encryption key from Web3Auth private key
    let clean_key = web3auth_key.strip_prefix("0x").unwrap_or(web3auth_key);
    let key_bytes = hex::decode(clean_key)
        .map_err(|_| "Invalid Web3Auth key format".to_string())?;
    
    if key_bytes.len() != 32 {
        return Err("Web3Auth key must be 32 bytes".to_string());
    }
    
    // Hash the key to create encryption key (domain separation)
    let mut hasher = Sha256::new();
    hasher.update(&key_bytes);
    hasher.update(b"elementa-encryption-v1");
    let encryption_key = hasher.finalize();
    
    // Generate random nonce
    let mut nonce = [0u8; 24];
    OsRng.fill_bytes(&mut nonce);
    
    // Encrypt with XChaCha20Poly1305
    let cipher = XChaCha20Poly1305::new((&encryption_key).into());
    let nonce_ref = XNonce::from(nonce);
    let ciphertext = cipher
        .encrypt(&nonce_ref, data)
        .map_err(|_| "Encryption failed".to_string())?;
    
    // Combine nonce + ciphertext and encode as hex
    let mut combined = Vec::new();
    combined.extend_from_slice(&nonce);
    combined.extend_from_slice(&ciphertext);
    
    Ok(hex::encode(combined))
}

/// Web3Auth Integration: Decrypt data using Web3Auth private key
pub fn decrypt_with_key(encrypted_hex: &str, web3auth_key: &str) -> Result<Vec<u8>, String> {
    use sha2::{Sha256, Digest};
    
    // Derive encryption key from Web3Auth private key
    let clean_key = web3auth_key.strip_prefix("0x").unwrap_or(web3auth_key);
    let key_bytes = hex::decode(clean_key)
        .map_err(|_| "Invalid Web3Auth key format".to_string())?;
    
    if key_bytes.len() != 32 {
        return Err("Web3Auth key must be 32 bytes".to_string());
    }
    
    // Hash the key to create encryption key (domain separation)
    let mut hasher = Sha256::new();
    hasher.update(&key_bytes);
    hasher.update(b"elementa-encryption-v1");
    let encryption_key = hasher.finalize();
    
    // Decode hex
    let combined = hex::decode(encrypted_hex)
        .map_err(|_| "Invalid encrypted data format".to_string())?;
    
    if combined.len() < 24 {
        return Err("Encrypted data too short".to_string());
    }
    
    // Split nonce and ciphertext
    let (nonce, ciphertext) = combined.split_at(24);
    
    // Decrypt with XChaCha20Poly1305
    let cipher = XChaCha20Poly1305::new((&encryption_key).into());
    let nonce_ref = XNonce::from(
        <[u8; 24]>::try_from(nonce).map_err(|_| "Invalid nonce length".to_string())?
    );
    let plaintext = cipher
        .decrypt(&nonce_ref, ciphertext)
        .map_err(|_| "Decryption failed - invalid key or corrupted data".to_string())?;
    
    Ok(plaintext)
}

