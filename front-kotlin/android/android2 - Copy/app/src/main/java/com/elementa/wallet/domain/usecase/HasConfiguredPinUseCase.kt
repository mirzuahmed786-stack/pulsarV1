package com.elementa.wallet.domain.usecase

import com.elementa.wallet.domain.repository.VaultRepository
import javax.inject.Inject

class HasConfiguredPinUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    // Returns true when a password has been configured.
    suspend fun execute(): Boolean {
        return repository.hasConfiguredPin()
    }
}
