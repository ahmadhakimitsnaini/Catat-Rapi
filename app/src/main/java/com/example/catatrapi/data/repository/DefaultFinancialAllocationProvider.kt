package com.example.catatrapi.data.repository

import com.example.catatrapi.data.local.dao.SavingGoalDao
import com.example.catatrapi.data.local.entity.GoalContributionEntity
import com.example.catatrapi.domain.repository.FinancialAllocationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hanya bergantung pada DAO (bukan repository) agar tidak membuat siklus Hilt dengan WalletRepositoryImpl.
 */
@Singleton
class DefaultFinancialAllocationProvider @Inject constructor(
    private val savingGoalDao: SavingGoalDao,
) : FinancialAllocationProvider {

    /** G = sum S_j untuk semua target aktif (tidak diarsipkan). */
    override fun observeTotalAllocatedGoals(): Flow<BigDecimal> =
        savingGoalDao.observeActiveGoalContributions()
            .map { it.totalAmount() }
            .distinctUntilChanged()

    override suspend fun getTotalAllocatedGoals(): BigDecimal =
        savingGoalDao.getActiveGoalContributions().totalAmount()

    // Tetap 0 sampai Sprint 4 (DataStore terenkripsi).
    override fun observeMinimumBuffer(): Flow<BigDecimal> = flowOf(BigDecimal.ZERO)

    override suspend fun pauseRecurringTransactionsForWallet(walletId: Long) {
        // Akan dihubungkan ke RecurringTransactionDao pada Sprint 9 (FT-07)
    }

    private fun List<GoalContributionEntity>.totalAmount(): BigDecimal =
        fold(BigDecimal.ZERO) { acc, c -> acc + c.amount }
}