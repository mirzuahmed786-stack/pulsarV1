use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum AuthProvider {
    Google,
    Apple,
}

impl AuthProvider {
    pub fn as_str(&self) -> &'static str {
        match self {
            AuthProvider::Google => "google",
            AuthProvider::Apple => "apple",
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CloudAuthUser {
    pub sub: String,
    #[serde(default)]
    pub email: String,
    #[serde(default)]
    pub name: String,
    #[serde(default)]
    pub picture: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CloudAuthExchangeResult {
    pub ok: bool,
    pub provider: AuthProvider,
    pub user: CloudAuthUser,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub kek: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ApplePrepareResponse {
    pub ok: bool,
    pub state: String,
    pub nonce: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GooglePrepareResponse {
    pub ok: bool,
    pub state: String,
    pub nonce: String,
}
