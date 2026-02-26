use hmac::{Hmac, Mac};
use sha2::{Digest, Sha256};

type HmacSha256 = Hmac<Sha256>;

pub fn derive_cloud_kek_key(secret: &str) -> [u8; 32] {
    let mut h = Sha256::new();
    h.update(secret.as_bytes());
    let out = h.finalize();
    let mut key = [0u8; 32];
    key.copy_from_slice(&out[..]);
    key
}

pub fn derive_cloud_kek_hex(key: &[u8; 32], provider: &str, sub: &str) -> String {
    let mut mac = HmacSha256::new_from_slice(key).expect("HMAC key length");
    mac.update(format!("{}:{}", provider, sub).as_bytes());
    let out = mac.finalize().into_bytes();
    format!("0x{}", hex::encode(out))
}
