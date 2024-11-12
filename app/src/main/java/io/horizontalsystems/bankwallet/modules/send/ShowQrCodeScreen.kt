package io.horizontalsystems.bankwallet.modules.send

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.receive.ui.QrCodeImage
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportant
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer

@Composable
fun ShowQrCodeScreen(
    navController: NavController,
    title: String,
    qrData: String,
    hint: String,
    buttonsSlot: @Composable () -> Unit,
) {
    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = title,
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                }
            )
        },
        bottomBar = {
            ButtonsGroupWithShade {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = {
                        buttonsSlot()
                    }
                )
            }
        }
    ) {
        Column(
            Modifier
                .padding(it)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ComposeAppTheme.colors.white)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeImage(qrData)
                }
                VSpacer(12.dp)
                TextImportant(
                    modifier = Modifier,
                    text = hint,
                    borderColor = ComposeAppTheme.colors.steelDark,
                    backgroundColor = ComposeAppTheme.colors.steelDark,
                    textColor = ComposeAppTheme.colors.grey50,
                    iconColor = ComposeAppTheme.colors.lucian
                )
            }
        }
    }
}
