package com.example.test_wallet.domain.model

data class WalletBalance(
    val confirmed: Long,
    val pending: Long
)