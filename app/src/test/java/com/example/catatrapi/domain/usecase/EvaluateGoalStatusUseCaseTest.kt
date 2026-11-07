package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.ContributionType
import com.example.catatrapi.domain.model.GoalContribution
import com.example.catatrapi.domain.model.GoalStatus
import com.example.catatrapi.domain.model.SavingGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

class EvaluateGoalStatusUseCaseTest {

    private val utc = ZoneOffset.UTC
    private val now = at("2026-11-07", 12)

    private fun at(date: String, hour: Int = 0): Long =
        LocalDate.parse(date).atStartOfDay(utc).plusHours(hour.toLong()).toInstant().toEpochMilli()

    private fun bd(value: String) = BigDecimal(value)

    private fun assertBd(expected: String, actual: BigDecimal) =
        assertEquals("Diharapkan $expected tetapi $actual", 0, bd(expected).compareTo(actual))

    private fun goal(
        target: String = "1000000",
        deadline: Long,
        createdAt: Long = at("2026-10-08"), // tepat 30 hari sebelum now
    ) = SavingGoal(
        id = 1,
        name = "Laptop",
        targetAmount = bd(target),
        deadline = deadline,
        createdAt = createdAt,
    )

    private fun contribution(
        amount: String,
        createdAt: Long,
        type: ContributionType = if (amount.startsWith("-")) ContributionType.WITHDRAW else ContributionType.DEPOSIT,
    ) = GoalContribution(goalId = 1, walletId = 1, amount = bd(amount), type = type, createdAt = createdAt)

    private fun evaluate(goal: SavingGoal, contributions: List<GoalContribution>) =
        EvaluateGoalStatusUseCase.evaluate(goal, contributions, now, utc)

    @Test
    fun `S sama dengan T menjadi ACHIEVED`() {
        val result = evaluate(
            goal(deadline = at("2027-01-20")),
            listOf(contribution("1000000", at("2026-10-20"))),
        )
        assertEquals(GoalStatus.ACHIEVED, result.status)
        assertEquals(0L, result.etaDays)
    }

    @Test
    fun `S lebih besar dari T tetap ACHIEVED walau tenggat sudah lewat`() {
        val result = evaluate(
            goal(deadline = at("2026-11-01")),
            listOf(contribution("1200000", at("2026-10-20"))),
        )
        assertEquals(GoalStatus.ACHIEVED, result.status)
    }

    @Test
    fun `tanpa kontribusi velocity nol ETA tidak terdefinisi dan DELAYED`() {
        val result = evaluate(goal(deadline = at("2027-01-20")), emptyList())
        assertEquals(GoalStatus.DELAYED, result.status)
        assertNull(result.etaDays)
        assertBd("0", result.velocityPerDay)
    }

    @Test
    fun `velocity negatif karena penarikan menjadi DELAYED dengan ETA null`() {
        val result = evaluate(
            goal(deadline = at("2027-01-20"), createdAt = at("2026-08-01")),
            listOf(
                contribution("100000", at("2026-09-01")), // di luar jendela 30 hari
                contribution("-50000", at("2026-10-25")), // di dalam jendela
            ),
        )
        assertEquals(GoalStatus.DELAYED, result.status)
        assertNull(result.etaDays)
        // -50.000 / 30 hari = -1666,67 per hari
        assertEquals(-1, result.velocityPerDay.signum())
        assertEquals(
            0,
            BigDecimal("-1666.67").compareTo(result.velocityPerDay.setScale(2, java.math.RoundingMode.HALF_UP))
        )
    }

    @Test
    fun `ETA sebelum tenggat menjadi ON_TRACK`() {
        // S = 300.000, v = 300.000/30 = 10.000/hari, sisa 700.000 -> ETA 70 hari; tenggat 74 hari lagi.
        val result = evaluate(
            goal(deadline = at("2027-01-20")),
            listOf(contribution("300000", at("2026-10-20", 12))),
        )
        assertEquals(GoalStatus.ON_TRACK, result.status)
        assertBd("10000", result.velocityPerDay)
        assertEquals(70L, result.etaDays)
        assertEquals(74L, result.daysUntilDeadline)
    }

    @Test
    fun `ETA tepat sama dengan tenggat masih ON_TRACK`() {
        val result = evaluate(
            goal(deadline = at("2027-01-16")), // 70 hari lagi
            listOf(contribution("300000", at("2026-10-20", 12))),
        )
        assertEquals(70L, result.etaDays)
        assertEquals(70L, result.daysUntilDeadline)
        assertEquals(GoalStatus.ON_TRACK, result.status)
    }

    @Test
    fun `ETA melewati tenggat menjadi DELAYED`() {
        val result = evaluate(
            goal(deadline = at("2027-01-10")), // 64 hari lagi
            listOf(contribution("300000", at("2026-10-20", 12))),
        )
        assertEquals(70L, result.etaDays)
        assertEquals(GoalStatus.DELAYED, result.status)
    }

    @Test
    fun `tenggat terlewat dan S kurang dari T menjadi DELAYED walau velocity positif`() {
        val result = evaluate(
            goal(deadline = at("2026-11-01")),
            listOf(contribution("990000", at("2026-11-02"))),
        )
        assertEquals(1L, result.etaDays)
        assertEquals(-6L, result.daysUntilDeadline)
        assertEquals(GoalStatus.DELAYED, result.status)
    }

    @Test
    fun `target dibuat hari ini memakai pembagi minimal satu hari`() {
        val result = evaluate(
            goal(deadline = at("2026-12-07"), createdAt = now),
            listOf(contribution("100000", now)),
        )
        assertBd("100000", result.velocityPerDay) // 100.000 / 1 hari
        assertEquals(9L, result.etaDays)          // sisa 900.000 / 100.000
        assertEquals(GoalStatus.ON_TRACK, result.status)
    }

    @Test
    fun `kontribusi lebih lama dari 30 hari tidak dihitung dalam velocity`() {
        val result = evaluate(
            goal(deadline = at("2027-01-20"), createdAt = at("2026-08-01")),
            listOf(contribution("500000", at("2026-09-01"))),
        )
        assertBd("0", result.velocityPerDay)
        assertNull(result.etaDays)
        assertEquals(GoalStatus.DELAYED, result.status)
    }

    @Test
    fun `pembagi velocity memakai hari berjalan bila kurang dari 30`() {
        // Dibuat 10 hari lalu, setoran 100.000 -> v = 10.000/hari
        val velocity = EvaluateGoalStatusUseCase.calculateVelocity(
            contributions = listOf(contribution("100000", at("2026-11-01"))),
            goalCreatedAt = at("2026-10-28"),
            now = now,
            zoneId = utc,
        )
        assertBd("10000", velocity)
    }
}