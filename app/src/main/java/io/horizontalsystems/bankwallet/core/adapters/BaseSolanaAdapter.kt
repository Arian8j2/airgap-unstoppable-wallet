package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.managers.SolanaKitWrapper
import io.horizontalsystems.solanakit.Signer
import io.horizontalsystems.solanakit.models.Address
import io.horizontalsystems.solanakit.transactions.BlockData
import io.horizontalsystems.solanakit.transactions.RawTransaction
import java.math.BigDecimal

abstract class BaseSolanaAdapter(
        solanaKitWrapper: SolanaKitWrapper,
        val decimal: Int
) : IAdapter, IBalanceAdapter, IReceiveAdapter {

    val solanaKit = solanaKitWrapper.solanaKit
    protected val signer: Signer? = solanaKitWrapper.signer

    override val debugInfo: String
        get() = solanaKit.debugInfo()

    val statusInfo: Map<String, Any>
        get() = solanaKit.statusInfo()

    val recentBlockData: BlockData
        get() = solanaKit.getLatestBlockData()

    fun signTransaction(rawTransaction: RawTransaction) {
        solanaKit.signTransaction(signer!!, rawTransaction)
    }

    suspend fun publishTransaction(rawTransaction: RawTransaction) {
        solanaKit.publish(rawTransaction)
    }

    // IReceiveAdapter

    override val receiveAddress: String
        get() = solanaKit.receiveAddress

    override val isMainNet: Boolean
        get() = solanaKit.isMainnet

    companion object {
        const val confirmationsThreshold: Int = 12
    }

}
