package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.meta
import io.horizontalsystems.bankwallet.entities.AccountType.BitcoinAddress
import io.horizontalsystems.bankwallet.entities.AccountType.EvmAddress
import io.horizontalsystems.bankwallet.entities.AccountType.SolanaAddress
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.parcelize.Parcelize
import java.util.Objects

@Parcelize
data class Wallet(
    val token: Token,
    val account: Account
) : Parcelable {

    val coin
        get() = token.coin

    val decimal
        get() = token.decimals

    val badge
        get() = token.badge

    val supportsAirGap: Boolean
        get() {
            val isNative = token.type is TokenType.Native
            return when (account.type) {
                is EvmAddress -> true
                is BitcoinAddress -> true
                is SolanaAddress -> isNative
                else -> false
            }
        }

    val transactionSource get() = TransactionSource(token.blockchain, account, token.type.meta)

    override fun equals(other: Any?): Boolean {
        if (other is Wallet) {
            return token == other.token && account == other.account
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return Objects.hash(token, account)
    }
}
