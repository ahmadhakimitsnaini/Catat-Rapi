package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.SavingGoal
import com.example.catatrapi.domain.model.SavingGoalProgress
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionCategory
import com.example.catatrapi.domain.model.TransactionType
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoSweepSurplusUseCaseTest {

    private val now = 1_760_000_000_000L

    private fun bd(value: String) = BigDecimal(value)

    private fun assertBd(expected: String, actual: BigDecimal) =
        assertEquals("Diharapkan $expected tetapi $actual", 0, bd(expected).compareTo(actual))

    private fun goalProgress(
        id: Long,
        name: String,
        priority: Int,
        target: String,
        saved: String,
    ) = SavingGoalProgress(
        goal = SavingGoal(
            id = id,
            name = name,
            targetAmount = bd(target),
            deadline = now + 86_400_000L * 60,
            priority = priority,
            createdAt = now - 86_400_000L * 10,
        ),
        savedAmount = bd(saved),
    )

    @Test
    fun `resolveSweepBuffer menggunakan 3 kali rata-rata pengeluaran harian bila tanpa override`() {
        // Total pengeluaran 30 hari = 3.000.000 -> mu = 100.000 -> B_sweep = 3 * 100.000 = 300.000
        val txs = listOf(
            Transaction(
                id = 1,
                walletId = 1,
                type = TransactionType.EXPENSE,
                amount = bd("3000000"),
                category = TransactionCategory.FOOD_BEVERAGE,
                occurredAt = now - 86_400_000L,
                createdAt = now - 86_400_000L,
            )
        )
        val buffer = AutoSweepSurplusUseCase.resolveSweepBuffer(
            sweepBufferOverride = null,
            transactionsLast30Days = txs,
            now = now,
        )
        assertBd("300000", buffer)
    }

    @Test
    fun `resolveSweepBuffer menggunakan override pengguna bila tersedia`() {
        val buffer = AutoSweepSurplusUseCase.resolveSweepBuffer(
            sweepBufferOverride = bd("750000"),
            transactionsLast30Days = emptyList(),
            now = now,
        )
        assertBd("750000", buffer)
    }

    @Test
    fun `planAllocations menghasilkan nol bila Spendable kurang dari atau sama dengan B_sweep`() {
        val goals = listOf(goalProgress(1, "Dana Darurat", 1, "5000000", "1000000"))
        val plan = AutoSweepSurplusUseCase.planAllocations(
            spendableBalance = bd("300000"),
            sweepBuffer = bd("300000"),
            activeGoals = goals,
        )

        assertFalse(plan.canExecute)
        assertBd("0", plan.rawSurplus)
        assertBd("0", plan.totalAllocatedAmount)
        assertTrue(plan.allocations.isEmpty())
    }

    @Test
    fun `planAllocations mendistribusikan surplus berurutan sesuai prioritas hingga target penuh`() {
        // Spendable = 1.500.000, B_sweep = 300.000 -> rawSurplus = 1.200.000
        // Goal P1 butuh 500.000 (T=2.000.000, S=1.500.000) -> penuh (500.000)
        // Goal P2 butuh 1.000.000 (T=3.000.000, S=2.000.000) -> terima sisa 700.000
        // Goal P3 butuh 500.000 -> tidak kebagian
        val goals = listOf(
            goalProgress(2, "Liburan", priority = 2, target = "3000000", saved = "2000000"),
            goalProgress(1, "Dana Darurat", priority = 1, target = "2000000", saved = "1500000"),
            goalProgress(3, "Gadget", priority = 3, target = "500000", saved = "0"),
        )

        val plan = AutoSweepSurplusUseCase.planAllocations(
            spendableBalance = bd("1500000"),
            sweepBuffer = bd("300000"),
            activeGoals = goals,
        )

        assertTrue(plan.canExecute)
        assertBd("1200000", plan.rawSurplus)
        assertBd("1200000", plan.totalAllocatedAmount)
        assertEquals(2, plan.allocations.size)

        val first = plan.allocations[0]
        assertEquals(1L, first.goalId)
        assertBd("500000", first.allocatedAmount)
        assertTrue(first.willAchieveGoal)

        val second = plan.allocations[1]
        assertEquals(2L, second.goalId)
        assertBd("700000", second.allocatedAmount)
        assertFalse(second.willAchieveGoal)
    }

    @Test
    fun `planAllocations membatasi alokasi hanya sebesar sisa kebutuhan seluruh target bila surplus berlebih`() {
        // rawSurplus = 2.000.000, tetapi satu-satunya target hanya butuh 400.000
        val goals = listOf(goalProgress(1, "Motor", priority = 1, target = "1000000", saved = "600000"))
        val plan = AutoSweepSurplusUseCase.planAllocations(
            spendableBalance = bd("2300000"),
            sweepBuffer = bd("300000"),
            activeGoals = goals,
        )

        assertTrue(plan.canExecute)
        assertBd("2000000", plan.rawSurplus)
        assertBd("400000", plan.totalAllocatedAmount)
        assertEquals(1, plan.allocations.size)
        assertTrue(plan.allocations.single().willAchieveGoal)
    }
}
