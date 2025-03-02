package io.horizontalsystems.bankwallet.modules.airgap.transaction

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.entities.Address
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
    val rbfEnabled: Boolean
) : AirGapTransaction() {

    @Serializable
    data class SerializedUnspentOutput(
        val txHash: @Contextual ByteArray,
        val txIndex: Int,
        val value: Long,
        val address: String
    ) {
        fun toUnspentOutput(
            addressConverter: AddressConverterChain,
            publicKey: PublicKey
        ): UnspentOutput {
            val trxOutput = TransactionOutput()
            trxOutput.value = value
            trxOutput.address = address
            trxOutput.transactionHash = txHash
            trxOutput.index = txIndex
            val addressInfo = addressConverter.convert(address)
            trxOutput.lockingScript = addressInfo.lockingScript
            trxOutput.lockingScriptPayload = addressInfo.lockingScriptPayload
            trxOutput.scriptType = addressInfo.scriptType
            return UnspentOutput(
                output = trxOutput,
                publicKey = publicKey,
                transaction = Transaction(),
                block = null
            )
        }
    }

    private fun getMappedUnspentOutputs(): List<UnspentOutput> =
        unspentOutputs.map {
            it.toUnspentOutput(addressConverter, adapter!!.getPublicKey())
        }

    private fun getMappedInputToSigns(): List<InputToSign> =
        getMappedUnspentOutputs().map { inputToSign(it) }

    val addressConverter: AddressConverterChain by lazy {
        val network = MainNet()
        AddressConverterChain().apply {
            prependConverter(SegwitAddressConverter(network.addressSegwitHrp))
            prependConverter(
                Base58AddressConverter(
                    network.addressVersion,
                    network.addressScriptVersion
                )
            )
        }
    }

    private fun inputToSign(unspentOutput: UnspentOutput): InputToSign {
        val previousOutput = unspentOutput.output
        val sequence = if (rbfEnabled) {
            0x00
        } else {
            0xfffffffe
        }
        val transactionInput = TransactionInput(
            previousOutput.transactionHash,
            previousOutput.index.toLong(),
            sequence = sequence
        )
        return InputToSign(transactionInput, previousOutput, unspentOutput.publicKey)
    }

    @Composable
    override fun ShowSigningConfirmationScreen(navController: NavController) {
        val outputs = getMappedUnspentOutputs()
        val feeInfo = adapter!!.bitcoinFeeInfoWithSpecificOutputs(
            amount,
            feeRate,
            to,
            memo = null,
            unspentOutputs = outputs,
            pluginData = null
        )!!
        SendConfirmationScreen(
            navController = navController,
            coinMaxAllowedDecimals = wallet!!.token.decimals,
            feeCoinMaxAllowedDecimals = wallet!!.token.decimals,
            amountInputType = AmountInputType.COIN,
            rate = null,
            feeCoinRate = null,
            sendResult = null,
            blockchainType = BlockchainType.Bitcoin,
            coin = wallet!!.coin,
            feeCoin = wallet!!.coin,
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
        val signatures: List<Signature>,
        val outputs: List<Output>
    ) : AirGapSignature()

    @Serializable
    data class Signature(
        val witness: List<@Contextual ByteArray>,
        val sigScript: @Contextual ByteArray
    )

    @Serializable
    data class Output(
        val amount: Long,
        val address: String
    )

    override fun sign(): AirGapSignature {
        val signedTransaction = adapter!!.sign(
            amount = amount,
            address = to,
            memo = null,
            feeRate = feeRate,
            unspentOutputs = getMappedUnspentOutputs(),
            pluginData = null,
            rbfEnabled = rbfEnabled,
        )
        return extractSignature(signedTransaction)
    }

    private fun extractSignature(transaction: FullTransaction): AirGapSignature {
        val inputs = transaction.inputs.map {
            Signature(
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
            signatures = inputs,
            outputs = outputs
        )
    }

    override suspend fun publish(signature: AirGapSignature) {
        val btcSignature = signature as AirGapBitcoinSignature
        val mutableTransaction = MutableTransaction()

        getMappedInputToSigns().forEachIndexed { index, it ->
            val signatures = btcSignature.signatures[index]
            it.input.witness = signatures.witness
            it.input.sigScript = signatures.sigScript
            mutableTransaction.addInput(it)
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
        adapter!!.publish(mutableTransaction.build())
    }

    override fun isAdapterAvailable(): Boolean = adapter != null

    override fun adapterName(): String = "Bitcoin"

    private val adapter: ISendBitcoinAdapter? by lazy {
        wallet?.let {
            App.adapterManager.getAdapterForWallet(it) as ISendBitcoinAdapter
        }
    }

    private val wallet: Wallet? by lazy {
        val account = App.accountManager.activeAccount!!
        val wallets = App.walletManager.getWallets(account)
        wallets.find {
            App.adapterManager.getAdapterForWallet(it) is ISendBitcoinAdapter
        }
    }
}
