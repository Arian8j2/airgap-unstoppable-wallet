package io.horizontalsystems.bankwallet.modules.send.evm

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigInteger

data class EvmRawTransactionData(
    val rawTransaction: RawTransaction,
    val blockchainType: BlockchainType
) {
    fun toJson(): String {
        val json = JsonObject()
        json.add("rawTransaction", Gson().toJsonTree(rawTransaction))
        json.addProperty("blockchainType", blockchainType.uid)
        return json.toString()
    }

    companion object {
        fun fromJson(input: String): EvmRawTransactionData? {
            return try {
                val json = JsonParser.parseString(input).asJsonObject
                val rawTransactionObject = json.get("rawTransaction").asJsonObject
                val gasPriceObject = rawTransactionObject.get("gasPrice").asJsonObject
                val legacyGasPriceElement = gasPriceObject.get("legacyGasPrice")
                val gasPrice = if (legacyGasPriceElement == null) {
                    GasPrice.Eip1559(
                        maxFeePerGas = gasPriceObject.get("maxFeePerGas").asLong,
                        maxPriorityFeePerGas = gasPriceObject.get("maxPriorityFeePerGas").asLong
                    )
                } else {
                    GasPrice.Legacy(
                        legacyGasPrice = legacyGasPriceElement.asLong
                    )
                }

                // i could just make GasPrice serializable and make my life easier
                // but i didn't want to touch ethereum kit
                val gson = Gson()
                val rawTransaction = RawTransaction(
                    gasPrice = gasPrice,
                    gasLimit = gson.fromJson(
                        rawTransactionObject.get("gasLimit"),
                        Long::class.java
                    ),
                    to = gson.fromJson(rawTransactionObject.get("to"), Address::class.java),
                    value = gson.fromJson(
                        rawTransactionObject.get("value"),
                        BigInteger::class.java
                    ),
                    nonce = gson.fromJson(rawTransactionObject.get("nonce"), Long::class.java),
                    data = gson.fromJson(rawTransactionObject.get("data"), ByteArray::class.java),
                )
                val blockchainType = BlockchainType.fromUid(json.get("blockchainType").asString)
                EvmRawTransactionData(
                    rawTransaction,
                    blockchainType
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
