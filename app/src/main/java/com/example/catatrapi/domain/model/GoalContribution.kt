package com.example.catatrapi.domain.model

import java.math.BigDecimal

data class GoalContribution(
    val id: Long = 0,
    val goalId: Long,
    val walletId: Long?,
    /** Positif untuk DEPOSIT/AUTO_SWEEP, negatif untuk WITHDRAW. */
    val amount: BigDecimal,
    val type: ContributionType,
    val createdAt: Long = System.currentTimeMillis(),
)