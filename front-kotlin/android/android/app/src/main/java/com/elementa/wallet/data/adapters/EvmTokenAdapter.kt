package com.elementa.wallet.data.adapters

import com.elementa.wallet.data.config.EvmConfig
import com.elementa.wallet.data.storage.StoredEvmToken
import com.elementa.wallet.data.storage.TokenStorage
import com.elementa.wallet.domain.model.AddTokenRequest
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.domain.model.TokenAsset
import com.elementa.wallet.domain.model.TokenMetadata
import com.elementa.wallet.domain.model.TokenSource
import com.elementa.wallet.domain.model.TokenStandard
import com.elementa.wallet.rpc.EvmRpc
import com.elementa.wallet.rpc.RpcException
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvmTokenAdapter @Inject constructor(
    private val storage: TokenStorage,
    private val rpc: EvmRpc
) {
    suspend fun addCustomToken(request: AddTokenRequest): TokenAsset {
        require(request.chain.isEvm) { "Invalid chain" }
        val address = request.address
        if (!isValidEvmAddress(address)) {
            throw IllegalArgumentException("Invalid address")
        }

        val existing = storage.getCustomEvmTokens(request.walletScope, request.chain, request.network)
        if (existing.any { it.address.equals(address, ignoreCase = true) }) {
            throw IllegalStateException("This token has already been imported.")
        }

        val common = EvmConfig.getCommonTokens(request.chain, request.network)
        val matchCommon = common.firstOrNull { it.address.equals(address, ignoreCase = true) }
        if (matchCommon != null) {
            val watched = storage.getWatchedDefaultTokens(request.walletScope, request.chain, request.network)
            val next = (watched + address).distinct()
            storage.saveWatchedDefaultTokens(request.walletScope, request.chain, request.network, next)
            return TokenAsset(
                address = matchCommon.address,
                chain = request.chain,
                network = request.network,
                symbol = matchCommon.symbol,
                name = matchCommon.name,
                decimals = matchCommon.decimals,
                balance = "0",
                balanceInUsd = 0.0,
                source = matchCommon.source ?: TokenSource.KNOWN_ADDRESS,
                isCustom = false,
                isWatchedDefault = true,
                tokenStandard = matchCommon.tokenStandard,
                tokenId = matchCommon.tokenId
            )
        }

        if (request.network == NetworkType.TESTNET && request.manualMetadata == null) {
            throw IllegalStateException("Testnet token imports require manual metadata.")
        }

        val bytecode = rpc.getBytecode(request.chain, request.network, address)
        if (bytecode.isBlank() || bytecode == "0x") {
            throw IllegalStateException("Token contract not found on ${Chain.toWire(request.chain)} ${NetworkType.toWire(request.network)}. Check the network mode.")
        }

        val erc20Ok = try {
            rpc.validateErc20Interface(request.chain, request.network, address)
        } catch (error: Throwable) {
            val message = error.message.orEmpty()
            if (message.contains("rpc", true) || message.contains("timeout", true)) {
                throw IllegalStateException("RPC provider is temporarily unavailable while verifying this token. Please retry in a few seconds.")
            }
            throw error
        }
        if (!erc20Ok) {
            throw IllegalStateException("This contract does not implement the ERC-20 token standard. Only ERC-20 tokens can be imported.")
        }

        val stored = if (request.manualMetadata != null) {
            resolveManualToken(request, existing)
        } else {
            resolveAutoToken(request, existing)
        }

        return TokenAsset(
            address = stored.address,
            chain = request.chain,
            network = request.network,
            symbol = stored.symbol,
            name = stored.name,
            decimals = stored.decimals,
            balance = "0",
            balanceInUsd = 0.0,
            source = stored.source,
            isCustom = true,
            isWatchedDefault = false,
            tokenStandard = stored.tokenStandard,
            tokenId = stored.tokenId
        )
    }

    suspend fun removeCustomToken(scope: String, chain: Chain, network: NetworkType, address: String) {
        val existing = storage.getCustomEvmTokens(scope, chain, network)
        val updated = existing.filterNot { it.address.equals(address, ignoreCase = true) }
        storage.saveCustomEvmTokens(scope, chain, network, updated)
        val watched = storage.getWatchedDefaultTokens(scope, chain, network)
        val next = watched.filterNot { it.equals(address, ignoreCase = true) }
        storage.saveWatchedDefaultTokens(scope, chain, network, next)
    }

    suspend fun getPortfolio(scope: String, chain: Chain, network: NetworkType, walletAddress: String): List<TokenAsset> {
        val common = EvmConfig.getCommonTokens(chain, network)
        val custom = storage.getCustomEvmTokens(scope, chain, network)
        val watched = storage.getWatchedDefaultTokens(scope, chain, network).toSet()
        val combined = (common.map { it.address.lowercase() } + custom.map { it.address.lowercase() })
            .distinct()
            .mapNotNull { addr ->
                custom.firstOrNull { it.address.equals(addr, true) }
                    ?: common.firstOrNull { it.address.equals(addr, true) }
            }

        val results = mutableListOf<TokenAsset>()
        for (token in combined) {
            val upgraded = if (token is StoredEvmToken && token.source == TokenSource.MANUAL) {
                attemptMetadataUpgrade(scope, chain, network, token)
            } else {
                token
            }
            val isCustom = upgraded is StoredEvmToken
            val isWatchedDefault = when (upgraded) {
                is StoredEvmToken -> watched.contains(upgraded.address.lowercase())
                is EvmConfig.CommonToken -> watched.contains(upgraded.address.lowercase())
                else -> throw IllegalStateException("Unsupported token type")
            }
            val balance = fetchBalance(walletAddress, chain, network, upgraded)
            val asset = when (upgraded) {
                is StoredEvmToken -> TokenAsset(
                    address = upgraded.address,
                    chain = chain,
                    network = network,
                    symbol = upgraded.symbol,
                    name = upgraded.name,
                    decimals = upgraded.decimals,
                    balance = balance,
                    balanceInUsd = 0.0,
                    source = upgraded.source,
                    isCustom = isCustom,
                    isWatchedDefault = isWatchedDefault,
                    tokenStandard = upgraded.tokenStandard,
                    tokenId = upgraded.tokenId
                )
                is EvmConfig.CommonToken -> TokenAsset(
                    address = upgraded.address,
                    chain = chain,
                    network = network,
                    symbol = upgraded.symbol,
                    name = upgraded.name,
                    decimals = upgraded.decimals,
                    balance = balance,
                    balanceInUsd = 0.0,
                    source = upgraded.source ?: TokenSource.KNOWN_ADDRESS,
                    isCustom = isCustom,
                    isWatchedDefault = isWatchedDefault,
                    tokenStandard = upgraded.tokenStandard,
                    tokenId = upgraded.tokenId
                )
                else -> throw IllegalStateException("Unsupported token type")
            }
            val balanceValue = balance.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
            if (balanceValue > java.math.BigDecimal.ZERO || isCustom || isWatchedDefault) {
                results.add(asset)
            }
        }

        // Include native chain asset (ETH, AVAX, MATIC, BNB) as a synthetic TokenAsset so dashboards can show native balances
        try {
            val nativeBalanceBig = rpc.getNativeBalance(chain, network, walletAddress)
            val nativeBalanceStr = formatUnits(nativeBalanceBig, 18)
            val nativeSymbol = when (chain) {
                Chain.ETHEREUM -> "ETH"
                Chain.BSC -> "BNB"
                Chain.AVALANCHE -> "AVAX"
                Chain.POLYGON -> "MATIC"
                Chain.LOCALHOST -> "ETH"
                else -> ""
            }
            if (nativeSymbol.isNotBlank()) {
                val nativeAsset = TokenAsset(
                    address = "",
                    chain = chain,
                    network = network,
                    symbol = nativeSymbol,
                    name = nativeSymbol,
                    decimals = 18,
                    balance = nativeBalanceStr,
                    balanceInUsd = 0.0,
                    source = TokenSource.KNOWN_ADDRESS,
                    isCustom = false,
                    isWatchedDefault = false,
                    tokenStandard = TokenStandard.ERC20,
                    tokenId = null
                )
                // Add native asset to front so it appears as the primary token
                results.add(0, nativeAsset)
            }
        } catch (_: Throwable) {
            // Ignore native balance failures; leave results as-is
        }
        return results
    }

    private suspend fun resolveManualToken(
        request: AddTokenRequest,
        existing: List<StoredEvmToken>
    ): StoredEvmToken {
        val manual = request.manualMetadata ?: throw IllegalStateException("Missing manual metadata")
        val validated = validateManual(manual)
        var resolved = validated
        var resolvedSource = TokenSource.MANUAL

        val metaResults = fetchOnChainMetadata(request.chain, request.network, request.address)
        val hasOnChainMeta = metaResults.metadata != null

        if (request.network == NetworkType.MAINNET) {
            if (!hasOnChainMeta) {
                throw if (metaResults.error?.let { isRpcInfraFailure(it) } == true) {
                    IllegalStateException("RPC provider is temporarily unavailable while verifying this token. Please retry in a few seconds.")
                } else {
                    IllegalStateException("Unable to verify on-chain token metadata on mainnet.")
                }
            }
            val onChain = metaResults.metadata
            if (onChain != null && !metadataMatches(onChain, validated)) {
                throw IllegalStateException("Manual metadata does not match on-chain token details.")
            }
            resolved = onChain ?: validated
            resolvedSource = TokenSource.ON_CHAIN
        } else if (hasOnChainMeta) {
            resolved = metaResults.metadata ?: validated
            resolvedSource = TokenSource.ON_CHAIN
        }

        val newToken = StoredEvmToken(
            address = request.address,
            symbol = resolved.symbol,
            name = resolved.name,
            decimals = resolved.decimals,
            chain = Chain.toWire(request.chain),
            network = NetworkType.toWire(request.network),
            isVerified = false,
            source = resolvedSource
        )

        storage.saveCustomEvmTokens(request.walletScope, request.chain, request.network, existing + newToken)
        return newToken
    }

    private suspend fun resolveAutoToken(
        request: AddTokenRequest,
        existing: List<StoredEvmToken>
    ): StoredEvmToken {
        val known = EvmConfig.knownAddresses[request.address.lowercase()]
        val metaResults = fetchOnChainMetadata(request.chain, request.network, request.address)
        val hasOnChain = metaResults.metadata != null

        val name = metaResults.metadata?.name ?: known?.name ?: "Unknown Token (${request.address.take(6)})"
        val symbol = metaResults.metadata?.symbol ?: known?.symbol ?: "???"
        val decimals = metaResults.metadata?.decimals ?: known?.decimals ?: 18

        val source = when {
            metaResults.metadata != null -> TokenSource.ON_CHAIN
            known != null -> TokenSource.KNOWN_ADDRESS
            else -> TokenSource.MANUAL
        }

        if (request.network == NetworkType.MAINNET) {
            val hasOnChainData = metaResults != null
            val nameOk = name.isNotBlank() && !name.startsWith("Unknown Token")
            val symbolOk = symbol.isNotBlank() && symbol != "???"
            if (known == null && metaResults.error?.let { isRpcInfraFailure(it) } == true) {
                throw IllegalStateException("RPC provider is temporarily unavailable while verifying this token. Please retry in a few seconds.")
            }
            if (known == null && (!hasOnChainData || !nameOk || !symbolOk)) {
                throw IllegalStateException("Unable to verify token metadata on mainnet.")
            }
        }

        val newToken = StoredEvmToken(
            address = request.address,
            symbol = symbol,
            name = name,
            decimals = decimals,
            chain = Chain.toWire(request.chain),
            network = NetworkType.toWire(request.network),
            isVerified = hasOnChain || known != null,
            source = source
        )

        storage.saveCustomEvmTokens(request.walletScope, request.chain, request.network, existing + newToken)
        return newToken
    }

    private suspend fun attemptMetadataUpgrade(
        scope: String,
        chain: Chain,
        network: NetworkType,
        token: StoredEvmToken
    ): StoredEvmToken {
        val metadata = fetchOnChainMetadata(chain, network, token.address).metadata ?: return token
        val updated = token.copy(
            name = metadata.name,
            symbol = metadata.symbol,
            decimals = metadata.decimals,
            source = TokenSource.ON_CHAIN
        )
        val existing = storage.getCustomEvmTokens(scope, chain, network)
        val next = existing.map { if (it.address.equals(token.address, true)) updated else it }
        storage.saveCustomEvmTokens(scope, chain, network, next)
        return updated
    }

    private suspend fun fetchOnChainMetadata(chain: Chain, network: NetworkType, address: String): MetadataResult {
        return try {
            val name = rpc.erc20Name(chain, network, address)
            val symbol = rpc.erc20Symbol(chain, network, address)
            val decimals = rpc.erc20Decimals(chain, network, address)
            MetadataResult(TokenMetadata(name.trim(), symbol.trim(), decimals), null)
        } catch (error: Throwable) {
            MetadataResult(null, error)
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

    private fun metadataMatches(a: TokenMetadata, b: TokenMetadata): Boolean {
        return a.name.equals(b.name, true)
            && a.symbol.equals(b.symbol, true)
            && a.decimals == b.decimals
    }

    private fun isRpcInfraFailure(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: ""
        return message.contains("too many requests")
            || message.contains("429")
            || message.contains("unauthorized")
            || message.contains("timeout")
            || message.contains("gateway")
            || message.contains("rpc")
    }

    private data class MetadataResult(
        val metadata: TokenMetadata?,
        val error: Throwable?
    )

    private suspend fun fetchBalance(
        owner: String,
        chain: Chain,
        network: NetworkType,
        token: Any
    ): String {
        return try {
            when (token) {
                is StoredEvmToken -> {
                    if (token.tokenStandard == TokenStandard.ERC1155 && token.tokenId != null) {
                        formatUnits(rpc.erc1155BalanceOf(chain, network, token.address, owner, token.tokenId), token.decimals)
                    } else {
                        formatUnits(rpc.erc20BalanceOf(chain, network, token.address, owner), token.decimals)
                    }
                }
                is EvmConfig.CommonToken -> {
                    if (token.tokenStandard == TokenStandard.ERC1155 && token.tokenId != null) {
                        formatUnits(rpc.erc1155BalanceOf(chain, network, token.address, owner, token.tokenId), token.decimals)
                    } else {
                        formatUnits(rpc.erc20BalanceOf(chain, network, token.address, owner), token.decimals)
                    }
                }
                else -> "0"
            }
        } catch (error: Throwable) {
            if (token is StoredEvmToken) {
                "0"
            } else {
                throw RpcException("Failed to fetch token balance", error)
            }
        }
    }

    private fun formatUnits(value: BigInteger, decimals: Int): String {
        if (decimals <= 0) return value.toString()
        val divisor = BigInteger.TEN.pow(decimals)
        val integerPart = value.divide(divisor)
        val fractionalPart = value.mod(divisor).toString().padStart(decimals, '0')
        val trimmedFraction = fractionalPart.trimEnd('0')
        return if (trimmedFraction.isEmpty()) {
            integerPart.toString()
        } else {
            "${integerPart}.${trimmedFraction}"
        }
    }

    private fun isValidEvmAddress(address: String): Boolean {
        return Regex("^0x[a-fA-F0-9]{40}$").matches(address)
    }
}
