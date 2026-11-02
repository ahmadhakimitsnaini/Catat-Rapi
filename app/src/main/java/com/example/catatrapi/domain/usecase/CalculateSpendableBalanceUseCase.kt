package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.SpendableBalance
import com.example.catatrapi.domain.repository.FinancialAllocationProvider
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * FT-03 (Rumus 2.2.1): Spendable = W - G - B_min
 * Mempertahankan nilai negatif (Spendable < 0) sebagai status peringatan "alokasi melebihi saldo",
 * tidak pernah dibulatkan ke nol.
 */
class CalculateSpendableBalanceUseCase @Inject constructor(
    private val getTotalWalletBalanceUseCase: GetTotalWalletBalanceUseCase,
    private val allocationProvider: FinancialAllocationProvider,
) {
    operator fun invoke(): Flow<SpendableBalance> = combine(
        getTotalWalletBalanceUseCase(),
        allocationProvider.observeTotalAllocatedGoals(),
        allocationProvider.observeMinimumBuffer(),
    ) { totalWalletBalance, allocatedGoals, minimumBuffer ->
        calculate(
            totalWalletBalance = totalWalletBalance,
            allocatedGoalsAmount = allocatedGoals,
            minimumBuffer = minimumBuffer,
        )
    }

    companion object {
        fun calculate(
            totalWalletBalance: BigDecimal,
            allocatedGoalsAmount: BigDecimal = BigDecimal.ZERO,
            minimumBuffer: BigDecimal = BigDecimal.ZERO,
        ): SpendableBalance {
            val spendable = totalWalletBalance - allocatedGoalsAmount - minimumBuffer
            return SpendableBalance(
                totalWalletBalance = totalWalletBalance,
                allocatedGoalsAmount = allocatedGoalsAmount,
                minimumBuffer = minimumBuffer,
                spendableAmount = spendable,
            )
        }
    }
}
