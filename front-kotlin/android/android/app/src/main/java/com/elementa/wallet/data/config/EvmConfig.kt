package com.elementa.wallet.data.config

import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.domain.model.TokenSource
import com.elementa.wallet.domain.model.TokenStandard

object EvmConfig {
    data class CommonToken(
        val address: String,
        val symbol: String,
        val name: String,
        val decimals: Int,
        val source: TokenSource? = null,
        val tokenStandard: TokenStandard = TokenStandard.ERC20,
        val tokenId: Long? = null
    )

    val knownAddresses: Map<String, CommonToken> = mapOf(
        "0xdac17f958d2ee523a2206206994597c13d831ec7" to CommonToken(
            "0xdac17f958d2ee523a2206206994597c13d831ec7", "USDT", "Tether USD", 6
        ),
        "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48" to CommonToken(
            "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", "USDC", "USD Coin", 6
        ),
        "0x1f9840a85d5af5bf1d1762f925bdaddc4201f984" to CommonToken(
            "0x1f9840a85d5af5bf1d1762f925bdaddc4201f984", "UNI", "Uniswap", 18
        ),
        "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359" to CommonToken(
            "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359", "USDC", "USD Coin", 6
        ),
        "0x2791bca1f2de4661ed88a30c99a7a9449aa84174" to CommonToken(
            "0x2791bca1f2de4661ed88a30c99a7a9449aa84174", "USDC.e", "USD Coin (PoS)", 6
        ),
        "0x77114023342d64731885cc0d603e9114d643851b" to CommonToken(
            "0x77114023342d64731885cc0d603e9114d643851b", "USDT", "Tether USD (Test)", 6
        ),
        "0x2260fac5e5542a773aa44fbcfedf7c193bc2c599" to CommonToken(
            "0x2260fac5e5542a773aa44fbcfedf7c193bc2c599", "WBTC", "Wrapped Bitcoin", 8
        )
    )

    val commonTokens: Map<Chain, Map<NetworkType, List<CommonToken>>> = mapOf(
        Chain.ETHEREUM to mapOf(
            NetworkType.TESTNET to listOf(
                CommonToken("0x77114023342d64731885cc0d603e9114d643851b", "USDT", "Tether USD", 6),
                CommonToken("0x1c7d4b196cb0c7b01d743fbc6116a902379c7238", "USDC", "USD Coin", 6),
                CommonToken("0x326c977e6efc84e512bb9c30f76e30c160ed06fb", "LINK", "Chainlink", 18)
            ),
            NetworkType.MAINNET to listOf(
                CommonToken("0xdac17f958d2ee523a2206206994597c13d831ec7", "USDT", "Tether USD", 6),
                CommonToken("0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", "USDC", "USD Coin", 6),
                CommonToken("0x1f9840a85d5af5bf1d1762f925bdaddc4201f984", "UNI", "Uniswap", 18)
            )
        ),
        Chain.BSC to mapOf(
            NetworkType.TESTNET to listOf(
                CommonToken("0x337610d27c2423e9f433ad70730d979593735549", "USDT", "Tether USD", 18),
                CommonToken("0x64544969ed7eb0db94199c27c24a298815776a3b", "USDC", "USD Coin", 18)
            ),
            NetworkType.MAINNET to listOf(
                CommonToken("0x55d398326f99059ff775485246999027b3197955", "USDT", "Tether USD", 18)
            )
        ),
        Chain.POLYGON to mapOf(
            NetworkType.TESTNET to listOf(
                CommonToken("0x41e94eb019c0762f9bfcf9fb1e58725610929766", "USDT", "Tether USD", 6)
            ),
            NetworkType.MAINNET to listOf(
                CommonToken("0x3c499c542cef5e3811e1192ce70d8cc03d5c3359", "USDC", "USD Coin", 6),
                CommonToken("0x2791bca1f2de4661ed88a30c99a7a9449aa84174", "USDC.e", "USD Coin (PoS)", 6),
                CommonToken("0xc2132d05d31c914a87c6611c10748aeb04b58e8f", "USDT", "Tether USD", 6)
            )
        ),
        Chain.AVALANCHE to mapOf(
            NetworkType.TESTNET to listOf(
                CommonToken("0xab971664d4b260938ff759535f2a1b9454f05646", "USDT", "Tether USD", 6)
            ),
            NetworkType.MAINNET to listOf(
                CommonToken("0x9702230a8ea53601f5cd2dc00fdbc13d4df4a8c7", "USDT", "Tether USD", 6)
            )
        ),
        Chain.LOCALHOST to mapOf(
            NetworkType.TESTNET to listOf(
                CommonToken("0x5fbdb2315678afecb367f032d93f642f64180aa3", "EATH", "Earth Token", 18),
                CommonToken("0xe7f1725e7734ce288f8367e1bb143e90bb3f0512", "Ag", "Silver", 0, tokenStandard = TokenStandard.ERC1155, tokenId = 1),
                CommonToken("0xe7f1725e7734ce288f8367e1bb143e90bb3f0512", "Au", "Gold", 0, tokenStandard = TokenStandard.ERC1155, tokenId = 2),
                CommonToken("0xe7f1725e7734ce288f8367e1bb143e90bb3f0512", "Pt", "Platinum", 0, tokenStandard = TokenStandard.ERC1155, tokenId = 3),
                CommonToken("0xe7f1725e7734ce288f8367e1bb143e90bb3f0512", "U", "Uranium", 0, tokenStandard = TokenStandard.ERC1155, tokenId = 4),
                CommonToken("0xe7f1725e7734ce288f8367e1bb143e90bb3f0512", "D", "Diamond", 0, tokenStandard = TokenStandard.ERC1155, tokenId = 5)
            ),
            NetworkType.MAINNET to emptyList()
        )
    )

    fun getCommonTokens(chain: Chain, network: NetworkType): List<CommonToken> {
        return commonTokens[chain]?.get(network).orEmpty()
    }
}
