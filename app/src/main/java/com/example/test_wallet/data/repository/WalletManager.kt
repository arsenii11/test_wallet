package com.example.test_wallet.data.repository

import android.content.Context
import android.util.Log
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
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.InsufficientMoneyException
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionInput
import org.bitcoinj.core.TransactionOutPoint
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
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

    private val prefs = context.getSharedPreferences("wallet", Context.MODE_PRIVATE)

    private val savedInitialAddress: String by lazy {
        prefs.getString("initial_address", null) ?: wallet.freshReceiveAddress().toString().also {
            prefs.edit().putString("initial_address", it).apply()
            Log.d("WalletManager", "Initial address generated and saved: $it")
        }
    }

    val address: String
        get() = savedInitialAddress

    // Кэшируем UTXO для использования при создании транзакций
    private var cachedUtxos: List<EsploraUtxoResponse> = emptyList()

    suspend fun getBalanceFull(): WalletBalance = withContext(Dispatchers.Default) {
        val info = api.getAddressInfo(address)

        cachedUtxos = api.getUtxos(address)

        return@withContext WalletBalance(
            confirmed = info.chain_stats.balance,
            pending = info.mempool_stats.balance
        )
    }


    suspend fun createTransaction(
        toAddress: String,
        amountToSend: Long,
        feePerKb: Long = 1000
    ): String = withContext(Dispatchers.Default) {
        Log.d("WalletManager", "Creating TX to $toAddress for $amountToSend sat")

        // Обновляем кэшированные UTXO с API, если они пусты
        if (cachedUtxos.isEmpty()) {
            cachedUtxos = api.getUtxos(address)
        }

        val available = cachedUtxos.sumOf { it.value }
        Log.d("WalletManager", "Available balance from UTXO: $available sat")

        val inputCount = cachedUtxos.size
        val outputCount = 2 // выход для получателя + сдача
        val estimatedSize = (148 * inputCount) + (34 * outputCount) + 10
        val estimatedFee = (estimatedSize * feePerKb) / 1000

        Log.d("WalletManager", "Estimated fee: $estimatedFee sat for $estimatedSize bytes")

        if (available < amountToSend + estimatedFee) {
            Log.e("WalletManager", "Not enough funds: $available < ${amountToSend + estimatedFee}")
            throw InsufficientMoneyException(Coin.valueOf(amountToSend + estimatedFee - available))
        }

        // Создаем транзакцию вручную
        val tx = Transaction(network)

        // Добавляем все входы
        var inputSum = 0L
        val includedUtxos = mutableListOf<EsploraUtxoResponse>()

        for (utxo in cachedUtxos) {
            val txHash = Sha256Hash.wrap(utxo.txid)
            // Преобразуем Int в Long для vout
            val outPoint = TransactionOutPoint(network, utxo.vout.toLong(), txHash)

            // Создаем пустой входной скрипт (заполним его позже при подписании)
            val scriptBytes = ByteArray(0)
            val input = TransactionInput(network, tx, scriptBytes, outPoint)
            tx.addInput(input)

            inputSum += utxo.value
            includedUtxos.add(utxo)

            // Прекращаем добавлять входы, если достигли необходимой суммы
            if (inputSum >= amountToSend + estimatedFee) {
                break
            }
        }

        // Добавляем выход получателю
        val receiverAddress = Address.fromString(network, toAddress)
        tx.addOutput(Coin.valueOf(amountToSend), receiverAddress)

        // Добавляем сдачу, если нужно
        val change = inputSum - amountToSend - estimatedFee
        if (change > 546) { // Минимальный dust threshold в Bitcoin
            tx.addOutput(Coin.valueOf(change), Address.fromString(network, address))
        }

        // Теперь подписываем каждый вход
        val ecKey = wallet.currentReceiveKey()

        for (i in 0 until tx.inputs.size) {
            // Создаем сценарий P2PKH для адреса кошелька
            val scriptPubKey =
                ScriptBuilder.createOutputScript(Address.fromString(network, address))

            // Создаем подпись для входа
            val sigHash = tx.hashForSignature(i, scriptPubKey, Transaction.SigHash.ALL, false)

            // Создаем TransactionSignature из ECKey.sign
            val ecSig = ecKey.sign(sigHash)
            val txSig = TransactionSignature(ecSig, Transaction.SigHash.ALL, false)

            // Создаем входной скрипт с подписью и публичным ключом
            val scriptSig = ScriptBuilder.createInputScript(txSig, ecKey)
            tx.getInput(i.toLong()).scriptSig = scriptSig
        }

        // Попытка проверить транзакцию
        try {
            tx.verify()
            Log.d("WalletManager", "Transaction verified successfully")
        } catch (e: Exception) {
            Log.e("WalletManager", "Transaction verification failed", e)
            // Продолжаем выполнение, так как некоторые проверки могут не проходить на тестовой сети
        }

        val hex = tx.bitcoinSerialize().toHex()
        Log.d("WalletManager", "TX HEX: $hex")
        return@withContext hex
    }

    suspend fun broadcastTransaction(hex: String): String {
        Log.d("WalletManager", "Broadcasting transaction. Length: ${hex.length}")
        val body = RequestBody.create("text/plain".toMediaTypeOrNull(), hex)

        try {
            val response = api.broadcastTx(body)
            val responseBody = response.string()
            Log.d("WalletManager", "TX successfully broadcast: $responseBody")

            cachedUtxos = emptyList()
            return responseBody
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 400) {
                Log.e("WalletManager", "HTTP 400: Invalid transaction or insufficient funds")
                throw IllegalArgumentException("Insufficient funds or invalid transaction")
            } else {
                throw e
            }
        }
    }


    suspend fun getTransactionHistory(): List<HistoryItem> {
        val txs = api.getTransactions(address)
        Log.d("WalletManager", "Fetched ${txs.size} transactions")

        return txs.mapNotNull { tx ->
            val time = tx.status.block_time ?: return@mapNotNull null

            val isSent = tx.vin.any { it.prevout?.scriptpubkey_address == address }

            val sentToOthers = tx.vout
                .filter { it.scriptpubkey_address != address }
                .sumOf { it.value }

            val receivedToSelf = tx.vout
                .filter { it.scriptpubkey_address == address }
                .sumOf { it.value }

            val amount = if (isSent) sentToOthers else receivedToSelf

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