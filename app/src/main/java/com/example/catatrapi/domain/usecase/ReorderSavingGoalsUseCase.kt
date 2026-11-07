package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.repository.SavingGoalRepository
import javax.inject.Inject

/** GS-07. Mengatur ulang urutan prioritas target aktif (1 = tertinggi) untuk penerima Auto-Sweep. */
class ReorderSavingGoalsUseCase @Inject constructor(
    private val savingGoalRepository: SavingGoalRepository,
) {
    suspend operator fun invoke(orderedGoalIds: List<Long>) {
        savingGoalRepository.reorderActiveGoalsAtomically(orderedGoalIds)
    }
}
