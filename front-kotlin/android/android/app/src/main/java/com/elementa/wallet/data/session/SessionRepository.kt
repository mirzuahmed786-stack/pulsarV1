package com.elementa.wallet.data.session

import com.elementa.wallet.domain.model.WalletSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor() {
    private val session = MutableStateFlow(WalletSession())

    fun observe(): StateFlow<WalletSession> = session

    fun updateEvmAddress(address: String) {
        session.value = session.value.copy(evmAddress = address)
    }

    fun updateSolanaAddress(address: String) {
        session.value = session.value.copy(solanaAddress = address)
    }
    
    fun updateBitcoinAddress(address: String) {
        session.value = session.value.copy(bitcoinAddress = address)
    }
}
