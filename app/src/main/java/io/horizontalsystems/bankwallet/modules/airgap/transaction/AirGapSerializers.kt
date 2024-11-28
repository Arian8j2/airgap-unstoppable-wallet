package io.horizontalsystems.bankwallet.modules.airgap.transaction

import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.util.Base64

object ByteArrayBase64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "ByteArrayBase64Serializer",
        PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: ByteArray) {
        val base64String = Base64.getEncoder().encodeToString(value)
        encoder.encodeString(base64String)
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        val base64String = decoder.decodeString()
        return Base64.getDecoder().decode(base64String)
    }
}

@Serializable
data class SerializableGasPrice(
    val legacyGasPrice: Long? = null,
    val maxFeePerGas: Long? = null,
    val maxPriorityFeePerGas: Long? = null
) {
    companion object {
        fun fromGasPrice(gasPrice: GasPrice): SerializableGasPrice {
            return when (gasPrice) {
                is GasPrice.Legacy -> SerializableGasPrice(legacyGasPrice = gasPrice.legacyGasPrice)
                is GasPrice.Eip1559 -> SerializableGasPrice(
                    maxFeePerGas = gasPrice.maxFeePerGas,
                    maxPriorityFeePerGas = gasPrice.maxPriorityFeePerGas
                )
            }
        }
    }

    fun toGasPrice(): GasPrice {
        return if (legacyGasPrice == null) {
            GasPrice.Eip1559(maxFeePerGas!!, maxPriorityFeePerGas!!)
        } else {
            GasPrice.Legacy(legacyGasPrice)
        }
    }
}

object GasPriceSerializer : KSerializer<GasPrice> {
    private val serializer = SerializableGasPrice.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: GasPrice) =
        encoder.encodeSerializableValue(serializer, SerializableGasPrice.fromGasPrice(value))

    override fun deserialize(decoder: Decoder): GasPrice =
        decoder.decodeSerializableValue(serializer).toGasPrice()
}

//object TransactionDataSortModeSerializer : KSerializer<TransactionDataSortMode> {
//    override val descriptor: SerialDescriptor =
//        PrimitiveSerialDescriptor("TransactionDataSortModeSerializer", PrimitiveKind.STRING)
//
//    override fun serialize(encoder: Encoder, value: TransactionDataSortMode) =
//        encoder.encodeString(value.raw)
//
//    override fun deserialize(decoder: Decoder): TransactionDataSortMode =
//        TransactionDataSortMode.fromRaw(decoder.decodeString())!!
//}

object BlockchainTypeSerializer : KSerializer<BlockchainType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BlockchainTypeSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BlockchainType) =
        encoder.encodeString(value.uid)

    override fun deserialize(decoder: Decoder): BlockchainType {
        val type = BlockchainType.fromUid(decoder.decodeString())
        assert(type !is BlockchainType.Unsupported)
        return type
    }
}

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimalSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): BigDecimal = BigDecimal(decoder.decodeString())
}