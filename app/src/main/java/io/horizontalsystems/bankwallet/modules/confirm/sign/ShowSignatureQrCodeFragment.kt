package io.horizontalsystems.bankwallet.modules.confirm.sign

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.modules.send.ShowQrCodeScreen
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import kotlinx.parcelize.Parcelize

class ShowSignatureQrCodeFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val signatureJson = navController.requireInput<Input>()
            .signatureJson
        ShowQrCodeScreen(
            navController,
            title = stringResource(R.string.AirGap_Signature_Title),
            qrData = signatureJson,
            hint = stringResource(R.string.AirGap_Signature_Hint),
            buttonsSlot = {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.Button_Done),
                    onClick = {
                        navController.popBackStack()
                        navController.popBackStack()
                    }
                )
            }
        )
    }

    @Parcelize
    data class Input(val signatureJson: String) : Parcelable
}
