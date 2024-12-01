package io.horizontalsystems.bankwallet.modules.airgap.transaction

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.adapters.SolanaAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationScreen
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.solanakit.SolanaKit
import io.horizontalsystems.solanakit.models.Address
import io.horizontalsystems.solanakit.transactions.RawTransaction
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
@SerialName("solana")
data class AirGapSolanaTransaction(
    val amount: @Contextual BigDecimal,
    val to: String,
    val recentBlockHash: String
) : AirGapTransaction() {
    @Composable
    override fun ShowSigningConfirmationScreen(navController: NavController) {
        SendConfirmationScreen(
            navController = navController,
            coinMaxAllowedDecimals = wallet!!.token.decimals,
            feeCoinMaxAllowedDecimals = wallet!!.token.decimals,
            amountInputType = AmountInputType.COIN,
            rate = null,
            feeCoinRate = null,
            sendResult = null,
            blockchainType = BlockchainType.Solana,
            coin = wallet!!.coin,
            feeCoin = wallet!!.coin,
            amount = amount,
            address = io.horizontalsystems.bankwallet.entities.Address(to),
            contact = null,
            fee = SolanaKit.fee,
            lockTimeInterval = null,
            memo = null,
            rbfEnabled = null,
            onClickSend = {},
            sendEntryPointDestId = 0,
            isScreen = false
        )
    }

    private fun toRawTransaction(): RawTransaction {
        return solanaAdapter!!.craftSendTransaction(
            address,
            Address(to),
            amount,
            recentBlockHash
        )
    }

    @SerialName("solana")
    @Serializable
    data class AirGapSolanaSignature(
        val signatures: List<@Contextual ByteArray?>
    ) : AirGapSignature()

    override fun sign(): AirGapSignature {
        val transaction = toRawTransaction()
        solanaAdapter!!.signTransaction(transaction)
        return AirGapSolanaSignature(
            signatures = transaction.getSignatures()
        )
    }

    override suspend fun publish(signature: AirGapSignature) {
        val transaction = toRawTransaction()
        val signatures = (signature as AirGapSolanaSignature).signatures
        signatures.forEach {
            transaction.addSignature(address, it)
        }
        solanaAdapter!!.publishTransaction(transaction)
    }

    override fun isAdapterAvailable(): Boolean = solanaAdapter != null

    override fun adapterName(): String = "Solana"

    private val solanaAdapter: SolanaAdapter? by lazy {
        wallet?.let {
            App.adapterManager.getAdapterForWallet(it) as SolanaAdapter
        }
    }

    private val wallet: Wallet? by lazy {
        val account = App.accountManager.activeAccount!!
        val wallets = App.walletManager.getWallets(account)
        wallets.find {
            App.adapterManager.getAdapterForWallet(it) is SolanaAdapter
        }
    }

    private val address: Address by lazy {
        val receiveAdapter = solanaAdapter!! as IReceiveAdapter
        Address(receiveAdapter.receiveAddress)
    }
}