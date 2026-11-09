package com.example.catatrapi.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.example.catatrapi.ui.components.CurrencyFormatter
import com.example.catatrapi.ui.components.DateFormatterId
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, targetAmount: BigDecimal, deadline: Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var pickedUtcMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amount = amountInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val deadlineMillis: Long? = pickedUtcMillis?.let { pickerUtcToLocalStartOfDay(it) }

    val inlineValidationError: String? = when {
        name.isBlank() -> "Nama target wajib diisi."
        amount.signum() <= 0 -> "Nominal target harus lebih besar dari nol."
        deadlineMillis == null -> "Pilih tenggat waktu."
        deadlineMillis <= System.currentTimeMillis() -> "Tenggat waktu harus berada di masa depan."
        else -> null
    }
    val showError = name.isNotEmpty() || amountInput.isNotEmpty() || pickedUtcMillis != null

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
                text = "Buat Target Tabungan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                label = { Text("Nama target") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = amountInput,
                onValueChange = { raw -> amountInput = raw.filter { it.isDigit() } },
                label = { Text("Nominal target") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text("Terformat: ${CurrencyFormatter.format(amount)}") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = deadlineMillis?.let { "Tenggat: ${DateFormatterId.full(it)}" } ?: "Pilih tenggat waktu",
                )
            }

            if (showError && inlineValidationError != null) {
                Text(
                    text = inlineValidationError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) { Text("Batal") }
                Button(
                    onClick = {
                        if (deadlineMillis != null) onSave(name.trim(), amount, deadlineMillis)
                    },
                    enabled = inlineValidationError == null,
                ) {
                    Text("Simpan")
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = pickedUtcMillis,
            selectableDates = FutureOnlyDates,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { pickedUtcMillis = it }
                        showDatePicker = false
                    },
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Hanya tanggal setelah hari ini (zona waktu perangkat) yang bisa dipilih. */
@OptIn(ExperimentalMaterial3Api::class)
private object FutureOnlyDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val todayUtcMidnight = LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        return utcTimeMillis > todayUtcMidnight
    }

    override fun isSelectableYear(year: Int): Boolean = year >= LocalDate.now().year
}

/** DatePicker mengembalikan tengah malam UTC. Diubah ke awal hari di zona waktu perangkat. */
private fun pickerUtcToLocalStartOfDay(utcMillis: Long): Long =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()