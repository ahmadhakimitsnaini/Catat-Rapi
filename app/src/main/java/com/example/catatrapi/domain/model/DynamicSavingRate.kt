package com.example.catatrapi.domain.model

import java.math.BigDecimal

data class DynamicSavingRate(
    /** D_sisa = max(1, tenggat - hari ini), dalam hari. */
    val remainingDays: Long,
    /** max(0, T - S). */
    val remainingAmount: BigDecimal,
    val dailyRate: BigDecimal,
    val weeklyRate: BigDecimal,
    /** True bila tenggat sudah lewat dan S < T (status DELAYED menurut GS-03). */
    val isDelayed: Boolean,
)