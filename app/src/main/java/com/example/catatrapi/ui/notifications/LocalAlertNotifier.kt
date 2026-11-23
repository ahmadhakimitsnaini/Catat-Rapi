package com.example.catatrapi.ui.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.catatrapi.R
import com.example.catatrapi.domain.model.CategoryBudgetEvaluation
import com.example.catatrapi.domain.model.GoalImpactWarning
import com.example.catatrapi.domain.repository.AlertNotificationSink
import com.example.catatrapi.ui.components.CurrencyFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementasi notifikasi lokal untuk FT-05 (Anggaran 80%/100%) dan GS-04 (Impact Warning prioritas tinggi).
 */
@Singleton
class LocalAlertNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AlertNotificationSink {

    init {
        ensureChannelsCreated()
    }

    override fun notifyBudgetThreshold(evaluation: CategoryBudgetEvaluation) {
        if (!hasNotificationPermission()) return

        val categoryLabel = evaluation.budget.category.labelId
        val limitText = CurrencyFormatter.format(evaluation.budget.limitAmount)
        val spentText = CurrencyFormatter.format(evaluation.spentAmount)
        val is100 = evaluation.shouldTrigger100Alert

        val title = if (is100) {
            "Anggaran $categoryLabel Terlampaui (${evaluation.usagePercentage}%)"
        } else {
            "Peringatan Anggaran $categoryLabel (${evaluation.usagePercentage}%)"
        }
        val body = "Pengeluaran $categoryLabel bulan ini mencapai $spentText dari batas $limitText."

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (is100) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_BUDGET_BASE + evaluation.budget.category.ordinal, notification)
        }
    }

    override fun notifyHighPriorityImpactWarning(warning: GoalImpactWarning) {
        if (!hasNotificationPermission()) return

        val excessFormatted = CurrencyFormatter.format(
            warning.excessAmount.setScale(0, RoundingMode.HALF_UP)
        )
        val title = "Target \"${warning.goalName}\" Berubah Menjadi Tertunda!"
        val body = "Belanja hari ini $excessFormatted di atas rata-rata; target ${warning.goalName} mundur ${warning.delayDays} hari melewati tenggat."

        val notification = NotificationCompat.Builder(context, CHANNEL_IMPACT_WARNINGS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_IMPACT_BASE + warning.goalId.toInt(), notification)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun ensureChannelsCreated() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val budgetChannel = NotificationChannel(
            CHANNEL_BUDGET_ALERTS,
            "Peringatan Anggaran Kategori",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifikasi saat pengeluaran kategori mencapai 80% atau 100% batas anggaran bulanan"
        }

        val impactChannel = NotificationChannel(
            CHANNEL_IMPACT_WARNINGS,
            "Impact Warning Target Tabungan",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifikasi prioritas tinggi saat pengeluaran harian membuat status Target Tabungan jatuh ke Tertunda"
        }

        manager.createNotificationChannel(budgetChannel)
        manager.createNotificationChannel(impactChannel)
    }

    companion object {
        const val CHANNEL_BUDGET_ALERTS = "catatrapi_budget_alerts"
        const val CHANNEL_IMPACT_WARNINGS = "catatrapi_impact_warnings"
        private const val NOTIFICATION_ID_BUDGET_BASE = 41_000
        private const val NOTIFICATION_ID_IMPACT_BASE = 42_000
    }
}
