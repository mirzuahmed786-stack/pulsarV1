package com.elementa.wallet.data.adapters

import com.elementa.wallet.domain.model.AddTokenRequest
import com.elementa.wallet.domain.model.TokenAsset
import com.elementa.wallet.domain.service.TokenImportAdapter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenImportAdapterImpl @Inject constructor(
    private val evmAdapter: EvmTokenAdapter,
    private val solanaAdapter: SolanaTokenAdapter
) : TokenImportAdapter {
    override suspend fun addCustomToken(request: AddTokenRequest): TokenAsset {
        return if (request.chain.isEvm) {
            evmAdapter.addCustomToken(request)
        } else {
            solanaAdapter.addCustomToken(request)
        }
    }
}
