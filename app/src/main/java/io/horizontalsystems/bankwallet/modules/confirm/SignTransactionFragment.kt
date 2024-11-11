package io.horizontalsystems.bankwallet.modules.confirm

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.title
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.confirm.sign.ShowSignatureQrCodeFragment
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceEvm
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.evm.EvmRawTransactionData
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionView
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewItemFactory
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.ethereumkit.models.Signature
import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.BigInteger

class SignTransactionFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.requireInput<Input>()
        val evmData = EvmRawTransactionData.fromJson(input.evmDataJson) ?: return

        val evmKitManager = App.evmBlockchainManager.getEvmKitManager(evmData.blockchainType)
        // it's required for SendTransactionServiceEvm
        if (evmKitManager.evmKitWrapper == null) {
            val view = LocalView.current
            val errorMsg = String.format(
                "Enable %s blockchain in Coin manager to continue",
                evmData.blockchainType.title
            )
            HudHelper.showErrorMessage(view, errorMsg)
            navController.popBackStack()
            return
        }

        SignTransactionScreen(
            navController,
            evmData
        )
    }

    @Parcelize
    data class Input(val evmDataJson: String) : Parcelable
}

@Composable
private fun SignTransactionScreen(
    navController: NavController,
    evmData: EvmRawTransactionData
) {
    ConfirmTransactionScreen(
        title = "Confirm Offline Transaction",
        onClickBack = { navController.popBackStack() },
        onClickSettings = null,
        onClickClose = null,
        buttonsSlot = {
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
                title = stringResource(R.string.Button_Sign),
                onClick = {
                    val signature = signTransaction(evmData)
                    val signatureJson = Gson().toJson(signature)!!
                    navController.slideFromBottom(
                        R.id.showSignatureQrCodeFragment,
                        ShowSignatureQrCodeFragment.Input(signatureJson)
                    )
                }
            )
        },
        content = {
            SendEvmTransactionView(
                navController,
                items = craftItems(evmData),
                cautions = listOf(),
                transactionFields = listOf(),
                networkFee = getNetworkFee(evmData),
                statPage = StatPage.SendConfirmation
            )
        }
    )
}

private fun signTransaction(evmData: EvmRawTransactionData): Signature {
    val evmKitWrapper =
        App.evmBlockchainManager.getEvmKitManager(evmData.blockchainType).evmKitWrapper!!
    return evmKitWrapper.signer!!.signature(evmData.rawTransaction)
}

private fun getNetworkFee(evmData: EvmRawTransactionData): SendModule.AmountData {
    val feeToken = App.evmBlockchainManager.getBaseToken(evmData.blockchainType)!!
    val amount =
        BigInteger.valueOf(evmData.rawTransaction.gasPrice.max) * BigInteger.valueOf(evmData.rawTransaction.gasLimit)
    val amountWithDecimals = BigDecimal(amount, feeToken.decimals)
    val coinValue = CoinValue(feeToken, amountWithDecimals)
    return SendModule.AmountData(
        primary = SendModule.AmountInfo.CoinValueInfo(coinValue),
        secondary = null
    )
}

private fun craftItems(evmData: EvmRawTransactionData): List<SectionViewItem> {
    val blockchainType = evmData.blockchainType
    val transactionData = TransactionData(
        to = evmData.rawTransaction.to,
        value = evmData.rawTransaction.value,
        input = evmData.rawTransaction.data
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
