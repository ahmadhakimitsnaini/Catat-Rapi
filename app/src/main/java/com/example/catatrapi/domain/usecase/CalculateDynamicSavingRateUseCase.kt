package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.DynamicSavingRate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * GS-03 Dynamic Saving Rate.
 *  D_sisa      = max(1, tenggat - hari ini)
 *  R_harian    = max(0, T - S) / D_sisa
 *  R_mingguan  = max(0, T - S) / ceil(D_sisa / 7)
 *  DELAYED     bila tenggat lewat dan S < T
 */
class CalculateDynamicSavingRateUseCase @Inject constructor() {

    operator fun invoke(
        targetAmount: BigDecimal,
        savedAmount: BigDecimal,
        deadline: Long,
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): DynamicSavingRate = calculate(targetAmount, savedAmount, deadline, now, zoneId)

    companion object {
        private const val DAYS_PER_WEEK = 7L
        private const val MONEY_SCALE = 2

        fun calculate(
            targetAmount: BigDecimal,
            savedAmount: BigDecimal,
            deadline: Long,
            now: Long,
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): DynamicSavingRate {
            val remainingAmount = (targetAmount - savedAmount).max(BigDecimal.ZERO)
            val daysUntilDeadline = daysBetween(now, deadline, zoneId)
            val remainingDays = maxOf(1L, daysUntilDeadline) // D_sisa >= 1, tidak pernah membagi nol

            if (remainingAmount.signum() == 0) {
                // S >= T: tidak ada kekurangan, tidak mungkin DELAYED.
                return DynamicSavingRate(
                    remainingDays = remainingDays,
                    remainingAmount = BigDecimal.ZERO,
                    dailyRate = BigDecimal.ZERO,
                    weeklyRate = BigDecimal.ZERO,
                    isDelayed = false,
                )
            }

            val remainingWeeks = (remainingDays + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK // ceil, minimal 1
            return DynamicSavingRate(
                remainingDays = remainingDays,
                remainingAmount = remainingAmount,
                dailyRate = remainingAmount.divide(BigDecimal.valueOf(remainingDays), MONEY_SCALE, RoundingMode.CEILING),
                weeklyRate = remainingAmount.divide(BigDecimal.valueOf(remainingWeeks), MONEY_SCALE, RoundingMode.CEILING),
                isDelayed = daysUntilDeadline < 0,
            )
        }

        /** Selisih hari kalender (zona [zoneId]) dari [fromMillis] ke [toMillis]. Negatif bila [toMillis] lebih awal. */
        fun daysBetween(fromMillis: Long, toMillis: Long, zoneId: ZoneId): Long {
            val from = Instant.ofEpochMilli(fromMillis).atZone(zoneId).toLocalDate()
            val to = Instant.ofEpochMilli(toMillis).atZone(zoneId).toLocalDate()
            return ChronoUnit.DAYS.between(from, to)
        }
    }
}