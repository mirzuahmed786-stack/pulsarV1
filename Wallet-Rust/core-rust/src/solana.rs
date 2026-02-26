use ed25519_dalek::{Signer, SigningKey};

pub fn sign_solana_message(secret_key_bytes: &[u8], message: &[u8]) -> Result<Vec<u8>, String> {
    if secret_key_bytes.len() != 32 {
        return Err("Invalid secret key length for Solana (Ed25519)".to_string());
    }

    let signing_key = SigningKey::from_bytes(
        secret_key_bytes.try_into().map_err(|_| "Failed to convert secret key bytes")?
    );
    
    let signature = signing_key.sign(message);
    Ok(signature.to_bytes().to_vec())
}
