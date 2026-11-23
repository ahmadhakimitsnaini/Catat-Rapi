package com.example.catatrapi.data.repository

import com.example.catatrapi.data.local.dao.SavingGoalDao
import com.example.catatrapi.data.local.entity.GoalContributionEntity
import com.example.catatrapi.domain.repository.FinancialAllocationProvider
import com.example.catatrapi.domain.repository.FinancialPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hanya bergantung pada DAO dan FinancialPreferencesRepository agar tidak membuat siklus Hilt dengan WalletRepositoryImpl.
 */
@Singleton
class DefaultFinancialAllocationProvider @Inject constructor(
    private val savingGoalDao: SavingGoalDao,
    private val financialPreferencesRepository: FinancialPreferencesRepository,
) : FinancialAllocationProvider {

    /** G = sum S_j untuk semua target aktif (tidak diarsipkan). */
    override fun observeTotalAllocatedGoals(): Flow<BigDecimal> =
        savingGoalDao.observeActiveGoalContributions()
            .map { it.totalAmount() }
            .distinctUntilChanged()

    override suspend fun getTotalAllocatedGoals(): BigDecimal =
        savingGoalDao.getActiveGoalContributions().totalAmount()

    /** B_min dari EncryptedPreferencesDataStore (NFR-S3). */
    override fun observeMinimumBuffer(): Flow<BigDecimal> =
        financialPreferencesRepository.observeSettings()
            .map { it.minimumBuffer }
            .distinctUntilChanged()

    override suspend fun pauseRecurringTransactionsForWallet(walletId: Long) {
        // Akan dihubungkan ke RecurringTransactionDao pada Sprint 9 (FT-07)
    }

    private fun List<GoalContributionEntity>.totalAmount(): BigDecimal =
        fold(BigDecimal.ZERO) { acc, c -> acc + c.amount }
}