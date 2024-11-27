package io.horizontalsystems.bankwallet.modules.airgap

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.math.BigDecimal

@Serializable
abstract class AirGapSignature {
    companion object {
        fun fromJson(input: String): AirGapSignature =
            getConfiguredJson().decodeFromString(input)
    }

    fun toJson(): String =
        getConfiguredJson().encodeToString(this)
}

@Serializable
abstract class AirGapTransaction : Parcelable {
    @Composable
    abstract fun ShowSigningConfirmationScreen(navController: NavController)

    abstract fun sign(): AirGapSignature
    abstract suspend fun publish(signature: AirGapSignature)

    abstract fun isAdapterAvailable(): Boolean
    abstract fun adapterName(): String

    fun toJson(): String =
        getConfiguredJson().encodeToString(this)


    override fun writeToParcel(dest: Parcel, flags: Int) =
        dest.writeString(toJson())

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        var CREATOR: Parcelable.Creator<AirGapTransaction> =
            object : Parcelable.Creator<AirGapTransaction> {
                override fun createFromParcel(parcel: Parcel): AirGapTransaction =
                    getConfiguredJson().decodeFromString(parcel.readString()!!)

                override fun newArray(size: Int): Array<AirGapTransaction?> =
                    arrayOfNulls(size)
            }

        fun fromJson(input: String): AirGapTransaction? =
            try {
                getConfiguredJson().decodeFromString<AirGapTransaction>(input)
            } catch (e: Exception) {
                null
            }
    }

}

private fun getConfiguredJson(): Json = Json {
    serializersModule = SerializersModule {
        contextual(ByteArray::class, ByteArrayBase64Serializer)
        contextual(GasPrice::class, GasPriceSerializer)
        contextual(BlockchainType::class, BlockchainTypeSerializer)
        contextual(BigDecimal::class, BigDecimalSerializer)
    }
}