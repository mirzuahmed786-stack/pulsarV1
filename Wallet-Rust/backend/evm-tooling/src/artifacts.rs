use std::path::{Path, PathBuf};

use anyhow::{anyhow, Context, Result};
use serde::Deserialize;

#[derive(Debug, Clone, Deserialize)]
pub struct EvmArtifact {
    pub abi: serde_json::Value,
    pub bytecode: String,
}

pub fn artifacts_dir_default(repo_root: &Path) -> PathBuf {
    repo_root
        .join("Wallet-Rust")
        .join("backend")
        .join("resources")
        .join("evm-artifacts")
}

pub fn load_evm_artifact(artifacts_dir: &Path, rel_path: &str) -> Result<EvmArtifact> {
    let p = artifacts_dir.join(rel_path);
    let raw =
        std::fs::read_to_string(&p).with_context(|| format!("read artifact {}", p.display()))?;
    let a: EvmArtifact =
        serde_json::from_str(&raw).map_err(|e| anyhow!("parse artifact {}: {e}", p.display()))?;
    if !a.bytecode.starts_with("0x") && !a.bytecode.is_empty() {
        // Artifact bytecode should be 0x-prefixed; keep strict to avoid surprises.
        return Err(anyhow!(
            "artifact bytecode is not 0x-prefixed: {}",
            p.display()
        ));
    }
    Ok(a)
}
