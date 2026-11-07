package com.example.catatrapi.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.catatrapi.domain.model.GoalContribution
import com.example.catatrapi.domain.model.GoalStatus
import com.example.catatrapi.ui.components.CurrencyFormatter
import com.example.catatrapi.ui.components.DateFormatterId
import com.example.catatrapi.ui.theme.finance
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun GoalDetailScreen(
    uiState: GoalDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onContribute: (walletId: Long, amount: BigDecimal) -> Unit,
    onWithdraw: (amount: BigDecimal) -> Unit,
    onConsumeMessage: () -> Unit,
) {
    var sheetMode by remember { mutableStateOf<GoalAmountMode?>(null) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onConsumeMessage()
        }
    }

    val goal = uiState.goal
    when {
        uiState.isLoading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        goal == null -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Target tidak ditemukan.", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onBack) { Text("Kembali") }
        }

        else -> {
            val evaluation = uiState.evaluation
            val rate = uiState.rate
            val now = System.currentTimeMillis()
            val side = Modifier.padding(horizontal = 16.dp)
            val progress = if (goal.targetAmount.signum() <= 0) {
                0f
            } else {
                uiState.savedAmount.divide(goal.targetAmount, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        GoalStatusBadge(status = uiState.displayStatus)
                    }
                }

                item {
                    Card(modifier = side.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = CurrencyFormatter.format(uiState.savedAmount),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "dari ${CurrencyFormatter.format(goal.targetAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            LabeledRow("Tenggat", DateFormatterId.full(goal.deadline))
                            LabeledRow(
                                "Sisa waktu",
                                if (uiState.displayStatus == GoalStatus.ACHIEVED) {
                                    "Target tercapai"
                                } else {
                                    remainingDaysLabel(evaluation?.daysUntilDeadline ?: 0L)
                                },
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = side.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { sheetMode = GoalAmountMode.DEPOSIT },
                            enabled = uiState.wallets.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        ) { Text("Setor") }
                        OutlinedButton(
                            onClick = { sheetMode = GoalAmountMode.WITHDRAW },
                            enabled = uiState.savedAmount.signum() > 0,
                            modifier = Modifier.weight(1f),
                        ) { Text("Tarik") }
                        // TODO(Auto-Sweep): tombol Auto-Sweep ditambahkan di sini setelah use case-nya ada
                        // (belum ada di 3.1; sengaja tidak ditampilkan).
                    }
                }

                if (rate != null) {
                    item {
                        Card(modifier = side.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Saran Menabung",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (rate.remainingAmount.signum() == 0) {
                                    Text(
                                        text = "Target sudah terpenuhi, tidak perlu menabung lagi.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    LabeledRow("Per hari", CurrencyFormatter.format(rate.dailyRate.wholeRupiahCeiling()))
                                    LabeledRow("Per minggu", CurrencyFormatter.format(rate.weeklyRate.wholeRupiahCeiling()))
                                    LabeledRow("Kekurangan", CurrencyFormatter.format(rate.remainingAmount))
                                    if (rate.isDelayed) {
                                        Text(
                                            text = "Tenggat sudah terlewat. Saran dihitung untuk 1 hari.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (evaluation != null) {
                    item {
                        Card(modifier = side.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Kecepatan Menabung",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                LabeledRow(
                                    "Saving Velocity (30 hari)",
                                    "${CurrencyFormatter.format(evaluation.velocityPerDay.wholeRupiahHalfUp())} / hari",
                                )
                                LabeledRow("Perkiraan selesai (ETA)", etaLabel(evaluation.etaDays, now))
                                LabeledRow("Tenggat", DateFormatterId.short(goal.deadline))

                                val finance = MaterialTheme.finance
                                val etaDays = evaluation.etaDays
                                val (verdict, verdictColor) = when {
                                    uiState.displayStatus == GoalStatus.ACHIEVED -> null to Color.Unspecified
                                    etaDays == null ->
                                        "Belum ada tabungan bersih dalam 30 hari terakhir, sehingga ETA tidak dapat dihitung." to
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                    etaDays <= evaluation.daysUntilDeadline ->
                                        "Perkiraan selesai sebelum atau tepat pada tenggat." to finance.income
                                    else ->
                                        "Perkiraan selesai melewati tenggat." to finance.warning
                                }
                                if (verdict != null) {
                                    Text(
                                        text = verdict,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = verdictColor,
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.displayStatus != GoalStatus.ACHIEVED) {
                        item {
                            ImpactSimulatorCard(
                                targetAmount = goal.targetAmount,
                                savedAmount = uiState.savedAmount,
                                velocityPerDay = evaluation.velocityPerDay,
                                daysUntilDeadline = evaluation.daysUntilDeadline,
                                nowMillis = now,
                                modifier = side,
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Riwayat Kontribusi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = side,
                    )
                }

                if (uiState.contributions.isEmpty()) {
                    item {
                        Text(
                            text = "Belum ada setoran atau penarikan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = side,
                        )
                    }
                } else {
                    items(uiState.contributions, key = { it.id }) { contribution ->
                        ContributionRow(contribution = contribution, modifier = side)
                        HorizontalDivider(modifier = side)
                    }
                }
            }

            sheetMode?.let { mode ->
                GoalAmountSheet(
                    mode = mode,
                    savedAmount = uiState.savedAmount,
                    currentSpendable = uiState.spendable.spendableAmount,
                    wallets = uiState.wallets,
                    onDismiss = { sheetMode = null },
                    onConfirm = { walletId, amount ->
                        sheetMode = null
                        when (mode) {
                            GoalAmountMode.DEPOSIT -> walletId?.let { onContribute(it, amount) }
                            GoalAmountMode.WITHDRAW -> onWithdraw(amount)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ContributionRow(contribution: GoalContribution, modifier: Modifier = Modifier) {
    val isPositive = contribution.amount.signum() >= 0
    val amountColor = if (isPositive) MaterialTheme.finance.income else MaterialTheme.finance.expense
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contributionTypeLabel(contribution.type),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = DateFormatterId.short(contribution.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = (if (isPositive) "+" else "") + CurrencyFormatter.format(contribution.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = amountColor,
        )
    }
}