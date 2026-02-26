use axum::http::{HeaderMap, HeaderValue};

#[derive(Debug, Clone)]
pub struct CookieOpts {
    pub http_only: bool,
    pub max_age_s: Option<u64>,
    pub same_site: String, // Lax|Strict|None
    pub secure: bool,
    pub path: String,
}

impl Default for CookieOpts {
    fn default() -> Self {
        Self {
            http_only: true,
            max_age_s: None,
            same_site: "Lax".to_string(),
            secure: false,
            path: "/".to_string(),
        }
    }
}

fn normalize_same_site(raw: &str) -> &'static str {
    if raw.eq_ignore_ascii_case("strict") {
        "Strict"
    } else if raw.eq_ignore_ascii_case("none") {
        "None"
    } else {
        "Lax"
    }
}

fn is_production_env() -> bool {
    std::env::var("NODE_ENV")
        .ok()
        .map(|v| v.eq_ignore_ascii_case("production"))
        .unwrap_or(false)
}

fn apply_cookie_policy(mut opts: CookieOpts) -> CookieOpts {
    opts.same_site = normalize_same_site(&opts.same_site).to_string();
    if opts.path.trim().is_empty() {
        opts.path = "/".to_string();
    }
    if opts.same_site == "None" {
        // Browsers require Secure for SameSite=None. Enforce instead of emitting invalid cookies.
        opts.secure = true;
    }
    if is_production_env() {
        // In production all cookies must be secure transport only.
        opts.secure = true;
        // Tighten production default away from cross-site cookies unless explicitly required.
        if opts.same_site == "None" {
            opts.same_site = "Lax".to_string();
        }
    }
    opts
}

pub fn parse_cookie_header(headers: &HeaderMap) -> std::collections::HashMap<String, String> {
    let mut out = std::collections::HashMap::new();
    let Some(raw) = headers.get(axum::http::header::COOKIE) else {
        return out;
    };
    let Ok(s) = raw.to_str() else {
        return out;
    };
    for part in s.split(';') {
        let p = part.trim();
        if p.is_empty() {
            continue;
        }
        let Some(idx) = p.find('=') else {
            continue;
        };
        let k = p[..idx].trim();
        let v = p[idx + 1..].trim();
        if k.is_empty() {
            continue;
        }
        let v = urlencoding::decode(v).unwrap_or_else(|_| v.into());
        out.insert(k.to_string(), v.to_string());
    }
    out
}

pub fn append_set_cookie(headers: &mut HeaderMap, name: &str, value: &str, opts: CookieOpts) {
    let opts = apply_cookie_policy(opts);
    // Keep this very close to the existing Node backend behavior: we build the Set-Cookie header string directly.
    let mut parts = vec![
        format!("{}={}", name, urlencoding::encode(value)),
        format!("Path={}", opts.path),
        format!("SameSite={}", opts.same_site),
    ];
    if opts.http_only {
        parts.push("HttpOnly".to_string());
    }
    if let Some(max_age_s) = opts.max_age_s {
        parts.push(format!("Max-Age={}", max_age_s));
    }
    if opts.secure {
        parts.push("Secure".to_string());
    }

    let s = parts.join("; ");
    // Multiple Set-Cookie headers are allowed.
    if let Ok(v) = HeaderValue::from_str(&s) {
        headers.append(axum::http::header::SET_COOKIE, v);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    struct EnvGuard {
        key: String,
        prev: Option<String>,
    }

    impl EnvGuard {
        fn set<K: Into<String>, V: Into<String>>(key: K, value: V) -> Self {
            let key = key.into();
            let prev = std::env::var(&key).ok();
            std::env::set_var(&key, value.into());
            Self { key, prev }
        }
    }

    impl Drop for EnvGuard {
        fn drop(&mut self) {
            if let Some(v) = &self.prev {
                std::env::set_var(&self.key, v);
            } else {
                std::env::remove_var(&self.key);
            }
        }
    }

    fn first_set_cookie(headers: &HeaderMap) -> String {
        headers
            .get_all(axum::http::header::SET_COOKIE)
            .iter()
            .next()
            .and_then(|v| v.to_str().ok())
            .unwrap_or("")
            .to_string()
    }

    #[test]
    fn normalizes_invalid_samesite_to_lax() {
        let _env = EnvGuard::set("NODE_ENV", "development");
        let mut headers = HeaderMap::new();
        append_set_cookie(
            &mut headers,
            "test_cookie",
            "abc",
            CookieOpts {
                same_site: "weird".to_string(),
                ..CookieOpts::default()
            },
        );
        let cookie = first_set_cookie(&headers);
        assert!(cookie.contains("SameSite=Lax"));
    }

    #[test]
    fn forces_secure_when_samesite_none() {
        let _env = EnvGuard::set("NODE_ENV", "development");
        let mut headers = HeaderMap::new();
        append_set_cookie(
            &mut headers,
            "test_cookie",
            "abc",
            CookieOpts {
                same_site: "None".to_string(),
                secure: false,
                ..CookieOpts::default()
            },
        );
        let cookie = first_set_cookie(&headers);
        assert!(cookie.contains("SameSite=None"));
        assert!(cookie.contains("Secure"));
    }

    #[test]
    fn production_forces_secure_and_disallows_none() {
        let _env = EnvGuard::set("NODE_ENV", "production");
        let mut headers = HeaderMap::new();
        append_set_cookie(
            &mut headers,
            "test_cookie",
            "abc",
            CookieOpts {
                same_site: "None".to_string(),
                secure: false,
                ..CookieOpts::default()
            },
        );
        let cookie = first_set_cookie(&headers);
        assert!(cookie.contains("Secure"));
        assert!(cookie.contains("SameSite=Lax"));
    }
}
