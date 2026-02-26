package com.elementa.wallet.domain.usecase

import com.elementa.wallet.domain.repository.VaultRepository
import javax.inject.Inject

class UnlockVaultUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    // Verifies a PIN and unlocks the vault on success.
    suspend fun execute(pin: String): Result<Unit> {
        return repository.unlock(pin)
    }
}
