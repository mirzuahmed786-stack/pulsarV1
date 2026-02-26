// hassam dev: Gradle settings for Kotlin UniFFI bindings (Phase 2)

rootProject.name = "wallet-kotlin-bindings"

// hassam dev: Enable build cache for faster incremental builds (Optimization Step 2)
buildCache {
    local {
        isEnabled = true
    }
}
