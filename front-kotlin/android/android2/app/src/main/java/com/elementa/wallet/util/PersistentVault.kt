package com.elementa.wallet.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.elementa.wallet.WalletApp
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

private const val DATASTORE_NAME = "persistent_vault_store"
private val Context.persistentVaultDataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

object PersistentVault {
    private val KEY_VAULT_JSON = stringPreferencesKey("vault_json")

    private val ctx: Context
        get() = WalletApp.instance()

    fun saveJson(json: String) {
        runBlocking {
            ctx.persistentVaultDataStore.edit { prefs ->
                prefs[KEY_VAULT_JSON] = json
            }
        }
    }

    fun loadJson(): String? {
        return runBlocking {
            val prefs = ctx.persistentVaultDataStore.data.first()
            prefs[KEY_VAULT_JSON]
        }
    }
}
