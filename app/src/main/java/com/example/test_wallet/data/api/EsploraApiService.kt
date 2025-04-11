package com.example.test_wallet.data.api

import com.example.test_wallet.data.api.model.EsploraAddressInfo
import com.example.test_wallet.data.api.model.EsploraTxResponse
import com.example.test_wallet.data.api.model.EsploraUtxoResponse
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface EsploraApiService {
    @GET("address/{address}/utxo")
    suspend fun getUtxos(@Path("address") address: String): List<EsploraUtxoResponse>

    @GET("address/{address}")
    suspend fun getAddressInfo(@Path("address") address: String): EsploraAddressInfo

    @POST("tx")
    suspend fun broadcastTx(@Body txHex: RequestBody): ResponseBody

    @GET("address/{address}/txs")
    suspend fun getTransactions(@Path("address") address: String): List<EsploraTxResponse>
}
