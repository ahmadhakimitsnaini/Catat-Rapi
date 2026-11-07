package com.example.catatrapi.domain.model

import java.math.BigDecimal

data class GoalEvaluation(
    val status: GoalStatus,
    val savedAmount: BigDecimal,
    /** Saving Velocity per hari (net 30 hari terakhir). Bisa nol atau negatif. */
    val velocityPerDay: BigDecimal,
    /** Estimasi hari sampai target tercapai. Null bila tidak terdefinisi (v <= 0), 0 bila sudah tercapai. */
    val etaDays: Long?,
    /** Selisih hari kalender dari hari ini ke tenggat. Negatif bila tenggat sudah lewat. */
    val daysUntilDeadline: Long,
)