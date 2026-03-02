package com.elementa.wallet.data.adapters

import com.elementa.wallet.domain.model.AddTokenRequest
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.domain.model.TokenMetadata
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SolanaTokenAdapterTest {
    private val storage = FakeTokenStorage()
    private val rpc = FakeSolanaRpc()
    private val adapter = SolanaTokenAdapter(storage, rpc)
    private val scope = "sol"
    private val validMint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"

    @Test
    fun invalidPublicKeyRejects() = runTest {
        val request = AddTokenRequest(
            chain = Chain.SOLANA,
            network = NetworkType.TESTNET,
            address = "bad",
            walletScope = scope,
            manualMetadata = TokenMetadata("Name", "SYM", 6)
        )
        val error = runCatching { adapter.addCustomToken(request) }.exceptionOrNull()
        assertThat(error?.message).contains("Invalid Solana mint address")
    }

    @Test
    fun duplicateRejects() = runTest {
        val request = AddTokenRequest(
            chain = Chain.SOLANA,
            network = NetworkType.TESTNET,
            address = validMint,
            walletScope = scope,
            manualMetadata = TokenMetadata("Name", "SYM", 6)
        )
        adapter.addCustomToken(request)
        val error = runCatching { adapter.addCustomToken(request) }.exceptionOrNull()
        assertThat(error?.message).contains("Token already added")
    }

    @Test
    fun testnetRequiresManualMetadata() = runTest {
        val request = AddTokenRequest(
            chain = Chain.SOLANA,
            network = NetworkType.TESTNET,
            address = validMint,
            walletScope = scope,
            manualMetadata = null
        )
        val error = runCatching { adapter.addCustomToken(request) }.exceptionOrNull()
        assertThat(error?.message).contains("manual metadata")
    }

    @Test
    fun decimalsMismatchRejects() = runTest {
        rpc.decimals = 9
        val request = AddTokenRequest(
            chain = Chain.SOLANA,
            network = NetworkType.TESTNET,
            address = validMint,
            walletScope = scope,
            manualMetadata = TokenMetadata("Name", "SYM", 6)
        )
        val error = runCatching { adapter.addCustomToken(request) }.exceptionOrNull()
        assertThat(error?.message).contains("Decimals mismatch")
    }

    @Test
    fun successStoresAndReturns() = runTest {
        rpc.decimals = 6
        val request = AddTokenRequest(
            chain = Chain.SOLANA,
            network = NetworkType.TESTNET,
            address = validMint,
            walletScope = scope,
            manualMetadata = TokenMetadata("Name", "SYM", 6)
        )
        val token = adapter.addCustomToken(request)
        val stored = storage.getCustomSolanaTokens(scope, NetworkType.TESTNET)
        assertThat(stored).isNotEmpty()
        assertThat(token.balance).isEqualTo("0")
    }
}
