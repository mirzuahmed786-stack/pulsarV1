package com.elementa.wallet.di

import android.content.Context
import com.elementa.wallet.data.adapters.TokenImportAdapterImpl
import com.elementa.wallet.data.engine.DefaultWalletEngine
import com.elementa.wallet.data.engine.RustWalletEngineBridge
import com.elementa.wallet.data.storage.DataStoreTokenStorage
import com.elementa.wallet.data.storage.TokenStorage
import com.elementa.wallet.domain.engine.WalletEngine
import com.elementa.wallet.domain.service.TokenImportAdapter
import com.elementa.wallet.rpc.EvmRpc
import com.elementa.wallet.rpc.EvmRpcService
import com.elementa.wallet.rpc.SolanaRpc
import com.elementa.wallet.rpc.SolanaRpcService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTokenStorage(
        @ApplicationContext context: Context,
        cryptoManager: com.elementa.wallet.security.CryptoManager,
        json: Json
    ): TokenStorage = DataStoreTokenStorage(context, cryptoManager, json)

    @Provides
    @Singleton
    fun provideWalletEngine(
        defaultEngine: DefaultWalletEngine,
        rustBridge: RustWalletEngineBridge
    ): WalletEngine = rustBridge

    @Provides
    @Singleton
    fun provideTokenImportAdapter(
        adapter: TokenImportAdapterImpl
    ): TokenImportAdapter = adapter

    @Provides
    @Singleton
    fun provideEvmRpc(service: EvmRpcService): EvmRpc = service

    @Provides
    @Singleton
    fun provideVaultSecureStorage(
        @ApplicationContext context: Context,
        cryptoManager: com.elementa.wallet.security.CryptoManager
    ): com.elementa.wallet.data.storage.VaultSecureStorage = com.elementa.wallet.data.storage.VaultSecureStorage(
        context.applicationContext,
        cryptoManager
    )

    @Provides
    @Singleton
    fun provideSolanaRpc(service: SolanaRpcService): SolanaRpc = service
}
