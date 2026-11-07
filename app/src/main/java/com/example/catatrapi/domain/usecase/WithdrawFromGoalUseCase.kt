package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.repository.SavingGoalRepository
import java.math.BigDecimal
import javax.inject.Inject

/** GS-06. Validasi tarik <= S_j dilakukan di repository dalam transaksi. */
class WithdrawFromGoalUseCase @Inject constructor(
    private val savingGoalRepository: SavingGoalRepository,
) {
    suspend operator fun invoke(
        goalId: Long,
        walletId: Long?,
        amount: BigDecimal,
        now: Long = System.currentTimeMillis(),
    ): Long = savingGoalRepository.withdrawFromGoalAtomically(goalId, walletId, amount, now)
}