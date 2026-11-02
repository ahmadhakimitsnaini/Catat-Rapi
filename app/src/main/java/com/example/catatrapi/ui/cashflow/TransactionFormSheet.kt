package com.example.catatrapi.ui.cashflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.catatrapi.domain.model.SpendableBalance
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionCategory
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.domain.model.Wallet
import com.example.catatrapi.ui.components.CurrencyFormatter
import com.example.catatrapi.ui.theme.finance
import java.math.BigDecimal

/**
 * Form Input Transaksi & Transfer (FT-02, FT-03, FT-04).
 * Dirancang agar input harian selesai maksimal 3 ketuk:
 * 1. Ketik Nominal (atau ketuk chip nominal cepat)
 * 2. Ketuk Chip Kategori (default sudah terpilih kategori utama)
 * 3. Ketuk tombol Simpan
 * Dilengkapi validasi inline dan pratinjau Spendable Balance sebelum disimpan.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormSheet(
    wallets: List<Wallet>,
    currentSpendable: SpendableBalance,
    initialWalletId: Long? = null,
    editingTransaction: Transaction? = null,
    onDismiss: () -> Unit,
    onSaveIncomeOrExpense: (
        walletId: Long,
        type: TransactionType,
        amount: BigDecimal,
        category: TransactionCategory,
        note: String?,
    ) -> Unit,
    onSaveTransfer: (
        fromWalletId: Long,
        toWalletId: Long,
        amount: BigDecimal,
        fee: BigDecimal,
        note: String?,
    ) -> Unit,
    onSaveEdit: (Transaction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditMode = editingTransaction != null

    val supportedTypes = listOf(
        TransactionType.EXPENSE,
        TransactionType.INCOME,
        TransactionType.TRANSFER,
    )

    var selectedType by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.type ?: TransactionType.EXPENSE)
    }

    var selectedWalletId by remember(editingTransaction, wallets, initialWalletId) {
        mutableStateOf(
            editingTransaction?.walletId
                ?: initialWalletId
                ?: wallets.firstOrNull()?.id
                ?: 0L
        )
    }

    var selectedToWalletId by remember(editingTransaction, wallets, selectedWalletId) {
        mutableStateOf(
            editingTransaction?.toWalletId
                ?: wallets.firstOrNull { it.id != selectedWalletId }?.id
                ?: 0L
        )
    }

    var amountInput by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.amount?.toPlainString() ?: "")
    }

    var feeInput by remember(editingTransaction) {
        mutableStateOf(
            editingTransaction?.fee?.takeIf { it > BigDecimal.ZERO }?.toPlainString() ?: ""
        )
    }

    var selectedCategory by remember(editingTransaction) {
        mutableStateOf(
            editingTransaction?.category
                ?: TransactionCategory.FOOD_BEVERAGE
        )
    }

    var noteInput by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.note ?: "")
    }

    // Pastikan kategori selalu sinkron dengan tipe INCOME / EXPENSE
    LaunchedEffect(selectedType) {
        if (selectedType == TransactionType.INCOME || selectedType == TransactionType.EXPENSE) {
            val available = TransactionCategory.forType(selectedType)
            if (selectedCategory !in available) {
                selectedCategory = available.first()
            }
        }
    }

    val amount = amountInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val fee = if (selectedType == TransactionType.TRANSFER) {
        feeInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
    } else {
        BigDecimal.ZERO
    }

    val selectedWallet = wallets.firstOrNull { it.id == selectedWalletId }
    val selectedToWallet = wallets.firstOrNull { it.id == selectedToWalletId }

    // Kalkulasi dampak terhadap Spendable Balance setelah transaksi disimpan
    val previousSpendableDelta = remember(editingTransaction) {
        when (editingTransaction?.type) {
            TransactionType.INCOME -> -editingTransaction.amount
            TransactionType.EXPENSE -> editingTransaction.amount
            TransactionType.TRANSFER -> editingTransaction.fee
            else -> BigDecimal.ZERO
        }
    }

    val newSpendableDelta = when (selectedType) {
        TransactionType.INCOME -> amount
        TransactionType.EXPENSE -> -amount
        TransactionType.TRANSFER -> -fee // Pokok transfer tidak mengubah W maupun Spendable, hanya fee yang mengurangi
        else -> BigDecimal.ZERO
    }

    val projectedSpendable = currentSpendable.spendableAmount + previousSpendableDelta + newSpendableDelta
    val isProjectedNegative = projectedSpendable.signum() < 0

    // Validasi Inline
    val availableWalletBalance = remember(selectedWallet, editingTransaction) {
        val base = selectedWallet?.balance ?: BigDecimal.ZERO
        if (editingTransaction != null && editingTransaction.walletId == selectedWallet?.id) {
            when (editingTransaction.type) {
                TransactionType.EXPENSE -> base + editingTransaction.amount
                TransactionType.TRANSFER -> base + editingTransaction.amount + editingTransaction.fee
                TransactionType.INCOME -> base - editingTransaction.amount
                else -> base
            }
        } else {
            base
        }
    }

    val inlineValidationError: String? = when {
        wallets.isEmpty() -> "Belum ada dompet aktif. Tambahkan dompet terlebih dahulu."
        amount <= BigDecimal.ZERO -> "Masukkan nominal transaksi lebih dari Rp0."
        selectedType == TransactionType.EXPENSE && amount > availableWalletBalance ->
            "Saldo ${selectedWallet?.name ?: "dompet"} (${CurrencyFormatter.format(availableWalletBalance)}) tidak mencukupi."
        selectedType == TransactionType.TRANSFER && wallets.size < 2 ->
            "Minimal 2 dompet aktif diperlukan untuk melakukan transfer."
        selectedType == TransactionType.TRANSFER && selectedWalletId == selectedToWalletId ->
            "Dompet asal dan dompet tujuan tidak boleh sama."
        selectedType == TransactionType.TRANSFER && (amount + fee) > availableWalletBalance ->
            "Saldo ${selectedWallet?.name ?: "dompet asal"} tidak cukup untuk nominal transfer + biaya admin."
        else -> null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (isEditMode) "Edit Transaksi" else "Catat Transaksi Cepat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            // Pemilih Tipe Transaksi
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                supportedTypes.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = supportedTypes.size,
                        ),
                    ) {
                        Text(
                            text = when (type) {
                                TransactionType.EXPENSE -> "Pengeluaran"
                                TransactionType.INCOME -> "Pemasukan"
                                TransactionType.TRANSFER -> "Transfer"
                                else -> type.name
                            }
                        )
                    }
                }
            }

            // Ketuk 1: Input Nominal + Chip Nominal Cepat
            OutlinedTextField(
                value = amountInput,
                onValueChange = { raw -> amountInput = raw.filter { it.isDigit() } },
                label = {
                    Text(
                        if (selectedType == TransactionType.TRANSFER) "Nominal Transfer (Rp)"
                        else "Nominal (Rp)"
                    )
                },
                placeholder = { Text("Contoh: 50000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = {
                    Text("Terformat: ${CurrencyFormatter.format(amount)}")
                },
                modifier = Modifier.fillMaxWidth(),
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("15000", "25000", "50000", "100000", "250000").forEach { preset ->
                    val presetVal = BigDecimal(preset)
                    FilterChip(
                        selected = amountInput == preset,
                        onClick = { amountInput = preset },
                        label = { Text(CurrencyFormatter.format(presetVal)) },
                    )
                }
            }

            // Pemilih Dompet Asal
            Text(
                text = if (selectedType == TransactionType.TRANSFER) "Dari Dompet Asal" else "Dompet",
                style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                wallets.forEach { wallet ->
                    FilterChip(
                        selected = selectedWalletId == wallet.id,
                        onClick = {
                            selectedWalletId = wallet.id
                            if (selectedToWalletId == wallet.id) {
                                wallets.firstOrNull { it.id != wallet.id }?.let { other ->
                                    selectedToWalletId = other.id
                                }
                            }
                        },
                        label = {
                            Text("${wallet.name} (${CurrencyFormatter.format(wallet.balance)})")
                        },
                    )
                }
            }

            if (selectedType == TransactionType.TRANSFER) {
                Text(
                    text = "Ke Dompet Tujuan",
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    wallets.forEach { wallet ->
                        FilterChip(
                            selected = selectedToWalletId == wallet.id,
                            onClick = { selectedToWalletId = wallet.id },
                            enabled = wallet.id != selectedWalletId,
                            label = {
                                Text("${wallet.name} (${CurrencyFormatter.format(wallet.balance)})")
                            },
                        )
                    }
                }

                OutlinedTextField(
                    value = feeInput,
                    onValueChange = { raw -> feeInput = raw.filter { it.isDigit() } },
                    label = { Text("Biaya Admin (Opsional, Rp)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = {
                        if (fee > BigDecimal.ZERO) {
                            Text("Biaya admin ${CurrencyFormatter.format(fee)} akan dicatat sebagai pengeluaran Biaya Admin.")
                        } else {
                            Text("Pokok transfer tidak dihitung sebagai pengeluaran bulanan.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                // Ketuk 2: Pemilih Kategori Langsung (Tanpa Dropdown Berlapis)
                Text(
                    text = "Kategori",
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionCategory.forType(selectedType).forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.labelId) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                label = { Text("Catatan (Opsional)") },
                placeholder = { Text("Contoh: Makan siang, Transfer tabungan") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Kotak Pratinjau Spendable Balance Setelah Transaksi (FT-03)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isProjectedNegative) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Pratinjau Spendable Balance (Saldo Bebas Pakai)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Saat Ini",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = CurrencyFormatter.format(currentSpendable.spendableAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Text(
                            text = "→",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Setelah Transaksi",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = CurrencyFormatter.format(projectedSpendable),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isProjectedNegative -> MaterialTheme.colorScheme.error
                                    newSpendableDelta > BigDecimal.ZERO -> MaterialTheme.finance.income
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }

                    if (isProjectedNegative) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Peringatan: Transaksi ini membuat Spendable Balance menjadi negatif (alokasi target melebihi sisa saldo bebas pakai).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            if (inlineValidationError != null && amountInput.isNotEmpty()) {
                Text(
                    text = inlineValidationError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Ketuk 3: Tombol Simpan
            Button(
                onClick = {
                    if (editingTransaction != null) {
                        val updated = editingTransaction.copy(
                            walletId = selectedWalletId,
                            toWalletId = if (selectedType == TransactionType.TRANSFER) selectedToWalletId else null,
                            type = selectedType,
                            amount = amount,
                            fee = fee,
                            category = if (selectedType == TransactionType.TRANSFER) null else selectedCategory,
                            note = noteInput.trim().takeIf { it.isNotEmpty() },
                        )
                        onSaveEdit(updated)
                    } else if (selectedType == TransactionType.TRANSFER) {
                        onSaveTransfer(
                            selectedWalletId,
                            selectedToWalletId,
                            amount,
                            fee,
                            noteInput,
                        )
                    } else {
                        onSaveIncomeOrExpense(
                            selectedWalletId,
                            selectedType,
                            amount,
                            selectedCategory,
                            noteInput,
                        )
                    }
                },
                enabled = inlineValidationError == null && selectedWallet != null &&
                        (selectedType != TransactionType.TRANSFER || selectedToWallet != null),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isEditMode) "Simpan Perubahan" else "Simpan Transaksi")
            }
        }
    }
}
