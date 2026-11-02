package com.example.catatrapi.domain.repository

import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

/**
 * Kontrak penyedia data lintas modul untuk rumus Spendable Balance (2.2.1)
 * dan aturan pengarsipan dompet (FT-01 & GS-02).
 * Di Sprint 2 menggunakan implementasi default (0), dan di Sprint 3/4/9 akan dihubung ke SavingGoalDao,
 * EncryptedPreferencesDataStore, dan RecurringTransactionDao.
 */
interface FinancialAllocationProvider {
    fun observeTotalAllocatedGoals(): Flow<BigDecimal>
    suspend fun getTotalAllocatedGoals(): BigDecimal
    fun observeMinimumBuffer(): Flow<BigDecimal>
    suspend fun pauseRecurringTransactionsForWallet(walletId: Long)
}
