package io.horizontalsystems.bankwallet.modules.send.bitcoin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationScreen
import io.horizontalsystems.bankwallet.modules.airgap.ShowAirGapTransactionFragment
import kotlinx.coroutines.launch

@Composable
fun SendBitcoinConfirmationScreen(
    navController: NavController,
    sendViewModel: SendBitcoinViewModel,
    amountInputModeViewModel: AmountInputModeViewModel,
    sendEntryPointDestId: Int
) {
    var confirmationData by remember { mutableStateOf(sendViewModel.getConfirmationData()) }
    var refresh by remember { mutableStateOf(false) }

    LifecycleResumeEffect {
        if (refresh) {
            confirmationData = sendViewModel.getConfirmationData()
        }

        onPauseOrDispose {
            refresh = true
        }
    }

    SendConfirmationScreen(
        navController = navController,
        coinMaxAllowedDecimals = sendViewModel.coinMaxAllowedDecimals,
        feeCoinMaxAllowedDecimals = sendViewModel.coinMaxAllowedDecimals,
        amountInputType = amountInputModeViewModel.inputType,
        rate = sendViewModel.coinRate,
        feeCoinRate = sendViewModel.coinRate,
        sendResult = sendViewModel.sendResult,
        blockchainType = sendViewModel.blockchainType,
        coin = confirmationData.coin,
        feeCoin = confirmationData.coin,
        amount = confirmationData.amount,
        address = confirmationData.address,
        contact = confirmationData.contact,
        fee = confirmationData.fee,
        lockTimeInterval = confirmationData.lockTimeInterval,
        memo = confirmationData.memo,
        rbfEnabled = confirmationData.rbfEnabled,
        onClickSend = {
            if (sendViewModel.wallet.isAirGapWatchWallet) {
                sendViewModel.viewModelScope.launch {
                    val input = sendViewModel.createAirGapInput()
                    navController.slideFromRight(
                        R.id.showAirGapTransactionFragment,
                        ShowAirGapTransactionFragment.Input(
                            input,
                            null
                        )
                    )
                }
            } else {
                sendViewModel.onClickSend()
            }
        },
        sendEntryPointDestId = sendEntryPointDestId
    )
}