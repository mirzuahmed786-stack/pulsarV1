package com.elementa.wallet.data.adapters

import com.elementa.wallet.domain.model.AddTokenRequest
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.domain.model.TokenMetadata
import com.elementa.wallet.domain.model.TokenSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EvmTokenAdapterTest {
    private val storage = FakeTokenStorage()
    private val rpc = FakeEvmRpc()
    private val adapter = EvmTokenAdapter(storage, rpc)
    private val scope = "0xabc"

    @Test
    fun rejectsDuplicateCustomToken() = runTest {
        val request = AddTokenRequest(
            chain = Chain.ETHEREUM,
            network = NetworkType.TESTNET,
            address = "0x0000000000000000000000000000000000000001",
            walletScope = scope,
            manualMetadata = TokenMetadata("Name", "SYM", 18)
        )
        adapter.addCustomToken(request)

        val error = runCatching { adapter.addCustomToken(request) }.exceptionOrNull()
        assertThat(error?.message).contains("already been imported")
    }

    @Test
    fun watchedDefaultPathWhenInDefaultList() = runTest {
        val request = AddTokenRequest(
            chain = Chain.ETHEREUM,
            network = NetworkType.TESTNET,
            address = "0x77114023342d64731885cc0d603e9114d643851b",
            walletScope = scope,
            manualMetadata = TokenMetadata("USDT", "USDT", 6)
        )
        val token = adapter.addCustomToken(request)
        val watched = storage.getWatchedDefaultTokens(scope, Chain.ETHEREUM, NetworkType.TESTNET)
        assertThat(watched).contains(request.address.lowercase())
        assertThat(token.isWatchedDefault).isTrue()
    }

    @Test
    fun testnetRequiresManualMetadata() = runTest {
        val request = AddTokenRequest(
            chain = Chain.ETHEREUM,
            network = NetworkType.TESTNET,
            address = "0x0000000000000000000000000000000000000002",
            walletScope = scope,
            manualMetadata = null
        )
        val error = runCatching { adapter.addCustomToken(request) }.exceptionOrNull()
        assertThat(error?.message).contains("manual metadata")
    }

    @Test
    fun bytecodeMissingRejects() = runTest {
        rpc.bytecode = "0x"
        val request = AddTokenRequest(
            chain = Chain.ETHEREUM,
            network = NetworkType.TESTNET,
            address = "0x0000000000000000000000000000000000000003",
            walletScope = scope,
            manualMetadata = TokenMetadata("Name", "SYM", 18)
        )
        val error = runCatching { adapter.addCustomToken(request) }.exceptionOrNull()
        assertThat(error?.message).contains("Token contract not found")
    }

    @Test
    fun erc20ProbeFailRejects() = runTest {
        rpc.erc20Valid = false
        val request = AddTokenRequest(
            chain = Chain.ETHEREUM,
            network = NetworkType.TESTNET,
            address = "0x0000000000000000000000000000000000000004",
            walletScope = scope,
            manualMetadata = TokenMetadata("Name", "SYM", 18)
        )
        val error = runCatching { adapter.addCustomToken(request) }.exceptionOrNull()
        assertThat(error?.message).contains("ERC-20")
    }

    @Test
    fun mainnetManualMismatchRejects() = runTest {
        rpc.name = "Actual"
        rpc.symbol = "ACT"
        rpc.decimals = 8
        val request = AddTokenRequest(
            chain = Chain.ETHEREUM,
            network = NetworkType.MAINNET,
            address = "0x0000000000000000000000000000000000000005",
            walletScope = scope,
            manualMetadata = TokenMetadata("Wrong", "WRG", 18)
        )
        val error = runCatching { adapter.addCustomToken(request) }.exceptionOrNull()
        assertThat(error?.message).contains("Manual metadata does not match")
    }

    @Test
    fun successStoresAndReturns() = runTest {
        val request = AddTokenRequest(
            chain = Chain.ETHEREUM,
            network = NetworkType.MAINNET,
            address = "0x0000000000000000000000000000000000000006",
            walletScope = scope,
            manualMetadata = TokenMetadata("Token", "TKN", 18)
        )
        val token = adapter.addCustomToken(request)
        val stored = storage.getCustomEvmTokens(scope, Chain.ETHEREUM, NetworkType.MAINNET)
        assertThat(stored).isNotEmpty()
        assertThat(token.balance).isEqualTo("0")
        assertThat(token.source).isEqualTo(TokenSource.ON_CHAIN)
    }
}
