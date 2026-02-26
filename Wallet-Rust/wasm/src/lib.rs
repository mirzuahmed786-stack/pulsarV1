use serde::{Deserialize, Serialize};
use wasm_bindgen::prelude::*;

use wallet_core::{
    create_vault,
    create_vault_from_mnemonic,
    create_vault_from_private_key,
    generate_mnemonic,
    get_multichain_addresses_by_index,
    migrate_vault,
    rotate_pin,
    sign_transaction,
    sign_transaction_eip1559,
    sign_transaction_eip1559_with_chain,
    sign_transaction_with_chain,
    verify_pin,
    UnsignedEip1559Tx,
    UnsignedLegacyTx,
    VaultRecord,
    derive_sol_address,
    derive_btc_address,
    create_wallet_from_web3auth_key,
    restore_wallet_from_web3auth_key,
    create_recovery_backup,
    restore_vault_from_recovery_backup,
    verify_recovery_backup,
    create_cloud_recovery_blob,
    restore_vault_from_cloud_recovery_blob,
    verify_cloud_recovery_blob,
    RecoveryBackup,
    CloudRecoveryBlob,
};
#[cfg(feature = "dangerous-key-export")]
use wallet_core::export_eth_private_key;

#[derive(Debug, Serialize, Deserialize)]
struct MultiChainAddresses {
    eth: String,
    btc: String,
    sol: String,
}

#[derive(Debug, Serialize, Deserialize)]
struct CreateVaultResponse {
    record: VaultRecord,
    address: String,
}

fn parse_record(record_json: &str) -> Result<VaultRecord, JsValue> {
    serde_json::from_str(record_json).map_err(|_| JsValue::from_str("Invalid vault record"))
}

fn parse_tx(tx_json: &str) -> Result<UnsignedLegacyTx, JsValue> {
    serde_json::from_str(tx_json).map_err(|_| JsValue::from_str("Invalid transaction payload"))
}

fn parse_eip1559_tx(tx_json: &str) -> Result<UnsignedEip1559Tx, JsValue> {
    serde_json::from_str(tx_json).map_err(|_| JsValue::from_str("Invalid EIP-1559 transaction payload"))
}

fn parse_recovery_backup(backup_json: &str) -> Result<RecoveryBackup, JsValue> {
    serde_json::from_str(backup_json).map_err(|_| JsValue::from_str("Invalid recovery backup payload"))
}

fn parse_cloud_recovery_blob(blob_json: &str) -> Result<CloudRecoveryBlob, JsValue> {
    serde_json::from_str(blob_json).map_err(|_| JsValue::from_str("Invalid cloud recovery blob payload"))
}

#[wasm_bindgen]
pub fn create_vault_wasm(pin: String) -> Result<JsValue, JsValue> {
    let record = create_vault(&pin).map_err(|e| JsValue::from_str(&e.to_string()))?;
    let response = CreateVaultResponse {
        address: record.public_address.clone(),
        record,
    };
    serde_wasm_bindgen::to_value(&response).map_err(|_| JsValue::from_str("Failed to serialize vault"))
}

#[wasm_bindgen]
pub fn generate_mnemonic_wasm() -> Result<String, JsValue> {
    generate_mnemonic().map_err(|e| JsValue::from_str(&e.to_string()))
}

#[wasm_bindgen]
pub fn create_vault_from_mnemonic_wasm(pin: String, mnemonic: String, path: String) -> Result<JsValue, JsValue> {
    let record = create_vault_from_mnemonic(&pin, &mnemonic, &path).map_err(|e| JsValue::from_str(&e.to_string()))?;
    let response = CreateVaultResponse {
        address: record.public_address.clone(),
        record,
    };
    serde_wasm_bindgen::to_value(&response).map_err(|_| JsValue::from_str("Failed to serialize vault"))
}

#[wasm_bindgen]
pub fn create_vault_from_private_key_wasm(pin: String, private_key_hex: String) -> Result<JsValue, JsValue> {
    let record = create_vault_from_private_key(&pin, &private_key_hex).map_err(|e| JsValue::from_str(&e.to_string()))?;
    let response = CreateVaultResponse {
        address: record.public_address.clone(),
        record,
    };
    serde_wasm_bindgen::to_value(&response).map_err(|_| JsValue::from_str("Failed to serialize vault"))
}

#[wasm_bindgen]
pub fn verify_pin_wasm(pin: String, record_json: String) -> Result<String, JsValue> {
    let record = parse_record(&record_json)?;
    verify_pin(&pin, &record).map_err(|e| JsValue::from_str(&e.to_string()))
}

#[cfg(feature = "dangerous-key-export")]
#[wasm_bindgen]
pub fn export_eth_private_key_wasm(pin: String, record_json: String) -> Result<String, JsValue> {
    let record = parse_record(&record_json)?;
    export_eth_private_key(&pin, &record).map_err(|e| JsValue::from_str(&e.to_string()))
}

#[wasm_bindgen]
pub fn sign_transaction_wasm(pin: String, record_json: String, tx_json: String) -> Result<String, JsValue> {
    let record = parse_record(&record_json)?;
    let tx = parse_tx(&tx_json)?;
    sign_transaction(&pin, &record, &tx).map_err(|e| JsValue::from_str(&e.to_string()))
}

#[wasm_bindgen]
pub fn sign_transaction_with_chain_wasm(
    pin: String,
    record_json: String,
    tx_json: String,
    expected_chain_id: u64
) -> Result<String, JsValue> {
    let record = parse_record(&record_json)?;
    let tx = parse_tx(&tx_json)?;
    sign_transaction_with_chain(&pin, &record, &tx, expected_chain_id).map_err(|e| JsValue::from_str(&e.to_string()))
}

#[wasm_bindgen]
pub fn sign_transaction_eip1559_wasm(pin: String, record_json: String, tx_json: String) -> Result<String, JsValue> {
    let record = parse_record(&record_json)?;
    let tx = parse_eip1559_tx(&tx_json)?;
    sign_transaction_eip1559(&pin, &record, &tx).map_err(|e| JsValue::from_str(&e.to_string()))
}

#[wasm_bindgen]
pub fn sign_transaction_eip1559_with_chain_wasm(
    pin: String,
    record_json: String,
    tx_json: String,
    expected_chain_id: u64
) -> Result<String, JsValue> {
    let record = parse_record(&record_json)?;
    let tx = parse_eip1559_tx(&tx_json)?;
    sign_transaction_eip1559_with_chain(&pin, &record, &tx, expected_chain_id).map_err(|e| JsValue::from_str(&e.to_string()))
}

#[wasm_bindgen]
pub fn get_address_wasm(record_json: String) -> Result<String, JsValue> {
    let record = parse_record(&record_json)?;
    Ok(record.public_address)
}

#[wasm_bindgen]
pub fn change_pin_wasm(old_pin: String, new_pin: String, record_json: String) -> Result<JsValue, JsValue> {
    let record = parse_record(&record_json)?;
    let updated = rotate_pin(&old_pin, &new_pin, &record).map_err(|e| JsValue::from_str(&e.to_string()))?;
    serde_wasm_bindgen::to_value(&updated).map_err(|_| JsValue::from_str("Failed to serialize vault"))
}

#[wasm_bindgen]
pub fn migrate_vault_wasm(pin: String, record_json: String) -> Result<JsValue, JsValue> {
    let record = parse_record(&record_json)?;
    let updated = migrate_vault(&pin, &record).map_err(|e| JsValue::from_str(&e.to_string()))?;
    serde_wasm_bindgen::to_value(&updated).map_err(|_| JsValue::from_str("Failed to serialize vault"))
}

#[wasm_bindgen]
pub fn sign_solana_transaction_wasm(pin: String, record_json: String, message: Vec<u8>) -> Result<Vec<u8>, JsValue> {
    let record = parse_record(&record_json)?;
    wallet_core::sign_solana_transaction(&pin, &record, &message).map_err(|e| JsValue::from_str(&e.to_string()))
}

#[wasm_bindgen]
pub fn sign_bitcoin_transaction_wasm(
    pin: String,
    record_json: String,
    sighash_hex: String,
    testnet: bool,
) -> Result<String, JsValue> {
    let record = parse_record(&record_json)?;
    wallet_core::sign_bitcoin_transaction(
        &pin,
        &record,
        &sighash_hex,
        testnet,
    ).map_err(|e| JsValue::from_str(&e.to_string()))
}

#[wasm_bindgen]
pub fn get_btc_public_key_wasm(pin: String, record_json: String, testnet: bool) -> Result<String, JsValue> {
    let record = parse_record(&record_json)?;
    wallet_core::get_btc_public_key(&pin, &record, testnet).map_err(|e| JsValue::from_str(&e.to_string()))
}

#[wasm_bindgen]
pub fn get_multichain_addresses_wasm(pin: String, record_json: String, testnet: bool) -> Result<JsValue, JsValue> {
    let record = parse_record(&record_json)?;
    let addresses = wallet_core::get_multichain_addresses(&pin, &record, testnet)
        .map_err(|e| JsValue::from_str(&e.to_string()))?;
    serde_wasm_bindgen::to_value(&addresses).map_err(|_| JsValue::from_str("Failed to serialize addresses"))
}

#[wasm_bindgen]
pub fn get_multichain_addresses_by_index_wasm(
    pin: String,
    record_json: String,
    testnet: bool,
    index: u32
) -> Result<JsValue, JsValue> {
    let record = parse_record(&record_json)?;
    let addresses = get_multichain_addresses_by_index(&pin, &record, testnet, index)
        .map_err(|e| JsValue::from_str(&e.to_string()))?;
    serde_wasm_bindgen::to_value(&addresses).map_err(|_| JsValue::from_str("Failed to serialize addresses"))
}

#[wasm_bindgen]
pub fn derive_multi_chain_addresses_wasm(mnemonic: String, testnet: bool) -> Result<JsValue, JsValue> {
    // Helper to see addresses from raw mnemonic without vault
    let eth_res = wallet_core::create_vault_from_mnemonic("ignore", &mnemonic, "m/44'/60'/0'/0/0")
        .map_err(|e| JsValue::from_str(&e.to_string()))?;
    
    let sol_addr = derive_sol_address(&mnemonic)
        .map_err(|e| JsValue::from_str(&e.to_string()))?;

    let btc_addr = derive_btc_address(&mnemonic, testnet)
        .map_err(|e| JsValue::from_str(&e.to_string()))?;
    
    let response = MultiChainAddresses {
        eth: eth_res.public_address,
        btc: btc_addr,
        sol: sol_addr,
    };
    
    serde_wasm_bindgen::to_value(&response).map_err(|_| JsValue::from_str("Failed to serialize addresses"))
}

/// Web3Auth Integration: Create wallet from OAuth private key (first-time login)
#[wasm_bindgen]
pub fn create_wallet_from_web3auth_key_wasm(
    web3auth_private_key: String,
    testnet: bool
) -> Result<JsValue, JsValue> {
    let result = create_wallet_from_web3auth_key(&web3auth_private_key, testnet)
        .map_err(|e| JsValue::from_str(&e.to_string()))?;
    
    serde_wasm_bindgen::to_value(&result)
        .map_err(|_| JsValue::from_str("Failed to serialize wallet result"))
}

/// Web3Auth Integration: Restore wallet from OAuth private key (returning user)
#[wasm_bindgen]
pub fn restore_wallet_from_web3auth_key_wasm(
    web3auth_private_key: String,
    encrypted_data: String,
    testnet: bool
) -> Result<JsValue, JsValue> {
    let result = restore_wallet_from_web3auth_key(&web3auth_private_key, &encrypted_data, testnet)
        .map_err(|e| JsValue::from_str(&e.to_string()))?;
    
    serde_wasm_bindgen::to_value(&result)
        .map_err(|_| JsValue::from_str("Failed to serialize wallet result"))
}

#[wasm_bindgen]
pub fn create_recovery_backup_wasm(
    pin: String,
    record_json: String,
    backup_passphrase: String
) -> Result<JsValue, JsValue> {
    let record = parse_record(&record_json)?;
    let backup = create_recovery_backup(&pin, &record, &backup_passphrase)
        .map_err(|e| JsValue::from_str(&e.to_string()))?;
    serde_wasm_bindgen::to_value(&backup).map_err(|_| JsValue::from_str("Failed to serialize recovery backup"))
}

#[wasm_bindgen]
pub fn restore_vault_from_recovery_backup_wasm(
    backup_passphrase: String,
    backup_json: String,
    new_pin: String
) -> Result<JsValue, JsValue> {
    let backup = parse_recovery_backup(&backup_json)?;
    let record = restore_vault_from_recovery_backup(&backup_passphrase, &backup, &new_pin)
        .map_err(|e| JsValue::from_str(&e.to_string()))?;
    let response = CreateVaultResponse {
        address: record.public_address.clone(),
        record,
    };
    serde_wasm_bindgen::to_value(&response).map_err(|_| JsValue::from_str("Failed to serialize vault"))
}

#[wasm_bindgen]
pub fn verify_recovery_backup_wasm(
    backup_passphrase: String,
    backup_json: String
) -> Result<bool, JsValue> {
    let backup = parse_recovery_backup(&backup_json)?;
    verify_recovery_backup(&backup_passphrase, &backup)
        .map_err(|e| JsValue::from_str(&e.to_string()))?;
    Ok(true)
}

#[wasm_bindgen]
pub fn create_cloud_recovery_blob_wasm(
    pin: String,
    record_json: String,
    oauth_kek: String
) -> Result<JsValue, JsValue> {
    let record = parse_record(&record_json)?;
    let blob = create_cloud_recovery_blob(&pin, &record, &oauth_kek)
        .map_err(|e| JsValue::from_str(&e.to_string()))?;
    serde_wasm_bindgen::to_value(&blob).map_err(|_| JsValue::from_str("Failed to serialize cloud recovery blob"))
}

#[wasm_bindgen]
pub fn restore_vault_from_cloud_recovery_blob_wasm(
    oauth_kek: String,
    blob_json: String,
    new_pin: String
) -> Result<JsValue, JsValue> {
    let blob = parse_cloud_recovery_blob(&blob_json)?;
    let record = restore_vault_from_cloud_recovery_blob(&oauth_kek, &blob, &new_pin)
        .map_err(|e| JsValue::from_str(&e.to_string()))?;
    let response = CreateVaultResponse {
        address: record.public_address.clone(),
        record,
    };
    serde_wasm_bindgen::to_value(&response).map_err(|_| JsValue::from_str("Failed to serialize vault"))
}

#[wasm_bindgen]
pub fn verify_cloud_recovery_blob_wasm(
    oauth_kek: String,
    blob_json: String
) -> Result<bool, JsValue> {
    let blob = parse_cloud_recovery_blob(&blob_json)?;
    verify_cloud_recovery_blob(&oauth_kek, &blob)
        .map_err(|e| JsValue::from_str(&e.to_string()))?;
    Ok(true)
}
