package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.repository.SavingGoalRepository
import java.math.BigDecimal
import javax.inject.Inject

/** GS-02. Validasi setoran <= Spendable dilakukan di repository dalam transaksi. */
class ContributeToGoalUseCase @Inject constructor(
    private val savingGoalRepository: SavingGoalRepository,
) {
    suspend operator fun invoke(
        goalId: Long,
        walletId: Long,
        amount: BigDecimal,
        now: Long = System.currentTimeMillis(),
    ): Long = savingGoalRepository.contributeToGoalAtomically(goalId, walletId, amount, now)
}