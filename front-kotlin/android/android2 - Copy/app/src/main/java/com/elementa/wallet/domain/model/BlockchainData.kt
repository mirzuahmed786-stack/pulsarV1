package com.elementa.wallet.domain.model

data class BlockchainData(
    val chain: Chain,
    val name: String,
    val symbol: String,
    val logoUrl: String,
    val currentPrice: Double,
    val priceChangePercent24h: Double,
    val marketCap: Double,
    val volume24h: Double,
    val sparkline: List<Double> = emptyList(),
    val networkType: NetworkType = NetworkType.MAINNET
) {
    companion object {
        fun getData(): List<BlockchainData> {
            return listOf(
                BlockchainData(
                    chain = Chain.BITCOIN,
                    name = "Bitcoin",
                    symbol = "BTC",
                    logoUrl = "https://assets.coingecko.com/coins/images/1/large/bitcoin.png",
                    currentPrice = 45250.50,
                    priceChangePercent24h = 2.15,
                    marketCap = 890000000000.0,
                    volume24h = 28000000000.0,
                    sparkline = generateSparkline(45000.0, 45500.0)
                ),
                BlockchainData(
                    chain = Chain.ETHEREUM,
                    name = "Ethereum",
                    symbol = "ETH",
                    logoUrl = "https://assets.coingecko.com/coins/images/279/large/ethereum.png",
                    currentPrice = 2345.75,
                    priceChangePercent24h = 3.42,
                    marketCap = 281000000000.0,
                    volume24h = 15000000000.0,
                    sparkline = generateSparkline(2300.0, 2400.0)
                ),
                BlockchainData(
                    chain = Chain.SOLANA,
                    name = "Solana",
                    symbol = "SOL",
                    logoUrl = "https://assets.coingecko.com/coins/images/4128/large/solana.png",
                    currentPrice = 178.45,
                    priceChangePercent24h = 5.20,
                    marketCap = 75000000000.0,
                    volume24h = 3500000000.0,
                    sparkline = generateSparkline(170.0, 180.0)
                ),
                BlockchainData(
                    chain = Chain.BSC,
                    name = "BNB Chain",
                    symbol = "BNB",
                    logoUrl = "https://assets.coingecko.com/coins/images/825/large/bnb-icon2_2x.png",
                    currentPrice = 612.50,
                    priceChangePercent24h = 2.87,
                    marketCap = 93000000000.0,
                    volume24h = 2100000000.0,
                    sparkline = generateSparkline(600.0, 620.0)
                ),
                BlockchainData(
                    chain = Chain.POLYGON,
                    name = "Polygon",
                    symbol = "POL",
                    logoUrl = "https://assets.coingecko.com/coins/images/12171/large/polygon.png",
                    currentPrice = 0.642,
                    priceChangePercent24h = 4.15,
                    marketCap = 6200000000.0,
                    volume24h = 185000000.0,
                    sparkline = generateSparkline(0.61, 0.66)
                ),
                BlockchainData(
                    chain = Chain.AVALANCHE,
                    name = "Avalanche",
                    symbol = "AVAX",
                    logoUrl = "https://assets.coingecko.com/coins/images/12144/large/avalanche-2.png",
                    currentPrice = 36.85,
                    priceChangePercent24h = 1.90,
                    marketCap = 1300000000.0,
                    volume24h = 340000000.0,
                    sparkline = generateSparkline(35.0, 37.5)
                )
            )
        }

        private fun generateSparkline(min: Double, max: Double, points: Int = 24): List<Double> {
            return (0 until points).map { i ->
                val ratio = i.toDouble() / points
                min + (max - min) * (0.5 + 0.5 * kotlin.math.sin(ratio * Math.PI))
            }
        }
    }
}
