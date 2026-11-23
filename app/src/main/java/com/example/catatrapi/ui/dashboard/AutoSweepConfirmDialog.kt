package com.example.catatrapi.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.catatrapi.domain.model.AutoSweepPlan
import com.example.catatrapi.ui.components.CurrencyFormatter
import com.example.catatrapi.ui.theme.finance

/**
 * GS-05: Dialog konfirmasi bernominal untuk One-Tap Auto-Sweep Surplus.
 * Menampilkan Spendable saat ini, buffer B_sweep yang disisakan, total surplus yang dialokasikan,
 * serta rincian distribusi per target berdasarkan urutan prioritas.
 */
@Composable
fun AutoSweepConfirmDialog(
    plan: AutoSweepPlan,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Konfirmasi Auto-Sweep Surplus",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Surplus Saldo Spendable di atas buffer akhir periode akan dialokasikan secara berurutan ke Target Tabungan prioritas tertinggi:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SummaryRow("Spendable Saat Ini", CurrencyFormatter.format(plan.spendableBalance))
                        SummaryRow("Buffer Disisakan (B_sweep)", CurrencyFormatter.format(plan.sweepBuffer))
                        HorizontalDivider()
                        SummaryRow(
                            label = "Total Dialokasikan",
                            value = CurrencyFormatter.format(plan.totalAllocatedAmount),
                            highlight = true,
                        )
                    }
                }

                if (plan.allocations.isNotEmpty()) {
                    Text(
                        text = "Rincian Penerima Target:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    plan.allocations.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "#${item.priority} ${item.goalName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                if (item.willAchieveGoal) {
                                    Text(
                                        text = "Target akan tercapai penuh!",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.finance.income,
                                    )
                                }
                            }
                            Text(
                                text = "+${CurrencyFormatter.format(item.allocatedAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.finance.income,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = plan.canExecute,
            ) {
                Text("Alokasikan Sekarang", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
    )
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (highlight) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}
