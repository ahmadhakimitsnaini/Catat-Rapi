package com.example.catatrapi.domain.model

import java.math.BigDecimal

data class SavingGoalProgress(
    val goal: SavingGoal,
    /** S_j: total dana terkumpul pada target ini. */
    val savedAmount: BigDecimal,
)