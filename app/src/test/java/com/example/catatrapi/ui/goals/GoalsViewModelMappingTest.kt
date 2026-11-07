package com.example.catatrapi.ui.goals

import com.example.catatrapi.domain.model.GoalStatus
import com.example.catatrapi.domain.model.SavingGoal
import com.example.catatrapi.domain.model.SavingGoalProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

class GoalsViewModelMappingTest {

    private val utc = ZoneOffset.UTC
    private val now = LocalDate.parse("2026-11-07").atStartOfDay(utc).toInstant().toEpochMilli()

    private fun at(date: String): Long =
        LocalDate.parse(date).atStartOfDay(utc).toInstant().toEpochMilli()

    private fun progress(
        target: String = "1000000",
        saved: String = "0",
        deadline: Long = at("2026-12-07"),
        status: GoalStatus = GoalStatus.ON_TRACK,
    ) = SavingGoalProgress(
        goal = SavingGoal(
            id = 1L,
            name = "Dana Darurat",
            targetAmount = BigDecimal(target),
            deadline = deadline,
            priority = 1,
            status = status,
            createdAt = now,
        ),
        savedAmount = BigDecimal(saved),
    )

    @Test
    fun `target baru belum lewat tenggat mempertahankan status ON_TRACK`() {
        val item = progress(saved = "250000").toListItem(now, utc)
        assertEquals(GoalStatus.ON_TRACK, item.status)
        assertEquals(0.25f, item.progress, 0.0001f)
        assertEquals(30L, item.daysRemaining)
    }

    @Test
    fun `target yang tenggatnya sudah lewat dan S kurang dari T dikoreksi menjadi DELAYED`() {
        val item = progress(
            saved = "500000",
            deadline = at("2026-11-01"),
            status = GoalStatus.ON_TRACK,
        ).toListItem(now, utc)
        assertEquals(GoalStatus.DELAYED, item.status)
        assertEquals(-6L, item.daysRemaining)
    }

    @Test
    fun `target dengan S lebih besar atau sama dengan T dikoreksi menjadi ACHIEVED`() {
        val item = progress(
            saved = "1000000",
            deadline = at("2026-11-01"),
            status = GoalStatus.DELAYED,
        ).toListItem(now, utc)
        assertEquals(GoalStatus.ACHIEVED, item.status)
        assertEquals(1.0f, item.progress, 0.0001f)
    }

    @Test
    fun `computeReorderedGoalIds memindahkan item ke atas dan ke bawah dengan benar`() {
        val ids = listOf(10L, 20L, 30L)
        assertEquals(listOf(20L, 10L, 30L), computeReorderedGoalIds(ids, goalId = 20L, moveUp = true))
        assertEquals(listOf(10L, 30L, 20L), computeReorderedGoalIds(ids, goalId = 20L, moveUp = false))
    }

    @Test
    fun `computeReorderedGoalIds mengembalikan null bila perpindahan di luar batas atau id tidak ditemukan`() {
        val ids = listOf(10L, 20L, 30L)
        assertNull(computeReorderedGoalIds(ids, goalId = 10L, moveUp = true))
        assertNull(computeReorderedGoalIds(ids, goalId = 30L, moveUp = false))
        assertNull(computeReorderedGoalIds(ids, goalId = 99L, moveUp = true))
    }
}
