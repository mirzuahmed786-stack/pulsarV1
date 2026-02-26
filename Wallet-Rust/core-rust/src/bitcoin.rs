use bitcoin::consensus::encode::deserialize;
use bitcoin::sighash::{SighashCache, EcdsaSighashType};
use bitcoin::{Transaction, ScriptBuf, Witness};
use bitcoin::hashes::Hash;
use k256::ecdsa::{SigningKey, signature::hazmat::PrehashSigner};

pub fn sign_bitcoin_p2wpkh_tx(
    secret_key_bytes: &[u8],
    unsigned_tx_hex: &str,
    input_index: usize,
    prev_script_pubkey_hex: &str,
    value_sats: u64,
) -> Result<String, String> {
    let signing_key = SigningKey::from_bytes(secret_key_bytes.into())
        .map_err(|e| format!("Invalid secret key: {}", e))?;

    let unsigned_tx_bytes = hex::decode(unsigned_tx_hex)
        .map_err(|e| format!("Invalid tx hex: {}", e))?;
    
    let mut tx: Transaction = deserialize(&unsigned_tx_bytes)
        .map_err(|e| format!("Failed to deserialize tx: {}", e))?;

    let script_pubkey = ScriptBuf::from_hex(prev_script_pubkey_hex)
        .map_err(|e| format!("Invalid script pubkey: {}", e))?;

    let mut cache = SighashCache::new(&tx);
    let sighash = cache.segwit_signature_hash(
        input_index,
        &script_pubkey,
        value_sats,
        EcdsaSighashType::All,
    ).map_err(|e| format!("Failed to calculate sighash: {}", e))?;

    // Sign the sighash using k256
    let signature = signing_key.sign_prehash(sighash.as_byte_array())
        .map_err(|e| format!("Signing failed: {}", e))?;

    // Create the witness: [signature + sighash_type, public_key]
    let mut sig_with_type = signature.to_der().to_vec();
    sig_with_type.push(EcdsaSighashType::All as u8);

    // Get compressed public key
    let public_key = signing_key.verifying_key().to_encoded_point(true);
    let pubkey_bytes = public_key.as_bytes().to_vec();

    let mut witness = Witness::new();
    witness.push(sig_with_type);
    witness.push(pubkey_bytes);

    // Update the transaction witness for the specific input
    tx.input[input_index].witness = witness;

    Ok(hex::encode(bitcoin::consensus::encode::serialize(&tx)))
}
