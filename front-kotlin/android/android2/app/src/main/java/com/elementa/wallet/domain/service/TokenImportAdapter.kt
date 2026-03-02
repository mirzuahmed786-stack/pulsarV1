package com.elementa.wallet.domain.service

import com.elementa.wallet.domain.model.AddTokenRequest
import com.elementa.wallet.domain.model.TokenAsset

interface TokenImportAdapter {
    suspend fun addCustomToken(request: AddTokenRequest): TokenAsset
}
