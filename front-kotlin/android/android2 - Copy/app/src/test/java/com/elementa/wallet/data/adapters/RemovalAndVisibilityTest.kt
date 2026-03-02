package com.elementa.wallet.data.adapters

import com.elementa.wallet.data.storage.StoredEvmToken
import com.elementa.wallet.data.storage.StoredSolanaToken
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.domain.model.TokenSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigInteger

class RemovalAndVisibilityTest {
    private val storage = FakeTokenStorage()
    private val evmRpc = FakeEvmRpc()
    private val solRpc = FakeSolanaRpc()
    private val evmAdapter = EvmTokenAdapter(storage, evmRpc)
    private val solAdapter = SolanaTokenAdapter(storage, solRpc)
    private val scope = "scope"

    @Test
    fun evmRemovalDeletesCustomAndWatched() = runTest {
        val token = StoredEvmToken(
            address = "0x0000000000000000000000000000000000000007",
            symbol = "TKN",
            name = "Token",
            decimals = 18,
            chain = "ethereum",
            network = "testnet",
            source = TokenSource.MANUAL
        )
        storage.saveCustomEvmTokens(scope, Chain.ETHEREUM, NetworkType.TESTNET, listOf(token))
        storage.saveWatchedDefaultTokens(scope, Chain.ETHEREUM, NetworkType.TESTNET, listOf(token.address))

        evmAdapter.removeCustomToken(scope, Chain.ETHEREUM, NetworkType.TESTNET, token.address)

        val remainingCustom = storage.getCustomEvmTokens(scope, Chain.ETHEREUM, NetworkType.TESTNET)
        val remainingWatched = storage.getWatchedDefaultTokens(scope, Chain.ETHEREUM, NetworkType.TESTNET)
        assertThat(remainingCustom).isEmpty()
        assertThat(remainingWatched).isEmpty()
    }

    @Test
    fun solanaRemovalDeletesCustomToken() = runTest {
        val token = StoredSolanaToken(
            address = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            symbol = "USDC",
            name = "USD Coin",
            decimals = 6,
            network = "testnet"
        )
        storage.saveCustomSolanaTokens(scope, NetworkType.TESTNET, listOf(token))

        solAdapter.removeCustomToken(scope, NetworkType.TESTNET, token.address)
        val remaining = storage.getCustomSolanaTokens(scope, NetworkType.TESTNET)
        assertThat(remaining).isEmpty()
    }

    @Test
    fun portfolioVisibilityRules() = runTest {
        val token = StoredEvmToken(
            address = "0x0000000000000000000000000000000000000008",
            symbol = "TKN",
            name = "Token",
            decimals = 18,
            chain = "ethereum",
            network = "testnet",
            source = TokenSource.MANUAL
        )
        storage.saveCustomEvmTokens(scope, Chain.ETHEREUM, NetworkType.TESTNET, listOf(token))
        evmRpc.balance = BigInteger.ZERO

        val evmTokens = evmAdapter.getPortfolio(scope, Chain.ETHEREUM, NetworkType.TESTNET, scope)
        assertThat(evmTokens).isNotEmpty()

        val solToken = StoredSolanaToken(
            address = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            symbol = "USDC",
            name = "USD Coin",
            decimals = 6,
            network = "testnet"
        )
        storage.saveCustomSolanaTokens(scope, NetworkType.TESTNET, listOf(solToken))
        val solTokens = solAdapter.getPortfolio(scope, NetworkType.TESTNET, scope)
        assertThat(solTokens).isNotEmpty()
    }
}
