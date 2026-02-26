package com.elementa.wallet.domain.usecase

import com.elementa.wallet.data.adapters.EvmTokenAdapter
import com.elementa.wallet.data.adapters.SolanaTokenAdapter
import com.elementa.wallet.domain.model.RemoveTokenRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRemovalUseCase @Inject constructor(
    private val evmAdapter: EvmTokenAdapter,
    private val solanaAdapter: SolanaTokenAdapter
) {
    /**
     * Removes a token from the wallet's persisted storage.
     * Matches web wallet semantics:
     * - EVM: removes from both custom_tokens and watched_default_tokens.
     * - Solana: removes from persisted custom mint storage.
     */
    suspend fun execute(request: RemoveTokenRequest) {
        if (request.chain.isEvm) {
            evmAdapter.removeCustomToken(
                scope = request.walletScope,
                chain = request.chain,
                network = request.network,
                address = request.address
            )
        } else {
            solanaAdapter.removeCustomToken(
                scope = request.walletScope,
                network = request.network,
                address = request.address
            )
        }
    }
}
