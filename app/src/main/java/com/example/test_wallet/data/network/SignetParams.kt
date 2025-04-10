package com.example.test_wallet.data.network

import org.bitcoinj.params.TestNet3Params

object SignetParams : TestNet3Params() {
    init {
        id = ID_TESTNET
        packetMagic = 0x0a03cf40L
    }

    override fun getId(): String = ID_TESTNET
}
