package com.example.test_wallet.domain.model

data class HistoryItem(
    val txid: String,
    val timestamp: Long,
    val direction: TxDirection,
    val amountSat: Long
)

enum class TxDirection { Sent, Received }
