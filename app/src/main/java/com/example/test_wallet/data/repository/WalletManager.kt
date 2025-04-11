package com.example.test_wallet.data.repository

import android.content.Context
import com.example.test_wallet.data.api.EsploraApiService
import com.example.test_wallet.data.api.model.EsploraUtxoResponse
import com.example.test_wallet.data.network.SignetParams
import com.example.test_wallet.domain.model.HistoryItem
import com.example.test_wallet.domain.model.TxDirection
import com.example.test_wallet.domain.model.WalletBalance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.bitcoinj.core.*
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import java.io.File

class WalletManager(
    private val api: EsploraApiService,
    private val context: Context
) {
    private val network: NetworkParameters = SignetParams

    private val walletFile = File(context.filesDir, "wallet.dat")

    private val wallet: Wallet = if (walletFile.exists()) {
        Wallet.loadFromFile(walletFile)
    } else {
        Wallet.createDeterministic(SignetParams, Script.ScriptType.P2PKH).also {
            it.saveToFile(walletFile)
        }
    }


    val address: String
        get() = wallet.currentReceiveAddress().toString()

    suspend fun getUtxos(): List<EsploraUtxoResponse> {
        return api.getUtxos(address)
    }


    suspend fun getBalanceFull(): WalletBalance {
        val info = api.getAddressInfo(address)
        return WalletBalance(
            confirmed = info.chain_stats.balance,
            pending = info.mempool_stats.balance
        )
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

    suspend fun getTransactionHistory(): List<HistoryItem> {
        val txs = api.getTransactions(address)
        return txs.mapNotNull { tx ->
            val time = tx.status.block_time ?: return@mapNotNull null
            val isSent = tx.vin.any { it.prevout?.scriptpubkey_address == address }
            val amount = tx.vout
                .filter { it.scriptpubkey_address != address }
                .sumOf { it.value }

            HistoryItem(
                txid = tx.txid,
                timestamp = time,
                direction = if (isSent) TxDirection.Sent else TxDirection.Received,
                amountSat = amount
            )
        }
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }
}
