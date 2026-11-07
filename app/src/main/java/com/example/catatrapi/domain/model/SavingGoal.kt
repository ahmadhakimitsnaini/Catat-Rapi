package com.example.catatrapi.domain.model

import java.math.BigDecimal

data class SavingGoal(
    val id: Long = 0,
    val name: String,
    val targetAmount: BigDecimal,
    val deadline: Long,
    val priority: Int = 0,
    val status: GoalStatus = GoalStatus.ON_TRACK,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)