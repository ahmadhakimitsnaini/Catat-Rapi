package com.example.catatrapi.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

class CalculateDynamicSavingRateUseCaseTest {

    private val utc = ZoneOffset.UTC
    private val now = at("2026-11-07", 12)

    private fun at(date: String, hour: Int = 0): Long =
        LocalDate.parse(date).atStartOfDay(utc).plusHours(hour.toLong()).toInstant().toEpochMilli()

    private fun bd(value: String) = BigDecimal(value)

    private fun assertBd(expected: String, actual: BigDecimal) =
        assertEquals("Diharapkan $expected tetapi $actual", 0, bd(expected).compareTo(actual))

    private fun calc(target: String, saved: String, deadline: Long) =
        CalculateDynamicSavingRateUseCase.calculate(bd(target), bd(saved), deadline, now, utc)

    @Test
    fun `kasus normal menghitung rate harian dan mingguan`() {
        val result = calc("1000000", "400000", at("2026-11-17")) // 10 hari
        assertEquals(10L, result.remainingDays)
        assertBd("600000", result.remainingAmount)
        assertBd("60000", result.dailyRate)
        assertBd("300000", result.weeklyRate) // ceil(10/7) = 2 minggu
        assertFalse(result.isDelayed)
    }

    @Test
    fun `tenggat hari ini memakai sisa hari minimal satu`() {
        val result = calc("1000000", "400000", at("2026-11-07", 23))
        assertEquals(1L, result.remainingDays)
        assertBd("600000", result.dailyRate)
        assertBd("600000", result.weeklyRate)
        assertFalse(result.isDelayed)
    }

    @Test
    fun `tenggat terlewat dan S kurang dari T menjadi delayed tanpa pembagian nol`() {
        val result = calc("1000000", "400000", at("2026-11-01"))
        assertEquals(1L, result.remainingDays)
        assertBd("600000", result.dailyRate)
        assertBd("600000", result.weeklyRate)
        assertTrue(result.isDelayed)
    }

    @Test
    fun `S sama dengan T menghasilkan rate nol dan tidak delayed walau tenggat lewat`() {
        val result = calc("1000000", "1000000", at("2026-11-01"))
        assertBd("0", result.remainingAmount)
        assertBd("0", result.dailyRate)
        assertBd("0", result.weeklyRate)
        assertFalse(result.isDelayed)
    }

    @Test
    fun `S lebih besar dari T dibatasi ke nol bukan negatif`() {
        val result = calc("1000000", "1500000", at("2026-12-07"))
        assertBd("0", result.remainingAmount)
        assertBd("0", result.dailyRate)
        assertBd("0", result.weeklyRate)
        assertFalse(result.isDelayed)
    }

    @Test
    fun `pembulatan rate selalu ke atas agar target tetap tercapai`() {
        val result = calc("100", "0", at("2026-11-10")) // 3 hari
        assertBd("33.34", result.dailyRate)
        assertBd("100", result.weeklyRate) // ceil(3/7) = 1 minggu
    }

    @Test
    fun `tepat tujuh hari menghasilkan satu minggu`() {
        val result = calc("700", "0", at("2026-11-14"))
        assertEquals(7L, result.remainingDays)
        assertBd("100", result.dailyRate)
        assertBd("700", result.weeklyRate)
    }

    @Test
    fun `delapan hari menghasilkan dua minggu`() {
        val result = calc("800", "0", at("2026-11-15"))
        assertEquals(8L, result.remainingDays)
        assertBd("100", result.dailyRate)
        assertBd("400", result.weeklyRate)
    }
}