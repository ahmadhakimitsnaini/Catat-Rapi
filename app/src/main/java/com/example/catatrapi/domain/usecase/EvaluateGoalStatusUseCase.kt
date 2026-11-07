package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.GoalContribution
import com.example.catatrapi.domain.model.GoalEvaluation
import com.example.catatrapi.domain.model.GoalStatus
import com.example.catatrapi.domain.model.SavingGoal
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.ZoneId
import javax.inject.Inject

/**
 * Rumus 2.2.3 Evaluasi Status Target.
 *  v = sum(c_k, 30 hari terakhir) / min(30, d_berjalan)
 *  S >= T                      -> ACHIEVED
 *  v <= 0                      -> ETA tidak terdefinisi, DELAYED
 *  S < T, v > 0, ETA <= tenggat -> ON_TRACK, selain itu DELAYED
 */
class EvaluateGoalStatusUseCase @Inject constructor() {

    operator fun invoke(
        goal: SavingGoal,
        contributions: List<GoalContribution>,
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): GoalEvaluation = evaluate(goal, contributions, now, zoneId)

    companion object {
        private const val VELOCITY_WINDOW_DAYS = 30L
        private const val MILLIS_PER_DAY = 86_400_000L

        fun evaluate(
            goal: SavingGoal,
            contributions: List<GoalContribution>,
            now: Long,
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): GoalEvaluation {
            val savedAmount = contributions.fold(BigDecimal.ZERO) { acc, c -> acc + c.amount }
            val daysUntilDeadline = CalculateDynamicSavingRateUseCase.daysBetween(now, goal.deadline, zoneId)
            val velocity = calculateVelocity(contributions, goal.createdAt, now, zoneId)

            if (savedAmount >= goal.targetAmount) {
                return GoalEvaluation(GoalStatus.ACHIEVED, savedAmount, velocity, 0L, daysUntilDeadline)
            }
            if (velocity.signum() <= 0) {
                return GoalEvaluation(GoalStatus.DELAYED, savedAmount, velocity, null, daysUntilDeadline)
            }

            // S < T dan v > 0: sisa > 0 sehingga ETA >= 1 hari.
            val remaining = goal.targetAmount - savedAmount
            val etaDays = remaining.divide(velocity, 0, RoundingMode.CEILING).toLongClamped()
            // Tenggat lewat (daysUntilDeadline < 0) otomatis menghasilkan DELAYED karena etaDays >= 1.
            val status = if (etaDays <= daysUntilDeadline) GoalStatus.ON_TRACK else GoalStatus.DELAYED
            return GoalEvaluation(status, savedAmount, velocity, etaDays, daysUntilDeadline)
        }

        /** Velocity bersih per hari. Penarikan (negatif) ikut mengurangi. */
        fun calculateVelocity(
            contributions: List<GoalContribution>,
            goalCreatedAt: Long,
            now: Long,
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): BigDecimal {
            val windowStart = now - VELOCITY_WINDOW_DAYS * MILLIS_PER_DAY
            val windowSum = contributions
                .filter { it.createdAt in windowStart..now }
                .fold(BigDecimal.ZERO) { acc, c -> acc + c.amount }
            val elapsedDays = maxOf(1L, CalculateDynamicSavingRateUseCase.daysBetween(goalCreatedAt, now, zoneId))
            val divisor = minOf(VELOCITY_WINDOW_DAYS, elapsedDays) // minimal 1, tidak pernah membagi nol
            return windowSum.divide(BigDecimal.valueOf(divisor), MathContext.DECIMAL128)
        }

        private fun BigDecimal.toLongClamped(): Long =
            if (this > BigDecimal.valueOf(Long.MAX_VALUE)) Long.MAX_VALUE else toLong()
    }
}