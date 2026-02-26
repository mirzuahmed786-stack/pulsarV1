// hassam dev: JNI bindings for Android integration
// This module provides the native bridge between Kotlin and Rust

use jni::{
    objects::{JClass, JString, JObject, JByteArray},
    sys::{jstring, jbyteArray},
    JNIEnv,
};
use serde_json;

use crate::{create_vault, verify_pin, generate_mnemonic, create_vault_from_mnemonic, create_vault_from_private_key, rotate_pin, migrate_vault, export_eth_private_key, get_multichain_addresses, get_multichain_addresses_by_index, get_btc_public_key, sign_transaction, sign_transaction_eip1559, sign_transaction_with_chain, sign_transaction_eip1559_with_chain, sign_solana_transaction, sign_bitcoin_transaction, derive_btc_address, derive_sol_address, create_recovery_backup, restore_vault_from_recovery_backup, verify_recovery_backup, create_cloud_recovery_blob, restore_vault_from_cloud_recovery_blob, verify_cloud_recovery_blob, create_wallet_from_web3auth_key, restore_wallet_from_web3auth_key, types::{VaultRecord, UnsignedLegacyTx, UnsignedEip1559Tx, RecoveryBackup, CloudRecoveryBlob}};

/// JNI wrapper for createVault function
/// Called from Kotlin: WalletrustlibKt.createVaultFfi(pin: String): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_createVaultFfi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
) -> jstring {
    // Convert JString to Rust String
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Call the Rust create_vault function
    match create_vault(&pin_str) {
        Ok(vault_record) => {
            // Serialize VaultRecord to JSON
            match serde_json::to_string(&vault_record) {
                Ok(json) => match env.new_string(json) {
                    Ok(s) => s.into_raw(),
                    Err(_) => JObject::null().into_raw(),
                },
                Err(e) => {
                    let error_msg = format!("{{\"error\":\"Serialization failed: {}\"}}", e);
                    match env.new_string(error_msg) {
                        Ok(s) => s.into_raw(),
                        Err(_) => JObject::null().into_raw(),
                    }
                }
            }
        }
        Err(e) => {
            // Return error as JSON
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for verifyPin function
/// Called from Kotlin: WalletrustlibKt.verifyPinFfi(pin: String, record: String): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_verifyPinFfi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
    vault_json: JString,
) -> jstring {
    // Convert JString to Rust String for PIN
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Convert JString to Rust String for vault JSON
    let vault_json_str: String = match env.get_string(&vault_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read vault JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Deserialize VaultRecord from JSON
    let vault_record: VaultRecord = match serde_json::from_str(&vault_json_str) {
        Ok(v) => v,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse vault: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Call the Rust verify_pin function
    match verify_pin(&pin_str, &vault_record) {
        Ok(address) => {
            // Return address directly as a string
            match env.new_string(address) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
        Err(e) => {
            // Return error as JSON
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for generateMnemonic function
/// Called from Kotlin: WalletrustlibKt.generateMnemonicFfi(): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_generateMnemonicFfi(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    // Call the Rust generate_mnemonic function
    match generate_mnemonic() {
        Ok(mnemonic) => {
            // Return mnemonic directly as a string
            match env.new_string(mnemonic) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
        Err(e) => {
            // Return error as JSON
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for createVaultFromMnemonic function
/// Called from Kotlin: WalletrustlibKt.createVaultFromMnemonicFfi(pin: String, mnemonic: String, path: String): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_createVaultFromMnemonicFfi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
    mnemonic: JString,
    path: JString,
) -> jstring {
    // Convert JString to Rust String for PIN
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Convert JString to Rust String for mnemonic
    let mnemonic_str: String = match env.get_string(&mnemonic) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read mnemonic: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Convert JString to Rust String for path
    let path_str: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read path: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Call the Rust create_vault_from_mnemonic function
    match create_vault_from_mnemonic(&pin_str, &mnemonic_str, &path_str) {
        Ok(vault_record) => {
            // Serialize VaultRecord to JSON
            match serde_json::to_string(&vault_record) {
                Ok(json) => match env.new_string(json) {
                    Ok(s) => s.into_raw(),
                    Err(_) => JObject::null().into_raw(),
                },
                Err(e) => {
                    let error_msg = format!("{{\"error\":\"Serialization failed: {}\"}}", e);
                    match env.new_string(error_msg) {
                        Ok(s) => s.into_raw(),
                        Err(_) => JObject::null().into_raw(),
                    }
                }
            }
        }
        Err(e) => {
            // Return error as JSON
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for createVaultFromPrivateKey function
/// Called from Kotlin: WalletrustlibKt.createVaultFromPrivateKeyFfi(pin: String, privateKeyHex: String): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_createVaultFromPrivateKeyFfi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
    private_key_hex: JString,
) -> jstring {
    // Convert JString to Rust String for PIN
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Convert JString to Rust String for private key hex
    let private_key_hex_str: String = match env.get_string(&private_key_hex) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read private key: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Call the Rust create_vault_from_private_key function
    match create_vault_from_private_key(&pin_str, &private_key_hex_str) {
        Ok(vault_record) => {
            // Serialize VaultRecord to JSON
            match serde_json::to_string(&vault_record) {
                Ok(json) => match env.new_string(json) {
                    Ok(s) => s.into_raw(),
                    Err(_) => JObject::null().into_raw(),
                },
                Err(e) => {
                    let error_msg = format!("{{\"error\":\"Serialization failed: {}\"}}", e);
                    match env.new_string(error_msg) {
                        Ok(s) => s.into_raw(),
                        Err(_) => JObject::null().into_raw(),
                    }
                }
            }
        }
        Err(e) => {
            // Return error as JSON
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for rotatePin function
/// Called from Kotlin: WalletrustlibKt.rotatePinFfi(oldPin: String, newPin: String, record: String): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_rotatePinFfi(
    mut env: JNIEnv,
    _class: JClass,
    old_pin: JString,
    new_pin: JString,
    vault_json: JString,
) -> jstring {
    // Convert JString to Rust String for old PIN
    let old_pin_str: String = match env.get_string(&old_pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read old PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Convert JString to Rust String for new PIN
    let new_pin_str: String = match env.get_string(&new_pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read new PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Convert JString to Rust String for vault JSON
    let vault_json_str: String = match env.get_string(&vault_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read vault JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Deserialize VaultRecord from JSON
    let vault_record: VaultRecord = match serde_json::from_str(&vault_json_str) {
        Ok(v) => v,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse vault: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Call the Rust rotate_pin function
    match rotate_pin(&old_pin_str, &new_pin_str, &vault_record) {
        Ok(updated_vault) => {
            // Serialize updated VaultRecord to JSON
            match serde_json::to_string(&updated_vault) {
                Ok(json) => match env.new_string(json) {
                    Ok(s) => s.into_raw(),
                    Err(_) => JObject::null().into_raw(),
                },
                Err(e) => {
                    let error_msg = format!("{{\"error\":\"Serialization failed: {}\"}}", e);
                    match env.new_string(error_msg) {
                        Ok(s) => s.into_raw(),
                        Err(_) => JObject::null().into_raw(),
                    }
                }
            }
        }
        Err(e) => {
            // Return error as JSON
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for exportEthPrivateKey function
/// Called from Kotlin: WalletrustlibKt.exportEthPrivateKeyFfi(pin: String, record: String): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_exportEthPrivateKeyFfi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
    vault_json: JString,
) -> jstring {
    // Convert JString to Rust String for PIN
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Convert JString to Rust String for vault JSON
    let vault_json_str: String = match env.get_string(&vault_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read vault JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Deserialize VaultRecord from JSON
    let vault_record: VaultRecord = match serde_json::from_str(&vault_json_str) {
        Ok(v) => v,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse vault: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Call the Rust export_eth_private_key function
    match export_eth_private_key(&pin_str, &vault_record) {
        Ok(private_key) => {
            // Return private key directly as a string (format: 0x...)
            match env.new_string(private_key) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
        Err(e) => {
            // Return error as JSON
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for migrateVault function
/// Called from Kotlin: WalletrustlibKt.migrateVaultFfi(pin: String, record: String): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_migrateVaultFfi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
    vault_json: JString,
) -> jstring {
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_json_str: String = match env.get_string(&vault_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read vault JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_record: VaultRecord = match serde_json::from_str(&vault_json_str) {
        Ok(v) => v,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse vault: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    match migrate_vault(&pin_str, &vault_record) {
        Ok(updated_vault) => match serde_json::to_string(&updated_vault) {
            Ok(json) => match env.new_string(json) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            },
            Err(e) => {
                let error_msg = format!("{{\"error\":\"Serialization failed: {}\"}}", e);
                match env.new_string(error_msg) {
                    Ok(s) => s.into_raw(),
                    Err(_) => JObject::null().into_raw(),
                }
            }
        },
        Err(e) => {
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for getMultichainAddresses function
/// Called from Kotlin: WalletrustlibKt.getMultichainAddressesFfi(pin: String, record: String, testnet: Boolean): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_getMultichainAddressesFfi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
    vault_json: JString,
    testnet: jni::sys::jboolean,
) -> jstring {
    // Convert JString to Rust String for PIN
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Convert JString to Rust String for vault JSON
    let vault_json_str: String = match env.get_string(&vault_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read vault JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Deserialize VaultRecord from JSON
    let vault_record: VaultRecord = match serde_json::from_str(&vault_json_str) {
        Ok(v) => v,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse vault: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Convert jboolean to Rust bool
    let testnet_bool = testnet != 0;

    // Call the Rust get_multichain_addresses function
    match get_multichain_addresses(&pin_str, &vault_record, testnet_bool) {
        Ok(addresses) => {
            // Serialize MultichainAddresses to JSON
            match serde_json::to_string(&addresses) {
                Ok(json) => match env.new_string(json) {
                    Ok(s) => s.into_raw(),
                    Err(_) => JObject::null().into_raw(),
                },
                Err(e) => {
                    let error_msg = format!("{{\"error\":\"Serialization failed: {}\"}}", e);
                    match env.new_string(error_msg) {
                        Ok(s) => s.into_raw(),
                        Err(_) => JObject::null().into_raw(),
                    }
                }
            }
        }
        Err(e) => {
            // Return error as JSON
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for getMultichainAddressesByIndex function
/// Called from Kotlin: WalletrustlibKt.getMultichainAddressesByIndexFfi(pin: String, record: String, testnet: Boolean, index: UInt): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_getMultichainAddressesByIndexFfi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
    vault_json: JString,
    testnet: jni::sys::jboolean,
    index: jni::sys::jint,
) -> jstring {
    // Convert JString to Rust String for PIN
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Convert JString to Rust String for vault JSON
    let vault_json_str: String = match env.get_string(&vault_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read vault JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Deserialize VaultRecord from JSON
    let vault_record: VaultRecord = match serde_json::from_str(&vault_json_str) {
        Ok(v) => v,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse vault: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    // Convert jboolean to Rust bool
    let testnet_bool = testnet != 0;

    // Convert jint to u32 for index
    let index_u32 = index as u32;

    // Call the Rust get_multichain_addresses_by_index function
    match get_multichain_addresses_by_index(&pin_str, &vault_record, testnet_bool, index_u32) {
        Ok(addresses) => {
            // Serialize MultichainAddresses to JSON
            match serde_json::to_string(&addresses) {
                Ok(json) => match env.new_string(json) {
                    Ok(s) => s.into_raw(),
                    Err(_) => JObject::null().into_raw(),
                },
                Err(e) => {
                    let error_msg = format!("{{\"error\":\"Serialization failed: {}\"}}", e);
                    match env.new_string(error_msg) {
                        Ok(s) => s.into_raw(),
                        Err(_) => JObject::null().into_raw(),
                    }
                }
            }
        }
        Err(e) => {
            // Return error as JSON
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for getBtcPublicKey function
/// Called from Kotlin: WalletrustlibKt.getBtcPublicKeyFfi(pin: String, record: String, testnet: Boolean): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_getBtcPublicKeyFfi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
    vault_json: JString,
    testnet: jni::sys::jboolean,
) -> jstring {
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_json_str: String = match env.get_string(&vault_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read vault JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_record: VaultRecord = match serde_json::from_str(&vault_json_str) {
        Ok(v) => v,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse vault: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let testnet_bool = testnet != 0;

    match get_btc_public_key(&pin_str, &vault_record, testnet_bool) {
        Ok(pubkey) => match env.new_string(pubkey) {
            Ok(s) => s.into_raw(),
            Err(_) => JObject::null().into_raw(),
        },
        Err(e) => {
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for signTransaction function
/// Called from Kotlin: WalletrustlibKt.signTransactionFfi(pin: String, record: String, tx: String): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_signTransactionFfi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
    vault_json: JString,
    tx_json: JString,
) -> jstring {
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_json_str: String = match env.get_string(&vault_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read vault JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let tx_json_str: String = match env.get_string(&tx_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read transaction JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_record: VaultRecord = match serde_json::from_str(&vault_json_str) {
        Ok(v) => v,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse vault: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let tx: UnsignedLegacyTx = match serde_json::from_str(&tx_json_str) {
        Ok(t) => t,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse transaction: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    match sign_transaction(&pin_str, &vault_record, &tx) {
        Ok(signed_tx) => match env.new_string(signed_tx) {
            Ok(s) => s.into_raw(),
            Err(_) => JObject::null().into_raw(),
        },
        Err(e) => {
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for signTransactionWithChain function
/// Called from Kotlin: WalletrustlibKt.signTransactionWithChainFfi(pin: String, record: String, tx: String, expectedChainId: Long): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_signTransactionWithChainFfi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
    vault_json: JString,
    tx_json: JString,
    expected_chain_id: jni::sys::jlong,
) -> jstring {
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_json_str: String = match env.get_string(&vault_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read vault JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let tx_json_str: String = match env.get_string(&tx_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read transaction JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_record: VaultRecord = match serde_json::from_str(&vault_json_str) {
        Ok(v) => v,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse vault: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let tx: UnsignedLegacyTx = match serde_json::from_str(&tx_json_str) {
        Ok(t) => t,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse transaction: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let expected_chain_id_u64 = expected_chain_id as u64;
    match sign_transaction_with_chain(&pin_str, &vault_record, &tx, expected_chain_id_u64) {
        Ok(signed_tx) => match env.new_string(signed_tx) {
            Ok(s) => s.into_raw(),
            Err(_) => JObject::null().into_raw(),
        },
        Err(e) => {
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for signTransactionEip1559 function
/// Called from Kotlin: WalletrustlibKt.signTransactionEip1559Ffi(pin: String, record: String, tx: String): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_signTransactionEip1559Ffi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
    vault_json: JString,
    tx_json: JString,
) -> jstring {
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_json_str: String = match env.get_string(&vault_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read vault JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let tx_json_str: String = match env.get_string(&tx_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read transaction JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_record: VaultRecord = match serde_json::from_str(&vault_json_str) {
        Ok(v) => v,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse vault: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let tx: UnsignedEip1559Tx = match serde_json::from_str(&tx_json_str) {
        Ok(t) => t,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse transaction: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    match sign_transaction_eip1559(&pin_str, &vault_record, &tx) {
        Ok(signed_tx) => match env.new_string(signed_tx) {
            Ok(s) => s.into_raw(),
            Err(_) => JObject::null().into_raw(),
        },
        Err(e) => {
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for signTransactionEip1559WithChain function
/// Called from Kotlin: WalletrustlibKt.signTransactionEip1559WithChainFfi(pin: String, record: String, tx: String, expectedChainId: Long): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_signTransactionEip1559WithChainFfi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
    vault_json: JString,
    tx_json: JString,
    expected_chain_id: jni::sys::jlong,
) -> jstring {
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_json_str: String = match env.get_string(&vault_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read vault JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let tx_json_str: String = match env.get_string(&tx_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read transaction JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_record: VaultRecord = match serde_json::from_str(&vault_json_str) {
        Ok(v) => v,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse vault: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let tx: UnsignedEip1559Tx = match serde_json::from_str(&tx_json_str) {
        Ok(t) => t,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse transaction: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let expected_chain_id_u64 = expected_chain_id as u64;
    match sign_transaction_eip1559_with_chain(&pin_str, &vault_record, &tx, expected_chain_id_u64) {
        Ok(signed_tx) => match env.new_string(signed_tx) {
            Ok(s) => s.into_raw(),
            Err(_) => JObject::null().into_raw(),
        },
        Err(e) => {
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for signSolanaTransaction function
/// Called from Kotlin: WalletrustlibKt.signSolanaTransactionFfi(pin: String, record: String, message: ByteArray): ByteArray
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_signSolanaTransactionFfi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
    vault_json: JString,
    message: jbyteArray,
) -> jbyteArray {
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to read PIN: {}", e));
            return std::ptr::null_mut();
        }
    };

    let vault_json_str: String = match env.get_string(&vault_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to read vault JSON: {}", e));
            return std::ptr::null_mut();
        }
    };

    let message_array = unsafe { JByteArray::from_raw(message) };
    let message_bytes: Vec<u8> = match env.convert_byte_array(&message_array) {
        Ok(b) => b,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to read message: {}", e));
            return std::ptr::null_mut();
        }
    };

    let vault_record: VaultRecord = match serde_json::from_str(&vault_json_str) {
        Ok(v) => v,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to parse vault: {}", e));
            return std::ptr::null_mut();
        }
    };

    match sign_solana_transaction(&pin_str, &vault_record, &message_bytes) {
        Ok(signature) => match env.byte_array_from_slice(&signature) {
            Ok(arr) => arr.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", e);
            std::ptr::null_mut()
        }
    }
}

/// JNI wrapper for signBitcoinTransaction function
/// Called from Kotlin: WalletrustlibKt.signBitcoinTransactionFfi(pin: String, record: String, sighashHex: String, testnet: Boolean): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_signBitcoinTransactionFfi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
    vault_json: JString,
    sighash_hex: JString,
    testnet: jni::sys::jboolean,
) -> jstring {
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_json_str: String = match env.get_string(&vault_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read vault JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let sighash_hex_str: String = match env.get_string(&sighash_hex) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read sighash: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_record: VaultRecord = match serde_json::from_str(&vault_json_str) {
        Ok(v) => v,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse vault: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let testnet_bool = testnet != 0;
    match sign_bitcoin_transaction(&pin_str, &vault_record, &sighash_hex_str, testnet_bool) {
        Ok(signature) => match env.new_string(signature) {
            Ok(s) => s.into_raw(),
            Err(_) => JObject::null().into_raw(),
        },
        Err(e) => {
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for deriveBtcAddress function
/// Called from Kotlin: WalletrustlibKt.deriveBtcAddressFfi(mnemonic: String, testnet: Boolean): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_deriveBtcAddressFfi(
    mut env: JNIEnv,
    _class: JClass,
    mnemonic: JString,
    testnet: jni::sys::jboolean,
) -> jstring {
    let mnemonic_str: String = match env.get_string(&mnemonic) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read mnemonic: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let testnet_bool = testnet != 0;
    match derive_btc_address(&mnemonic_str, testnet_bool) {
        Ok(address) => match env.new_string(address) {
            Ok(s) => s.into_raw(),
            Err(_) => JObject::null().into_raw(),
        },
        Err(e) => {
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for deriveSolAddress function
/// Called from Kotlin: WalletrustlibKt.deriveSolAddressFfi(mnemonic: String): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_deriveSolAddressFfi(
    mut env: JNIEnv,
    _class: JClass,
    mnemonic: JString,
) -> jstring {
    let mnemonic_str: String = match env.get_string(&mnemonic) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read mnemonic: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    match derive_sol_address(&mnemonic_str) {
        Ok(address) => match env.new_string(address) {
            Ok(s) => s.into_raw(),
            Err(_) => JObject::null().into_raw(),
        },
        Err(e) => {
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for createRecoveryBackup function
/// Called from Kotlin: WalletrustlibKt.createRecoveryBackupFfi(pin: String, record: String, backupPassphrase: String): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_createRecoveryBackupFfi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
    vault_json: JString,
    backup_passphrase: JString,
) -> jstring {
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_json_str: String = match env.get_string(&vault_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read vault JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let backup_passphrase_str: String = match env.get_string(&backup_passphrase) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read passphrase: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_record: VaultRecord = match serde_json::from_str(&vault_json_str) {
        Ok(v) => v,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse vault: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    match create_recovery_backup(&pin_str, &vault_record, &backup_passphrase_str) {
        Ok(backup) => match serde_json::to_string(&backup) {
            Ok(json) => match env.new_string(json) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            },
            Err(e) => {
                let error_msg = format!("{{\"error\":\"Serialization failed: {}\"}}", e);
                match env.new_string(error_msg) {
                    Ok(s) => s.into_raw(),
                    Err(_) => JObject::null().into_raw(),
                }
            }
        },
        Err(e) => {
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for restoreVaultFromRecoveryBackup function
/// Called from Kotlin: WalletrustlibKt.restoreVaultFromRecoveryBackupFfi(backupPassphrase: String, backup: String, newPin: String): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_restoreVaultFromRecoveryBackupFfi(
    mut env: JNIEnv,
    _class: JClass,
    backup_passphrase: JString,
    backup_json: JString,
    new_pin: JString,
) -> jstring {
    let backup_passphrase_str: String = match env.get_string(&backup_passphrase) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read passphrase: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let backup_json_str: String = match env.get_string(&backup_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read backup JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let new_pin_str: String = match env.get_string(&new_pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read new PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let backup: RecoveryBackup = match serde_json::from_str(&backup_json_str) {
        Ok(b) => b,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse backup: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    match restore_vault_from_recovery_backup(&backup_passphrase_str, &backup, &new_pin_str) {
        Ok(vault_record) => match serde_json::to_string(&vault_record) {
            Ok(json) => match env.new_string(json) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            },
            Err(e) => {
                let error_msg = format!("{{\"error\":\"Serialization failed: {}\"}}", e);
                match env.new_string(error_msg) {
                    Ok(s) => s.into_raw(),
                    Err(_) => JObject::null().into_raw(),
                }
            }
        },
        Err(e) => {
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for verifyRecoveryBackup function
/// Called from Kotlin: WalletrustlibKt.verifyRecoveryBackupFfi(backupPassphrase: String, backup: String)
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_verifyRecoveryBackupFfi(
    mut env: JNIEnv,
    _class: JClass,
    backup_passphrase: JString,
    backup_json: JString,
) {
    let backup_passphrase_str: String = match env.get_string(&backup_passphrase) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to read passphrase: {}", e));
            return;
        }
    };

    let backup_json_str: String = match env.get_string(&backup_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to read backup JSON: {}", e));
            return;
        }
    };

    let backup: RecoveryBackup = match serde_json::from_str(&backup_json_str) {
        Ok(b) => b,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to parse backup: {}", e));
            return;
        }
    };

    if let Err(e) = verify_recovery_backup(&backup_passphrase_str, &backup) {
        let _ = env.throw_new("java/lang/RuntimeException", e);
    }
}

/// JNI wrapper for createCloudRecoveryBlob function
/// Called from Kotlin: WalletrustlibKt.createCloudRecoveryBlobFfi(pin: String, record: String, oauthKek: String): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_createCloudRecoveryBlobFfi(
    mut env: JNIEnv,
    _class: JClass,
    pin: JString,
    vault_json: JString,
    oauth_kek: JString,
) -> jstring {
    let pin_str: String = match env.get_string(&pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_json_str: String = match env.get_string(&vault_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read vault JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let oauth_kek_str: String = match env.get_string(&oauth_kek) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read OAuth KEK: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let vault_record: VaultRecord = match serde_json::from_str(&vault_json_str) {
        Ok(v) => v,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse vault: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    match create_cloud_recovery_blob(&pin_str, &vault_record, &oauth_kek_str) {
        Ok(blob) => match serde_json::to_string(&blob) {
            Ok(json) => match env.new_string(json) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            },
            Err(e) => {
                let error_msg = format!("{{\"error\":\"Serialization failed: {}\"}}", e);
                match env.new_string(error_msg) {
                    Ok(s) => s.into_raw(),
                    Err(_) => JObject::null().into_raw(),
                }
            }
        },
        Err(e) => {
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for restoreVaultFromCloudRecoveryBlob function
/// Called from Kotlin: WalletrustlibKt.restoreVaultFromCloudRecoveryBlobFfi(oauthKek: String, blob: String, newPin: String): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_restoreVaultFromCloudRecoveryBlobFfi(
    mut env: JNIEnv,
    _class: JClass,
    oauth_kek: JString,
    blob_json: JString,
    new_pin: JString,
) -> jstring {
    let oauth_kek_str: String = match env.get_string(&oauth_kek) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read OAuth KEK: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let blob_json_str: String = match env.get_string(&blob_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read blob JSON: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let new_pin_str: String = match env.get_string(&new_pin) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read new PIN: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let blob: CloudRecoveryBlob = match serde_json::from_str(&blob_json_str) {
        Ok(b) => b,
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to parse blob: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    match restore_vault_from_cloud_recovery_blob(&oauth_kek_str, &blob, &new_pin_str) {
        Ok(vault_record) => match serde_json::to_string(&vault_record) {
            Ok(json) => match env.new_string(json) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            },
            Err(e) => {
                let error_msg = format!("{{\"error\":\"Serialization failed: {}\"}}", e);
                match env.new_string(error_msg) {
                    Ok(s) => s.into_raw(),
                    Err(_) => JObject::null().into_raw(),
                }
            }
        },
        Err(e) => {
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for verifyCloudRecoveryBlob function
/// Called from Kotlin: WalletrustlibKt.verifyCloudRecoveryBlobFfi(oauthKek: String, blob: String)
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_verifyCloudRecoveryBlobFfi(
    mut env: JNIEnv,
    _class: JClass,
    oauth_kek: JString,
    blob_json: JString,
) {
    let oauth_kek_str: String = match env.get_string(&oauth_kek) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to read OAuth KEK: {}", e));
            return;
        }
    };

    let blob_json_str: String = match env.get_string(&blob_json) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to read blob JSON: {}", e));
            return;
        }
    };

    let blob: CloudRecoveryBlob = match serde_json::from_str(&blob_json_str) {
        Ok(b) => b,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to parse blob: {}", e));
            return;
        }
    };

    if let Err(e) = verify_cloud_recovery_blob(&oauth_kek_str, &blob) {
        let _ = env.throw_new("java/lang/RuntimeException", e);
    }
}

/// JNI wrapper for createWalletFromWeb3authKey function
/// Called from Kotlin: WalletrustlibKt.createWalletFromWeb3authKeyFfi(web3authPrivateKey: String, testnet: Boolean): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_createWalletFromWeb3authKeyFfi(
    mut env: JNIEnv,
    _class: JClass,
    web3auth_private_key: JString,
    testnet: jni::sys::jboolean,
) -> jstring {
    let web3auth_private_key_str: String = match env.get_string(&web3auth_private_key) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read Web3Auth key: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let testnet_bool = testnet != 0;
    match create_wallet_from_web3auth_key(&web3auth_private_key_str, testnet_bool) {
        Ok(result) => match serde_json::to_string(&result) {
            Ok(json) => match env.new_string(json) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            },
            Err(e) => {
                let error_msg = format!("{{\"error\":\"Serialization failed: {}\"}}", e);
                match env.new_string(error_msg) {
                    Ok(s) => s.into_raw(),
                    Err(_) => JObject::null().into_raw(),
                }
            }
        },
        Err(e) => {
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}

/// JNI wrapper for restoreWalletFromWeb3authKey function
/// Called from Kotlin: WalletrustlibKt.restoreWalletFromWeb3authKeyFfi(web3authPrivateKey: String, encryptedData: String, testnet: Boolean): String
#[no_mangle]
pub extern "system" fn Java_com_wallet_1rust_WalletrustlibKt_restoreWalletFromWeb3authKeyFfi(
    mut env: JNIEnv,
    _class: JClass,
    web3auth_private_key: JString,
    encrypted_data: JString,
    testnet: jni::sys::jboolean,
) -> jstring {
    let web3auth_private_key_str: String = match env.get_string(&web3auth_private_key) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read Web3Auth key: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let encrypted_data_str: String = match env.get_string(&encrypted_data) {
        Ok(s) => s.into(),
        Err(e) => {
            let error_msg = format!("{{\"error\":\"Failed to read encrypted data: {}\"}}", e);
            return match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            };
        }
    };

    let testnet_bool = testnet != 0;
    match restore_wallet_from_web3auth_key(&web3auth_private_key_str, &encrypted_data_str, testnet_bool) {
        Ok(result) => match serde_json::to_string(&result) {
            Ok(json) => match env.new_string(json) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            },
            Err(e) => {
                let error_msg = format!("{{\"error\":\"Serialization failed: {}\"}}", e);
                match env.new_string(error_msg) {
                    Ok(s) => s.into_raw(),
                    Err(_) => JObject::null().into_raw(),
                }
            }
        },
        Err(e) => {
            let error_msg = format!("{{\"error\":\"{}\"}}", e);
            match env.new_string(error_msg) {
                Ok(s) => s.into_raw(),
                Err(_) => JObject::null().into_raw(),
            }
        }
    }
}
