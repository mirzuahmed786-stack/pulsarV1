use axum::middleware;
use axum::Router;
use std::sync::Arc;
use tower_http::{compression::CompressionLayer, limit::RequestBodyLimitLayer, trace::TraceLayer};

use crate::{
    config::Config,
    http::origin::OriginPolicy,
    infra::state::{AppState, RealSolanaAmmApi},
};

pub async fn build_app(cfg: Config) -> anyhow::Result<Router> {
    let allowed_origins = parse_allowlist(&cfg.cors_origins);
    let origin_policy = OriginPolicy {
        node_env: cfg.node_env.clone(),
        allowed_origins: allowed_origins.clone(),
    };

    let cors = crate::infra::cors::build_cors_layer(allowed_origins);
    let api_router = crate::infra::router::assemble_api_router(origin_policy);

    let st = AppState::new_with_solana(cfg, Arc::new(RealSolanaAmmApi)).await?;
    let rate_limit_layer =
        middleware::from_fn_with_state(st.clone(), crate::rate_limit::rate_limit_middleware);

    let app = api_router
        .layer(middleware::from_fn_with_state(
            st.clone(),
            crate::request_context::request_context_middleware,
        ))
        .layer(rate_limit_layer)
        .layer(RequestBodyLimitLayer::new(10 * 1024 * 1024))
        .layer(CompressionLayer::new())
        .layer(cors)
        .layer(TraceLayer::new_for_http())
        .with_state(st);

    Ok(app)
}

fn parse_allowlist(raw: &str) -> Vec<String> {
    raw.split(',')
        .map(|s| s.trim().to_lowercase())
        .filter(|s| !s.is_empty())
        .collect()
}
