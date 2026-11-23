package com.example.catatrapi.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.catatrapi.domain.model.GoalImpactWarning
import com.example.catatrapi.domain.model.GoalStatus
import com.example.catatrapi.domain.model.ImpactWarningReport
import com.example.catatrapi.domain.model.SpendableBalance
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.ui.components.CurrencyFormatter
import com.example.catatrapi.ui.components.DateFormatterId
import com.example.catatrapi.ui.goals.GoalStatusBadge
import com.example.catatrapi.ui.goals.etaLabel
import com.example.catatrapi.ui.theme.finance
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Sprint 4.3: Tab 1 Dashboard (MVP)
 * Menampilkan:
 * 1. Kartu Saldo (Total Saldo W vs Spendable Balance berdampingan)
 * 2. Banner Impact Warning (GS-04)
 * 3. Tombol Aksi Cepat (Auto-Sweep Surplus GS-05, Transfer FT-04)
 * 4. Ringkasan 3 Target Prioritas Teratas
 * 5. 5 Transaksi Terbaru
 */
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    snackbarHostState: SnackbarHostState,
    onOpenGoalDetail: (goalId: Long) -> Unit,
    onOpenGoalsTab: () -> Unit,
    onOpenCashflowTab: () -> Unit,
    onQuickTransferClick: () -> Unit,
    onEditTransactionClick: (Transaction) -> Unit,
    onExecuteAutoSweep: () -> Unit,
    onDismissImpactBanner: () -> Unit,
    onSaveFinancialSettings: (
        minimumBuffer: BigDecimal,
        sweepBufferOverride: BigDecimal?,
        dailyExpenseLimitOverride: BigDecimal?,
        biometricLockEnabled: Boolean,
        lockTimeoutSeconds: Long,
    ) -> Unit,
    onConsumeMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAutoSweepDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onConsumeMessage()
        }
    }

    val walletNameById = remember(uiState.wallets) {
        uiState.wallets.associate { it.id to it.name }
    }

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header + Tombol Pengaturan Buffer & Keamanan
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Beranda CatatRapi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Ringkasan saldo bebas pakai & progres tujuan Anda",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { showSettingsSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Pengaturan Buffer & Keamanan",
                    )
                }
            }
        }

        // 1. Kartu Saldo: Total Saldo (W) vs Spendable Balance berdampingan (FT-03)
        item {
            DashboardBalanceCard(
                spendableBalance = uiState.spendableBalance,
                onAllocatedClick = onOpenGoalsTab,
                onBufferClick = { showSettingsSheet = true },
            )
        }

        // 2. Banner Impact Warning (GS-04)
        if (uiState.impactWarningReport.hasWarnings && !uiState.isImpactBannerDismissed) {
            item {
                ImpactWarningBanner(
                    report = uiState.impactWarningReport,
                    onClickGoal = { goalId -> onOpenGoalDetail(goalId) },
                    onDismiss = onDismissImpactBanner,
                )
            }
        }

        // 3. Tombol Aksi Cepat (Auto-Sweep & Transfer)
        item {
            QuickActionsSection(
                autoSweepAmount = uiState.autoSweepPlan.totalAllocatedAmount,
                canAutoSweep = uiState.autoSweepPlan.canExecute,
                canTransfer = uiState.wallets.size >= 2,
                onAutoSweepClick = { showAutoSweepDialog = true },
                onTransferClick = onQuickTransferClick,
            )
        }

        // 4. Ringkasan 3 Target Prioritas Teratas
        item {
            TopGoalsSection(
                topGoals = uiState.topGoals,
                totalActiveGoalCount = uiState.totalActiveGoalCount,
                onOpenGoalDetail = onOpenGoalDetail,
                onOpenGoalsTab = onOpenGoalsTab,
            )
        }

        // 5. 5 Transaksi Terbaru
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Transaksi Terbaru",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onOpenCashflowTab) {
                    Text("Lihat Semua")
                }
            }
        }

        if (uiState.recentTransactions.isEmpty()) {
            item {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Belum ada transaksi tercatat. Gunakan tombol '+ Transaksi' untuk mencatat pemasukan atau pengeluaran pertama Anda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            items(uiState.recentTransactions, key = { it.id }) { tx ->
                RecentTransactionCard(
                    transaction = tx,
                    walletName = walletNameById[tx.walletId] ?: "Dompet #${tx.walletId}",
                    toWalletName = tx.toWalletId?.let { walletNameById[it] ?: "Dompet #$it" },
                    onClick = { onEditTransactionClick(tx) },
                )
            }
        }
    }

    if (showAutoSweepDialog) {
        AutoSweepConfirmDialog(
            plan = uiState.autoSweepPlan,
            onDismiss = { showAutoSweepDialog = false },
            onConfirm = {
                showAutoSweepDialog = false
                onExecuteAutoSweep()
            },
        )
    }

    if (showSettingsSheet) {
        FinancialSettingsSheet(
            currentSettings = uiState.financialSettings,
            onDismiss = { showSettingsSheet = false },
            onSave = { minBuf, sweepBuf, dailyLimit, bioEnabled, timeoutSec ->
                showSettingsSheet = false
                onSaveFinancialSettings(minBuf, sweepBuf, dailyLimit, bioEnabled, timeoutSec)
            },
        )
    }
}

@Composable
private fun DashboardBalanceCard(
    spendableBalance: SpendableBalance,
    onAllocatedClick: () -> Unit,
    onBufferClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Saldo Dompet (W)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                    Text(
                        text = CurrencyFormatter.format(spendableBalance.totalWalletBalance),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAllocatedClick() },
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = "Spendable Balance",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                    Text(
                        text = CurrencyFormatter.format(spendableBalance.spendableAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (spendableBalance.isOverAllocated) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Dialokasikan ke Target (G): ${CurrencyFormatter.format(spendableBalance.allocatedGoalsAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    modifier = Modifier.clickable { onAllocatedClick() },
                )
                Text(
                    text = "Buffer (B_min): ${CurrencyFormatter.format(spendableBalance.minimumBuffer)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    modifier = Modifier.clickable { onBufferClick() },
                )
            }

            if (spendableBalance.isOverAllocated) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Alokasi melebihi saldo! Kurangi alokasi target atau tambah saldo dompet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImpactWarningBanner(
    report: ImpactWarningReport,
    onClickGoal: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val primary: GoalImpactWarning = report.primaryWarning ?: return
    val isHighPriority = report.hasHighPriorityWarning
    val containerColor = if (isHighPriority) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = if (isHighPriority) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }
    val excessFormatted = CurrencyFormatter.format(
        primary.excessAmount.setScale(0, RoundingMode.HALF_UP)
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickGoal(primary.goalId) },
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = if (isHighPriority) {
                            "Impact Warning Prioritas Tinggi!"
                        } else {
                            "Impact Warning Pengeluaran Harian"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup peringatan",
                        tint = contentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Text(
                text = "Belanja hari ini $excessFormatted di atas rata-rata; target ${primary.goalName} mundur ${primary.delayDays} hari" +
                        if (primary.droppedToDelayed) " (status jatuh ke Tertunda)." else ".",
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )

            if (report.warnings.size > 1) {
                Text(
                    text = "+${report.warnings.size - 1} target lainnya juga ikut mundur. Ketuk untuk melihat detail.",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    autoSweepAmount: BigDecimal,
    canAutoSweep: Boolean,
    canTransfer: Boolean,
    onAutoSweepClick: () -> Unit,
    onTransferClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Aksi Cepat",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(
                onClick = onAutoSweepClick,
                enabled = canAutoSweep,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (canAutoSweep) {
                        "Auto-Sweep (${CurrencyFormatter.format(autoSweepAmount)})"
                    } else {
                        "Auto-Sweep Surplus"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            OutlinedButton(
                onClick = onTransferClick,
                enabled = canTransfer,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Transfer")
            }
        }
    }
}

@Composable
private fun TopGoalsSection(
    topGoals: List<DashboardGoalSummary>,
    totalActiveGoalCount: Int,
    onOpenGoalDetail: (Long) -> Unit,
    onOpenGoalsTab: () -> Unit,
) {
    val now = System.currentTimeMillis()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Target Prioritas Teratas ($totalActiveGoalCount)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onOpenGoalsTab) {
                Text(if (topGoals.isEmpty()) "Buat Target" else "Kelola Target")
            }
        }

        if (topGoals.isEmpty()) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Belum ada Target Tabungan aktif.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Tetapkan tujuan tabungan agar saldo Anda terlindungi dari belanja impulsif.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onOpenGoalsTab) {
                        Text("Buat Target Pertama")
                    }
                }
            }
        } else {
            topGoals.forEachIndexed { index, summary ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenGoalDetail(summary.goal.id) },
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "#${index + 1} ${summary.goal.name}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            GoalStatusBadge(status = summary.status)
                        }

                        LinearProgressIndicator(
                            progress = { summary.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${CurrencyFormatter.format(summary.savedAmount)} / ${CurrencyFormatter.format(summary.goal.targetAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (summary.status == GoalStatus.ACHIEVED) {
                                    "Tercapai"
                                } else {
                                    "ETA: ${etaLabel(summary.etaDays, now)}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionCard(
    transaction: Transaction,
    walletName: String,
    toWalletName: String?,
    onClick: () -> Unit,
) {
    val financeColors = MaterialTheme.finance
    val (icon, tint, amountPrefix) = when (transaction.type) {
        TransactionType.INCOME -> Triple(Icons.AutoMirrored.Filled.TrendingUp, financeColors.income, "+")
        TransactionType.EXPENSE -> Triple(Icons.AutoMirrored.Filled.TrendingDown, financeColors.expense, "-")
        TransactionType.TRANSFER -> Triple(Icons.AutoMirrored.Filled.CompareArrows, MaterialTheme.colorScheme.primary, "")
        TransactionType.INVESTMENT_BUY -> Triple(Icons.AutoMirrored.Filled.ShowChart, MaterialTheme.colorScheme.tertiary, "-")
        TransactionType.INVESTMENT_SELL -> Triple(Icons.AutoMirrored.Filled.ShowChart, financeColors.income, "+")
    }

    val title = when (transaction.type) {
        TransactionType.TRANSFER -> "Transfer: $walletName → ${toWalletName ?: "-"}"
        TransactionType.INVESTMENT_BUY -> "Beli Aset Investasi"
        TransactionType.INVESTMENT_SELL -> "Jual Aset Investasi"
        else -> transaction.category?.labelId ?: transaction.type.name
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = buildString {
                    append(walletName)
                    append(" • ")
                    append(DateFormatterId.short(transaction.occurredAt))
                    if (!transaction.note.isNullOrBlank()) {
                        append(" • ")
                        append(transaction.note)
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = "$amountPrefix${CurrencyFormatter.format(transaction.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = tint,
            )
        }
    }
}
