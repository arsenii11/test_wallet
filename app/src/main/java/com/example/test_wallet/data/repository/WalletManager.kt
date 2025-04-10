package com.example.test_wallet.data.repository

import com.example.test_wallet.data.api.EsploraApiService
import com.example.test_wallet.data.api.model.EsploraUtxoResponse
import com.example.test_wallet.data.network.SignetParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.bitcoinj.core.*
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet

class WalletManager(
    private val api: EsploraApiService
) {
    private val network: NetworkParameters = SignetParams
    private val wallet: Wallet = Wallet.createDeterministic(
        network,
        Script.ScriptType.P2PKH
    )

    val address: String
        get() = wallet.freshReceiveAddress().toString()

    suspend fun getUtxos(): List<EsploraUtxoResponse> {
        return api.getUtxos(address)
    }

    suspend fun getBalance(): Long {
        val utxos = getUtxos()
        return utxos.sumOf { it.value }
    }

    suspend fun createTransaction(
        toAddress: String,
        amountToSend: Long,
        fee: Long = 100 // 0.000001 tBTC
    ): String = withContext(Dispatchers.Default) {
        val utxos = getUtxos()
        val totalInput = utxos.sumOf { it.value }

        if (totalInput < amountToSend + fee) {
            throw IllegalArgumentException("Insufficient funds")
        }

        val tx = Transaction(network)
        var accumulated = 0L

        utxos.forEach { utxo ->
            val outPoint = TransactionOutPoint(network, utxo.vout.toLong(), Sha256Hash.wrap(utxo.txid))
            val script = ScriptBuilder.createOutputScript(wallet.currentReceiveAddress())
            tx.addInput(TransactionInput(network, null, script.program, outPoint))
            accumulated += utxo.value
            if (accumulated >= amountToSend + fee) return@forEach
        }

        tx.addOutput(Coin.valueOf(amountToSend), Address.fromString(network, toAddress))

        val change = accumulated - amountToSend - fee
        if (change > 0) {
            tx.addOutput(Coin.valueOf(change), wallet.currentReceiveAddress())
        }

        wallet.signTransaction(SendRequest.forTx(tx)) // Подписываем

        tx.unsafeBitcoinSerialize().toHex() // Возвращаем hex-транзакцию
    }


    suspend fun broadcastTransaction(hex: String): String {
        val body = RequestBody.create("text/plain".toMediaTypeOrNull(), hex)
        val response = api.broadcastTx(body)
        return response.string()
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }
}
