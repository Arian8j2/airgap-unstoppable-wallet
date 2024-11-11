package io.horizontalsystems.bankwallet.modules.send

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
import com.google.gson.Gson
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceEvm
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.send.evm.EvmRawTransactionData
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationFragment
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.models.Signature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

class ShowTransactionQrCodeFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val evmDataJson = navController.requireInput<Input>()
            .evmDataJson
        val evmData = EvmRawTransactionData.fromJson(evmDataJson)!!
        ShowQrCodeScreen(
            navController,
            title = "Transaction Qr Code",
            qrData = evmDataJson,
            hint = "Scan this QR code with your offline wallet to sign it",
            buttonsSlot = {
                QrTransactionNextButton(
                    navController,
                    evmData,
                )
            }
        )
    }

    @Parcelize
    data class Input(val evmDataJson: String) : Parcelable
}

@Composable
private fun QrTransactionNextButton(
    navController: NavController,
    evmData: EvmRawTransactionData,
) {
    val sendTransactionService = SendTransactionServiceEvm(evmData.blockchainType)
    val view = LocalView.current
    val context = LocalContext.current

    var scannedSignature = remember { mutableStateOf<Signature?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val qrScannerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                return@rememberLauncherForActivityResult
            }

            val scannedText = result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""
            val signature = parseSignatureFromJson(scannedText)

            if (signature == null) {
                HudHelper.showErrorMessage(view, "Scanned Qr code is not a valid signature")
            } else {
                scannedSignature.value = signature
            }
        }

    suspend fun onScannedSignature(signature: Signature) = withContext(Dispatchers.Default) {
        HudHelper.showInProcessMessage(view, R.string.Send_Sending, SnackbarDuration.INDEFINITE)

        try {
            HudHelper.showInProcessMessage(view, R.string.Send_Sending, SnackbarDuration.INDEFINITE)
            sendTransactionService.publishTransaction(evmData.rawTransaction, signature)
            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
            delay(300)

            withContext(Dispatchers.Main) {
                navController.popBackStack()
                val result = SendEvmConfirmationFragment.Result(true)
                navController.setNavigationResultX(result)
                navController.popBackStack()
            }
        } catch (e: Exception) {
            HudHelper.showErrorMessage(view, e.javaClass.name)
        } catch (e: JsonRpc.ResponseError.RpcError) {
            val errorMsg = String.format("Rpc Error: %s", e.error.message)
            HudHelper.showErrorMessage(view, errorMsg)
        }
    }

    ButtonPrimaryYellow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp),
        title = stringResource(R.string.Send_DialogProceed),
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

fun parseSignatureFromJson(signatureJson: String): Signature? {
    return try {
        val signature = Gson().fromJson(signatureJson, Signature::class.java)
        if (signature?.r == null || signature.s == null) {
            null
        } else {
            signature
        }
    } catch (e: Exception) {
        null
    }
}
