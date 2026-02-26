package com.elementa.wallet.domain.usecase

import com.elementa.wallet.domain.repository.VaultRepository
import javax.inject.Inject

class SetPinUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    // Persists a new 6-digit PIN hash.
    suspend fun execute(pin: String): Result<Unit> {
        return repository.setPin(pin)
    }
}
