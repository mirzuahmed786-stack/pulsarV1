package com.elementa.wallet.domain.usecase

import com.elementa.wallet.domain.model.AddTokenRequest
import com.elementa.wallet.domain.model.TokenAsset
import com.elementa.wallet.domain.service.TokenImportAdapter
import com.elementa.wallet.domain.service.TokenImportErrorClassifier
import javax.inject.Inject

class TokenImportUseCase @Inject constructor(
    private val adapter: TokenImportAdapter,
    private val classifier: TokenImportErrorClassifier
) {
    suspend fun execute(request: AddTokenRequest): TokenAsset {
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            try {
                return adapter.addCustomToken(request)
            } catch (error: Throwable) {
                lastError = error
                val retryable = classifier.isRetryable(error)
                if (!retryable || attempt == 1) {
                    throw error
                }
                kotlinx.coroutines.delay(800)
            }
        }
        throw lastError ?: IllegalStateException("Failed to import token")
    }
}
