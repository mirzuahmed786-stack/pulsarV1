package com.elementa.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elementa.wallet.data.bindings.*
import com.elementa.wallet.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class WalletOperationState {
    object Idle : WalletOperationState()
    object Loading : WalletOperationState()
    data class Success(val message: String) : WalletOperationState()
    data class Error(val error: String) : WalletOperationState()
}

@HiltViewModel
class WalletOperationsViewModel @Inject constructor(
    private val generateMnemonicUseCase: GenerateMnemonicUseCase,
    private val createVaultUseCase: CreateVaultUseCase,
    private val createVaultFromMnemonicUseCase: CreateVaultFromMnemonicUseCase,
    private val createVaultFromPrivateKeyUseCase: CreateVaultFromPrivateKeyUseCase,
    private val verifyPinUseCase: VerifyPinUseCase,
    private val rotatePinUseCase: RotatePinUseCase,
    private val exportEthPrivateKeyUseCase: ExportEthPrivateKeyUseCase,
    private val migrateVaultUseCase: MigrateVaultUseCase,
    private val signTransactionUseCase: SignTransactionUseCase,
    private val signTransactionEip1559UseCase: SignTransactionEip1559UseCase,
    private val signTransactionWithChainUseCase: SignTransactionWithChainUseCase,
    private val signTransactionEip1559WithChainUseCase: SignTransactionEip1559WithChainUseCase,
    private val signSolanaTransactionUseCase: SignSolanaTransactionUseCase,
    private val signBitcoinTransactionUseCase: SignBitcoinTransactionUseCase,
    private val deriveBtcAddressUseCase: DeriveBtcAddressUseCase,
    private val deriveSolAddressUseCase: DeriveSolAddressUseCase,
    private val getBtcPublicKeyUseCase: GetBtcPublicKeyUseCase,
    private val getMultichainAddressesUseCase: GetMultichainAddressesUseCase,
    private val getMultichainAddressesByIndexUseCase: GetMultichainAddressesByIndexUseCase,
    private val createRecoveryBackupUseCase: CreateRecoveryBackupUseCase,
    private val restoreVaultFromRecoveryBackupUseCase: RestoreVaultFromRecoveryBackupUseCase,
    private val verifyRecoveryBackupUseCase: VerifyRecoveryBackupUseCase,
    private val createCloudRecoveryBlobUseCase: CreateCloudRecoveryBlobUseCase,
    private val restoreVaultFromCloudRecoveryBlobUseCase: RestoreVaultFromCloudRecoveryBlobUseCase,
    private val verifyCloudRecoveryBlobUseCase: VerifyCloudRecoveryBlobUseCase,
    private val createWalletFromWeb3authKeyUseCase: CreateWalletFromWeb3authKeyUseCase,
    private val restoreWalletFromWeb3authKeyUseCase: RestoreWalletFromWeb3authKeyUseCase
) : ViewModel() {

    // State Management
    private val _operationState = MutableStateFlow<WalletOperationState>(WalletOperationState.Idle)
    val operationState: StateFlow<WalletOperationState> = _operationState.asStateFlow()

    private val _currentVault = MutableStateFlow<VaultRecord?>(null)
    val currentVault: StateFlow<VaultRecord?> = _currentVault.asStateFlow()

    private val _generatedMnemonic = MutableStateFlow("")
    val generatedMnemonic: StateFlow<String> = _generatedMnemonic.asStateFlow()

    private val _multichainAddresses = MutableStateFlow<MultichainAddresses?>(null)
    val multichainAddresses: StateFlow<MultichainAddresses?> = _multichainAddresses.asStateFlow()

    private val _lastSignature = MutableStateFlow("")
    val lastSignature: StateFlow<String> = _lastSignature.asStateFlow()

    private val _recoveryBackup = MutableStateFlow<RecoveryBackup?>(null)
    val recoveryBackup: StateFlow<RecoveryBackup?> = _recoveryBackup.asStateFlow()

    private val _cloudRecoveryBlob = MutableStateFlow<CloudRecoveryBlob?>(null)
    val cloudRecoveryBlob: StateFlow<CloudRecoveryBlob?> = _cloudRecoveryBlob.asStateFlow()

    // ========================================
    // VAULT MANAGEMENT
    // ========================================

    fun generateMnemonic() {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val mnemonic = generateMnemonicUseCase()
                _generatedMnemonic.value = mnemonic
                _operationState.value = WalletOperationState.Success("Mnemonic generated")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun createVault(pin: String) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val vault = createVaultUseCase(pin)
                _currentVault.value = vault
                _operationState.value = WalletOperationState.Success("Vault created")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun createVaultFromMnemonic(pin: String, mnemonic: String, path: String = "m/44'/60'/0'/0/0") {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val vault = createVaultFromMnemonicUseCase(pin, mnemonic, path)
                _currentVault.value = vault
                _operationState.value = WalletOperationState.Success("Vault created from mnemonic")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun createVaultFromPrivateKey(pin: String, privateKeyHex: String) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val vault = createVaultFromPrivateKeyUseCase(pin, privateKeyHex)
                _currentVault.value = vault
                _operationState.value = WalletOperationState.Success("Vault created from private key")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun verifyPin(pin: String, vault: VaultRecord) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val address = verifyPinUseCase(pin, vault)
                _operationState.value = WalletOperationState.Success("PIN verified: $address")
            } catch (e: VaultError.InvalidPin) {
                _operationState.value = WalletOperationState.Error("Invalid PIN")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun rotatePin(oldPin: String, newPin: String, vault: VaultRecord) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val newVault = rotatePinUseCase(oldPin, newPin, vault)
                _currentVault.value = newVault
                _operationState.value = WalletOperationState.Success("PIN rotated successfully")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun exportEthPrivateKey(pin: String, vault: VaultRecord) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val privateKey = exportEthPrivateKeyUseCase(pin, vault)
                _lastSignature.value = privateKey
                _operationState.value = WalletOperationState.Success("Private key exported")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun migrateVault(pin: String, vault: VaultRecord) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val migratedVault = migrateVaultUseCase(pin, vault)
                _currentVault.value = migratedVault
                _operationState.value = WalletOperationState.Success("Vault migrated")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // ========================================
    // TRANSACTION SIGNING
    // ========================================

    fun signLegacyTransaction(pin: String, vault: VaultRecord, tx: UnsignedLegacyTx) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val signature = signTransactionUseCase(pin, vault, tx)
                _lastSignature.value = signature
                _operationState.value = WalletOperationState.Success("Transaction signed")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun signEip1559Transaction(pin: String, vault: VaultRecord, tx: UnsignedEip1559Tx) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val signature = signTransactionEip1559UseCase(pin, vault, tx)
                _lastSignature.value = signature
                _operationState.value = WalletOperationState.Success("Transaction signed (EIP-1559)")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun signSolanaMessage(pin: String, vault: VaultRecord, message: ByteArray) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val signature = signSolanaTransactionUseCase(pin, vault, message)
                _lastSignature.value = signature.joinToString("") { "%02x".format(it) }
                _operationState.value = WalletOperationState.Success("Solana message signed")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun signBitcoinTransaction(pin: String, vault: VaultRecord, sighashHex: String, testnet: Boolean) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val signature = signBitcoinTransactionUseCase(pin, vault, sighashHex, testnet)
                _lastSignature.value = signature
                _operationState.value = WalletOperationState.Success("Bitcoin transaction signed")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // ========================================
    // KEY DERIVATION & ADDRESSES
    // ========================================

    fun deriveBtcAddress(mnemonic: String, testnet: Boolean) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val address = deriveBtcAddressUseCase(mnemonic, testnet)
                _operationState.value = WalletOperationState.Success("BTC Address: $address")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun deriveSolAddress(mnemonic: String) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val address = deriveSolAddressUseCase(mnemonic)
                _operationState.value = WalletOperationState.Success("SOL Address: $address")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun getMultichainAddresses(pin: String, vault: VaultRecord, testnet: Boolean) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val addresses = getMultichainAddressesUseCase(pin, vault, testnet)
                _multichainAddresses.value = addresses
                _operationState.value = WalletOperationState.Success("Addresses retrieved")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun getMultichainAddressesByIndex(pin: String, vault: VaultRecord, testnet: Boolean, index: Int) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val addresses = getMultichainAddressesByIndexUseCase(pin, vault, testnet, index)
                _multichainAddresses.value = addresses
                _operationState.value = WalletOperationState.Success("Addresses at index $index retrieved")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // ========================================
    // RECOVERY & BACKUP
    // ========================================

    fun createRecoveryBackup(pin: String, vault: VaultRecord, passphrase: String) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val backup = createRecoveryBackupUseCase(pin, vault, passphrase)
                _recoveryBackup.value = backup
                _operationState.value = WalletOperationState.Success("Recovery backup created")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun verifyRecoveryBackup(passphrase: String, backup: RecoveryBackup) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                verifyRecoveryBackupUseCase(passphrase, backup)
                _operationState.value = WalletOperationState.Success("Recovery backup verified")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun restoreFromRecoveryBackup(passphrase: String, backup: RecoveryBackup, newPin: String) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val vault = restoreVaultFromRecoveryBackupUseCase(passphrase, backup, newPin)
                _currentVault.value = vault
                _operationState.value = WalletOperationState.Success("Vault restored from recovery backup")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun createCloudRecoveryBlob(pin: String, vault: VaultRecord, oauthKek: String) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val blob = createCloudRecoveryBlobUseCase(pin, vault, oauthKek)
                _cloudRecoveryBlob.value = blob
                _operationState.value = WalletOperationState.Success("Cloud recovery blob created")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun verifyCloudRecoveryBlob(oauthKek: String, blob: CloudRecoveryBlob) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                verifyCloudRecoveryBlobUseCase(oauthKek, blob)
                _operationState.value = WalletOperationState.Success("Cloud recovery blob verified")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun restoreFromCloudRecoveryBlob(oauthKek: String, blob: CloudRecoveryBlob, newPin: String) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val vault = restoreVaultFromCloudRecoveryBlobUseCase(oauthKek, blob, newPin)
                _currentVault.value = vault
                _operationState.value = WalletOperationState.Success("Vault restored from cloud recovery")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun createWalletFromWeb3auth(web3authKey: String, testnet: Boolean) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val result = createWalletFromWeb3authKeyUseCase(web3authKey, testnet)
                _operationState.value = WalletOperationState.Success("Web3Auth wallet created")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun restoreWalletFromWeb3auth(web3authKey: String, encryptedData: String, testnet: Boolean) {
        viewModelScope.launch {
            try {
                _operationState.value = WalletOperationState.Loading
                val result = restoreWalletFromWeb3authKeyUseCase(web3authKey, encryptedData, testnet)
                _operationState.value = WalletOperationState.Success("Web3Auth wallet restored")
            } catch (e: Exception) {
                _operationState.value = WalletOperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _operationState.value = WalletOperationState.Idle
    }
}
