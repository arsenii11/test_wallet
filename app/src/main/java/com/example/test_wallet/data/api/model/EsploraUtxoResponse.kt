package com.example.test_wallet.data.api.model

data class EsploraUtxoResponse(
    val txid: String,
    val vout: Int,
    val value: Long,
    val status: Status
)

data class Status(
    val confirmed: Boolean,
    val block_height: Int?,
    val block_hash: String?,
    val block_time: Long?
)
