use axum::{http::StatusCode, response::Response};
use std::{collections::HashSet, net::IpAddr, net::Ipv6Addr, sync::Arc, time::Duration};

use crate::app::{
    api_error_response, chrono_ms_now, AppState, CacheEntry, CacheOutcome, ProxyFetchError,
    PROXY_TIMEOUT_DEFAULT_S, PROXY_TIMEOUT_HISTORY_S, PROXY_TIMEOUT_PRICES_S,
};

fn parse_allowlist(raw: &str) -> Vec<String> {
    raw.split(',')
        .map(|s| s.trim().to_lowercase())
        .filter(|s| !s.is_empty())
        .collect()
}

pub(crate) fn proxy_timeout_for_type(req_type: &str) -> Duration {
    match req_type {
        "prices" => Duration::from_secs(PROXY_TIMEOUT_PRICES_S),
        "history" => Duration::from_secs(PROXY_TIMEOUT_HISTORY_S),
        _ => Duration::from_secs(PROXY_TIMEOUT_DEFAULT_S),
    }
}

pub(crate) fn cache_key(prefix: &str, params: &str) -> String {
    format!("{}:{}", prefix, params)
}

pub(crate) async fn cached_json_fetch(
    st: &AppState,
    cache_key: String,
    ttl: Duration,
    stale_if_error_ttl: Option<Duration>,
    url: url::Url,
    request_timeout: Duration,
) -> Result<(serde_json::Value, CacheOutcome), ProxyFetchError> {
    let now = chrono_ms_now();
    let mut stale_value: Option<serde_json::Value> = None;
    if let Some(v) = st.proxy_cache.get(&cache_key).await {
        if v.expires_at_ms > now {
            if v.is_error {
                st.api_metrics
                    .proxy_error_cache_hit
                    .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
                let msg = v
                    .value
                    .get("error")
                    .and_then(|x| x.as_str())
                    .unwrap_or("Proxy upstream error")
                    .to_string();
                return Err(ProxyFetchError {
                    message: msg,
                    from_cache: true,
                });
            }
            st.api_metrics
                .proxy_cache_hit
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
            return Ok((v.value.clone(), CacheOutcome::Hit));
        }
        if !v.is_error {
            stale_value = Some(v.value.clone());
        }
        st.proxy_cache.remove(&cache_key).await;
        st.proxy_inflight.remove(&cache_key);
    }

    let lock = st
        .proxy_inflight
        .entry(cache_key.clone())
        .or_insert_with(|| Arc::new(tokio::sync::Mutex::new(())))
        .clone();
    let _guard = lock.lock().await;
    let now_after_lock = chrono_ms_now();

    if let Some(v) = st.proxy_cache.get(&cache_key).await {
        if v.expires_at_ms > now_after_lock {
            if v.is_error {
                st.api_metrics
                    .proxy_error_cache_hit
                    .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
                let msg = v
                    .value
                    .get("error")
                    .and_then(|x| x.as_str())
                    .unwrap_or("Proxy upstream error")
                    .to_string();
                return Err(ProxyFetchError {
                    message: msg,
                    from_cache: true,
                });
            }
            st.api_metrics
                .proxy_cache_hit
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
            return Ok((v.value.clone(), CacheOutcome::Hit));
        }
        if !v.is_error {
            stale_value = Some(v.value.clone());
        }
        st.proxy_cache.remove(&cache_key).await;
        st.proxy_inflight.remove(&cache_key);
    }

    let res = crate::infra::rpc::fetch_with_host_policy(
        st,
        reqwest::Method::GET,
        &url,
        None,
        None,
        request_timeout,
        "proxy",
    )
    .await
    .map_err(|e| ProxyFetchError {
        message: e,
        from_cache: false,
    });
    let data = match res {
        Ok(d) => d,
        Err(e) => {
            if let (Some(stale), Some(stale_ttl)) = (stale_value, stale_if_error_ttl) {
                let now_for_stale = chrono_ms_now();
                st.proxy_cache
                    .put(
                        cache_key,
                        CacheEntry {
                            value: stale.clone(),
                            expires_at_ms: now_for_stale + stale_ttl.as_millis() as i64,
                            is_error: false,
                        },
                    )
                    .await;
                st.api_metrics
                    .proxy_cache_hit
                    .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
                return Ok((stale, CacheOutcome::Stale));
            }
            return Err(e);
        }
    };
    let now_for_insert = chrono_ms_now();
    st.proxy_cache
        .put(
            cache_key,
            CacheEntry {
                value: data.clone(),
                expires_at_ms: now_for_insert + ttl.as_millis() as i64,
                is_error: false,
            },
        )
        .await;
    st.api_metrics
        .proxy_cache_miss
        .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
    Ok((data, CacheOutcome::Miss))
}

pub(crate) fn status_from_proxy_error(err: &str) -> StatusCode {
    if err.contains("cooldown active")
        || err.contains("concurrency limiter")
        || err.contains("rate limit exceeded")
    {
        return StatusCode::TOO_MANY_REQUESTS;
    }
    if let Some(raw) = err.strip_prefix("HTTP ") {
        if let Ok(code) = raw.trim().parse::<u16>() {
            if let Ok(status) = StatusCode::from_u16(code) {
                return status;
            }
        }
    }
    // Handle "Upstream ... returned {code}" format
    if let Some(idx) = err.find(" returned ") {
        let after = &err[idx + 10..];
        let code_str = after.split_whitespace().next().unwrap_or("");
        if let Ok(code) = code_str.parse::<u16>() {
            if let Ok(status) = StatusCode::from_u16(code) {
                return status;
            }
        }
    }
    StatusCode::BAD_GATEWAY
}

pub(crate) fn should_expose_proxy_error_details(st: &AppState) -> bool {
    st.cfg.node_env != "production"
}

pub(crate) fn upstream_error_response(
    st: &AppState,
    source: &str,
    status: StatusCode,
    message: &str,
    details: String,
    request_id: Option<String>,
) -> Response {
    st.api_metrics.record_upstream_error(source);
    if should_expose_proxy_error_details(st) {
        api_error_response(status, "upstream_error", message, Some(details), request_id)
    } else {
        api_error_response(status, "upstream_error", message, None, request_id)
    }
}

pub(crate) fn validate_proxy_url(st: &AppState, raw_url: &str) -> Result<url::Url, String> {
    let url = url::Url::parse(raw_url).map_err(|_| "Invalid URL".to_string())?;
    if url.scheme() != "https" {
        return Err("Only https URLs are allowed".to_string());
    }
    let host = url.host_str().unwrap_or("").to_lowercase();
    if host.is_empty() {
        return Err("Invalid URL".to_string());
    }
    if host == "localhost" || is_private_ip_host(&host) {
        return Err("Private or localhost targets are not allowed".to_string());
    }
    let mut allow = HashSet::from([
        "api.coingecko.com".to_string(),
        "api.etherscan.io".to_string(),
        "api.0x.org".to_string(),
        "api.jup.ag".to_string(),
    ]);
    for h in parse_allowlist(&st.cfg.proxy_host_allowlist) {
        allow.insert(h);
    }
    for h in parse_allowlist(&st.cfg.external_api_allowlist) {
        allow.insert(h);
    }
    if !allow.contains(&host) {
        return Err("Target host is not allowlisted".to_string());
    }
    Ok(url)
}

pub(crate) fn validate_external_base_url(st: &AppState, raw_url: &str) -> Result<String, String> {
    let url = url::Url::parse(raw_url).map_err(|_| "Invalid URL".to_string())?;
    if url.scheme() != "https" {
        return Err("Only https URLs are allowed".to_string());
    }
    let host = url.host_str().unwrap_or("").to_lowercase();
    if host.is_empty() {
        return Err("Invalid URL".to_string());
    }
    if host == "localhost" || is_private_ip_host(&host) {
        return Err("Private or localhost targets are not allowed".to_string());
    }
    let mut allow = HashSet::from(["api.0x.org".to_string(), "api.jup.ag".to_string()]);
    for h in parse_allowlist(&st.cfg.external_host_allowlist) {
        allow.insert(h);
    }
    for h in parse_allowlist(&st.cfg.external_api_allowlist) {
        allow.insert(h);
    }
    if !allow.is_empty() && !allow.contains(&host) {
        return Err("Target host is not allowlisted".to_string());
    }
    Ok(url.to_string())
}

pub(crate) fn is_private_ip_host(host: &str) -> bool {
    if let Ok(ip) = host.parse::<IpAddr>() {
        return is_private_ip(ip);
    }
    false
}

fn is_private_ip(ip: IpAddr) -> bool {
    match ip {
        IpAddr::V4(v4) => {
            v4.is_loopback()
                || v4.is_private()
                || v4.is_link_local()
                || v4.octets()[0] == 169 && v4.octets()[1] == 254
        }
        IpAddr::V6(v6) => {
            v6.is_loopback()
                || v6.is_unique_local()
                || v6.is_unicast_link_local()
                || v6 == Ipv6Addr::LOCALHOST
        }
    }
}
