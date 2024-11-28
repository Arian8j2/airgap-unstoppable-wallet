package io.horizontalsystems.bankwallet.modules.airgap.transaction

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.title
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceEvm
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionView
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewItemFactory
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.horizontalsystems.ethereumkit.models.Signature
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.BigInteger

@Serializable
@SerialName("evm")
data class AirGapEvmTransaction(
    val blockchainType: @Contextual BlockchainType,
    val gasPrice: @Contextual GasPrice,
    val gasLimit: Long,
    val to: String,
    val value: String,
    val nonce: Long,
    val data: @Contextual ByteArray
) : AirGapTransaction() {
    @Composable
    override fun ShowSigningConfirmationScreen(navController: NavController) {
        val rawTransaction = toRawTransaction()
        SendEvmTransactionView(
            navController,
            items = craftItems(rawTransaction),
            cautions = listOf(),
            transactionFields = listOf(),
            networkFee = getNetworkFee(rawTransaction),
            statPage = StatPage.SendConfirmation
        )
    }

    private fun craftItems(rawTransaction: RawTransaction): List<SectionViewItem> {
        val blockchainType = blockchainType
        val transactionData = TransactionData(
            to = rawTransaction.to,
            value = rawTransaction.value,
            input = rawTransaction.data
        )

        val feeToken = App.evmBlockchainManager.getBaseToken(blockchainType)!!
        val coinServiceFactory = EvmCoinServiceFactory(
            feeToken,
            App.marketKit,
            App.currencyManager,
            App.coinManager
        )

        val sendEvmTransactionViewItemFactory = SendEvmTransactionViewItemFactory(
            App.evmLabelManager,
            coinServiceFactory,
            App.contactsRepository,
            blockchainType
        )
        val sendTransactionService = SendTransactionServiceEvm(blockchainType)
        val decoration = sendTransactionService.decorate(transactionData)
        return sendEvmTransactionViewItemFactory.getItems(
            transactionData,
            null,
            decoration
        )
    }

    private fun getNetworkFee(rawTransaction: RawTransaction): SendModule.AmountData {
        val blockchainType = blockchainType
        val feeToken = App.evmBlockchainManager.getBaseToken(blockchainType)!!
        val amount =
            BigInteger.valueOf(rawTransaction.gasPrice.max) * BigInteger.valueOf(rawTransaction.gasLimit)
        val amountWithDecimals = BigDecimal(amount, feeToken.decimals)
        val coinValue = CoinValue(feeToken, amountWithDecimals)
        return SendModule.AmountData(
            primary = SendModule.AmountInfo.CoinValueInfo(coinValue),
            secondary = null
        )
    }

    @SerialName("evm")
    @Serializable
    data class AirGapEvmSignature(
        val v: Int,
        val r: @Contextual ByteArray,
        val s: @Contextual ByteArray
    ) : AirGapSignature()

    override fun sign(): AirGapSignature {
        val evmKitWrapper =
            App.evmBlockchainManager.getEvmKitManager(blockchainType).evmKitWrapper!!
        val signature = evmKitWrapper.signer!!.signature(toRawTransaction())
        return AirGapEvmSignature(
            v = signature.v,
            s = signature.s,
            r = signature.r
        )
    }

    private fun toRawTransaction(): RawTransaction =
        RawTransaction(
            gasPrice = gasPrice,
            gasLimit = gasLimit,
            to = Address(to),
            value = value.toBigInteger(),
            nonce = nonce,
            data = data
        )

    override suspend fun publish(signature: AirGapSignature) {
        val evmSignature = signature as AirGapEvmSignature
        val signatureModel = Signature(
            v = evmSignature.v,
            r = evmSignature.r,
            s = evmSignature.s
        )
        SendTransactionServiceEvm(blockchainType).publishTransaction(
            toRawTransaction(),
            signatureModel
        )
    }

    override fun isAdapterAvailable(): Boolean =
        App.evmBlockchainManager.getEvmKitManager(blockchainType).evmKitWrapper != null

    override fun adapterName(): String = blockchainType.title
}