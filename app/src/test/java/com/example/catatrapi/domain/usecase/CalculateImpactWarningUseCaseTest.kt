package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.ContributionType
import com.example.catatrapi.domain.model.GoalContribution
import com.example.catatrapi.domain.model.GoalStatus
import com.example.catatrapi.domain.model.SavingGoal
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionCategory
import com.example.catatrapi.domain.model.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateImpactWarningUseCaseTest {

    private val utc = ZoneOffset.UTC
    private val now = at("2026-11-20", 18)

    private fun at(date: String, hour: Int = 12): Long =
        LocalDate.parse(date).atStartOfDay(utc).plusHours(hour.toLong()).toInstant().toEpochMilli()

    private fun bd(value: String) = BigDecimal(value)

    private fun assertBd(expected: String, actual: BigDecimal) =
        assertEquals("Diharapkan $expected tetapi $actual", 0, bd(expected).compareTo(actual))

    private fun expense(amount: String, occurredAt: Long, id: Long = 1L) = Transaction(
        id = id,
        walletId = 1L,
        type = TransactionType.EXPENSE,
        amount = bd(amount),
        category = TransactionCategory.FOOD_BEVERAGE,
        occurredAt = occurredAt,
        createdAt = occurredAt,
    )

    private fun goal(
        id: Long = 1L,
        name: String = "Motor",
        target: String = "15000000",
        deadline: Long = at("2027-02-01"),
        priority: Int = 1,
        createdAt: Long = at("2026-10-01"),
    ) = SavingGoal(
        id = id,
        name = name,
        targetAmount = bd(target),
        deadline = deadline,
        priority = priority,
        createdAt = createdAt,
    )

    private fun contribution(amount: String, createdAt: Long = at("2026-11-01")) = GoalContribution(
        id = 1L,
        goalId = 1L,
        walletId = 1L,
        amount = bd(amount),
        type = ContributionType.DEPOSIT,
        createdAt = createdAt,
    )

    @Test
    fun `nonaktif bila histori transaksi kurang dari 7 hari dan tanpa batas manual`() {
        val recentTx = listOf(expense("500000", now))
        val report = CalculateImpactWarningUseCase.calculate(
            transactionsLast30Days = recentTx,
            earliestTransactionTimestamp = at("2026-11-16"), // baru 4 hari
            goalsWithContributions = listOf(goal() to listOf(contribution("3000000"))),
            manualDailyLimit = null,
            now = now,
            zoneId = utc,
        )

        assertFalse(report.isEnabled)
        assertFalse(report.hasWarnings)
    }

    @Test
    fun `tetap aktif walau histori kurang dari 7 hari bila pengguna mengatur batas harian manual`() {
        // Batas harian manual = 100.000, belanja hari ini = 350.000 -> Delta = 250.000
        // Goal: T = 1.000.000, S = 300.000 (sisa 700.000), v = 300.000/30 = 10.000/hari
        // ETA awal = 700.000 / 10.000 = 70 hari; ETA baru = (700.000 + 250.000)/10.000 = 95 hari -> mundur 25 hari
        val g = goal(target = "1000000", deadline = at("2027-02-28"))
        val report = CalculateImpactWarningUseCase.calculate(
            transactionsLast30Days = listOf(expense("350000", now)),
            earliestTransactionTimestamp = now, // baru hari ini
            goalsWithContributions = listOf(g to listOf(contribution("300000"))),
            manualDailyLimit = bd("100000"),
            now = now,
            zoneId = utc,
        )

        assertTrue(report.isEnabled)
        assertTrue(report.hasWarnings)
        assertBd("250000", report.excessAmount)
        assertEquals(25L, report.warnings.single().delayDays)
    }

    @Test
    fun `menghitung mu 30 hari tidak termasuk pokok transfer dan investasi tetapi memasukkan biaya admin transfer`() {
        // Pengeluaran sebelumnya dalam 30 hari: 2.700.000
        // Transfer 5.000.000 dengan fee 6.500 -> hanya 6.500 yang dihitung
        // Investasi BUY 2.000.000 -> diabaikan
        // Belanja hari ini: 293.500 -> total 30 hari = 3.000.000 -> mu = 100.000/hari
        // Delta hari ini = 293.500 - 100.000 = 193.500
        val txs = listOf(
            expense("2700000", at("2026-11-01"), id = 1),
            Transaction(
                id = 2,
                walletId = 1,
                toWalletId = 2,
                type = TransactionType.TRANSFER,
                amount = bd("5000000"),
                fee = bd("6500"),
                occurredAt = at("2026-11-05"),
                createdAt = at("2026-11-05"),
            ),
            Transaction(
                id = 3,
                walletId = 1,
                type = TransactionType.INVESTMENT_BUY,
                amount = bd("2000000"),
                occurredAt = at("2026-11-10"),
                createdAt = at("2026-11-10"),
            ),
            expense("293500", now, id = 4),
        )
        val g = goal(target = "1000000", deadline = at("2027-03-01"))
        val report = CalculateImpactWarningUseCase.calculate(
            transactionsLast30Days = txs,
            earliestTransactionTimestamp = at("2026-11-01"),
            goalsWithContributions = listOf(g to listOf(contribution("300000"))), // v = 10.000/hari
            manualDailyLimit = null,
            now = now,
            zoneId = utc,
        )

        assertTrue(report.isEnabled)
        assertBd("100000", report.dailyAllowance)
        assertBd("293500", report.todayExpense)
        assertBd("193500", report.excessAmount)
        // ETA awal = ceil(700.000 / 10.000) = 70; ETA baru = ceil(893.500 / 10.000) = 90 -> mundur 20 hari
        assertEquals(20L, report.warnings.single().delayDays)
    }

    @Test
    fun `mendeteksi prioritas tinggi saat status jatuh dari ON_TRACK menjadi DELAYED`() {
        // S = 300.000, T = 1.000.000, v = 10.000/hari -> ETA awal = 70 hari
        // Tenggat = 72 hari dari now (2027-01-31) -> baseline ON_TRACK
        // Batas harian manual = 100.000, belanja hari ini = 150.000 -> Delta = 50.000
        // ETA baru = ceil((700.000 + 50.000) / 10.000) = 75 hari (> 72 hari) -> jatuh ke DELAYED!
        val g = goal(target = "1000000", deadline = at("2027-01-31"))
        val report = CalculateImpactWarningUseCase.calculate(
            transactionsLast30Days = listOf(expense("150000", now)),
            earliestTransactionTimestamp = at("2026-10-01"),
            goalsWithContributions = listOf(g to listOf(contribution("300000"))),
            manualDailyLimit = bd("100000"),
            now = now,
            zoneId = utc,
        )

        assertTrue(report.hasWarnings)
        assertTrue(report.hasHighPriorityWarning)
        val warning = report.warnings.single()
        assertEquals(GoalStatus.ON_TRACK, warning.baselineStatus)
        assertEquals(GoalStatus.DELAYED, warning.newStatus)
        assertTrue(warning.droppedToDelayed)
        assertEquals(5L, warning.delayDays)
    }

    @Test
    fun `mengabaikan target dengan velocity nol atau negatif dan target yang sudah ACHIEVED`() {
        val gNoVelocity = goal(id = 1, name = "Tanpa Setoran", target = "1000000")
        val gAchieved = goal(id = 2, name = "Lunas", target = "500000")

        val report = CalculateImpactWarningUseCase.calculate(
            transactionsLast30Days = listOf(expense("400000", now)),
            earliestTransactionTimestamp = at("2026-10-01"),
            goalsWithContributions = listOf(
                gNoVelocity to emptyList(),
                gAchieved to listOf(contribution("500000")),
            ),
            manualDailyLimit = bd("100000"),
            now = now,
            zoneId = utc,
        )

        assertTrue(report.isEnabled)
        assertFalse(report.hasWarnings)
    }
}
