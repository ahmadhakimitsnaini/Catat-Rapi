package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.BudgetAlertLevel
import com.example.catatrapi.domain.model.CategoryBudget
import com.example.catatrapi.domain.model.CategoryBudgetEvaluation
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionCategory
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.domain.repository.CategoryBudgetRepository
import com.example.catatrapi.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * FT-05: Evaluasi batas anggaran bulanan per kategori pada ambang 80% dan 100%.
 *
 * Aturan akuntansi:
 *  - Transaksi EXPENSE dihitung pada kategori masing-masing.
 *  - Biaya admin transfer (TRANSFER.fee > 0) dihitung sebagai pengeluaran pada kategori ADMIN_FEE.
 */
class EvaluateCategoryBudgetUseCase @Inject constructor(
    private val categoryBudgetRepository: CategoryBudgetRepository,
    private val transactionRepository: TransactionRepository,
) {

    fun observeForMonth(
        yearMonth: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Flow<List<CategoryBudgetEvaluation>> {
        val ymString = yearMonth.toString()
        val (startMillis, endMillis) = monthBounds(yearMonth, zoneId)
        return combine(
            categoryBudgetRepository.observeBudgetsByMonth(ymString),
            transactionRepository.observeTransactionsInRange(startMillis, endMillis),
        ) { budgets, transactions ->
            evaluateAll(budgets, transactions)
        }
    }

    suspend fun evaluateAndPersistFlagsForMonth(
        yearMonth: YearMonth,
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<CategoryBudgetEvaluation> {
        val ymString = yearMonth.toString()
        val (startMillis, endMillis) = monthBounds(yearMonth, zoneId)
        val budgets = categoryBudgetRepository.getBudgetsByMonth(ymString)
        val transactions = transactionRepository.getTransactionsInRange(startMillis, endMillis)
        val evaluations = evaluateAll(budgets, transactions)

        for (eval in evaluations) {
            if (eval.shouldTrigger100Alert) {
                categoryBudgetRepository.markNotificationSent(
                    budgetId = eval.budget.id,
                    notified80 = true,
                    notified100 = true,
                    now = now,
                )
            } else if (eval.shouldTrigger80Alert) {
                categoryBudgetRepository.markNotificationSent(
                    budgetId = eval.budget.id,
                    notified80 = true,
                    notified100 = eval.budget.notified100,
                    now = now,
                )
            }
        }
        return evaluations
    }

    companion object {
        val THRESHOLD_80: BigDecimal = BigDecimal("0.80")
        val THRESHOLD_100: BigDecimal = BigDecimal("1.00")
        private const val RATIO_SCALE = 4

        fun evaluateAll(
            budgets: List<CategoryBudget>,
            transactionsInMonth: List<Transaction>,
        ): List<CategoryBudgetEvaluation> {
            val spentByCategory = calculateSpentByCategory(transactionsInMonth)
            return budgets.map { budget ->
                evaluateSingle(
                    budget = budget,
                    spentAmount = spentByCategory[budget.category] ?: BigDecimal.ZERO,
                )
            }
        }

        fun evaluateSingle(
            budget: CategoryBudget,
            spentAmount: BigDecimal,
        ): CategoryBudgetEvaluation {
            val sanitizedSpent = spentAmount.max(BigDecimal.ZERO)
            val limit = budget.limitAmount
            val remaining = (limit - sanitizedSpent).max(BigDecimal.ZERO)

            val ratio = if (limit.signum() <= 0) {
                BigDecimal.ZERO
            } else {
                sanitizedSpent.divide(limit, RATIO_SCALE, RoundingMode.HALF_UP)
            }

            val percentage = ratio.multiply(BigDecimal(100))
                .setScale(0, RoundingMode.FLOOR)
                .toInt()
                .coerceAtLeast(0)

            val alertLevel = when {
                ratio >= THRESHOLD_100 -> BudgetAlertLevel.EXCEEDED_100
                ratio >= THRESHOLD_80 -> BudgetAlertLevel.WARNING_80
                else -> BudgetAlertLevel.NORMAL
            }

            val trigger100 = alertLevel == BudgetAlertLevel.EXCEEDED_100 && !budget.notified100
            val trigger80 = alertLevel == BudgetAlertLevel.WARNING_80 && !budget.notified80 && !budget.notified100

            return CategoryBudgetEvaluation(
                budget = budget,
                spentAmount = sanitizedSpent,
                remainingAmount = remaining,
                usageRatio = ratio,
                usagePercentage = percentage,
                alertLevel = alertLevel,
                shouldTrigger80Alert = trigger80,
                shouldTrigger100Alert = trigger100,
            )
        }

        fun calculateSpentByCategory(
            transactionsInMonth: List<Transaction>,
        ): Map<TransactionCategory, BigDecimal> {
            val result = mutableMapOf<TransactionCategory, BigDecimal>()
            for (tx in transactionsInMonth) {
                when (tx.type) {
                    TransactionType.EXPENSE -> {
                        val cat = tx.category ?: continue
                        result[cat] = (result[cat] ?: BigDecimal.ZERO) + tx.amount
                    }
                    TransactionType.TRANSFER -> {
                        if (tx.fee > BigDecimal.ZERO) {
                            val cat = TransactionCategory.ADMIN_FEE
                            result[cat] = (result[cat] ?: BigDecimal.ZERO) + tx.fee
                        }
                    }
                    TransactionType.INCOME, TransactionType.INVESTMENT_BUY, TransactionType.INVESTMENT_SELL -> Unit
                }
            }
            return result
        }

        fun monthBounds(yearMonth: YearMonth, zoneId: ZoneId): Pair<Long, Long> {
            val startMillis = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endMillis = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999_000_000)
                .atZone(zoneId).toInstant().toEpochMilli()
            return startMillis to endMillis
        }
    }
}
