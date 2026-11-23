package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.GoalContribution
import com.example.catatrapi.domain.model.GoalImpactWarning
import com.example.catatrapi.domain.model.GoalStatus
import com.example.catatrapi.domain.model.ImpactWarningReport
import com.example.catatrapi.domain.model.SavingGoal
import com.example.catatrapi.domain.model.SavingGoalProgress
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.domain.repository.FinancialPreferencesRepository
import com.example.catatrapi.domain.repository.SavingGoalRepository
import com.example.catatrapi.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * GS-04 (Rumus 2.2.3): Menghitung Impact Warning akibat pembengkakan pengeluaran harian terhadap ETA Target Tabungan.
 *
 * Rumus:
 *  - mu = rata-rata pengeluaran harian 30 hari terakhir (EXPENSE + TRANSFER.fee > 0) atau batas harian manual.
 *  - Nonaktif bila histori transaksi < 7 hari tanpa batas harian manual, atau bila v <= 0.
 *  - Delta = max(0, x - mu), dengan x = total pengeluaran hari ini.
 *  - Hari mundur = ceil((T - S + Delta) / v) - ceil((T - S) / v).
 *  - Peringatan muncul bila Delta > 0 dan Hari mundur >= 1.
 *  - Prioritas tinggi (droppedToDelayed) bila status jatuh dari ON_TRACK menjadi DELAYED.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalculateImpactWarningUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val savingGoalRepository: SavingGoalRepository,
    private val financialPreferencesRepository: FinancialPreferencesRepository,
) {

    fun observe(
        nowMillisProvider: () -> Long = { System.currentTimeMillis() },
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Flow<ImpactWarningReport> {
        val now = nowMillisProvider()
        val windowStart = now - EXPENSE_WINDOW_DAYS * MILLIS_PER_DAY
        return combine(
            transactionRepository.observeTransactionsInRange(windowStart, now + MILLIS_PER_DAY),
            transactionRepository.observeEarliestTransactionTimestamp(),
            savingGoalRepository.observeActiveGoalsWithProgress(),
            savingGoalRepository.observeActiveGoalContributions(),
            financialPreferencesRepository.observeSettings(),
        ) { recentTransactions, earliestTimestamp, goalsWithProgress, allContributions, settings ->
            val currentNow = nowMillisProvider()
            val contributionsByGoal = allContributions.groupBy { it.goalId }
            val goalsAndContributions = goalsWithProgress.map { progress ->
                progress.goal to (contributionsByGoal[progress.goal.id] ?: emptyList())
            }
            calculate(
                transactionsLast30Days = recentTransactions,
                earliestTransactionTimestamp = earliestTimestamp,
                goalsWithContributions = goalsAndContributions,
                manualDailyLimit = settings.dailyExpenseLimitOverride,
                now = currentNow,
                zoneId = zoneId,
            )
        }
    }

    suspend fun evaluateCurrent(
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): ImpactWarningReport {
        val windowStart = now - EXPENSE_WINDOW_DAYS * MILLIS_PER_DAY
        val transactions = transactionRepository.getTransactionsInRange(windowStart, now)
        val earliest = transactionRepository.getEarliestTransactionTimestamp()
        val goals = savingGoalRepository.getActiveGoalsWithProgress()
        val contributions = savingGoalRepository.getActiveGoalContributions().groupBy { it.goalId }
        val settings = financialPreferencesRepository.getSettings()
        return calculate(
            transactionsLast30Days = transactions,
            earliestTransactionTimestamp = earliest,
            goalsWithContributions = goals.map { it.goal to (contributions[it.goal.id] ?: emptyList()) },
            manualDailyLimit = settings.dailyExpenseLimitOverride,
            now = now,
            zoneId = zoneId,
        )
    }

    companion object {
        const val EXPENSE_WINDOW_DAYS = 30L
        const val MIN_HISTORY_DAYS = 7L
        private const val MILLIS_PER_DAY = 86_400_000L

        fun calculate(
            transactionsLast30Days: List<Transaction>,
            earliestTransactionTimestamp: Long?,
            goalsWithContributions: List<Pair<SavingGoal, List<GoalContribution>>>,
            manualDailyLimit: BigDecimal? = null,
            now: Long,
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): ImpactWarningReport {
            val hasManualLimit = manualDailyLimit != null && manualDailyLimit.signum() > 0
            val hasEnoughHistory = earliestTransactionTimestamp != null &&
                    CalculateDynamicSavingRateUseCase.daysBetween(earliestTransactionTimestamp, now, zoneId) >= MIN_HISTORY_DAYS

            if (!hasManualLimit && !hasEnoughHistory) {
                return ImpactWarningReport(isEnabled = false)
            }

            val todayExpense = calculateTodayExpense(transactionsLast30Days, now, zoneId)
            val dailyAllowance = if (hasManualLimit) {
                manualDailyLimit
            } else {
                calculateAverageDailyExpense30Days(transactionsLast30Days, now)
            }

            val excessAmount = (todayExpense - dailyAllowance).max(BigDecimal.ZERO)
            if (excessAmount.signum() <= 0) {
                return ImpactWarningReport(
                    isEnabled = true,
                    todayExpense = todayExpense,
                    dailyAllowance = dailyAllowance,
                    excessAmount = BigDecimal.ZERO,
                    warnings = emptyList(),
                )
            }

            val warnings = goalsWithContributions
                .filter { (goal, _) -> !goal.isArchived }
                .sortedWith(compareBy<Pair<SavingGoal, List<GoalContribution>>>({ it.first.priority }, { it.first.deadline }, { it.first.id }))
                .mapNotNull { (goal, contributions) ->
                    evaluateSingleGoalImpact(
                        goal = goal,
                        contributions = contributions,
                        todayExpense = todayExpense,
                        dailyAllowance = dailyAllowance,
                        excessAmount = excessAmount,
                        now = now,
                        zoneId = zoneId,
                    )
                }

            return ImpactWarningReport(
                isEnabled = true,
                todayExpense = todayExpense,
                dailyAllowance = dailyAllowance,
                excessAmount = excessAmount,
                warnings = warnings,
            )
        }

        /**
         * Menghitung rata-rata pengeluaran harian 30 hari terakhir (mu).
         * Tidak termasuk pokok transfer dan konversi aset investasi, namun menyertakan biaya admin transfer (TRANSFER.fee > 0).
         */
        fun calculateAverageDailyExpense30Days(
            transactions: List<Transaction>,
            now: Long,
        ): BigDecimal {
            val windowStart = now - EXPENSE_WINDOW_DAYS * MILLIS_PER_DAY
            val totalExpense30Days = transactions
                .filter { it.occurredAt in windowStart..now }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.consumptiveExpenseAmount() }
            return totalExpense30Days.divide(
                BigDecimal.valueOf(EXPENSE_WINDOW_DAYS),
                MathContext.DECIMAL128,
            )
        }

        /**
         * Menghitung total pengeluaran konsumtif pada tanggal hari ini (x).
         */
        fun calculateTodayExpense(
            transactions: List<Transaction>,
            now: Long,
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): BigDecimal {
            val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
            return transactions
                .filter { tx ->
                    Instant.ofEpochMilli(tx.occurredAt).atZone(zoneId).toLocalDate() == today
                }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.consumptiveExpenseAmount() }
        }

        internal fun Transaction.consumptiveExpenseAmount(): BigDecimal =
            when (type) {
                TransactionType.EXPENSE -> amount
                TransactionType.TRANSFER -> if (fee > BigDecimal.ZERO) fee else BigDecimal.ZERO
                TransactionType.INCOME, TransactionType.INVESTMENT_BUY, TransactionType.INVESTMENT_SELL -> BigDecimal.ZERO
            }

        private fun evaluateSingleGoalImpact(
            goal: SavingGoal,
            contributions: List<GoalContribution>,
            todayExpense: BigDecimal,
            dailyAllowance: BigDecimal,
            excessAmount: BigDecimal,
            now: Long,
            zoneId: ZoneId,
        ): GoalImpactWarning? {
            val evaluation = EvaluateGoalStatusUseCase.evaluate(goal, contributions, now, zoneId)
            val velocity = evaluation.velocityPerDay
            val baselineEta = evaluation.etaDays

            // Abaikan bila target sudah ACHIEVED (S >= T) atau velocity <= 0 (ETA tidak terdefinisi)
            if (evaluation.status == GoalStatus.ACHIEVED || velocity.signum() <= 0 || baselineEta == null) {
                return null
            }

            val remaining = (goal.targetAmount - evaluation.savedAmount).max(BigDecimal.ZERO)
            if (remaining.signum() <= 0) return null

            val newEta = (remaining + excessAmount)
                .divide(velocity, 0, RoundingMode.CEILING)
                .let { if (it > BigDecimal.valueOf(Long.MAX_VALUE)) Long.MAX_VALUE else it.toLong() }

            val delayDays = newEta - baselineEta
            if (delayDays < 1L) return null

            val newStatus = if (newEta <= evaluation.daysUntilDeadline) {
                GoalStatus.ON_TRACK
            } else {
                GoalStatus.DELAYED
            }
            val droppedToDelayed = evaluation.status == GoalStatus.ON_TRACK && newStatus == GoalStatus.DELAYED

            return GoalImpactWarning(
                goalId = goal.id,
                goalName = goal.name,
                priority = goal.priority,
                todayExpense = todayExpense,
                dailyAllowance = dailyAllowance,
                excessAmount = excessAmount,
                baselineEtaDays = baselineEta,
                newEtaDays = newEta,
                delayDays = delayDays,
                baselineStatus = evaluation.status,
                newStatus = newStatus,
                droppedToDelayed = droppedToDelayed,
            )
        }
    }
}
