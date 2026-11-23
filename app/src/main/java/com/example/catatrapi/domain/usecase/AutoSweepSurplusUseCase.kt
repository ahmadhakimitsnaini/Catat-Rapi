package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.AutoSweepAllocation
import com.example.catatrapi.domain.model.AutoSweepExecutionResult
import com.example.catatrapi.domain.model.AutoSweepPlan
import com.example.catatrapi.domain.model.SavingGoalProgress
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.repository.FinancialPreferencesRepository
import com.example.catatrapi.domain.repository.SavingGoalRepository
import com.example.catatrapi.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * GS-05 (Rumus 2.2.3): One-Tap Auto-Sweep Surplus.
 *
 * Rumus:
 *  - Sweep = max(0, Spendable - B_sweep), dengan default B_sweep = 3 * mu.
 *  - Mengalokasikan surplus secara berurutan ke target aktif berdasarkan `priority` tertinggi hingga penuh (T_j - S_j).
 *  - Mendukung mekanisme Undo 10 detik melalui `undo(contributionIds)`.
 */
class AutoSweepSurplusUseCase @Inject constructor(
    private val calculateSpendableBalanceUseCase: CalculateSpendableBalanceUseCase,
    private val savingGoalRepository: SavingGoalRepository,
    private val transactionRepository: TransactionRepository,
    private val financialPreferencesRepository: FinancialPreferencesRepository,
) {

    fun observePlan(
        nowMillisProvider: () -> Long = { System.currentTimeMillis() },
    ): Flow<AutoSweepPlan> {
        val now = nowMillisProvider()
        val windowStart = now - CalculateImpactWarningUseCase.EXPENSE_WINDOW_DAYS * MILLIS_PER_DAY
        return combine(
            calculateSpendableBalanceUseCase(),
            savingGoalRepository.observeActiveGoalsWithProgress(),
            transactionRepository.observeTransactionsInRange(windowStart, now + MILLIS_PER_DAY),
            financialPreferencesRepository.observeSettings(),
        ) { spendable, goalsWithProgress, recentTransactions, settings ->
            val currentNow = nowMillisProvider()
            val sweepBuffer = resolveSweepBuffer(
                sweepBufferOverride = settings.sweepBufferOverride,
                transactionsLast30Days = recentTransactions,
                now = currentNow,
            )
            planAllocations(
                spendableBalance = spendable.spendableAmount,
                sweepBuffer = sweepBuffer,
                activeGoals = goalsWithProgress,
            )
        }
    }

    suspend fun execute(
        now: Long = System.currentTimeMillis(),
    ): AutoSweepExecutionResult {
        val windowStart = now - CalculateImpactWarningUseCase.EXPENSE_WINDOW_DAYS * MILLIS_PER_DAY
        val recentTransactions = transactionRepository.getTransactionsInRange(windowStart, now)
        val settings = financialPreferencesRepository.getSettings()
        val sweepBuffer = resolveSweepBuffer(
            sweepBufferOverride = settings.sweepBufferOverride,
            transactionsLast30Days = recentTransactions,
            now = now,
        )
        return savingGoalRepository.autoSweepSurplusAtomically(
            sweepBuffer = sweepBuffer,
            now = now,
        )
    }

    suspend fun undo(
        contributionIds: List<Long>,
        now: Long = System.currentTimeMillis(),
    ) {
        savingGoalRepository.undoAutoSweepAtomically(
            contributionIds = contributionIds,
            now = now,
        )
    }

    companion object {
        const val DEFAULT_SWEEP_BUFFER_DAYS = 3L
        private const val MILLIS_PER_DAY = 86_400_000L

        /**
         * Menentukan nilai B_sweep: menggunakan override pengguna bila diatur, atau default 3 * mu (dibulatkan ke Rupiah terdekat).
         */
        fun resolveSweepBuffer(
            sweepBufferOverride: BigDecimal?,
            transactionsLast30Days: List<Transaction>,
            now: Long,
        ): BigDecimal {
            if (sweepBufferOverride != null) {
                return sweepBufferOverride.max(BigDecimal.ZERO)
            }
            val mu = CalculateImpactWarningUseCase.calculateAverageDailyExpense30Days(
                transactions = transactionsLast30Days,
                now = now,
            )
            return mu.multiply(BigDecimal.valueOf(DEFAULT_SWEEP_BUFFER_DAYS))
                .setScale(0, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
        }

        /**
         * Menghitung rencana alokasi surplus berurutan berdasarkan `priority` tertinggi hingga masing-masing target penuh (T_j - S_j).
         */
        fun planAllocations(
            spendableBalance: BigDecimal,
            sweepBuffer: BigDecimal,
            activeGoals: List<SavingGoalProgress>,
        ): AutoSweepPlan {
            val sanitizedBuffer = sweepBuffer.max(BigDecimal.ZERO)
            val rawSurplus = (spendableBalance - sanitizedBuffer).max(BigDecimal.ZERO)
            if (rawSurplus.signum() <= 0 || activeGoals.isEmpty()) {
                return AutoSweepPlan(
                    spendableBalance = spendableBalance,
                    sweepBuffer = sanitizedBuffer,
                    rawSurplus = rawSurplus,
                    totalAllocatedAmount = BigDecimal.ZERO,
                    allocations = emptyList(),
                )
            }

            val sortedGoals = activeGoals
                .filter { !it.goal.isArchived && it.savedAmount < it.goal.targetAmount }
                .sortedWith(
                    compareBy<SavingGoalProgress>(
                        { it.goal.priority },
                        { it.goal.deadline },
                        { it.goal.id },
                    )
                )

            var remainingSurplus = rawSurplus
            val allocations = mutableListOf<AutoSweepAllocation>()

            for (item in sortedGoals) {
                if (remainingSurplus.signum() <= 0) break
                val needed = (item.goal.targetAmount - item.savedAmount).max(BigDecimal.ZERO)
                if (needed.signum() <= 0) continue

                val allocated = remainingSurplus.min(needed)
                if (allocated.signum() > 0) {
                    val newSaved = item.savedAmount + allocated
                    allocations.add(
                        AutoSweepAllocation(
                            goalId = item.goal.id,
                            goalName = item.goal.name,
                            priority = item.goal.priority,
                            currentSavedAmount = item.savedAmount,
                            targetAmount = item.goal.targetAmount,
                            allocatedAmount = allocated,
                            willAchieveGoal = newSaved >= item.goal.targetAmount,
                        )
                    )
                    remainingSurplus -= allocated
                }
            }

            val totalAllocated = allocations.fold(BigDecimal.ZERO) { acc, a -> acc + a.allocatedAmount }
            return AutoSweepPlan(
                spendableBalance = spendableBalance,
                sweepBuffer = sanitizedBuffer,
                rawSurplus = rawSurplus,
                totalAllocatedAmount = totalAllocated,
                allocations = allocations,
            )
        }
    }
}
