use bip32::DerivationPath;
use bip39::{Language, Mnemonic};
use k256::ecdsa::SigningKey;
use rand::rngs::OsRng;
use rand::RngCore;
use sha3::{Digest, Keccak256};
use zeroize::Zeroizing;

pub struct KeyMaterial {
    pub secret_key: Zeroizing<[u8; 32]>,
    pub address: String,
}

pub fn public_key_to_address(uncompressed: &[u8]) -> String {
    let hash = Keccak256::digest(&uncompressed[1..]);
    let address = &hash[hash.len() - 20..];
    format!("0x{}", hex::encode(address))
}

pub fn public_key_to_btc_address(compressed_pubkey: &[u8], testnet: bool) -> String {
    use sha2::{Sha256, Digest as Sha2Digest};
    use ripemd::Ripemd160;
    use bech32::Variant;

    // 1. Hash160: RIPEMD160(SHA256(pubkey))
    let sha_hash = Sha256::digest(compressed_pubkey);
    let ripe_hash = Ripemd160::digest(&sha_hash);
    
    // 2. Bech32 Encode (BIP173)
    let hrp = if testnet { "tb" } else { "bc" };
    
    use bech32::u5;
    let mut data: Vec<u5> = vec![u5::try_from_u8(0).expect("0 is valid u5")];
    let hash_u5_bytes = bech32::convert_bits(ripe_hash.as_ref(), 8, 5, true).expect("Bit conversion failed");
    let hash_u5: Vec<u5> = hash_u5_bytes.into_iter()
        .map(|b| u5::try_from_u8(b).expect("Invalid u5"))
        .collect();
    data.extend_from_slice(&hash_u5);
    
    bech32::encode(hrp, data, Variant::Bech32).expect("Bech32 encoding failed")
}

pub fn public_key_to_sol_address(public_key: &[u8]) -> String {
    bs58::encode(public_key).into_string()
}

pub fn generate_mnemonic() -> Result<String, String> {
    let mut entropy = [0u8; 16];
    OsRng.fill_bytes(&mut entropy);
    let mnemonic = Mnemonic::from_entropy_in(Language::English, &entropy)
        .map_err(|_| "Failed to generate mnemonic".to_string())?;
    Ok(mnemonic.to_string())
}

pub fn derive_secp256k1_from_mnemonic(mnemonic: &str, path: &str) -> Result<KeyMaterial, String> {
    let mnemonic = Mnemonic::parse(mnemonic).map_err(|_| "Invalid mnemonic".to_string())?;
    let seed = mnemonic.to_seed("");
    let derivation_path: DerivationPath = path.parse().map_err(|_| "Invalid derivation path".to_string())?;
    let xprv = bip32::XPrv::derive_from_path(seed, &derivation_path)
        .map_err(|_| "Failed to derive key".to_string())?;
    let signing_key = SigningKey::from_bytes(&xprv.private_key().to_bytes())
        .map_err(|_| "Invalid derived key".to_string())?;
    let public = signing_key.verifying_key().to_encoded_point(false);
    let address = public_key_to_address(public.as_bytes());
    let secret_bytes = Zeroizing::new(signing_key.to_bytes().into());
    Ok(KeyMaterial {
        secret_key: secret_bytes,
        address,
    })
}

pub fn key_from_private_key_hex(hex_key: &str) -> Result<KeyMaterial, String> {
    let clean = hex_key.strip_prefix("0x").unwrap_or(hex_key);
    let bytes = hex::decode(clean).map_err(|_| "Invalid private key".to_string())?;
    if bytes.len() != 32 {
        return Err("Invalid private key length".to_string());
    }
    let signing_key = SigningKey::from_bytes(bytes.as_slice().into())
        .map_err(|_| "Invalid private key".to_string())?;
    let public = signing_key.verifying_key().to_encoded_point(false);
    let address = public_key_to_address(public.as_bytes());
    let secret_bytes = Zeroizing::new(signing_key.to_bytes().into());
    Ok(KeyMaterial {
        secret_key: secret_bytes,
        address,
    })
}

pub fn derive_btc_from_mnemonic(mnemonic: &str, testnet: bool) -> Result<KeyMaterial, String> {
    let mnemonic = Mnemonic::parse(mnemonic).map_err(|_| "Invalid mnemonic".to_string())?;
    let seed = mnemonic.to_seed("");
    // Native SegWit BIP84: m/84'/0'/0'/0/0
    let coin_type = if testnet { "1" } else { "0" };
    let path_str = format!("m/84'/{}'/0'/0/0", coin_type);
    let derivation_path: DerivationPath = path_str.parse().map_err(|_| "Invalid path".to_string())?;
    
    let xprv = bip32::XPrv::derive_from_path(seed, &derivation_path)
        .map_err(|_| "Failed to derive BTC key".to_string())?;
    
    let signing_key = SigningKey::from_bytes(&xprv.private_key().to_bytes())
        .map_err(|_| "Invalid BTC signing key".to_string())?;
    
    // For BTC, we use compressed public key
    let public = signing_key.verifying_key().to_encoded_point(true); 
    let address = public_key_to_btc_address(public.as_bytes(), testnet);
    let secret_bytes = Zeroizing::new(signing_key.to_bytes().into());
    
    Ok(KeyMaterial {
        secret_key: secret_bytes,
        address,
    })
}

pub fn derive_btc_from_mnemonic_with_index(mnemonic: &str, testnet: bool, index: u32) -> Result<KeyMaterial, String> {
    let mnemonic = Mnemonic::parse(mnemonic).map_err(|_| "Invalid mnemonic".to_string())?;
    let seed = mnemonic.to_seed("");
    // Native SegWit BIP84: m/84'/0'/0'/0/index
    let coin_type = if testnet { "1" } else { "0" };
    let path_str = format!("m/84'/{}'/0'/0/{}", coin_type, index);
    let derivation_path: DerivationPath = path_str.parse().map_err(|_| "Invalid path".to_string())?;

    let xprv = bip32::XPrv::derive_from_path(seed, &derivation_path)
        .map_err(|_| "Failed to derive BTC key".to_string())?;

    let signing_key = SigningKey::from_bytes(&xprv.private_key().to_bytes())
        .map_err(|_| "Invalid BTC signing key".to_string())?;

    let public = signing_key.verifying_key().to_encoded_point(true);
    let address = public_key_to_btc_address(public.as_bytes(), testnet);
    let secret_bytes = Zeroizing::new(signing_key.to_bytes().into());

    Ok(KeyMaterial {
        secret_key: secret_bytes,
        address,
    })
}

pub fn derive_sol_from_mnemonic(mnemonic: &str) -> Result<KeyMaterial, String> {
    let mnemonic = Mnemonic::parse(mnemonic).map_err(|_| "Invalid mnemonic".to_string())?;
    let seed = mnemonic.to_seed("");
    
    // Solana derivation: m/44'/501'/0'/0'
    use slip10::{derive_key_from_path, Curve};
    
    let path_str = "m/44'/501'/0'/0'";
    
    use slip10::BIP32Path;
    use std::str::FromStr;
    let path = BIP32Path::from_str(path_str).map_err(|_| "Invalid BIP32 path".to_string())?;

    let derived = derive_key_from_path(&seed, Curve::Ed25519, &path)
        .map_err(|_| "Solana derivation failed".to_string())?;
    
    let secret_key = ed25519_dalek::SigningKey::from_bytes(
        derived.key.as_slice().try_into().map_err(|_| "Invalid key length")?
    );
    let public_key = secret_key.verifying_key();
    let address = public_key_to_sol_address(public_key.as_bytes());
    
    let secret_bytes = secret_key.to_bytes();
    
    Ok(KeyMaterial {
        secret_key: Zeroizing::new(secret_bytes),
        address,
    })
}

pub fn derive_sol_from_mnemonic_with_index(mnemonic: &str, index: u32) -> Result<KeyMaterial, String> {
    let mnemonic = Mnemonic::parse(mnemonic).map_err(|_| "Invalid mnemonic".to_string())?;
    let seed = mnemonic.to_seed("");

    // Solana derivation with account index: m/44'/501'/index'/0'
    use slip10::{derive_key_from_path, Curve};
    use slip10::BIP32Path;
    use std::str::FromStr;

    let path_str = format!("m/44'/501'/{}'/0'", index);
    let path = BIP32Path::from_str(&path_str).map_err(|_| "Invalid BIP32 path".to_string())?;

    let derived = derive_key_from_path(&seed, Curve::Ed25519, &path)
        .map_err(|_| "Solana derivation failed".to_string())?;

    let secret_key = ed25519_dalek::SigningKey::from_bytes(
        derived.key.as_slice().try_into().map_err(|_| "Invalid key length")?
    );
    let public_key = secret_key.verifying_key();
    let address = public_key_to_sol_address(public_key.as_bytes());

    let secret_bytes = secret_key.to_bytes();

    Ok(KeyMaterial {
        secret_key: Zeroizing::new(secret_bytes),
        address,
    })
}

/// Web3Auth Integration: Convert Web3Auth private key to deterministic seed
/// This ensures the same Web3Auth key always produces the same wallet addresses
pub fn web3auth_key_to_seed(web3auth_private_key: &str) -> Result<Vec<u8>, String> {
    use sha2::{Sha256, Digest};
    
    // Remove 0x prefix if present
    let clean_key = web3auth_private_key.strip_prefix("0x").unwrap_or(web3auth_private_key);
    
    // Decode hex to bytes
    let key_bytes = hex::decode(clean_key)
        .map_err(|_| "Invalid Web3Auth private key format".to_string())?;
    
    if key_bytes.len() != 32 {
        return Err("Web3Auth private key must be 32 bytes".to_string());
    }
    
    // Use the Web3Auth key as entropy to generate a deterministic seed
    // Hash it to create 64-byte seed (BIP39 compatible)
    let mut hasher = Sha256::new();
    hasher.update(&key_bytes);
    hasher.update(b"elementa-wallet-v1"); // Salt for domain separation
    let first_hash = hasher.finalize();
    
    let mut hasher2 = Sha256::new();
    hasher2.update(&first_hash);
    hasher2.update(b"elementa-wallet-v1-second");
    let second_hash = hasher2.finalize();
    
    // Combine to create 64-byte seed
    let mut seed = Vec::with_capacity(64);
    seed.extend_from_slice(&first_hash);
    seed.extend_from_slice(&second_hash);
    
    Ok(seed)
}

/// Derive all multi-chain wallets from Web3Auth key
pub struct MultiChainWallets {
    pub ethereum: KeyMaterial,
    pub solana: KeyMaterial,
    pub bitcoin: KeyMaterial,
}

pub fn derive_multichain_from_web3auth(
    web3auth_private_key: &str,
    testnet: bool
) -> Result<MultiChainWallets, String> {
    // Get deterministic seed from Web3Auth key
    let seed = web3auth_key_to_seed(web3auth_private_key)?;
    
    // Derive Ethereum key (m/44'/60'/0'/0/0)
    let eth_path = "m/44'/60'/0'/0/0";
    let eth_derivation_path: DerivationPath = eth_path.parse()
        .map_err(|_| "Invalid ETH derivation path".to_string())?;
    
    let eth_xprv = bip32::XPrv::derive_from_path(&seed, &eth_derivation_path)
        .map_err(|_| "Failed to derive ETH key".to_string())?;
    
    let eth_signing_key = SigningKey::from_bytes(&eth_xprv.private_key().to_bytes())
        .map_err(|_| "Invalid ETH signing key".to_string())?;
    
    let eth_public = eth_signing_key.verifying_key().to_encoded_point(false);
    let eth_address = public_key_to_address(eth_public.as_bytes());
    let eth_secret = Zeroizing::new(eth_signing_key.to_bytes().into());
    
    let ethereum = KeyMaterial {
        secret_key: eth_secret,
        address: eth_address,
    };
    
    // Derive Bitcoin key (m/84'/0'/0'/0/0 or m/84'/1'/0'/0/0 for testnet)
    let coin_type = if testnet { "1" } else { "0" };
    let btc_path = format!("m/84'/{}'/0'/0/0", coin_type);
    let btc_derivation_path: DerivationPath = btc_path.parse()
        .map_err(|_| "Invalid BTC derivation path".to_string())?;
    
    let btc_xprv = bip32::XPrv::derive_from_path(&seed, &btc_derivation_path)
        .map_err(|_| "Failed to derive BTC key".to_string())?;
    
    let btc_signing_key = SigningKey::from_bytes(&btc_xprv.private_key().to_bytes())
        .map_err(|_| "Invalid BTC signing key".to_string())?;
    
    let btc_public = btc_signing_key.verifying_key().to_encoded_point(true);
    let btc_address = public_key_to_btc_address(btc_public.as_bytes(), testnet);
    let btc_secret = Zeroizing::new(btc_signing_key.to_bytes().into());
    
    let bitcoin = KeyMaterial {
        secret_key: btc_secret,
        address: btc_address,
    };
    
    // Derive Solana key (m/44'/501'/0'/0')
    use slip10::{derive_key_from_path, Curve, BIP32Path};
    use std::str::FromStr;
    
    let sol_path_str = "m/44'/501'/0'/0'";
    let sol_path = BIP32Path::from_str(sol_path_str)
        .map_err(|_| "Invalid SOL BIP32 path".to_string())?;
    
    let sol_derived = derive_key_from_path(&seed, Curve::Ed25519, &sol_path)
        .map_err(|_| "Solana derivation failed".to_string())?;
    
    let sol_secret_key = ed25519_dalek::SigningKey::from_bytes(
        sol_derived.key.as_slice().try_into()
            .map_err(|_| "Invalid SOL key length")?
    );
    
    let sol_public_key = sol_secret_key.verifying_key();
    let sol_address = public_key_to_sol_address(sol_public_key.as_bytes());
    let sol_secret = Zeroizing::new(sol_secret_key.to_bytes());
    
    let solana = KeyMaterial {
        secret_key: sol_secret,
        address: sol_address,
    };
    
    Ok(MultiChainWallets {
        ethereum,
        solana,
        bitcoin,
    })
}
