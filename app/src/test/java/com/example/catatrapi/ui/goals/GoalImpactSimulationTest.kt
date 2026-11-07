package com.example.catatrapi.ui.goals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class GoalImpactSimulationTest {

    private fun bd(value: String) = BigDecimal(value)

    private fun assertBd(expected: String, actual: BigDecimal) =
        assertEquals("Diharapkan $expected tetapi $actual", 0, bd(expected).compareTo(actual))

    private fun simulate(
        target: String = "1000000",
        saved: String = "400000",
        velocity: String,
        extra: String,
        daysUntilDeadline: Long = 90,
    ) = GoalImpactSimulation.simulate(bd(target), bd(saved), bd(velocity), bd(extra), daysUntilDeadline)

    @Test
    fun `tanpa belanja tambahan ETA simulasi sama dengan ETA dasar`() {
        val r = simulate(velocity = "10000", extra = "0")
        assertEquals(60L, r.baselineEtaDays) // 600.000 / 10.000
        assertEquals(60L, r.simulatedEtaDays)
        assertEquals(0L, r.etaDelayDays)
        assertTrue(r.baselineMeetsDeadline)
        assertTrue(r.simulatedMeetsDeadline)
        assertFalse(r.isAchieved)
    }

    @Test
    fun `belanja tambahan memperlambat ETA`() {
        // v' = 10.000 - 150.000/30 = 5.000 -> ETA' = 600.000 / 5.000 = 120 hari
        val r = simulate(velocity = "10000", extra = "150000")
        assertBd("5000", r.simulatedVelocityPerDay)
        assertEquals(60L, r.baselineEtaDays)
        assertEquals(120L, r.simulatedEtaDays)
        assertEquals(60L, r.etaDelayDays)
        assertTrue(r.baselineMeetsDeadline)   // 60 <= 90
        assertFalse(r.simulatedMeetsDeadline) // 120 > 90
    }

    @Test
    fun `velocity simulasi tepat nol membuat ETA tidak terdefinisi`() {
        val r = simulate(velocity = "5000", extra = "150000")
        assertBd("0", r.simulatedVelocityPerDay)
        assertEquals(120L, r.baselineEtaDays)
        assertNull(r.simulatedEtaDays)
        assertNull(r.etaDelayDays)
        assertFalse(r.simulatedMeetsDeadline)
    }

    @Test
    fun `velocity simulasi negatif membuat ETA tidak terdefinisi`() {
        val r = simulate(velocity = "5000", extra = "300000")
        assertEquals(-1, r.simulatedVelocityPerDay.signum())
        assertNull(r.simulatedEtaDays)
        assertNull(r.etaDelayDays)
    }

    @Test
    fun `velocity dasar nol membuat kedua ETA tidak terdefinisi tanpa pembagian nol`() {
        val r = simulate(velocity = "0", extra = "0")
        assertNull(r.baselineEtaDays)
        assertNull(r.simulatedEtaDays)
        assertNull(r.etaDelayDays)
        assertFalse(r.baselineMeetsDeadline)
        assertFalse(r.simulatedMeetsDeadline)
    }

    @Test
    fun `velocity dasar negatif tetap tidak terdefinisi walau belanja tambahan nol`() {
        val r = simulate(velocity = "-1500", extra = "0")
        assertNull(r.baselineEtaDays)
        assertNull(r.simulatedEtaDays)
    }

    @Test
    fun `S sama dengan T dianggap tercapai dan ETA nol`() {
        val r = simulate(target = "1000000", saved = "1000000", velocity = "0", extra = "500000")
        assertTrue(r.isAchieved)
        assertEquals(0L, r.baselineEtaDays)
        assertEquals(0L, r.simulatedEtaDays)
        assertEquals(0L, r.etaDelayDays)
        assertTrue(r.simulatedMeetsDeadline)
    }

    @Test
    fun `S lebih besar dari T dianggap tercapai`() {
        val r = simulate(target = "1000000", saved = "1500000", velocity = "10000", extra = "0")
        assertTrue(r.isAchieved)
        assertEquals(0L, r.simulatedEtaDays)
    }

    @Test
    fun `belanja tambahan negatif diperlakukan sebagai nol`() {
        val r = simulate(velocity = "10000", extra = "-100000")
        assertBd("10000", r.simulatedVelocityPerDay)
        assertEquals(60L, r.simulatedEtaDays)
    }

    @Test
    fun `ETA dibulatkan ke atas`() {
        // sisa 100 / 30 per hari = 3,33 -> 4 hari
        val r = simulate(target = "100", saved = "0", velocity = "30", extra = "0")
        assertEquals(4L, r.baselineEtaDays)
    }

    @Test
    fun `tenggat terlewat tidak pernah memenuhi tenggat`() {
        val r = simulate(velocity = "10000", extra = "0", daysUntilDeadline = -3)
        assertEquals(60L, r.baselineEtaDays)
        assertFalse(r.baselineMeetsDeadline)
        assertFalse(r.simulatedMeetsDeadline)
    }

    @Test
    fun `ETA tepat sama dengan sisa hari tenggat masih memenuhi`() {
        val r = simulate(velocity = "10000", extra = "0", daysUntilDeadline = 60)
        assertTrue(r.baselineMeetsDeadline)
    }

    @Test
    fun `velocity sangat kecil tidak overflow dan dibatasi ke Long MAX`() {
        val r = simulate(velocity = "0.000000000000000000000000000001", extra = "0")
        assertEquals(Long.MAX_VALUE, r.baselineEtaDays)
        assertEquals(Long.MAX_VALUE, r.simulatedEtaDays)
        assertEquals(0L, r.etaDelayDays)
    }
}