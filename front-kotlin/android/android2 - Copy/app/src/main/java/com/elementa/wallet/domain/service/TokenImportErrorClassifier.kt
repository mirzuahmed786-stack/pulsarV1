package com.elementa.wallet.domain.service

class TokenImportErrorClassifier @javax.inject.Inject constructor() {
    fun isRetryable(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: ""
        return message.contains("temporarily unavailable")
            || message.contains("too many requests")
            || message.contains("429")
            || message.contains("unauthorized")
            || message.contains("gateway")
            || message.contains("503")
            || message.contains("502")
            || message.contains("timeout")
            || message.contains("rpc upstream")
    }

    fun userHint(error: Throwable): String? {
        val message = error.message?.lowercase() ?: ""
        if (message.contains("unauthorized") || message.contains("api key")) {
            return "Provider authentication failed. Verify RPC/API keys."
        }
        if (message.contains("too many requests") || message.contains("429")) {
            return "RPC rate limit reached. Retry in a few seconds or switch provider."
        }
        if (message.contains("blocked rpc url") || message.contains("allowlisted")) {
            return "RPC endpoint is blocked by backend policy. Update allowlist for this host."
        }
        if (message.contains("gateway") || message.contains("503") || message.contains("502")) {
            return "Upstream provider is degraded. Retry shortly or switch provider endpoint."
        }
        return null
    }
}
