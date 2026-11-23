package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.CategoryBudgetEvaluation
import com.example.catatrapi.domain.model.GoalImpactWarning
import com.example.catatrapi.domain.repository.AlertNotificationSink
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class PostTransactionAlertSummary(
    val triggeredBudgets: List<CategoryBudgetEvaluation> = emptyList(),
    val highPriorityImpactWarnings: List<GoalImpactWarning> = emptyList(),
)

/**
 * Mengevaluasi batas anggaran kategori (FT-05) dan Impact Warning prioritas tinggi (GS-04)
 * segera setelah transaksi pengeluaran atau transfer berbiaya admin dicatat/diedit.
 */
class EvaluatePostTransactionAlertsUseCase @Inject constructor(
    private val evaluateCategoryBudgetUseCase: EvaluateCategoryBudgetUseCase,
    private val calculateImpactWarningUseCase: CalculateImpactWarningUseCase,
    private val alertNotificationSink: AlertNotificationSink,
) {
    suspend operator fun invoke(
        occurredAtMillis: Long = System.currentTimeMillis(),
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): PostTransactionAlertSummary {
        val targetMonth = YearMonth.from(Instant.ofEpochMilli(occurredAtMillis).atZone(zoneId))
        val budgetEvaluations = evaluateCategoryBudgetUseCase.evaluateAndPersistFlagsForMonth(
            yearMonth = targetMonth,
            now = nowMillis,
            zoneId = zoneId,
        )
        val triggeredBudgets = budgetEvaluations.filter {
            it.shouldTrigger80Alert || it.shouldTrigger100Alert
        }
        triggeredBudgets.forEach { eval ->
            alertNotificationSink.notifyBudgetThreshold(eval)
        }

        val impactReport = calculateImpactWarningUseCase.evaluateCurrent(
            now = nowMillis,
            zoneId = zoneId,
        )
        val highPriorityWarnings = impactReport.warnings.filter { it.droppedToDelayed }
        highPriorityWarnings.forEach { warning ->
            alertNotificationSink.notifyHighPriorityImpactWarning(warning)
        }

        return PostTransactionAlertSummary(
            triggeredBudgets = triggeredBudgets,
            highPriorityImpactWarnings = highPriorityWarnings,
        )
    }
}
