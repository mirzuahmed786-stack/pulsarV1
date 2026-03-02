package com.elementa.wallet.data.config

import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType

object RpcConfig {
    private val rpcUrls: Map<Chain, Map<NetworkType, List<String>>> = mapOf(
        Chain.ETHEREUM to mapOf(
            NetworkType.TESTNET to listOf("https://rpc.sepolia.org", "https://sepolia.drpc.org"),
            NetworkType.MAINNET to listOf("https://cloudflare-eth.com", "https://eth.llamarpc.com")
        ),
        Chain.BSC to mapOf(
            NetworkType.TESTNET to listOf(
                "https://data-seed-prebsc-1-s1.binance.org:8545",
                "https://bsc-testnet.drpc.org",
                "https://bsc-testnet.publicnode.com"
            ),
            NetworkType.MAINNET to listOf(
                "https://binance.llamarpc.com",
                "https://bsc-dataseed.binance.org"
            )
        ),
        Chain.POLYGON to mapOf(
            NetworkType.TESTNET to listOf(
                "https://rpc-amoy.polygon.technology",
                "https://polygon-amoy.drpc.org",
                "https://polygon-amoy-bor-rpc.publicnode.com"
            ),
            NetworkType.MAINNET to listOf(
                "https://polygon-rpc.com",
                "https://polygon.llamarpc.com",
                "https://polygon-bor-rpc.publicnode.com"
            )
        ),
        Chain.AVALANCHE to mapOf(
            NetworkType.TESTNET to listOf(
                "https://api.avax-test.network/ext/bc/C/rpc",
                "https://avalanche-fuji.drpc.org",
                "https://avalanche-fuji-c-chain-rpc.publicnode.com"
            ),
            NetworkType.MAINNET to listOf(
                "https://avalanche.llamarpc.com",
                "https://api.avax.network/ext/bc/C/rpc"
            )
        ),
        Chain.LOCALHOST to mapOf(
            NetworkType.TESTNET to listOf("http://127.0.0.1:8545"),
            NetworkType.MAINNET to listOf("http://127.0.0.1:8545")
        )
    )

    fun getRpcUrls(chain: Chain, network: NetworkType): List<String> {
        return rpcUrls[chain]?.get(network).orEmpty()
    }
}
