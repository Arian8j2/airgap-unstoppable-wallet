package io.horizontalsystems.bankwallet.modules.airgap.transaction

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationScreen
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.utils.AddressConverterChain
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.SegwitAddressConverter
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@SerialName("bitcoin")
@Serializable
data class AirGapBitcoinTransaction(
    val amount: @Contextual BigDecimal,
    val to: String,
    val feeRate: Int,
    val unspentOutputs: List<SerializedUnspentOutput>,
    val sortingType: TransactionDataSortMode,
    val rbfEnabled: Boolean
) : AirGapTransaction() {

    private fun getMappedUnspentOutputs(): List<UnspentOutput> {
        val addressConverter = getBtcMainNetAddressConverter()
        val adapter = getBitcoinAdapter()
        return unspentOutputs.mapIndexed { index, it ->
            val trxOutput = TransactionOutput()
            trxOutput.value = it.value
            trxOutput.address = it.address
            trxOutput.transactionHash = it.txHash
            trxOutput.index = index
            val addressInfo = addressConverter.convert(it.address)
            trxOutput.lockingScript = addressInfo.lockingScript
            trxOutput.lockingScriptPayload = addressInfo.lockingScriptPayload
            trxOutput.scriptType = addressInfo.scriptType
            UnspentOutput(
                output = trxOutput,
                publicKey = adapter.getPublicKey(),
                transaction = Transaction(),
                block = null
            )
        }
    }

    @Composable
    override fun ShowSigningConfirmationScreen(navController: NavController) {
        val wallet = getCurrentBitcoinWallet()
        val adapter = getBitcoinAdapter()
        val outputs = getMappedUnspentOutputs()
        val feeInfo = adapter.bitcoinFeeInfoWithSpecificOutputs(
            amount,
            feeRate,
            to,
            memo = null,
            unspentOutputs = outputs,
            pluginData = null
        )!!
        SendConfirmationScreen(
            navController = navController,
            coinMaxAllowedDecimals = wallet.token.decimals,
            feeCoinMaxAllowedDecimals = wallet.token.decimals,
            amountInputType = AmountInputType.COIN,
            rate = null,
            feeCoinRate = null,
            sendResult = null,
            blockchainType = BlockchainType.Bitcoin,
            coin = wallet.coin,
            feeCoin = wallet.coin,
            amount = amount,
            address = Address(to),
            contact = null,
            fee = feeInfo.fee,
            lockTimeInterval = null,
            memo = null,
            rbfEnabled = rbfEnabled,
            onClickSend = {},
            sendEntryPointDestId = 0,
            isScreen = false
        )
    }

    @SerialName("bitcoin")
    @Serializable
    data class AirGapBitcoinSignature(
        val isSegwit: Boolean,
        val inputSignatures: List<InputSignature>,
        val outputs: List<Output>
    ) : AirGapSignature()

    override fun sign(): AirGapSignature {
        val adapter = getBitcoinAdapter()
        val signedTransaction = adapter.sign(
            amount = amount,
            address = to,
            memo = null,
            feeRate = feeRate,
            unspentOutputs = getMappedUnspentOutputs(),
            pluginData = null,
            transactionSorting = sortingType,
            rbfEnabled = rbfEnabled,
        )
        return extractSignature(signedTransaction)
    }

    private fun extractSignature(transaction: FullTransaction): AirGapSignature {
        val inputs = transaction.inputs.map {
            InputSignature(
                witness = it.witness,
                sigScript = it.sigScript
            )
        }
        val outputs = transaction.outputs.map {
            Output(
                amount = it.value,
                address = it.address!!
            )
        }
        return AirGapBitcoinSignature(
            isSegwit = transaction.header.segwit,
            inputSignatures = inputs,
            outputs = outputs
        )
    }

    override suspend fun publish(signature: AirGapSignature) {
        val btcSignature = signature as AirGapBitcoinSignature
        val mutableTransaction = MutableTransaction()
        val network = MainNet()
        val addressConverter = AddressConverterChain().apply {
            prependConverter(SegwitAddressConverter(network.addressSegwitHrp))
            prependConverter(
                Base58AddressConverter(
                    network.addressVersion,
                    network.addressScriptVersion
                )
            )
        }

        unspentOutputs.forEachIndexed { index, it ->
            val inputToSign = it.toInputToSign()
            val inputSignature = btcSignature.inputSignatures[index]
            inputToSign.input.witness = inputSignature.witness
            inputToSign.input.sigScript = inputSignature.sigScript
            mutableTransaction.addInput(inputToSign)
        }

        mutableTransaction.outputs = btcSignature.outputs.mapIndexed { index, it ->
            val address = addressConverter.convert(it.address)
            TransactionOutput(
                it.amount,
                index,
                address.lockingScript,
                address.scriptType
            )
        }

        mutableTransaction.transaction.segwit = btcSignature.isSegwit
        val adapter = getBitcoinAdapter()
        adapter.publish(mutableTransaction.build())
    }

    override fun isAdapterAvailable(): Boolean {
        return try {
            getBitcoinAdapter()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun adapterName(): String = "Bitcoin"
}

@Serializable
data class SerializedUnspentOutput(
    val txHash: @Contextual ByteArray,
    val txIndex: Int,
    val value: Long,
    val address: String
) {
    fun toInputToSign(): InputToSign {
        val addressConverter = getBtcMainNetAddressConverter()
        val address = addressConverter.convert(address)
        val transactionInput = TransactionInput(
            previousOutputTxHash = txHash,
            previousOutputIndex = txIndex.toLong(),
            sequence = 0
        )
        val previousOutput = TransactionOutput(
            value,
            txIndex,
            address.lockingScript,
            address.scriptType
        )
        previousOutput.redeemScript = null // TODO: do something about this
        previousOutput.transactionHash = txHash
        val publicKey = PublicKey()
        publicKey.publicKeyHash = address.lockingScriptPayload // TODO: recheck if this is correct
        return InputToSign(
            transactionInput,
            previousOutput,
            publicKey
        )
    }
}

@Serializable
data class InputSignature(
    val witness: List<@Contextual ByteArray>,
    val sigScript: @Contextual ByteArray
)

@Serializable
data class Output(
    val amount: Long,
    val address: String
)

private fun getBitcoinAdapter(): ISendBitcoinAdapter {
    val wallet = getCurrentBitcoinWallet()
    return (App.adapterManager.getAdapterForWallet(wallet) as? ISendBitcoinAdapter)
        ?: throw IllegalStateException("SendBitcoinAdapter is null")
}

private fun getCurrentBitcoinWallet(): Wallet {
    val account = App.accountManager.activeAccount!!
    return App.walletManager.getWallets(account).find {
        it.coin.uid == "bitcoin"
    }!!
}

private fun getBtcMainNetAddressConverter(): AddressConverterChain {
    val network = MainNet()
    return AddressConverterChain().apply {
        prependConverter(SegwitAddressConverter(network.addressSegwitHrp))
        prependConverter(
            Base58AddressConverter(
                network.addressVersion,
                network.addressScriptVersion
            )
        )
    }
}
