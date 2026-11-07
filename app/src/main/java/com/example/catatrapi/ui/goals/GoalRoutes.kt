package com.example.catatrapi.ui.goals

/**
 * Prefiks "goals_wealth" HARUS sama dengan MainTab.GOALS_WEALTH.route
 * agar tab Goals tersorot di rute detail.
 */
object GoalRoutes {
    const val GOAL_ID_ARG = "goalId"
    const val DETAIL_PATTERN = "goals_wealth/goal/{$GOAL_ID_ARG}"

    fun detail(goalId: Long): String = "goals_wealth/goal/$goalId"
}