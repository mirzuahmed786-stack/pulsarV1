use crate::http::origin::OriginPolicy;
use crate::infra::observability::{handler_404, health, metrics};
use crate::infra::state::AppState;
use axum::{routing::get, Router};

pub fn assemble_api_router(origin_policy: OriginPolicy) -> Router<AppState> {
    let api_auth = crate::routes::auth::router(origin_policy.clone());
    let api_wallet = crate::routes::wallet::router(origin_policy.clone());
    let api_proxy = crate::routes::proxy::router();
    let api_evm = crate::routes::evm::router();
    let api_jupiter = crate::routes::jupiter::router();
    let api_testnet_amm = crate::routes::testnet_amm::router();
    let api_solana_amm = crate::routes::solana_amm::router();
    let api_swap_v1 = crate::routes::swap_v1::router();
    let api_system = crate::routes::system::router();

    Router::new()
        .route("/health", get(health))
        .route("/metrics", get(metrics))
        .nest("/api", api_system)
        .nest("/api/auth", api_auth)
        .nest("/api/wallet", api_wallet)
        .nest("/api/proxy", api_proxy)
        .nest("/api/evm", api_evm)
        .nest("/api/jupiter", api_jupiter)
        .nest("/api/testnet-amm", api_testnet_amm)
        .nest("/api/solana/amm", api_solana_amm)
        .nest("/api/v1/swap", api_swap_v1)
        .fallback(handler_404)
}
