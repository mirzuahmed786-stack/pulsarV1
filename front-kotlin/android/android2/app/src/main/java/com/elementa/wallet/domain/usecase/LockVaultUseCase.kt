package com.elementa.wallet.domain.usecase

import com.elementa.wallet.domain.repository.VaultRepository
import javax.inject.Inject

class LockVaultUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    // Locks the vault and persists the state.
    suspend fun execute() {
        repository.lock()
    }
}
