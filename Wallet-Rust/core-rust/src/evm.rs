use ethereum_types::U256;
use k256::ecdsa::signature::hazmat::PrehashSigner;
use k256::ecdsa::{RecoveryId, Signature, SigningKey, VerifyingKey};
use rlp::RlpStream;
use sha3::{Digest, Keccak256};

use crate::types::{AccessListItem, UnsignedEip1559Tx, UnsignedLegacyTx};

fn strip_0x(input: &str) -> &str {
    input.strip_prefix("0x").unwrap_or(input)
}

fn decode_hex(input: &str) -> Result<Vec<u8>, String> {
    let clean = strip_0x(input);
    if clean.is_empty() {
        return Ok(Vec::new());
    }
    hex::decode(clean).map_err(|_| "Invalid hex input".to_string())
}

fn encode_access_list(access_list: &[AccessListItem]) -> Result<Vec<u8>, String> {
    let mut outer = RlpStream::new_list(access_list.len());
    for item in access_list {
        let address = decode_hex(&item.address)?;
        if address.len() != 20 {
            return Err("Invalid access list address".to_string());
        }

        let mut item_stream = RlpStream::new_list(2);
        item_stream.append(&address);

        let mut keys_stream = RlpStream::new_list(item.storage_keys.len());
        for key in &item.storage_keys {
            let key_bytes = decode_hex(key)?;
            if key_bytes.len() != 32 {
                return Err("Invalid access list storage key".to_string());
            }
            keys_stream.append(&key_bytes);
        }

        item_stream.append_raw(&keys_stream.out(), 1);
        outer.append_raw(&item_stream.out(), 1);
    }
    Ok(outer.out().to_vec())
}

fn encode_unsigned_legacy(tx: &UnsignedLegacyTx) -> Result<Vec<u8>, String> {
    let to_bytes = decode_hex(&tx.to)?;
    let data = decode_hex(&tx.data)?;

    if !to_bytes.is_empty() && to_bytes.len() != 20 {
        return Err("Invalid to address".to_string());
    }

    let gas_price = U256::from_dec_str(&tx.gas_price).map_err(|_| "Invalid gas price".to_string())?;
    let value = U256::from_dec_str(&tx.value).map_err(|_| "Invalid value".to_string())?;

    let mut stream = RlpStream::new_list(9);
    stream.append(&tx.nonce);
    stream.append(&gas_price);
    stream.append(&tx.gas_limit);
    if to_bytes.is_empty() {
        stream.append_empty_data();
    } else {
        stream.append(&to_bytes);
    }
    stream.append(&value);
    stream.append(&data);
    stream.append(&tx.chain_id);
    stream.append(&0u8);
    stream.append(&0u8);

    Ok(stream.out().to_vec())
}

fn encode_signed_legacy(tx: &UnsignedLegacyTx, v: u64, r: U256, s: U256) -> Result<Vec<u8>, String> {
    let to_bytes = decode_hex(&tx.to)?;
    let data = decode_hex(&tx.data)?;

    let gas_price = U256::from_dec_str(&tx.gas_price).map_err(|_| "Invalid gas price".to_string())?;
    let value = U256::from_dec_str(&tx.value).map_err(|_| "Invalid value".to_string())?;

    let mut stream = RlpStream::new_list(9);
    stream.append(&tx.nonce);
    stream.append(&gas_price);
    stream.append(&tx.gas_limit);
    if to_bytes.is_empty() {
        stream.append_empty_data();
    } else {
        stream.append(&to_bytes);
    }
    stream.append(&value);
    stream.append(&data);
    stream.append(&v);
    stream.append(&r);
    stream.append(&s);

    Ok(stream.out().to_vec())
}

fn encode_unsigned_eip1559(tx: &UnsignedEip1559Tx) -> Result<Vec<u8>, String> {
    let to_bytes = decode_hex(&tx.to)?;
    let data = decode_hex(&tx.data)?;

    if !to_bytes.is_empty() && to_bytes.len() != 20 {
        return Err("Invalid to address".to_string());
    }

    let max_priority_fee_per_gas =
        U256::from_dec_str(&tx.max_priority_fee_per_gas).map_err(|_| "Invalid max priority fee".to_string())?;
    let max_fee_per_gas = U256::from_dec_str(&tx.max_fee_per_gas).map_err(|_| "Invalid max fee".to_string())?;
    let value = U256::from_dec_str(&tx.value).map_err(|_| "Invalid value".to_string())?;
    let access_list = encode_access_list(&tx.access_list)?;

    let mut stream = RlpStream::new_list(9);
    stream.append(&tx.chain_id);
    stream.append(&tx.nonce);
    stream.append(&max_priority_fee_per_gas);
    stream.append(&max_fee_per_gas);
    stream.append(&tx.gas_limit);
    if to_bytes.is_empty() {
        stream.append_empty_data();
    } else {
        stream.append(&to_bytes);
    }
    stream.append(&value);
    stream.append(&data);
    stream.append_raw(&access_list, 1);

    Ok(stream.out().to_vec())
}

fn encode_signed_eip1559(tx: &UnsignedEip1559Tx, y_parity: u8, r: U256, s: U256) -> Result<Vec<u8>, String> {
    let to_bytes = decode_hex(&tx.to)?;
    let data = decode_hex(&tx.data)?;

    if !to_bytes.is_empty() && to_bytes.len() != 20 {
        return Err("Invalid to address".to_string());
    }

    let max_priority_fee_per_gas =
        U256::from_dec_str(&tx.max_priority_fee_per_gas).map_err(|_| "Invalid max priority fee".to_string())?;
    let max_fee_per_gas = U256::from_dec_str(&tx.max_fee_per_gas).map_err(|_| "Invalid max fee".to_string())?;
    let value = U256::from_dec_str(&tx.value).map_err(|_| "Invalid value".to_string())?;
    let access_list = encode_access_list(&tx.access_list)?;

    let mut stream = RlpStream::new_list(12);
    stream.append(&tx.chain_id);
    stream.append(&tx.nonce);
    stream.append(&max_priority_fee_per_gas);
    stream.append(&max_fee_per_gas);
    stream.append(&tx.gas_limit);
    if to_bytes.is_empty() {
        stream.append_empty_data();
    } else {
        stream.append(&to_bytes);
    }
    stream.append(&value);
    stream.append(&data);
    stream.append_raw(&access_list, 1);
    stream.append(&y_parity);
    stream.append(&r);
    stream.append(&s);

    Ok(stream.out().to_vec())
}

pub fn sign_legacy_tx(secret_key_bytes: &[u8], tx: &UnsignedLegacyTx) -> Result<String, String> {
    if secret_key_bytes.len() != 32 {
        return Err("Invalid secret key length".to_string());
    }
    let signing_key = SigningKey::from_bytes(secret_key_bytes.try_into().map_err(|_| "Invalid secret key".to_string())?)
        .map_err(|_| "Invalid secret key".to_string())?;
    let verifying_key = signing_key.verifying_key();

    let unsigned = encode_unsigned_legacy(tx)?;
    let hash = Keccak256::digest(&unsigned);

    let sig: Signature = signing_key
        .sign_prehash(&hash)
        .map_err(|_| "Failed to sign transaction".to_string())?;
    let recid = recover_id(&hash, &sig, verifying_key)?;
    let sig_bytes = sig.to_bytes();
    let v = (recid.is_y_odd() as u64) + 35 + (tx.chain_id * 2);

    let r = U256::from_big_endian(&sig_bytes[0..32]);
    let s = U256::from_big_endian(&sig_bytes[32..64]);

    let signed = encode_signed_legacy(tx, v, r, s)?;
    Ok(format!("0x{}", hex::encode(signed)))
}

pub fn sign_eip1559_tx(secret_key_bytes: &[u8], tx: &UnsignedEip1559Tx) -> Result<String, String> {
    if secret_key_bytes.len() != 32 {
        return Err("Invalid secret key length".to_string());
    }
    let signing_key = SigningKey::from_bytes(secret_key_bytes.try_into().map_err(|_| "Invalid secret key".to_string())?)
        .map_err(|_| "Invalid secret key".to_string())?;
    let verifying_key = signing_key.verifying_key();

    let unsigned = encode_unsigned_eip1559(tx)?;
    let mut payload = Vec::with_capacity(1 + unsigned.len());
    payload.push(0x02);
    payload.extend_from_slice(&unsigned);
    let hash = Keccak256::digest(&payload);

    let sig: Signature = signing_key
        .sign_prehash(&hash)
        .map_err(|_| "Failed to sign transaction".to_string())?;
    let recid = recover_id(&hash, &sig, verifying_key)?;
    let sig_bytes = sig.to_bytes();
    let y_parity = recid.is_y_odd() as u8;

    let r = U256::from_big_endian(&sig_bytes[0..32]);
    let s = U256::from_big_endian(&sig_bytes[32..64]);

    let signed = encode_signed_eip1559(tx, y_parity, r, s)?;
    let mut out = Vec::with_capacity(1 + signed.len());
    out.push(0x02);
    out.extend_from_slice(&signed);
    Ok(format!("0x{}", hex::encode(out)))
}

fn recover_id(hash: &[u8], sig: &Signature, verifying_key: &VerifyingKey) -> Result<RecoveryId, String> {
    for is_y_odd in [false, true] {
        for is_x_reduced in [false, true] {
            let recid = RecoveryId::new(is_y_odd, is_x_reduced);
            if let Ok(recovered) = VerifyingKey::recover_from_prehash(hash, sig, recid) {
                if &recovered == verifying_key {
                    return Ok(recid);
                }
            }
        }
    }
    Err("Failed to derive recovery id".to_string())
}
