package com.example.catatrapi.ui.dashboard

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.example.catatrapi.domain.model.FinancialSettings
import java.math.BigDecimal

/**
 * Sheet pengaturan Buffer Finansial (B_min, B_sweep, Batas Harian Manual) dan Keamanan Biometrik (NFR-S1, NFR-S3).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FinancialSettingsSheet(
    currentSettings: FinancialSettings,
    onDismiss: () -> Unit,
    onSave: (
        minimumBuffer: BigDecimal,
        sweepBufferOverride: BigDecimal?,
        dailyExpenseLimitOverride: BigDecimal?,
        biometricLockEnabled: Boolean,
        lockTimeoutSeconds: Long,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var minBufferText by remember(currentSettings) {
        mutableStateOf(
            currentSettings.minimumBuffer.takeIf { it > BigDecimal.ZERO }?.toPlainString() ?: ""
        )
    }
    var sweepBufferText by remember(currentSettings) {
        mutableStateOf(currentSettings.sweepBufferOverride?.toPlainString() ?: "")
    }
    var dailyLimitText by remember(currentSettings) {
        mutableStateOf(currentSettings.dailyExpenseLimitOverride?.toPlainString() ?: "")
    }
    var biometricEnabled by remember(currentSettings) {
        mutableStateOf(currentSettings.biometricLockEnabled)
    }
    var timeoutSeconds by remember(currentSettings) {
        mutableStateOf(currentSettings.lockTimeoutSeconds)
    }

    val timeoutOptions = listOf(
        0L to "Segera",
        30L to "30 Detik",
        60L to "1 Menit",
        300L to "5 Menit",
    )

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
                text = "Pengaturan Buffer & Keamanan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Buffer Finansial (Rumus 2.2.1 & 2.2.3)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = minBufferText,
                onValueChange = { raw -> minBufferText = raw.filter { it.isDigit() } },
                label = { Text("Buffer Minimum Spendable / B_min (Rp)") },
                supportingText = { Text("Dana cadangan minimum yang tidak ikut dihitung sebagai Spendable.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = sweepBufferText,
                onValueChange = { raw -> sweepBufferText = raw.filter { it.isDigit() } },
                label = { Text("Buffer Auto-Sweep / B_sweep Opsional (Rp)") },
                supportingText = { Text("Kosongkan untuk otomatis menggunakan rata-rata pengeluaran 3 hari (3 × μ).") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = dailyLimitText,
                onValueChange = { raw -> dailyLimitText = raw.filter { it.isDigit() } },
                label = { Text("Batas Pengeluaran Harian Manual Opsional (Rp)") },
                supportingText = { Text("Kosongkan untuk otomatis menggunakan rata-rata harian 30 hari terakhir (μ).") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            Text(
                text = "Keamanan Lokal (NFR-S1 & NFR-S3)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kunci Biometrik / PIN Perangkat",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Minta verifikasi sidik jari/wajah atau PIN saat membuka aplikasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = { biometricEnabled = it },
                )
            }

            if (biometricEnabled) {
                Text(
                    text = "Kunci ulang otomatis setelah di latar belakang:",
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    timeoutOptions.forEach { (sec, label) ->
                        FilterChip(
                            selected = timeoutSeconds == sec,
                            onClick = { timeoutSeconds = sec },
                            label = { Text(label) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Batal")
                }
                Button(
                    onClick = {
                        val minBuffer = minBufferText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        val sweepOverride = sweepBufferText.takeIf { it.isNotBlank() }?.toBigDecimalOrNull()
                        val dailyOverride = dailyLimitText.takeIf { it.isNotBlank() }?.toBigDecimalOrNull()
                        onSave(
                            minBuffer,
                            sweepOverride,
                            dailyOverride,
                            biometricEnabled,
                            timeoutSeconds,
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Simpan")
                }
            }
        }
    }
}
