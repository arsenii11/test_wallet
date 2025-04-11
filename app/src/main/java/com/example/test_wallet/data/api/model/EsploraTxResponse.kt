package com.example.test_wallet.data.api.model

data class EsploraTxResponse(
    val txid: String,
    val status: EsploraStatus,
    val vin: List<EsploraVin>,
    val vout: List<EsploraVout>
)

data class EsploraStatus(
    val confirmed: Boolean,
    val block_time: Long? // UNIX timestamp
)

data class EsploraVin(
    val prevout: EsploraVout?
)

data class EsploraVout(
    val scriptpubkey_address: String,
    val value: Long // in satoshis
)
