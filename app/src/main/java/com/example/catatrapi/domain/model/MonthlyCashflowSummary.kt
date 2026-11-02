package com.example.catatrapi.domain.model

import java.math.BigDecimal

data class MonthlyCashflowSummary(
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalExpense: BigDecimal = BigDecimal.ZERO,
) {
    val netCashflow: BigDecimal
        get() = totalIncome - totalExpense
}
