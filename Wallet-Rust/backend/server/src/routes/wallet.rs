use axum::{
    extract::State,
    http::{HeaderMap, StatusCode},
    middleware,
    response::IntoResponse,
    routing::get,
    Json, Router,
};
use std::sync::atomic::Ordering;

use crate::{
    app::{
        api_error_response, chrono_ms_now, consume_rate_limit_bucket, inc_reject_reason_counter,
        incoming_request_id, rate_limit_client_key, require_csrf_for_state_change, AppState,
        CLOUD_BLOB_READ_MAX_REQUESTS, CLOUD_BLOB_READ_WINDOW_MS, CLOUD_BLOB_WRITE_MAX_REQUESTS,
        CLOUD_BLOB_WRITE_WINDOW_MS,
    },
    auth::session::require_session,
    http::{
        csrf::issue_csrf_token,
        origin::{require_allowed_origin_for_state_change, OriginPolicy},
    },
};

pub fn router(origin_policy: OriginPolicy) -> Router<AppState> {
    Router::new()
        .route("/blob", get(wallet_get_blob).put(wallet_put_blob))
        .layer(middleware::from_fn_with_state(
            origin_policy,
            require_allowed_origin_for_state_change,
        ))
}

pub(crate) async fn wallet_get_blob(
    State(st): State<AppState>,
    headers: HeaderMap,
) -> impl IntoResponse {
    let request_id = incoming_request_id(Some(&headers));
    let auth = match require_session(&headers, &st.session_key) {
        Ok(a) => a,
        Err(msg) => {
            return (
                StatusCode::UNAUTHORIZED,
                Json(serde_json::json!({ "error": msg })),
            )
                .into_response();
        }
    };
    let key = format!("{}:{}", auth.provider, auth.sub);
    let client = rate_limit_client_key(&headers);
    let now = chrono_ms_now();
    let rl_key = format!("{}:/api/wallet/blob:read:{}", client, key);
    if consume_rate_limit_bucket(
        &st,
        &rl_key,
        now,
        CLOUD_BLOB_READ_WINDOW_MS,
        CLOUD_BLOB_READ_MAX_REQUESTS,
    )
    .await
    {
        st.api_metrics
            .cloud_blob_read_rate_limited
            .fetch_add(1, Ordering::Relaxed);
        inc_reject_reason_counter(&st.api_metrics, "cloud_blob_read_rate_limited");
        return api_error_response(
            StatusCode::TOO_MANY_REQUESTS,
            "cloud_blob_read_rate_limited",
            "Cloud recovery blob read rate limit exceeded",
            None,
            request_id,
        );
    }

    let blob = match st.store.get(&key).await {
        Ok(v) => v,
        Err(e) => {
            return (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(
                    serde_json::json!({ "error": "Failed to read blob", "details": e.to_string() }),
                ),
            )
                .into_response();
        }
    };

    let mut out_headers = HeaderMap::new();
    issue_csrf_token(
        &mut out_headers,
        &st.cfg.cookie_samesite,
        st.cfg.cookie_secure(),
        crate::app::SESSION_TTL_S,
    );
    (
        out_headers,
        Json(serde_json::json!({ "ok": true, "blob": blob })),
    )
        .into_response()
}

pub(crate) async fn wallet_put_blob(
    State(st): State<AppState>,
    headers: HeaderMap,
    Json(input): Json<serde_json::Value>,
) -> impl IntoResponse {
    let request_id = incoming_request_id(Some(&headers));
    if !require_csrf_for_state_change("PUT", &headers) {
        return (
            StatusCode::FORBIDDEN,
            Json(serde_json::json!({ "error": "CSRF check failed" })),
        )
            .into_response();
    }
    let auth = match require_session(&headers, &st.session_key) {
        Ok(a) => a,
        Err(msg) => {
            return (
                StatusCode::UNAUTHORIZED,
                Json(serde_json::json!({ "error": msg })),
            )
                .into_response();
        }
    };

    let key = format!("{}:{}", auth.provider, auth.sub);
    let client = rate_limit_client_key(&headers);
    let now = chrono_ms_now();
    let rl_key = format!("{}:/api/wallet/blob:write:{}", client, key);
    if consume_rate_limit_bucket(
        &st,
        &rl_key,
        now,
        CLOUD_BLOB_WRITE_WINDOW_MS,
        CLOUD_BLOB_WRITE_MAX_REQUESTS,
    )
    .await
    {
        st.api_metrics
            .cloud_blob_write_rate_limited
            .fetch_add(1, Ordering::Relaxed);
        inc_reject_reason_counter(&st.api_metrics, "cloud_blob_write_rate_limited");
        return api_error_response(
            StatusCode::TOO_MANY_REQUESTS,
            "cloud_blob_write_rate_limited",
            "Cloud recovery blob write rate limit exceeded",
            None,
            request_id.clone(),
        );
    }

    let encrypted_seed_blob = input
        .get("encryptedSeedBlob")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .to_string();
    let wallet_id = input
        .get("walletId")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .to_string();
    let version = input.get("version").and_then(|v| v.as_i64()).unwrap_or(0);
    let created_at = input.get("createdAt").and_then(|v| v.as_i64()).unwrap_or(0);
    let secret_type = input
        .get("secretType")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .to_string();
    let hd_index = input.get("hdIndex").and_then(|v| v.as_i64()).unwrap_or(-1);
    let kek_kdf = input.get("kekKdf").cloned();

    if encrypted_seed_blob.is_empty() || encrypted_seed_blob.len() > 2_000_000 {
        inc_reject_reason_counter(&st.api_metrics, "cloud_blob_validation_failed");
        return api_error_response(
            StatusCode::BAD_REQUEST,
            "cloud_blob_validation_failed",
            "Invalid encryptedSeedBlob",
            None,
            request_id.clone(),
        );
    }
    if wallet_id.is_empty() || wallet_id.len() > 128 {
        inc_reject_reason_counter(&st.api_metrics, "cloud_blob_validation_failed");
        return api_error_response(
            StatusCode::BAD_REQUEST,
            "cloud_blob_validation_failed",
            "Invalid walletId",
            None,
            request_id.clone(),
        );
    }
    if !(1..=10).contains(&version) {
        inc_reject_reason_counter(&st.api_metrics, "cloud_blob_validation_failed");
        return api_error_response(
            StatusCode::BAD_REQUEST,
            "cloud_blob_validation_failed",
            "Invalid version",
            None,
            request_id.clone(),
        );
    }
    if secret_type != "mnemonic" && secret_type != "raw32" {
        inc_reject_reason_counter(&st.api_metrics, "cloud_blob_validation_failed");
        return api_error_response(
            StatusCode::BAD_REQUEST,
            "cloud_blob_validation_failed",
            "Invalid secretType",
            None,
            request_id.clone(),
        );
    }
    if !(0..=0x7fffffff).contains(&hd_index) {
        inc_reject_reason_counter(&st.api_metrics, "cloud_blob_validation_failed");
        return api_error_response(
            StatusCode::BAD_REQUEST,
            "cloud_blob_validation_failed",
            "Invalid hdIndex",
            None,
            request_id.clone(),
        );
    }
    if let Err(e) = validate_kek_kdf(kek_kdf.as_ref()) {
        inc_reject_reason_counter(&st.api_metrics, "cloud_blob_validation_failed");
        return api_error_response(
            StatusCode::BAD_REQUEST,
            "cloud_blob_validation_failed",
            &e,
            None,
            request_id.clone(),
        );
    }

    let record = serde_json::json!({
        "encryptedSeedBlob": encrypted_seed_blob,
        "walletId": wallet_id,
        "version": version,
        "createdAt": if created_at > 0 { created_at } else { chrono_ms_now() },
        "secretType": secret_type,
        "hdIndex": hd_index,
        "kekKdf": kek_kdf,
        "updatedAt": chrono_ms_now(),
    });

    if let Err(e) = st.store.put(&key, record).await {
        return (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(serde_json::json!({ "error": "Failed to store blob", "details": e.to_string() })),
        )
            .into_response();
    }
    (StatusCode::OK, Json(serde_json::json!({ "ok": true }))).into_response()
}

fn validate_kek_kdf(kek_kdf: Option<&serde_json::Value>) -> Result<(), String> {
    let Some(v) = kek_kdf else {
        return Ok(());
    };
    if v.is_null() {
        return Ok(());
    }
    let Some(obj) = v.as_object() else {
        return Err("Invalid kekKdf".to_string());
    };
    if obj.get("name").and_then(|x| x.as_str()) != Some("scrypt") {
        return Err("Invalid kekKdf.name".to_string());
    }

    let salt_hex = obj.get("saltHex").and_then(|x| x.as_str()).unwrap_or("");
    let n = obj.get("N").and_then(|x| x.as_u64()).unwrap_or(0);
    let r = obj.get("r").and_then(|x| x.as_u64()).unwrap_or(0);
    let p = obj.get("p").and_then(|x| x.as_u64()).unwrap_or(0);
    let dk_len = obj.get("dkLen").and_then(|x| x.as_u64()).unwrap_or(0);

    let salt_ok = !salt_hex.is_empty()
        && salt_hex.len() <= 256
        && salt_hex.len() % 2 == 0
        && salt_hex.as_bytes().iter().all(|b| b.is_ascii_hexdigit());
    if !salt_ok {
        return Err("Invalid kekKdf.saltHex".to_string());
    }
    if !(1024..=1_048_576).contains(&n) || !n.is_power_of_two() {
        return Err("Invalid kekKdf.N".to_string());
    }
    if r == 0 || r > 32 {
        return Err("Invalid kekKdf.r".to_string());
    }
    if p == 0 || p > 16 {
        return Err("Invalid kekKdf.p".to_string());
    }
    if dk_len != 32 {
        return Err("Invalid kekKdf.dkLen".to_string());
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::validate_kek_kdf;

    #[test]
    fn validate_kek_kdf_accepts_valid_scrypt() {
        let kdf = serde_json::json!({
            "name": "scrypt",
            "saltHex": "a1b2c3d4",
            "N": 16384,
            "r": 8,
            "p": 1,
            "dkLen": 32
        });
        assert_eq!(validate_kek_kdf(Some(&kdf)), Ok(()));
    }

    #[test]
    fn validate_kek_kdf_rejects_invalid_params() {
        let bad_name = serde_json::json!({
            "name": "argon2",
            "saltHex": "a1b2",
            "N": 16384,
            "r": 8,
            "p": 1,
            "dkLen": 32
        });
        assert_eq!(
            validate_kek_kdf(Some(&bad_name)),
            Err("Invalid kekKdf.name".to_string())
        );

        let bad_n = serde_json::json!({
            "name": "scrypt",
            "saltHex": "a1b2",
            "N": 1000,
            "r": 8,
            "p": 1,
            "dkLen": 32
        });
        assert_eq!(
            validate_kek_kdf(Some(&bad_n)),
            Err("Invalid kekKdf.N".to_string())
        );
    }
}
