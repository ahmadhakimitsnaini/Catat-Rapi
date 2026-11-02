package com.example.catatrapi.ui.cashflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.catatrapi.domain.model.Wallet
import com.example.catatrapi.domain.model.WalletType
import com.example.catatrapi.ui.components.CurrencyFormatter
import com.example.catatrapi.ui.onboarding.toReadableLabel
import java.math.BigDecimal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletFormDialog(
    editingWallet: Wallet? = null,
    onDismiss: () -> Unit,
    onConfirmCreate: (name: String, type: WalletType, initialBalance: BigDecimal) -> Unit,
    onConfirmUpdate: (walletId: Long, name: String, type: WalletType) -> Unit,
) {
    var name by remember(editingWallet) { mutableStateOf(editingWallet?.name ?: "") }
    var selectedType by remember(editingWallet) { mutableStateOf(editingWallet?.type ?: WalletType.BANK) }
    var initialBalanceRaw by remember(editingWallet) {
        mutableStateOf(editingWallet?.balance?.toPlainString() ?: "")
    }
    var inlineError by remember { mutableStateOf<String?>(null) }

    val isEditMode = editingWallet != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (isEditMode) "Edit Dompet" else "Tambah Dompet Baru")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        inlineError = null
                    },
                    label = { Text("Nama Dompet") },
                    placeholder = { Text("Contoh: BCA, Mandiri, GoPay") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Tipe Dompet",
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WalletType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.toReadableLabel()) },
                        )
                    }
                }

                if (!isEditMode) {
                    val parsed = initialBalanceRaw.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    OutlinedTextField(
                        value = initialBalanceRaw,
                        onValueChange = { raw ->
                            initialBalanceRaw = raw.filter { it.isDigit() }
                            inlineError = null
                        },
                        label = { Text("Saldo Awal (Rp)") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        supportingText = {
                            Text("Format: ${CurrencyFormatter.format(parsed)}")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                inlineError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        inlineError = "Nama dompet tidak boleh kosong."
                        return@TextButton
                    }
                    if (editingWallet != null) {
                        onConfirmUpdate(editingWallet.id, trimmed, selectedType)
                    } else {
                        val balance = initialBalanceRaw.takeIf { it.isNotEmpty() }
                            ?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        onConfirmCreate(trimmed, selectedType, balance)
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
    )
}
