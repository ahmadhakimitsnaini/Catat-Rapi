package com.example.catatrapi.ui.goals

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class ImpactSimulationResult(
    /** True bila S >= T. Simulasi tidak relevan dan kedua ETA bernilai 0. */
    val isAchieved: Boolean,
    /** ETA tanpa belanja tambahan. Null bila tidak terdefinisi (v <= 0). */
    val baselineEtaDays: Long?,
    /** ETA setelah belanja tambahan. Null bila v' <= 0. */
    val simulatedEtaDays: Long?,
    /** v' = v - X/30 (per hari). Bisa nol atau negatif. */
    val simulatedVelocityPerDay: BigDecimal,
    /** simulatedEta - baselineEta. Null bila salah satu tidak terdefinisi. */
    val etaDelayDays: Long?,
    val baselineMeetsDeadline: Boolean,
    val simulatedMeetsDeadline: Boolean,
)

/**
 * Simulasi Dampak (fungsi murni).
 *
 * Asumsi:
 *  1. X adalah belanja tambahan PER BULAN (1 bulan = 30 hari, sama dengan jendela velocity).
 *  2. Belanja tambahan mengurangi kecepatan menabung bersih: v' = v - X/30 per hari.
 *  3. Bila v' <= 0, ETA tidak terdefinisi (null).
 *  4. ETA' = ceil((T - S) / v') hari dari hari ini.
 *  5. X negatif diperlakukan sebagai 0.
 *  6. ETA dianggap memenuhi tenggat bila ETA <= daysUntilDeadline (negatif bila tenggat lewat).
 */
object GoalImpactSimulation {
    private const val DAYS_PER_MONTH = 30L

    fun simulate(
        targetAmount: BigDecimal,
        savedAmount: BigDecimal,
        velocityPerDay: BigDecimal,
        extraMonthlySpending: BigDecimal,
        daysUntilDeadline: Long,
    ): ImpactSimulationResult {
        val extra = extraMonthlySpending.max(BigDecimal.ZERO)
        val simulatedVelocity = velocityPerDay -
                extra.divide(BigDecimal.valueOf(DAYS_PER_MONTH), MathContext.DECIMAL128)

        if (savedAmount >= targetAmount) {
            return ImpactSimulationResult(
                isAchieved = true,
                baselineEtaDays = 0L,
                simulatedEtaDays = 0L,
                simulatedVelocityPerDay = simulatedVelocity,
                etaDelayDays = 0L,
                baselineMeetsDeadline = true,
                simulatedMeetsDeadline = true,
            )
        }

        val remaining = targetAmount - savedAmount // > 0
        val baselineEta = etaDays(remaining, velocityPerDay)
        val simulatedEta = etaDays(remaining, simulatedVelocity)
        val delay = if (baselineEta != null && simulatedEta != null) simulatedEta - baselineEta else null

        return ImpactSimulationResult(
            isAchieved = false,
            baselineEtaDays = baselineEta,
            simulatedEtaDays = simulatedEta,
            simulatedVelocityPerDay = simulatedVelocity,
            etaDelayDays = delay,
            baselineMeetsDeadline = baselineEta != null && baselineEta <= daysUntilDeadline,
            simulatedMeetsDeadline = simulatedEta != null && simulatedEta <= daysUntilDeadline,
        )
    }

    /** Null bila kecepatan <= 0 (tidak ada pembagian nol). Hasil dibulatkan ke atas, minimal 1 hari. */
    private fun etaDays(remaining: BigDecimal, velocityPerDay: BigDecimal): Long? {
        if (velocityPerDay.signum() <= 0) return null
        val eta = remaining.divide(velocityPerDay, 0, RoundingMode.CEILING)
        return if (eta > BigDecimal.valueOf(Long.MAX_VALUE)) Long.MAX_VALUE else eta.toLong()
    }
}