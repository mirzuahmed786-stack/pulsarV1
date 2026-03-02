package com.elementa.wallet.domain.usecase

import com.elementa.wallet.domain.repository.VaultRepository
import javax.inject.Inject

class SetPinUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    // Persists a new password hash.
    suspend fun execute(password: String): Result<Unit> {
        return repository.setPin(password)
    }
}
