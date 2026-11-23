package com.example.catatrapi.domain.model

import java.math.BigDecimal

/**
 * GS-04 (Rumus 2.2.3): Peringatan dampak pembengkakan pengeluaran harian terhadap ETA satu Target Tabungan.
 */
data class GoalImpactWarning(
    val goalId: Long,
    val goalName: String,
    val priority: Int,
    val todayExpense: BigDecimal,
    val dailyAllowance: BigDecimal,
    val excessAmount: BigDecimal,
    val baselineEtaDays: Long,
    val newEtaDays: Long,
    val delayDays: Long,
    val baselineStatus: GoalStatus,
    val newStatus: GoalStatus,
    /** True bila status target jatuh dari ON_TRACK menjadi DELAYED akibat pengeluaran hari ini. */
    val droppedToDelayed: Boolean,
)

/**
 * Hasil evaluasi Impact Warning seluruh target aktif pada hari berjalan.
 */
data class ImpactWarningReport(
    val isEnabled: Boolean = false,
    val todayExpense: BigDecimal = BigDecimal.ZERO,
    val dailyAllowance: BigDecimal = BigDecimal.ZERO,
    val excessAmount: BigDecimal = BigDecimal.ZERO,
    val warnings: List<GoalImpactWarning> = emptyList(),
) {
    val hasWarnings: Boolean
        get() = isEnabled && warnings.isNotEmpty()

    val hasHighPriorityWarning: Boolean
        get() = warnings.any { it.droppedToDelayed }

    val primaryWarning: GoalImpactWarning?
        get() = warnings.firstOrNull { it.droppedToDelayed } ?: warnings.firstOrNull()
}
