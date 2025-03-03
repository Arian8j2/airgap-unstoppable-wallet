package io.horizontalsystems.bankwallet.modules.airgap.transaction

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.BitcoinBaseAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationScreen
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.utils.AddressConverterChain
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.SegwitAddressConverter
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.bitcoinkit.TestNet
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
    )

    private fun getMappedUnspentOutputs(): List<UnspentOutput> =
        unspentOutputs.map {
            val trxOutput = TransactionOutput()
            val address = adapter!!.receiveAddress
            trxOutput.value = it.value
            trxOutput.address = address
            trxOutput.transactionHash = it.txHash
            trxOutput.index = it.txIndex
            val addressInfo = addressConverter.convert(address)
            trxOutput.lockingScript = addressInfo.lockingScript
            trxOutput.lockingScriptPayload = addressInfo.lockingScriptPayload
            trxOutput.scriptType = addressInfo.scriptType
            UnspentOutput(
                output = trxOutput,
                publicKey = adapter!!.kit.receivePublicKey(),
                transaction = Transaction(),
                block = null
            )
        }

    val addressConverter: AddressConverterChain by lazy {
        val network = if (adapter!!.isMainNet) MainNet() else TestNet()
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

    @Composable
    override fun ShowSigningConfirmationScreen(navController: NavController) {
        val transaction = createTransaction()
        val inputsTotalValue = transaction.inputsToSign.sumOf { it.previousOutput.value }
        val outputsTotalValue = transaction.recipientValue + transaction.changeValue
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
            fee = adapter!!.satoshiToBTC(inputsTotalValue - outputsTotalValue)!!,
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
    ) : AirGapSignature()

    @Serializable
    data class Signature(
        val witness: List<@Contextual ByteArray>?,
        val sigScript: @Contextual ByteArray?
    )

    override fun sign(): AirGapSignature {
        val transaction = createTransaction()
        adapter!!.kit.sign(transaction)
        return extractSignature(transaction.build())
    }

    private fun createTransaction(): MutableTransaction {
        val transaction = adapter!!.buildTransaction(
            amount = amount,
            address = to,
            memo = null,
            feeRate = feeRate,
            unspentOutputs = getMappedUnspentOutputs(),
            pluginData = null,
            rbfEnabled = rbfEnabled,
        )
        transaction.transaction.lockTime = 0
        return transaction
    }

    private fun extractSignature(transaction: FullTransaction): AirGapSignature {
        val inputs = transaction.inputs.map {
            Signature(
                witness = it.witness.takeIf { it.isNotEmpty() },
                sigScript = it.sigScript.takeIf { it.isNotEmpty() }
            )
        }
        return AirGapBitcoinSignature(
            isSegwit = transaction.header.segwit,
            signatures = inputs,
        )
    }

    override suspend fun publish(signature: AirGapSignature) {
        val btcSignature = signature as AirGapBitcoinSignature
        val transaction = createTransaction()
        if (transaction.inputsToSign.size != btcSignature.signatures.size) {
            throw Throwable("Signatures size don't match inputs")
        }

        transaction.inputsToSign.forEachIndexed { index, it ->
            val signatures = btcSignature.signatures[index]
            it.input.witness = signatures.witness.orEmpty()
            it.input.sigScript = signatures.sigScript ?: ByteArray(0)
        }
        transaction.transaction.segwit = btcSignature.isSegwit
        adapter!!.kit.publish(transaction.build())
    }

    override fun isAdapterAvailable(): Boolean = adapter != null

    override fun adapterName(): String = "Bitcoin"

    private val adapter: BitcoinBaseAdapter? by lazy {
        wallet?.let {
            App.adapterManager.getAdapterForWallet(it) as BitcoinBaseAdapter
        }
    }

    private val wallet: Wallet? by lazy {
        val account = App.accountManager.activeAccount!!
        val wallets = App.walletManager.getWallets(account)
        wallets.find {
            App.adapterManager.getAdapterForWallet(it) is BitcoinBaseAdapter
        }
    }
}
