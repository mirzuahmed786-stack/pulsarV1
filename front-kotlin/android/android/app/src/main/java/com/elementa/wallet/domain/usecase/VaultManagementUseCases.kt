package com.elementa.wallet.domain.usecase

import com.elementa.wallet.data.bindings.VaultRecord
import com.elementa.wallet.data.repository.WalletOperationsRepository
import javax.inject.Inject

class GenerateMnemonicUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(): String {
        return walletOpsRepository.generateMnemonic()
    }
}

class CreateVaultUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(pin: String): VaultRecord {
        return walletOpsRepository.createVault(pin)
    }
}

class CreateVaultFromMnemonicUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(
        pin: String,
        mnemonic: String,
        path: String = "m/44'/60'/0'/0/0"
    ): VaultRecord {
        return walletOpsRepository.createVaultFromMnemonic(pin, mnemonic, path)
    }
}

class CreateVaultFromPrivateKeyUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(pin: String, privateKeyHex: String): VaultRecord {
        return walletOpsRepository.createVaultFromPrivateKey(pin, privateKeyHex)
    }
}

class VerifyPinUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(pin: String, record: VaultRecord): String {
        return walletOpsRepository.verifyPin(pin, record)
    }
}

class RotatePinUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(oldPin: String, newPin: String, record: VaultRecord): VaultRecord {
        return walletOpsRepository.rotatePin(oldPin, newPin, record)
    }
}

class ExportEthPrivateKeyUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(pin: String, record: VaultRecord): String {
        return walletOpsRepository.exportEthPrivateKey(pin, record)
    }
}

class MigrateVaultUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(pin: String, record: VaultRecord): VaultRecord {
        return walletOpsRepository.migrateVault(pin, record)
    }
}
