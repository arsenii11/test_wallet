package com.example.test_wallet.data.api.model

data class EsploraAddressInfo(
    val address: String,
    val chain_stats: ChainStats,
    val mempool_stats: ChainStats
)

data class ChainStats(
    val funded_txo_sum: Long,
    val spent_txo_sum: Long
) {
    val balance: Long get() = funded_txo_sum - spent_txo_sum
}