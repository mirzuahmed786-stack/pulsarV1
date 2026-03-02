package com.elementa.wallet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SessionInput(
    evmAddress: String,
    solanaAddress: String,
    onEvmChange: (String) -> Unit,
    onSolanaChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Wallet Scope")
        OutlinedTextField(
            value = evmAddress,
            onValueChange = onEvmChange,
            label = { Text("EVM Address") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = solanaAddress,
            onValueChange = onSolanaChange,
            label = { Text("Solana Address") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
