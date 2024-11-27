package io.horizontalsystems.bankwallet.modules.airgap

import android.app.Activity
import android.os.Parcelable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

class ShowAirGapTransactionFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.requireInput<Input>()
        ShowQrCodeScreen(
            navController,
            title = stringResource(R.string.AirGap_Transaction_Title),
            qrData = input.airGapTransaction.toJson(),
            hint = stringResource(R.string.AirGap_Transaction_Hint),
            buttonsSlot = {
                QrTransactionNextButton(
                    navController,
                    input.airGapTransaction,
                    input.successNavigationResult
                )
            }
        )
    }

    @Parcelize
    data class Input(
        val airGapTransaction: AirGapTransaction,
        val successNavigationResult: Parcelable?
    ) : Parcelable
}

@Composable
private fun QrTransactionNextButton(
    navController: NavController,
    airGapTransaction: AirGapTransaction,
    successNavigationResult: Parcelable?
) {
    val view = LocalView.current
    val context = LocalContext.current

    val scannedSignature = remember { mutableStateOf<AirGapSignature?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val qrScannerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                return@rememberLauncherForActivityResult
            }

            val scannedText = result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""
            try {
                val signature = AirGapSignature.fromJson(scannedText)
                scannedSignature.value = signature
            } catch (e: Exception) {
                HudHelper.showErrorMessage(
                    view,
                    R.string.AirGap_Transaction_Error_WrongSignature
                )
            }
        }

    suspend fun onScannedSignature(signature: AirGapSignature) =
        withContext(Dispatchers.Default) {
            HudHelper.showInProcessMessage(
                view,
                R.string.Send_Sending,
                SnackbarDuration.INDEFINITE
            )

            try {
                HudHelper.showInProcessMessage(
                    view,
                    R.string.Send_Sending,
                    SnackbarDuration.INDEFINITE
                )
                airGapTransaction.publish(signature)
                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                delay(500)

                withContext(Dispatchers.Main) {
                    navController.popBackStack()
                    navController.popBackStack()
                    successNavigationResult?.let {
                        navController.setNavigationResultX(it)
                    }
                    navController.popBackStack()
                }
            } catch (e: Exception) {
                HudHelper.showErrorMessage(view, e.javaClass.name)
            } catch (e: JsonRpc.ResponseError.RpcError) {
                val errorMsg = String.format("%s: %s", e.javaClass.name, e.error.message)
                HudHelper.showErrorMessage(view, errorMsg)
            }
        }

    ButtonPrimaryYellow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp),
        title = stringResource(R.string.AirGap_Button_ScanSignature),
        onClick = {
            qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context, false))
        }
    )

    scannedSignature.value?.let {
        coroutineScope.launch {
            onScannedSignature(it)
        }
    }
}