use anyhow::{anyhow, Context, Result};
use jsonwebtoken::{decode, decode_header, Algorithm, DecodingKey, Validation};
use moka::future::Cache;
use once_cell::sync::Lazy;
use serde::Deserialize;
use std::time::Duration;

static JWKS_CACHE: Lazy<Cache<String, Jwks>> = Lazy::new(|| {
    Cache::builder()
        .time_to_live(Duration::from_secs(10 * 60))
        .max_capacity(32)
        .build()
});

#[derive(Debug, Clone, Deserialize)]
pub struct Jwks {
    pub keys: Vec<Jwk>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct Jwk {
    pub kty: String,
    #[serde(default)]
    pub kid: String,
    #[serde(default)]
    pub n: String,
    #[serde(default)]
    pub e: String,
}

pub async fn fetch_jwks(st: &crate::app::AppState, url: &str) -> Result<Jwks> {
    if let Some(v) = JWKS_CACHE.get(url).await {
        return Ok(v);
    }
    let parsed_url = url::Url::parse(url).map_err(|e| anyhow!("invalid jwks url: {e}"))?;
    let v = crate::infra::rpc::fetch_with_host_policy(
        st,
        reqwest::Method::GET,
        &parsed_url,
        None,
        None,
        Duration::from_secs(10),
        "jwks",
    )
    .await
    .map_err(|e| anyhow!(e))?;

    let jwks: Jwks = serde_json::from_value(v).context("parse jwks")?;
    JWKS_CACHE.insert(url.to_string(), jwks.clone()).await;
    Ok(jwks)
}

pub fn decode_rs256<T: for<'de> Deserialize<'de>>(
    jwt: &str,
    jwks: &Jwks,
    audience: &str,
    issuers: &[&str],
) -> Result<T> {
    let header = decode_header(jwt).context("decode jwt header")?;
    let kid = header.kid.unwrap_or_default();
    if kid.is_empty() {
        return Err(anyhow!("missing kid"));
    }

    let jwk = jwks
        .keys
        .iter()
        .find(|k| k.kid == kid && k.kty == "RSA" && !k.n.is_empty() && !k.e.is_empty())
        .ok_or_else(|| anyhow!("jwks key not found for kid"))?;

    // jsonwebtoken accepts JWK `n`/`e` as base64url.
    let decoding_key = DecodingKey::from_rsa_components(&jwk.n, &jwk.e)?;

    let mut validation = Validation::new(Algorithm::RS256);
    validation.set_audience(&[audience]);
    validation.set_issuer(issuers);

    let data = decode::<T>(jwt, &decoding_key, &validation).context("jwt verify")?;
    Ok(data.claims)
}
