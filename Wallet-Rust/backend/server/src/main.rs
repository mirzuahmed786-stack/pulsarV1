mod app;
mod auth;
mod cloud_store;
mod config;
mod http;
mod infra;
mod policy;
mod rate_limit;
mod request_context;
mod routes;

use crate::config::Config;
use std::path::PathBuf;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(tracing_subscriber::EnvFilter::from_default_env())
        .init();

    // Use override mode so empty process/user env vars don't shadow .env values.
    // Load both CWD .env and an explicit backend/.env path for robustness.
    if let Err(e) = dotenvy::dotenv_override() {
        tracing::warn!("dotenv_override() did not load a .env from CWD: {e}");
    }
    let backend_env = PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("..")
        .join(".env");
    if let Err(e) = dotenvy::from_path_override(&backend_env) {
        tracing::warn!("from_path_override({}) failed: {e}", backend_env.display());
    }

    let cfg = Config::from_env()?;
    tracing::info!(
        google_client_id_loaded = !cfg.google_oauth_client_id.trim().is_empty(),
        google_client_secret_loaded = !cfg.google_oauth_client_secret.trim().is_empty(),
        "oauth env load status"
    );
    let app = app::build_app(cfg.clone()).await?;

    let addr = std::net::SocketAddr::from(([0, 0, 0, 0], cfg.port));
    tracing::info!(%addr, "earth-backend listening");

    let listener = tokio::net::TcpListener::bind(addr).await?;
    axum::serve(listener, app).await?;
    Ok(())
}
