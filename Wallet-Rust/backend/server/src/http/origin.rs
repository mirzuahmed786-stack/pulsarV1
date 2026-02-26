use axum::{
    extract::State,
    http::{HeaderMap, Method, StatusCode},
    middleware::Next,
    response::{IntoResponse, Response},
};

#[derive(Debug, Clone)]
pub struct OriginPolicy {
    pub node_env: String,
    pub allowed_origins: Vec<String>, // normalized lowercase
}

fn is_state_changing(method: &Method) -> bool {
    !matches!(*method, Method::GET | Method::HEAD | Method::OPTIONS)
}

pub async fn require_allowed_origin_for_state_change(
    State(policy): State<OriginPolicy>,
    method: Method,
    headers: HeaderMap,
    req: axum::extract::Request,
    next: Next,
) -> Response {
    if !is_state_changing(&method) {
        return next.run(req).await;
    }

    let origin = headers
        .get(axum::http::header::ORIGIN)
        .and_then(|v| v.to_str().ok())
        .unwrap_or("")
        .trim()
        .to_lowercase();

    if origin.is_empty() {
        // Node backend requires Origin in production; in dev it lets it through.
        if policy.node_env == "production" {
            return (
                StatusCode::FORBIDDEN,
                axum::Json(serde_json::json!({ "error": "Origin required" })),
            )
                .into_response();
        }
        return next.run(req).await;
    }

    if policy.allowed_origins.is_empty() && policy.node_env != "production" {
        // Dev fallback allowlist matches Node.
        let dev_allowed = [
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:3000",
            "http://127.0.0.1:3000",
        ];
        if dev_allowed.contains(&origin.as_str()) {
            return next.run(req).await;
        }
        return (
            StatusCode::FORBIDDEN,
            axum::Json(serde_json::json!({ "error": "Origin not allowed" })),
        )
            .into_response();
    }

    if policy.allowed_origins.iter().any(|o| o == &origin) {
        return next.run(req).await;
    }

    (
        StatusCode::FORBIDDEN,
        axum::Json(serde_json::json!({ "error": "Origin not allowed" })),
    )
        .into_response()
}
