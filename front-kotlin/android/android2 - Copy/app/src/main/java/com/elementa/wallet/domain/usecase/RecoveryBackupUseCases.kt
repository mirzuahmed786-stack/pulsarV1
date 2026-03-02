package com.elementa.wallet.domain.usecase

import com.elementa.wallet.data.bindings.*
import com.elementa.wallet.data.repository.WalletOperationsRepository
import javax.inject.Inject

class CreateRecoveryBackupUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(
        pin: String,
        record: VaultRecord,
        backupPassphrase: String
    ): RecoveryBackup {
        return walletOpsRepository.createRecoveryBackup(pin, record, backupPassphrase)
    }
}

class RestoreVaultFromRecoveryBackupUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(
        backupPassphrase: String,
        backup: RecoveryBackup,
        newPin: String
    ): VaultRecord {
        return walletOpsRepository.restoreVaultFromRecoveryBackup(backupPassphrase, backup, newPin)
    }
}

class VerifyRecoveryBackupUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(backupPassphrase: String, backup: RecoveryBackup) {
        walletOpsRepository.verifyRecoveryBackup(backupPassphrase, backup)
    }
}

class CreateCloudRecoveryBlobUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(
        pin: String,
        record: VaultRecord,
        oauthKek: String
    ): CloudRecoveryBlob {
        return walletOpsRepository.createCloudRecoveryBlob(pin, record, oauthKek)
    }
}

class RestoreVaultFromCloudRecoveryBlobUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(
        oauthKek: String,
        blob: CloudRecoveryBlob,
        newPin: String
    ): VaultRecord {
        return walletOpsRepository.restoreVaultFromCloudRecoveryBlob(oauthKek, blob, newPin)
    }
}

class VerifyCloudRecoveryBlobUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(oauthKek: String, blob: CloudRecoveryBlob) {
        walletOpsRepository.verifyCloudRecoveryBlob(oauthKek, blob)
    }
}

class CreateWalletFromWeb3authKeyUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(
        web3authPrivateKey: String,
        testnet: Boolean
    ): Web3AuthWalletResult {
        return walletOpsRepository.createWalletFromWeb3authKey(web3authPrivateKey, testnet)
    }
}

class RestoreWalletFromWeb3authKeyUseCase @Inject constructor(
    private val walletOpsRepository: WalletOperationsRepository
) {
    suspend operator fun invoke(
        web3authPrivateKey: String,
        encryptedData: String,
        testnet: Boolean
    ): Web3AuthWalletResult {
        return walletOpsRepository.restoreWalletFromWeb3authKey(web3authPrivateKey, encryptedData, testnet)
    }
}
