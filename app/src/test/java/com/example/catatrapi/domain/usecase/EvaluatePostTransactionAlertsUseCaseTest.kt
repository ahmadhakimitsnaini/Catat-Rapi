package com.example.catatrapi.domain.usecase

import androidx.paging.PagingData
import com.example.catatrapi.domain.model.AutoSweepExecutionResult
import com.example.catatrapi.domain.model.CategoryBudget
import com.example.catatrapi.domain.model.CategoryBudgetEvaluation
import com.example.catatrapi.domain.model.ContributionType
import com.example.catatrapi.domain.model.FinancialSettings
import com.example.catatrapi.domain.model.GoalContribution
import com.example.catatrapi.domain.model.GoalImpactWarning
import com.example.catatrapi.domain.model.SavingGoal
import com.example.catatrapi.domain.model.SavingGoalProgress
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionCategory
import com.example.catatrapi.domain.model.TransactionFilter
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.domain.repository.AlertNotificationSink
import com.example.catatrapi.domain.repository.CategoryBudgetRepository
import com.example.catatrapi.domain.repository.FinancialPreferencesRepository
import com.example.catatrapi.domain.repository.SavingGoalRepository
import com.example.catatrapi.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluatePostTransactionAlertsUseCaseTest {

    private val utc = ZoneOffset.UTC
    private val now = LocalDate.parse("2026-11-20").atStartOfDay(utc).plusHours(15).toInstant().toEpochMilli()

    @Test
    fun `memicu notifikasi anggaran 80 persen dan impact warning prioritas tinggi saat syarat terpenuhi`() = runTest {
        val budget = CategoryBudget(
            id = 10L,
            category = TransactionCategory.FOOD_BEVERAGE,
            yearMonth = "2026-11",
            limitAmount = BigDecimal("200000"),
            notified80 = false,
            notified100 = false,
        )
        val todayTx = Transaction(
            id = 1L,
            walletId = 1L,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("170000"), // 85% dari 200.000, sekaligus > batas harian manual 100.000 (Delta = 70.000)
            category = TransactionCategory.FOOD_BEVERAGE,
            occurredAt = now,
            createdAt = now,
        )
        val goal = SavingGoal(
            id = 1L,
            name = "Motor",
            targetAmount = BigDecimal("1000000"),
            deadline = LocalDate.parse("2027-01-31").atStartOfDay(utc).toInstant().toEpochMilli(), // 72 hari lagi
            priority = 1,
            createdAt = LocalDate.parse("2026-10-01").atStartOfDay(utc).toInstant().toEpochMilli(),
        )
        val contributions = listOf(
            GoalContribution(
                id = 1L,
                goalId = 1L,
                walletId = 1L,
                amount = BigDecimal("300000"), // v = 10.000/hari -> ETA awal = 70 hari (ON_TRACK), ETA baru = 77 hari (DELAYED)
                type = ContributionType.DEPOSIT,
                createdAt = LocalDate.parse("2026-11-01").atStartOfDay(utc).toInstant().toEpochMilli(),
            )
        )

        var marked80 = false
        val budgetRepo = object : CategoryBudgetRepository {
            override fun observeBudgetsByMonth(yearMonth: String): Flow<List<CategoryBudget>> = flowOf(listOf(budget))
            override suspend fun getBudgetsByMonth(yearMonth: String): List<CategoryBudget> = listOf(budget)
            override suspend fun getBudget(category: TransactionCategory, yearMonth: String): CategoryBudget = budget
            override suspend fun upsertBudget(category: TransactionCategory, yearMonth: String, limitAmount: BigDecimal, now: Long): Long = 10L
            override suspend fun markNotificationSent(budgetId: Long, notified80: Boolean, notified100: Boolean, now: Long) {
                marked80 = notified80
            }
            override suspend fun deleteBudget(budgetId: Long) = Unit
        }

        val txRepo = object : TransactionRepository {
            override fun observePagedTransactions(filter: TransactionFilter): Flow<PagingData<Transaction>> = flowOf()
            override fun observeTransactionsInRange(from: Long, to: Long): Flow<List<Transaction>> = flowOf(listOf(todayTx))
            override suspend fun getTransactionsInRange(from: Long, to: Long): List<Transaction> = listOf(todayTx)
            override fun observeEarliestTransactionTimestamp(): Flow<Long?> = flowOf(now)
            override suspend fun getEarliestTransactionTimestamp(): Long = now
            override fun observeRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(listOf(todayTx))
            override suspend fun getTransactionById(id: Long): Transaction = todayTx
            override suspend fun recordTransactionAtomically(transaction: Transaction): Long = 1L
            override suspend fun transferBetweenWalletsAtomically(transfer: Transaction): Long = 1L
            override suspend fun editTransactionAtomically(updatedTransaction: Transaction) = Unit
            override suspend fun deleteTransactionAtomically(transactionId: Long) = Unit
        }

        val goalRepo = object : SavingGoalRepository {
            override fun observeActiveGoalsWithProgress(): Flow<List<SavingGoalProgress>> =
                flowOf(listOf(SavingGoalProgress(goal, BigDecimal("300000"))))
            override fun observeActiveGoalContributions(): Flow<List<GoalContribution>> = flowOf(contributions)
            override suspend fun getActiveGoalsWithProgress(): List<SavingGoalProgress> =
                listOf(SavingGoalProgress(goal, BigDecimal("300000")))
            override suspend fun getActiveGoalContributions(): List<GoalContribution> = contributions
            override fun observeGoalById(id: Long): Flow<SavingGoal?> = flowOf(goal)
            override fun observeContributions(goalId: Long): Flow<List<GoalContribution>> = flowOf(contributions)
            override fun observeSavedAmount(goalId: Long): Flow<BigDecimal> = flowOf(BigDecimal("300000"))
            override suspend fun getGoalById(id: Long): SavingGoal = goal
            override suspend fun getSavedAmount(goalId: Long): BigDecimal = BigDecimal("300000")
            override suspend fun createGoalAtomically(goal: SavingGoal): Long = 1L
            override suspend fun contributeToGoalAtomically(goalId: Long, walletId: Long, amount: BigDecimal, now: Long): Long = 1L
            override suspend fun withdrawFromGoalAtomically(goalId: Long, walletId: Long?, amount: BigDecimal, now: Long): Long = 1L
            override suspend fun reorderActiveGoalsAtomically(orderedGoalIds: List<Long>) = Unit
            override suspend fun autoSweepSurplusAtomically(sweepBuffer: BigDecimal, now: Long): AutoSweepExecutionResult =
                throw UnsupportedOperationException()
            override suspend fun undoAutoSweepAtomically(contributionIds: List<Long>, now: Long) = Unit
        }

        val prefsRepo = object : FinancialPreferencesRepository {
            private val settings = FinancialSettings(dailyExpenseLimitOverride = BigDecimal("100000"))
            override fun observeSettings(): Flow<FinancialSettings> = MutableStateFlow(settings)
            override suspend fun getSettings(): FinancialSettings = settings
            override suspend fun updateMinimumBuffer(amount: BigDecimal) = Unit
            override suspend fun updateSweepBufferOverride(amount: BigDecimal?) = Unit
            override suspend fun updateDailyExpenseLimitOverride(amount: BigDecimal?) = Unit
            override suspend fun updateBiometricLock(enabled: Boolean, timeoutSeconds: Long) = Unit
        }

        val notifiedBudgets = mutableListOf<CategoryBudgetEvaluation>()
        val notifiedImpacts = mutableListOf<GoalImpactWarning>()
        val sink = object : AlertNotificationSink {
            override fun notifyBudgetThreshold(evaluation: CategoryBudgetEvaluation) {
                notifiedBudgets.add(evaluation)
            }
            override fun notifyHighPriorityImpactWarning(warning: GoalImpactWarning) {
                notifiedImpacts.add(warning)
            }
        }

        val useCase = EvaluatePostTransactionAlertsUseCase(
            evaluateCategoryBudgetUseCase = EvaluateCategoryBudgetUseCase(budgetRepo, txRepo),
            calculateImpactWarningUseCase = CalculateImpactWarningUseCase(txRepo, goalRepo, prefsRepo),
            alertNotificationSink = sink,
        )

        val summary = useCase(occurredAtMillis = now, nowMillis = now, zoneId = utc)

        assertTrue(marked80)
        assertEquals(1, summary.triggeredBudgets.size)
        assertEquals(1, notifiedBudgets.size)
        assertEquals(1, summary.highPriorityImpactWarnings.size)
        assertEquals(1, notifiedImpacts.size)
        assertEquals("Motor", notifiedImpacts.single().goalName)
    }
}
