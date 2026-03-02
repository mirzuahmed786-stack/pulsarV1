package com.elementa.wallet

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WalletApp : Application()

{
	companion object {
		@Volatile
		private var INSTANCE: WalletApp? = null

		fun instance(): WalletApp {
			return INSTANCE ?: throw IllegalStateException("WalletApp not initialized")
		}
	}

	override fun onCreate() {
		super.onCreate()
		INSTANCE = this
	}
}
