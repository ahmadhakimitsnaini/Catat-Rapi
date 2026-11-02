package com.example.catatrapi.data.repository

import com.example.catatrapi.domain.repository.FinancialAllocationProvider
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Singleton
class DefaultFinancialAllocationProvider @Inject constructor() : FinancialAllocationProvider {
    override fun observeTotalAllocatedGoals(): Flow<BigDecimal> = flowOf(BigDecimal.ZERO)

    override suspend fun getTotalAllocatedGoals(): BigDecimal = BigDecimal.ZERO

    override fun observeMinimumBuffer(): Flow<BigDecimal> = flowOf(BigDecimal.ZERO)

    override suspend fun pauseRecurringTransactionsForWallet(walletId: Long) {
        // Akan dihubungkan ke RecurringTransactionDao pada Sprint 9 (FT-07)
    }
}
