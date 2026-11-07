package com.example.catatrapi.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.catatrapi.domain.model.Wallet
import com.example.catatrapi.ui.components.CurrencyFormatter
import java.math.BigDecimal
import java.math.RoundingMode

enum class GoalAmountMode { DEPOSIT, WITHDRAW }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoalAmountSheet(
    mode: GoalAmountMode,
    savedAmount: BigDecimal,
    currentSpendable: BigDecimal,
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    /** walletId bernilai null untuk Tarik. */
    onConfirm: (walletId: Long?, amount: BigDecimal) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDeposit = mode == GoalAmountMode.DEPOSIT

    var amountInput by remember { mutableStateOf("") }
    var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id) }

    val amount = amountInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val maxAmount = if (isDeposit) currentSpendable.max(BigDecimal.ZERO) else savedAmount

    val projectedSpendable = if (isDeposit) currentSpendable - amount else currentSpendable + amount
    val projectedSaved = if (isDeposit) savedAmount + amount else savedAmount - amount

    val inlineValidationError: String? = when {
        amount.signum() <= 0 -> "Nominal harus lebih besar dari nol."
        isDeposit && selectedWalletId == null -> "Pilih dompet terlebih dahulu."
        isDeposit && amount > currentSpendable -> "Nominal setoran melebihi Saldo Spendable yang tersedia."
        !isDeposit && amount > savedAmount -> "Nominal penarikan melebihi dana yang terkumpul pada target ini."
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
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (isDeposit) "Setor ke Target" else "Tarik dari Target",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = if (isDeposit) {
                    "Spendable saat ini: ${CurrencyFormatter.format(currentSpendable)}"
                } else {
                    "Dana terkumpul: ${CurrencyFormatter.format(savedAmount)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isDeposit) {
                Text(
                    text = "Dompet",
                    style = MaterialTheme.typography.labelLarge,
                )
                if (wallets.isEmpty()) {
                    Text(
                        text = "Belum ada dompet aktif.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        wallets.forEach { wallet ->
                            FilterChip(
                                selected = selectedWalletId == wallet.id,
                                onClick = { selectedWalletId = wallet.id },
                                label = { Text(wallet.name) },
                            )
                        }
                    }
                    Text(
                        text = "Dana tetap berada di dompet. Setoran hanya mengalokasikannya ke target ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = amountInput,
                onValueChange = { raw -> amountInput = raw.filter { it.isDigit() } },
                label = { Text("Nominal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text("Terformat: ${CurrencyFormatter.format(amount)}") },
                modifier = Modifier.fillMaxWidth(),
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(50_000L, 100_000L, 500_000L).forEach { quick ->
                    FilterChip(
                        selected = false,
                        onClick = { amountInput = (amount + BigDecimal.valueOf(quick)).toPlainString() },
                        label = { Text("+${CurrencyFormatter.format(BigDecimal.valueOf(quick))}") },
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = {
                        val whole = maxAmount.setScale(0, RoundingMode.DOWN)
                        amountInput = if (whole.signum() > 0) whole.toPlainString() else ""
                    },
                    label = { Text("Maks") },
                )
            }

            if (amountInput.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (projectedSpendable.signum() < 0) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Pratinjau",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Spendable: ${CurrencyFormatter.format(currentSpendable)} -> ${CurrencyFormatter.format(projectedSpendable)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Dana target: ${CurrencyFormatter.format(savedAmount)} -> ${CurrencyFormatter.format(projectedSaved)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (projectedSpendable.signum() < 0) {
                            Text(
                                text = "Peringatan: Spendable akan bernilai negatif.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                if (inlineValidationError != null) {
                    Text(
                        text = inlineValidationError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) { Text("Batal") }
                Button(
                    onClick = { onConfirm(if (isDeposit) selectedWalletId else null, amount) },
                    enabled = amountInput.isNotEmpty() && inlineValidationError == null,
                ) {
                    Text(if (isDeposit) "Setor" else "Tarik")
                }
            }
        }
    }
}