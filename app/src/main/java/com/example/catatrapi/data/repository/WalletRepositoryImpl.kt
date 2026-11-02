package com.example.catatrapi.data.repository

import androidx.room.withTransaction
import com.example.catatrapi.data.local.dao.WalletDao
import com.example.catatrapi.data.local.db.CatatRapiDatabase
import com.example.catatrapi.data.mapper.toDomain
import com.example.catatrapi.data.mapper.toEntity
import com.example.catatrapi.domain.model.DomainException
import com.example.catatrapi.domain.model.Wallet
import com.example.catatrapi.domain.repository.FinancialAllocationProvider
import com.example.catatrapi.domain.repository.WalletRepository
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val database: CatatRapiDatabase,
    private val walletDao: WalletDao,
    private val allocationProvider: FinancialAllocationProvider,
) : WalletRepository {

    override fun observeActiveWallets(): Flow<List<Wallet>> =
        walletDao.observeActive().map { list -> list.map { it.toDomain() } }

    override fun observeWalletById(id: Long): Flow<Wallet?> =
        walletDao.observeById(id).map { it?.toDomain() }

    override fun observeHasAnyWallet(): Flow<Boolean> =
        walletDao.observeTotalWalletCount().map { count -> count > 0 }

    override suspend fun getActiveWallets(): List<Wallet> =
        walletDao.getActiveWallets().map { it.toDomain() }

    override suspend fun getWalletById(id: Long): Wallet? =
        walletDao.getById(id)?.toDomain()

    override suspend fun createWallet(wallet: Wallet): Long =
        walletDao.insert(wallet.toEntity())

    override suspend fun updateWallet(wallet: Wallet) {
        walletDao.update(wallet.toEntity())
    }

    override suspend fun archiveWalletAtomically(walletId: Long) {
        database.withTransaction {
            val targetWallet = walletDao.getById(walletId)
                ?: throw DomainException.InvalidTransactionException("Dompet tidak ditemukan.")
            if (targetWallet.isArchived) return@withTransaction

            val activeWallets = walletDao.getActiveWallets()
            val totalActiveBalance = activeWallets.fold(BigDecimal.ZERO) { acc, w -> acc + w.balance }
            val allocatedGoals = allocationProvider.getTotalAllocatedGoals()
            val remainingBalanceAfterArchive = totalActiveBalance - targetWallet.balance

            if (remainingBalanceAfterArchive < allocatedGoals) {
                throw DomainException.WalletArchiveBlockedByGoalException()
            }

            walletDao.setArchived(walletId, true)
            allocationProvider.pauseRecurringTransactionsForWallet(walletId)
        }
    }
}
