use std::{collections::HashSet, sync::Arc, time::Duration};

use crate::app::{
    chrono_ms_now, AppState, CacheEntry, CacheOutcome, RPC_COOLDOWN_BASE_MS, RPC_COOLDOWN_MAX_MS,
};

fn parse_allowlist(raw: &str) -> Vec<String> {
    raw.split(',')
        .map(|s| s.trim().to_lowercase())
        .filter(|s| !s.is_empty())
        .collect()
}

pub(crate) fn validate_rpc_url(st: &AppState, raw_url: &str) -> Result<url::Url, String> {
    let url = url::Url::parse(raw_url).map_err(|_| "Invalid URL".to_string())?;
    let raw_lower = raw_url.to_lowercase();
    if raw_lower.contains("goerli") {
        return Err("Goerli RPC endpoints are deprecated; use Sepolia.".to_string());
    }
    if url.scheme() != "https" {
        return Err("Only https URLs are allowed".to_string());
    }
    if !url.username().is_empty() || url.password().is_some() {
        return Err("URL auth credentials are not allowed".to_string());
    }
    let host = normalize_host(url.host_str().unwrap_or(""))?;
    if host.is_empty() {
        return Err("Invalid URL".to_string());
    }
    if host == "localhost" || crate::infra::proxy::is_private_ip_host(&host) {
        return Err("Private or localhost targets are not allowed".to_string());
    }

    let explicit_allow = parse_allowlist(&st.cfg.rpc_url_allowlist);
    let default_exact_hosts = HashSet::from([
        "rpc.ankr.com".to_string(),
        "ethereum.publicnode.com".to_string(),
        "eth-mainnet.g.alchemy.com".to_string(),
        "eth-sepolia.g.alchemy.com".to_string(),
    ]);
    let default_domains = [
        "infura.io",
        "alchemy.com",
        "ankr.com",
        "getblock.io",
        "quiknode.pro",
        "quicknode.com",
        "chainstack.com",
        "blastapi.io",
        "pokt.network",
        "publicnode.com",
        "binance.org",
        "rpc.org",
        "polygon.technology",
        "avax.network",
        "avax-test.network",
        "drpc.org",
        "llamarpc.com",
        "1rpc.io",
        "sepolia.org",
        "cloudflare-eth.com",
        "polygon-rpc.com",
        "solana.com",
    ];

    let explicit_ok = !explicit_allow.is_empty()
        && explicit_allow
            .iter()
            .any(|v| host_matches_allow_entry(&host, v));
    let default_ok = default_exact_hosts.contains(&host)
        || default_domains
            .iter()
            .any(|d| host_matches_domain_or_subdomain(&host, d));
    if !explicit_ok && !default_ok {
        return Err("RPC host is not allowlisted".to_string());
    }
    Ok(url)
}

fn normalize_host(raw_host: &str) -> Result<String, String> {
    let host = raw_host.trim().trim_end_matches('.').to_lowercase();
    if host.is_empty() {
        return Err("Invalid URL".to_string());
    }
    if !host.is_ascii() {
        return Err("Non-ASCII host is not allowed".to_string());
    }
    if host.contains("..") {
        return Err("Invalid URL host".to_string());
    }
    Ok(host)
}

pub(crate) fn host_matches_domain_or_subdomain(host: &str, domain: &str) -> bool {
    host == domain
        || (host.len() > domain.len()
            && host.ends_with(domain)
            && host.as_bytes()[host.len() - domain.len() - 1] == b'.')
}

fn host_matches_allow_entry(host: &str, entry: &str) -> bool {
    let entry = entry.trim().trim_end_matches('.').to_lowercase();
    if entry.is_empty() {
        return false;
    }
    host_matches_domain_or_subdomain(host, &entry)
}

pub(crate) fn validate_rpc_payload(payload: &serde_json::Value) -> Result<(), String> {
    let Some(obj) = payload.as_object() else {
        return Err("Invalid JSON-RPC payload".to_string());
    };
    for key in obj.keys() {
        if !["jsonrpc", "id", "method", "params"].contains(&key.as_str()) {
            return Err(format!("Unknown JSON-RPC field: {}", key));
        }
    }
    let method = obj
        .get("method")
        .and_then(|v| v.as_str())
        .map(|s| s.trim())
        .unwrap_or("");
    if method.is_empty() {
        return Err("Missing JSON-RPC method".to_string());
    }
    if !crate::policy::is_allowed_rpc_method(method) {
        return Err(format!("RPC method not allowed: {}", method));
    }
    if let Some(v) = obj.get("params") {
        if !(v.is_array() || v.is_object() || v.is_null()) {
            return Err("Invalid JSON-RPC params".to_string());
        }
    }
    if let Some(v) = obj.get("jsonrpc") {
        if let Some(ver) = v.as_str() {
            if ver != "2.0" {
                return Err("Unsupported JSON-RPC version".to_string());
            }
        } else {
            return Err("Invalid JSON-RPC version".to_string());
        }
    }
    if let Some(v) = obj.get("id") {
        if !(v.is_null() || v.is_string() || v.is_number()) {
            return Err("Invalid JSON-RPC id".to_string());
        }
    }
    Ok(())
}

fn json_value_as_text(v: &serde_json::Value) -> Option<String> {
    if let Some(s) = v.as_str() {
        return Some(s.to_string());
    }
    if let Some(n) = v.as_u64() {
        return Some(n.to_string());
    }
    if let Some(n) = v.as_i64() {
        return Some(n.to_string());
    }
    None
}

pub(crate) fn extract_chain_id_hint(payload: &serde_json::Value) -> String {
    if let Some(params) = payload.get("params") {
        if let Some(obj) = params.as_object() {
            if let Some(v) = obj.get("chainId").and_then(json_value_as_text) {
                return v;
            }
        }
        if let Some(arr) = params.as_array() {
            if let Some(first) = arr.first() {
                if let Some(v) = first
                    .as_object()
                    .and_then(|m| m.get("chainId"))
                    .and_then(json_value_as_text)
                {
                    return v;
                }
            }
        }
    }
    "unknown".to_string()
}

pub(crate) fn rpc_error_envelope(
    payload: &serde_json::Value,
    code: i64,
    message: &str,
) -> serde_json::Value {
    serde_json::json!({
        "jsonrpc": payload.get("jsonrpc").and_then(|v| v.as_str()).unwrap_or("2.0"),
        "id": payload.get("id").cloned().unwrap_or(serde_json::Value::Null),
        "error": {
            "code": code,
            "message": message
        }
    })
}

pub(crate) async fn rpc_call_with_cache(
    st: &AppState,
    url: &url::Url,
    payload: serde_json::Value,
) -> Result<(serde_json::Value, CacheOutcome), String> {
    let method_name = payload.get("method").and_then(|v| v.as_str()).unwrap_or("");
    let cacheable = is_cacheable_rpc(&payload);
    let cache_key = format!("{}|{}", url.as_str(), payload);
    let now = chrono_ms_now();
    if cacheable {
        if let Some(v) = st.rpc_cache.get(&cache_key).await {
            if v.expires_at_ms > now {
                st.api_metrics
                    .rpc_cache_hit
                    .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
                return Ok((v.value.clone(), CacheOutcome::Hit));
            }
            st.rpc_cache.remove(&cache_key).await;
        }
    }

    let lock = st
        .rpc_inflight
        .entry(cache_key.clone())
        .or_insert_with(|| Arc::new(tokio::sync::Mutex::new(())))
        .clone();
    let _guard = lock.lock().await;

    if cacheable {
        if let Some(v) = st.rpc_cache.get(&cache_key).await {
            if v.expires_at_ms > now {
                st.api_metrics
                    .rpc_cache_hit
                    .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
                return Ok((v.value.clone(), CacheOutcome::Hit));
            }
            st.rpc_cache.remove(&cache_key).await;
        }
    }

    let ttl = rpc_ttl_ms(method_name);
    let response = fetch_rpc_with_retry(st, url, &payload).await?;
    if cacheable {
        st.rpc_cache
            .put(
                cache_key.clone(),
                CacheEntry {
                    value: response.clone(),
                    expires_at_ms: now + ttl as i64,
                    is_error: false,
                },
            )
            .await;
    }
    st.api_metrics
        .rpc_cache_miss
        .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
    st.rpc_inflight.remove(&cache_key);
    Ok((response, CacheOutcome::Miss))
}

fn is_cacheable_rpc(payload: &serde_json::Value) -> bool {
    let method = payload.get("method").and_then(|v| v.as_str()).unwrap_or("");
    if method.is_empty() {
        return false;
    }
    if method == "eth_call" {
        let block_tag = payload
            .get("params")
            .and_then(|p| p.as_array())
            .and_then(|a| a.get(1))
            .and_then(|v| v.as_str())
            .unwrap_or("");
        if block_tag.is_empty() || block_tag == "latest" {
            return false;
        }
    }
    matches!(
        method,
        "eth_chainId"
            | "net_version"
            | "eth_getBalance"
            | "eth_getCode"
            | "eth_getTransactionCount"
            | "eth_blockNumber"
            | "eth_gasPrice"
            | "eth_feeHistory"
            | "eth_getBlockByNumber"
            | "eth_getBlockByHash"
            | "eth_call"
    )
}

fn rpc_ttl_ms(method: &str) -> u64 {
    match method {
        "eth_chainId" | "net_version" => 10 * 60 * 1000,
        "eth_getBalance" | "eth_getCode" | "eth_getTransactionCount" => 30 * 1000,
        "eth_blockNumber" | "eth_gasPrice" | "eth_feeHistory" => 10 * 1000,
        _ => 15 * 1000,
    }
}

fn rpc_method_timeout(method: &str) -> Duration {
    match method {
        "eth_blockNumber" | "eth_gasPrice" | "eth_feeHistory" | "eth_chainId" | "net_version" => {
            Duration::from_secs(3)
        }
        "eth_getBalance" | "eth_getCode" | "eth_getTransactionCount" => Duration::from_secs(4),
        _ => Duration::from_secs(6),
    }
}

fn rpc_adjust_host_penalty(st: &AppState, host: &str, delta: i32) -> u32 {
    let current = st.rpc_host_penalty.get(host).map(|v| *v).unwrap_or(0);
    let next = if delta < 0 {
        current.saturating_sub((-delta) as u32)
    } else {
        current.saturating_add(delta as u32).min(20)
    };
    if next == 0 {
        st.rpc_host_penalty.remove(host);
    } else {
        st.rpc_host_penalty.insert(host.to_string(), next);
    }
    next
}

fn rpc_set_host_cooldown(st: &AppState, host: &str, base_ms: i64, penalty: u32) {
    let scaled = base_ms.saturating_mul((1 + penalty.min(10)) as i64);
    let bounded = scaled.clamp(RPC_COOLDOWN_BASE_MS, RPC_COOLDOWN_MAX_MS);
    st.rpc_host_cooldown_until_ms
        .insert(host.to_string(), chrono_ms_now() + bounded);
}

pub(crate) async fn fetch_with_host_policy(
    st: &AppState,
    method: reqwest::Method,
    url: &url::Url,
    payload: Option<serde_json::Value>,
    headers: Option<reqwest::header::HeaderMap>,
    timeout: Duration,
    source: &str,
) -> Result<serde_json::Value, String> {
    let host = url.host_str().unwrap_or("").to_lowercase();
    let host_sem = crate::policy::host_semaphore(
        &st.rpc_host_semaphores,
        &host,
        st.cfg.rpc_per_host_max_concurrency,
    );
    let _host_permit = host_sem
        .acquire()
        .await
        .map_err(|_| format!("{source} host concurrency limiter unavailable"))?;

    if let Some(until) = st.rpc_host_cooldown_until_ms.get(&host) {
        let now = chrono_ms_now();
        if now < *until {
            st.api_metrics
                .rpc_cooldown_hits
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
            tracing::warn!(
                source = %source,
                upstream_host = %host,
                reject_reason = "host_cooldown_active",
                "upstream call skipped due to cooldown"
            );
            return Err(format!(
                "Upstream host cooldown active ({}). Retry in {} ms.",
                host,
                (*until - now).max(0)
            ));
        }
    }

    let max_retries = if method == reqwest::Method::GET {
        std::env::var("HTTP_MAX_RETRIES")
            .ok()
            .and_then(|v| v.parse::<u32>().ok())
            .unwrap_or(2)
    } else {
        0 // Only retry GETs by default
    };

    let mut attempt: u32 = 0;
    let mut last_err = "Request failed".to_string();

    while attempt <= max_retries {
        let mut req = st
            .http
            .request(method.clone(), url.as_str())
            .timeout(timeout);
        if let Some(ref p) = payload {
            req = req.json(p);
        }
        if let Some(ref h) = headers {
            req = req.headers(h.clone());
        }

        let res = req.send().await;
        match res {
            Ok(resp) => {
                let status = resp.status();
                if status.as_u16() == 429 {
                    let penalty = rpc_adjust_host_penalty(st, &host, 2);
                    rpc_set_host_cooldown(st, &host, 8_000, penalty);
                    return Err(format!("Upstream {} returned 429", host));
                }
                if status.as_u16() >= 500 {
                    last_err = format!("Upstream {} returned {}", host, status.as_u16());
                    let penalty = rpc_adjust_host_penalty(st, &host, 1);
                    if [520, 522, 524, 503, 504].contains(&status.as_u16()) {
                        rpc_set_host_cooldown(st, &host, 3_000, penalty);
                    }
                    if method == reqwest::Method::GET {
                        sleep_with_backoff(attempt).await;
                        attempt += 1;
                        continue;
                    }
                    return Err(last_err);
                }
                if status.as_u16() >= 400 {
                    let t = resp.text().await.unwrap_or_default();
                    return Err(format!(
                        "Upstream {} returned {}: {}",
                        host,
                        status.as_u16(),
                        truncate_100(&t)
                    ));
                }
                let ct = resp
                    .headers()
                    .get(axum::http::header::CONTENT_TYPE)
                    .and_then(|v| v.to_str().ok())
                    .unwrap_or("");
                if !ct.contains("application/json") {
                    let t = resp.text().await.unwrap_or_default();
                    return Err(format!(
                        "Upstream {} returned non-JSON response: {}",
                        host,
                        truncate_100(&t)
                    ));
                }
                let bytes = resp.bytes().await.map_err(|e| e.to_string())?;
                crate::policy::enforce_rpc_response_size_limit(
                    bytes.len(),
                    st.cfg.rpc_max_response_body_bytes,
                )?;
                let data = serde_json::from_slice::<serde_json::Value>(&bytes)
                    .map_err(|e| e.to_string())?;
                rpc_adjust_host_penalty(st, &host, -1);
                return Ok(data);
            }
            Err(e) => {
                last_err = e.to_string();
                let penalty = rpc_adjust_host_penalty(st, &host, 1);
                rpc_set_host_cooldown(st, &host, 2_000, penalty);
                if method == reqwest::Method::GET {
                    sleep_with_backoff(attempt).await;
                    attempt += 1;
                    continue;
                }
                return Err(last_err);
            }
        }
    }

    Err(last_err)
}

async fn fetch_rpc_with_retry(
    st: &AppState,
    url: &url::Url,
    payload: &serde_json::Value,
) -> Result<serde_json::Value, String> {
    let method_name = payload.get("method").and_then(|v| v.as_str()).unwrap_or("");
    let timeout = rpc_method_timeout(method_name);
    fetch_with_host_policy(
        st,
        reqwest::Method::POST,
        url,
        Some(payload.clone()),
        None,
        timeout,
        "rpc",
    )
    .await
}

async fn sleep_with_backoff(attempt: u32) {
    let jitter = (rand::random::<u16>() % 300) as u64;
    let backoff = 500_u64.saturating_mul(2u64.saturating_pow(attempt)) + jitter;
    tokio::time::sleep(Duration::from_millis(backoff)).await;
}

fn truncate_100(s: &str) -> String {
    if s.len() <= 100 {
        return s.to_string();
    }
    s[..100].to_string()
}
