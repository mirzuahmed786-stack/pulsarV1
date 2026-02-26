package com.elementa.wallet.data.adapters

import com.elementa.wallet.data.config.SolanaConfig
import com.elementa.wallet.data.storage.StoredSolanaToken
import com.elementa.wallet.data.storage.TokenStorage
import com.elementa.wallet.domain.model.AddTokenRequest
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.domain.model.TokenAsset
import com.elementa.wallet.domain.model.TokenMetadata
import com.elementa.wallet.domain.model.TokenSource
import com.elementa.wallet.rpc.SolanaAddressValidator
import com.elementa.wallet.rpc.SolanaRpc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SolanaTokenAdapter @Inject constructor(
    private val storage: TokenStorage,
    private val rpc: SolanaRpc
) {
    suspend fun addCustomToken(request: AddTokenRequest): TokenAsset {
        if (!SolanaAddressValidator.isValidPublicKey(request.address)) {
            throw IllegalStateException("Invalid Solana mint address")
        }
        val existing = storage.getCustomSolanaTokens(request.walletScope, request.network)
        if (existing.any { it.address.equals(request.address, true) }) {
            throw IllegalStateException("Token already added")
        }

        val supply = rpc.getTokenSupply(request.network, request.address)
        val onChainDecimals = supply.decimals

        if (request.network == NetworkType.TESTNET && request.manualMetadata == null) {
            throw IllegalStateException("Solana token imports require manual metadata.")
        }

        val known = SolanaConfig.knownMint(request.address)
        val fallbackName = known?.name ?: "SPL Token (${request.address.take(4)}..${request.address.takeLast(4)})"
        val fallbackSymbol = known?.symbol ?: "SPL"
        val fallbackDecimals = known?.decimals ?: onChainDecimals

        val resolved = request.manualMetadata?.let { validateManual(it) }
            ?: TokenMetadata(fallbackName, fallbackSymbol, fallbackDecimals)

        if (onChainDecimals != resolved.decimals) {
            throw IllegalStateException("Decimals mismatch. On-chain decimals: $onChainDecimals")
        }

        val newToken = StoredSolanaToken(
            address = request.address,
            symbol = resolved.symbol,
            name = resolved.name,
            decimals = resolved.decimals,
            network = NetworkType.toWire(request.network)
        )

        storage.saveCustomSolanaTokens(request.walletScope, request.network, existing + newToken)

        return TokenAsset(
            address = request.address,
            chain = Chain.SOLANA,
            network = request.network,
            symbol = resolved.symbol,
            name = resolved.name,
            decimals = resolved.decimals,
            balance = "0",
            balanceInUsd = 0.0,
            source = TokenSource.MANUAL,
            isCustom = true
        )
    }

    suspend fun removeCustomToken(scope: String, network: NetworkType, address: String) {
        val existing = storage.getCustomSolanaTokens(scope, network)
        val updated = existing.filterNot { it.address.equals(address, true) }
        storage.saveCustomSolanaTokens(scope, network, updated)
    }

    suspend fun getPortfolio(scope: String, network: NetworkType, walletAddress: String): List<TokenAsset> {
        val customTokens = storage.getCustomSolanaTokens(scope, network)
        val customByMint = customTokens.associateBy { it.address.lowercase() }
        val accounts = rpc.getTokenAccountsByOwner(network, walletAddress)

        val aggregated = mutableMapOf<String, TokenAsset>()
        for (account in accounts) {
            val mint = account.mint
            val known = SolanaConfig.knownMint(mint)
            val decimals = account.decimals
            val balance = account.uiAmountString
            val name = known?.name ?: "SPL Token (${mint.take(4)}..${mint.takeLast(4)})"
            val symbol = known?.symbol ?: "Unknown"
            aggregated[mint.lowercase()] = TokenAsset(
                address = mint,
                chain = Chain.SOLANA,
                network = network,
                symbol = symbol,
                name = name,
                decimals = decimals,
                balance = balance,
                balanceInUsd = 0.0,
                source = known?.source ?: TokenSource.ON_CHAIN,
                isCustom = false
            )
        }

        for (custom in customTokens) {
            val key = custom.address.lowercase()
            val existing = aggregated[key]
            if (existing != null) {
                aggregated[key] = existing.copy(
                    symbol = custom.symbol,
                    name = custom.name,
                    decimals = custom.decimals,
                    source = TokenSource.MANUAL,
                    isCustom = true
                )
            } else {
                aggregated[key] = TokenAsset(
                    address = custom.address,
                    chain = Chain.SOLANA,
                    network = network,
                    symbol = custom.symbol,
                    name = custom.name,
                    decimals = custom.decimals,
                    balance = "0",
                    balanceInUsd = 0.0,
                    source = TokenSource.MANUAL,
                    isCustom = true
                )
            }
        }

        return aggregated.values.filter { token ->
            val isCustom = token.isCustom || customByMint.containsKey(token.address.lowercase())
            val balance = token.balance.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
            balance > java.math.BigDecimal.ZERO || isCustom
        }
    }

    private fun validateManual(metadata: TokenMetadata): TokenMetadata {
        val name = metadata.name.trim()
        val symbol = metadata.symbol.trim()
        val decimals = metadata.decimals
        if (name.isBlank() || symbol.isBlank() || decimals < 0 || decimals > 255) {
            throw IllegalStateException("Please provide valid token name, symbol, and decimals.")
        }
        return TokenMetadata(name, symbol, decimals)
    }
}
