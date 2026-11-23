package com.example.catatrapi.domain.repository

import com.example.catatrapi.domain.model.CategoryBudgetEvaluation
import com.example.catatrapi.domain.model.GoalImpactWarning

/**
 * Kontrak pengiriman notifikasi peringatan anggaran (FT-05) dan Impact Warning prioritas tinggi (GS-04),
 * memisahkan Domain layer dari API NotificationManager Android.
 */
interface AlertNotificationSink {
    fun notifyBudgetThreshold(evaluation: CategoryBudgetEvaluation)
    fun notifyHighPriorityImpactWarning(warning: GoalImpactWarning)
}
