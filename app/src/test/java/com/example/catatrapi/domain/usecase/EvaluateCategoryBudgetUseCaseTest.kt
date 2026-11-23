package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.BudgetAlertLevel
import com.example.catatrapi.domain.model.CategoryBudget
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionCategory
import com.example.catatrapi.domain.model.TransactionType
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluateCategoryBudgetUseCaseTest {

    private fun bd(value: String) = BigDecimal(value)

    private fun budget(
        category: TransactionCategory = TransactionCategory.FOOD_BEVERAGE,
        limit: String = "1000000",
        notified80: Boolean = false,
        notified100: Boolean = false,
    ) = CategoryBudget(
        id = 1L,
        category = category,
        yearMonth = "2026-11",
        limitAmount = bd(limit),
        notified80 = notified80,
        notified100 = notified100,
    )

    @Test
    fun `pengeluaran di bawah 80 persen berstatus NORMAL dan tidak memicu notifikasi`() {
        val eval = EvaluateCategoryBudgetUseCase.evaluateSingle(
            budget = budget(limit = "1000000"),
            spentAmount = bd("799000"),
        )

        assertEquals(BudgetAlertLevel.NORMAL, eval.alertLevel)
        assertEquals(79, eval.usagePercentage)
        assertFalse(eval.shouldTrigger80Alert)
        assertFalse(eval.shouldTrigger100Alert)
    }

    @Test
    fun `pengeluaran mencapai 80 persen memicu alert 80 persen hanya bila belum pernah dikirim`() {
        val firstEval = EvaluateCategoryBudgetUseCase.evaluateSingle(
            budget = budget(limit = "1000000", notified80 = false),
            spentAmount = bd("800000"),
        )
        assertEquals(BudgetAlertLevel.WARNING_80, firstEval.alertLevel)
        assertEquals(80, firstEval.usagePercentage)
        assertTrue(firstEval.shouldTrigger80Alert)
        assertFalse(firstEval.shouldTrigger100Alert)

        val secondEval = EvaluateCategoryBudgetUseCase.evaluateSingle(
            budget = budget(limit = "1000000", notified80 = true),
            spentAmount = bd("850000"),
        )
        assertEquals(BudgetAlertLevel.WARNING_80, secondEval.alertLevel)
        assertFalse(secondEval.shouldTrigger80Alert)
        assertFalse(secondEval.shouldTrigger100Alert)
    }

    @Test
    fun `pengeluaran mencapai atau melebihi 100 persen memicu alert 100 persen sekali`() {
        val firstEval = EvaluateCategoryBudgetUseCase.evaluateSingle(
            budget = budget(limit = "1000000", notified80 = false, notified100 = false),
            spentAmount = bd("1150000"),
        )
        assertEquals(BudgetAlertLevel.EXCEEDED_100, firstEval.alertLevel)
        assertEquals(115, firstEval.usagePercentage)
        assertFalse(firstEval.shouldTrigger80Alert)
        assertTrue(firstEval.shouldTrigger100Alert)

        val secondEval = EvaluateCategoryBudgetUseCase.evaluateSingle(
            budget = budget(limit = "1000000", notified80 = true, notified100 = true),
            spentAmount = bd("1200000"),
        )
        assertFalse(secondEval.shouldTrigger80Alert)
        assertFalse(secondEval.shouldTrigger100Alert)
    }

    @Test
    fun `calculateSpentByCategory memasukkan biaya admin transfer ke kategori ADMIN_FEE`() {
        val txs = listOf(
            Transaction(
                id = 1,
                walletId = 1,
                type = TransactionType.EXPENSE,
                amount = bd("250000"),
                category = TransactionCategory.FOOD_BEVERAGE,
                occurredAt = 1000L,
                createdAt = 1000L,
            ),
            Transaction(
                id = 2,
                walletId = 1,
                toWalletId = 2,
                type = TransactionType.TRANSFER,
                amount = bd("5000000"),
                fee = bd("6500"),
                occurredAt = 2000L,
                createdAt = 2000L,
            ),
        )

        val spentMap = EvaluateCategoryBudgetUseCase.calculateSpentByCategory(txs)
        assertEquals(0, bd("250000").compareTo(spentMap[TransactionCategory.FOOD_BEVERAGE]))
        assertEquals(0, bd("6500").compareTo(spentMap[TransactionCategory.ADMIN_FEE]))
    }
}
