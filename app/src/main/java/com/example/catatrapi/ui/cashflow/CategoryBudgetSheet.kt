package com.example.catatrapi.ui.cashflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.catatrapi.domain.model.CategoryBudgetEvaluation
import com.example.catatrapi.domain.model.TransactionCategory
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.ui.components.CurrencyFormatter
import java.math.BigDecimal
import java.time.YearMonth

/**
 * FT-05: Sheet pengelolaan batas anggaran bulanan per kategori pengeluaran.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryBudgetSheet(
    selectedMonth: YearMonth,
    existingEvaluations: List<CategoryBudgetEvaluation>,
    onDismiss: () -> Unit,
    onSaveBudget: (category: TransactionCategory, limitAmount: BigDecimal) -> Unit,
    onDeleteBudget: (budgetId: Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val expenseCategories = remember {
        TransactionCategory.forType(TransactionType.EXPENSE)
    }

    var selectedCategory by remember {
        mutableStateOf(expenseCategories.first())
    }

    val existingForSelected = existingEvaluations.firstOrNull { it.budget.category == selectedCategory }

    var limitText by remember(selectedCategory, existingForSelected) {
        mutableStateOf(existingForSelected?.budget?.limitAmount?.toPlainString() ?: "")
    }

    val parsedLimit = limitText.toBigDecimalOrNull() ?: BigDecimal.ZERO

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
                text = "Atur Anggaran Kategori ($selectedMonth)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Pilih kategori pengeluaran untuk mengatur batas bulanan. Peringatan akan muncul otomatis pada 80% dan 100%.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                expenseCategories.forEach { category ->
                    val hasBudget = existingEvaluations.any { it.budget.category == category }
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(if (hasBudget) "${category.labelId} ✓" else category.labelId)
                        },
                    )
                }
            }

            OutlinedTextField(
                value = limitText,
                onValueChange = { raw -> limitText = raw.filter { it.isDigit() } },
                label = { Text("Batas Anggaran ${selectedCategory.labelId} (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (existingForSelected != null) {
                    IconButton(
                        onClick = {
                            onDeleteBudget(existingForSelected.budget.id)
                            limitText = ""
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus anggaran kategori ini",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Tutup")
                }
                Button(
                    onClick = {
                        onSaveBudget(selectedCategory, parsedLimit)
                    },
                    enabled = parsedLimit.signum() > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (existingForSelected != null) "Perbarui" else "Simpan")
                }
            }

            if (existingEvaluations.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Daftar Anggaran Aktif Bulan Ini:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                existingEvaluations.forEach { eval ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = eval.budget.category.labelId,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "${CurrencyFormatter.format(eval.spentAmount)} / ${CurrencyFormatter.format(eval.budget.limitAmount)} (${eval.usagePercentage}%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
