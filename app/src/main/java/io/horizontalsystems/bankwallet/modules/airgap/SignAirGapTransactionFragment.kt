package io.horizontalsystems.bankwallet.modules.airgap

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.airgap.transaction.AirGapTransaction
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.parcelize.Parcelize

class SignAirGapTransactionFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.requireInput<Input>()
        if (!input.airGapTransaction.isAdapterAvailable()) {
            val view = LocalView.current
            val errorMsg = Translator.getString(
                R.string.AirGap_SignTransaction_Error_NoKit,
                input.airGapTransaction.adapterName()
            )
            HudHelper.showErrorMessage(view, errorMsg)
            navController.popBackStack()
            return
        }

        SignConfirmationScreen(
            navController,
            input.airGapTransaction
        )
        // TODO: move ShowSignatureQrCodeFragment to this file
    }

    @Parcelize
    data class Input(val airGapTransaction: AirGapTransaction) : Parcelable
}

@Composable
private fun SignConfirmationScreen(
    navController: NavController,
    airGapTransaction: AirGapTransaction
) {
    ConfirmTransactionScreen(
        title = stringResource(R.string.AirGap_SignTransaction_Title),
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
                    val signature = airGapTransaction.sign()
                    navController.slideFromBottom(
                        R.id.showSignatureQrCodeFragment,
                        ShowSignatureQrCodeFragment.Input(signature.toJson())
                    )
                }
            )
        },
        content = {
            airGapTransaction.ShowSigningConfirmationScreen(navController)
        }
    )
}