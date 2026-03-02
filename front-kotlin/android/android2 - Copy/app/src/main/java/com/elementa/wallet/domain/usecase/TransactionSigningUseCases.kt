package com.elementa.wallet.domain.usecase

import com.elementa.wallet.data.bindings.*
import com.elementa.wallet.data.repository.WalletOperationsRepository
import javax.inject.Inject

class SignTransactionUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(
        pin: String,
        record: VaultRecord,
        tx: UnsignedLegacyTx
    ): String {
        return walletOpsRepository.signTransaction(pin, record, tx)
    }
}

class SignTransactionEip1559UseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(
        pin: String,
        record: VaultRecord,
        tx: UnsignedEip1559Tx
    ): String {
        return walletOpsRepository.signTransactionEip1559(pin, record, tx)
    }
}

class SignTransactionWithChainUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(
        pin: String,
        record: VaultRecord,
        tx: UnsignedLegacyTx,
        expectedChainId: Long
    ): String {
        return walletOpsRepository.signTransactionWithChain(pin, record, tx, expectedChainId)
    }
}

class SignTransactionEip1559WithChainUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(
        pin: String,
        record: VaultRecord,
        tx: UnsignedEip1559Tx,
        expectedChainId: Long
    ): String {
        return walletOpsRepository.signTransactionEip1559WithChain(pin, record, tx, expectedChainId)
    }
}

class SignSolanaTransactionUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(
        pin: String,
        record: VaultRecord,
        message: ByteArray
    ): ByteArray {
        return walletOpsRepository.signSolanaTransaction(pin, record, message)
    }
}

class SignBitcoinTransactionUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(
        pin: String,
        record: VaultRecord,
        sighashHex: String,
        testnet: Boolean
    ): String {
        return walletOpsRepository.signBitcoinTransaction(pin, record, sighashHex, testnet)
    }
}
