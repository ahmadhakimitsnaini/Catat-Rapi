package com.example.catatrapi.ui.dashboard

import com.example.catatrapi.domain.model.ContributionType
import com.example.catatrapi.domain.model.GoalContribution
import com.example.catatrapi.domain.model.GoalStatus
import com.example.catatrapi.domain.model.SavingGoal
import com.example.catatrapi.domain.model.SavingGoalProgress
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardViewModelTest {

    private val now = 1_760_000_000_000L

    private fun goalProgress(
        id: Long,
        name: String,
        priority: Int,
        target: String = "1000000",
        saved: String = "200000",
        deadlineOffsetDays: Long = 50L,
    ) = SavingGoalProgress(
        goal = SavingGoal(
            id = id,
            name = name,
            targetAmount = BigDecimal(target),
            deadline = now + deadlineOffsetDays * 86_400_000L,
            priority = priority,
            createdAt = now - 10L * 86_400_000L,
        ),
        savedAmount = BigDecimal(saved),
    )

    @Test
    fun `mapTopGoals mengurutkan berdasarkan prioritas dan membatasi maksimal 3 target`() {
        val goals = listOf(
            goalProgress(4, "Target P4", priority = 4),
            goalProgress(2, "Target P2", priority = 2),
            goalProgress(1, "Target P1", priority = 1),
            goalProgress(3, "Target P3", priority = 3),
        )
        val contribs = listOf(
            GoalContribution(
                id = 1L,
                goalId = 1L,
                walletId = 1L,
                amount = BigDecimal("200000"),
                type = ContributionType.DEPOSIT,
                createdAt = now - 86_400_000L,
            )
        )

        val top = DashboardViewModel.mapTopGoals(
            goalsWithProgress = goals,
            allContributions = contribs,
            now = now,
        )

        assertEquals(3, top.size)
        assertEquals(listOf(1L, 2L, 3L), top.map { it.goal.id })
        assertEquals(GoalStatus.ON_TRACK, top[0].status)
        // Target P2 belum punya kontribusi tetapi belum lewat tenggat -> tetap ditampilkan ON_TRACK
        assertEquals(GoalStatus.ON_TRACK, top[1].status)
    }
}
