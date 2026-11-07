package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.GoalStatus
import com.example.catatrapi.domain.model.SavingGoal
import com.example.catatrapi.domain.repository.SavingGoalRepository
import java.math.BigDecimal
import javax.inject.Inject

/** GS-01. Validasi (T > 0, tenggat di masa depan) dilakukan di repository dalam transaksi. */
class CreateSavingGoalUseCase @Inject constructor(
    private val savingGoalRepository: SavingGoalRepository,
) {
    suspend operator fun invoke(
        name: String,
        targetAmount: BigDecimal,
        deadline: Long,
        priority: Int = 0,
        now: Long = System.currentTimeMillis(),
    ): Long = savingGoalRepository.createGoalAtomically(
        SavingGoal(
            name = name,
            targetAmount = targetAmount,
            deadline = deadline,
            priority = priority,
            status = GoalStatus.ON_TRACK,
            isArchived = false,
            createdAt = now,
        )
    )
}