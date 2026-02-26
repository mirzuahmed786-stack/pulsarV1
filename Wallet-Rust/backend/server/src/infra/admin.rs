use axum::http::HeaderMap;
use std::path::PathBuf;

use crate::app::AppState;

pub(crate) fn is_admin_allowed(st: &AppState, headers: &HeaderMap) -> bool {
    if st.cfg.disable_admin_scripts {
        return false;
    }
    if st.cfg.allow_insecure_admin_dev && st.cfg.node_env != "production" {
        return true;
    }
    if st.cfg.admin_token.trim().is_empty() {
        return false;
    }
    let token = headers
        .get("x-admin-token")
        .and_then(|v| v.to_str().ok())
        .unwrap_or("");
    token == st.cfg.admin_token
}

pub(crate) fn get_private_key() -> Option<String> {
    std::env::var("DEPLOYER_PRIVATE_KEY")
        .ok()
        .filter(|s| !s.trim().is_empty())
        .or_else(|| {
            std::env::var("PRIVATE_KEY")
                .ok()
                .filter(|s| !s.trim().is_empty())
        })
}

pub(crate) fn repo_root_from_server_crate() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("..")
        .join("..")
        .join("..")
}

fn backend_root_from_server_crate() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("..")
}

fn backend_state_dir() -> PathBuf {
    backend_root_from_server_crate().join("state")
}

pub(crate) fn backend_state_file(name: &str) -> PathBuf {
    backend_state_dir().join(name)
}
