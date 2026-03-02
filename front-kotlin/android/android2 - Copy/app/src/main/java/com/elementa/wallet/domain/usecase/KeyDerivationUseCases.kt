package com.elementa.wallet.domain.usecase

import com.elementa.wallet.data.bindings.MultichainAddresses
import com.elementa.wallet.data.bindings.VaultRecord
import com.elementa.wallet.data.repository.WalletOperationsRepository
import javax.inject.Inject

class DeriveBtcAddressUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(mnemonic: String, testnet: Boolean): String {
        return walletOpsRepository.deriveBtcAddress(mnemonic, testnet)
    }
}

class DeriveSolAddressUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(mnemonic: String): String {
        return walletOpsRepository.deriveSolAddress(mnemonic)
    }
}

class GetBtcPublicKeyUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(pin: String, record: VaultRecord, testnet: Boolean): String {
        return walletOpsRepository.getBtcPublicKey(pin, record, testnet)
    }
}

class GetMultichainAddressesUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(
        pin: String,
        record: VaultRecord,
        testnet: Boolean
    ): MultichainAddresses {
        return walletOpsRepository.getMultichainAddresses(pin, record, testnet)
    }
}

class GetMultichainAddressesByIndexUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(
        pin: String,
        record: VaultRecord,
        testnet: Boolean,
        index: Int
    ): MultichainAddresses {
        return walletOpsRepository.getMultichainAddressesByIndex(pin, record, testnet, index)
    }
}
