package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.CategoryBudget
import com.example.catatrapi.domain.model.ContributionType
import com.example.catatrapi.domain.model.GoalContribution
import com.example.catatrapi.domain.model.SavingGoal
import com.example.catatrapi.domain.model.SavingGoalProgress
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionCategory
import com.example.catatrapi.domain.model.TransactionType
import java.math.BigDecimal
import java.time.ZoneOffset
import kotlin.system.measureTimeMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Quality Gate 1: Uji performa dan ketahanan seluruh rumus 2.2.1, 2.2.2, dan 2.2.3
 * pada dataset 10.000 transaksi (NFR-P2 <= 500 ms, NFR-P4).
 */
class QualityGate1BenchmarkTest {

    @Test
    fun `evaluasi 10000 transaksi untuk ringkasan bulanan anggaran dan impact warning selesai di bawah 500 ms`() {
        val now = 1_763_000_000_000L
        val categories = TransactionCategory.forType(TransactionType.EXPENSE)

        // Bangun dataset 10.000 transaksi tersebar dalam 30 hari terakhir
        val dataset = List(10_000) { index ->
            val isExpense = index % 5 != 0
            val dayOffset = (index % 30).toLong()
            Transaction(
                id = (index + 1).toLong(),
                walletId = 1L,
                type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
                amount = BigDecimal.valueOf(15_000L + (index % 50) * 1_000L),
                category = if (isExpense) {
                    categories[index % categories.size]
                } else {
                    TransactionCategory.SALARY
                },
                occurredAt = now - dayOffset * 86_400_000L,
                createdAt = now - dayOffset * 86_400_000L,
            )
        }

        val goals = List(5) { idx ->
            SavingGoal(
                id = (idx + 1).toLong(),
                name = "Target #${idx + 1}",
                targetAmount = BigDecimal.valueOf(10_000_000L),
                deadline = now + 90L * 86_400_000L,
                priority = idx + 1,
                createdAt = now - 30L * 86_400_000L,
            ) to listOf(
                GoalContribution(
                    id = (idx + 1).toLong(),
                    goalId = (idx + 1).toLong(),
                    walletId = 1L,
                    amount = BigDecimal.valueOf(1_500_000L),
                    type = ContributionType.DEPOSIT,
                    createdAt = now - 10L * 86_400_000L,
                )
            )
        }

        val budgets = categories.mapIndexed { idx, cat ->
            CategoryBudget(
                id = (idx + 1).toLong(),
                category = cat,
                yearMonth = "2026-11",
                limitAmount = BigDecimal.valueOf(25_000_000L),
            )
        }

        val elapsedMs = measureTimeMillis {
            val summary = CalculateMonthlyCashflowSummaryUseCase.summarize(dataset)
            assertTrue(summary.totalExpense > BigDecimal.ZERO)

            val budgetEvaluations = EvaluateCategoryBudgetUseCase.evaluateAll(budgets, dataset)
            assertEquals(categories.size, budgetEvaluations.size)

            val impactReport = CalculateImpactWarningUseCase.calculate(
                transactionsLast30Days = dataset,
                earliestTransactionTimestamp = now - 29L * 86_400_000L,
                goalsWithContributions = goals,
                manualDailyLimit = null,
                now = now,
                zoneId = ZoneOffset.UTC,
            )
            assertTrue(impactReport.isEnabled)

            val sweepBuffer = AutoSweepSurplusUseCase.resolveSweepBuffer(
                sweepBufferOverride = null,
                transactionsLast30Days = dataset,
                now = now,
            )
            val sweepPlan = AutoSweepSurplusUseCase.planAllocations(
                spendableBalance = sweepBuffer + BigDecimal.valueOf(2_000_000L),
                sweepBuffer = sweepBuffer,
                activeGoals = goals.map { (g, c) ->
                    SavingGoalProgress(g, c.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount })
                },
            )
            assertTrue(sweepPlan.canExecute)
        }

        assertTrue(
            "Diharapkan evaluasi 10.000 transaksi <= 500 ms (NFR-P2), tetapi memakan ${elapsedMs} ms",
            elapsedMs <= 500L,
        )
    }
}
