use axum::http::Method;
use tower_http::cors::{AllowCredentials, AllowHeaders, AllowMethods, AllowOrigin, CorsLayer};

pub fn build_cors_layer(allowed_origins: Vec<String>) -> CorsLayer {
    CorsLayer::new()
        .allow_credentials(AllowCredentials::yes())
        .allow_methods(AllowMethods::list([
            Method::GET,
            Method::POST,
            Method::PUT,
            Method::DELETE,
            Method::OPTIONS,
        ]))
        .allow_headers(AllowHeaders::list([
            axum::http::header::CONTENT_TYPE,
            axum::http::header::HeaderName::from_static("x-csrf-token"),
            axum::http::header::HeaderName::from_static("x-admin-token"),
        ]))
        .allow_origin(AllowOrigin::predicate(move |origin, _req_parts| {
            let Ok(s) = origin.to_str() else { return false };
            let normalized = s.trim().to_lowercase();
            if normalized.is_empty() {
                return true;
            }
            if !allowed_origins.is_empty() {
                return allowed_origins.iter().any(|o| o == &normalized);
            }
            matches!(
                normalized.as_str(),
                "http://localhost:5173"
                    | "http://127.0.0.1:5173"
                    | "http://localhost:3000"
                    | "http://127.0.0.1:3000"
            )
        }))
}
