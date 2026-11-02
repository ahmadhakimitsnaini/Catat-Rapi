package com.example.catatrapi.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.example.catatrapi.data.local.dao.TransactionDao
import com.example.catatrapi.data.local.dao.WalletDao
import com.example.catatrapi.data.local.db.CatatRapiDatabase
import com.example.catatrapi.data.local.entity.TransactionEntity
import com.example.catatrapi.data.mapper.toDomain
import com.example.catatrapi.data.mapper.toEntity
import com.example.catatrapi.domain.model.DomainException
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionFilter
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.domain.repository.TransactionRepository
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val database: CatatRapiDatabase,
    private val transactionDao: TransactionDao,
    private val walletDao: WalletDao,
) : TransactionRepository {

    override fun observePagedTransactions(filter: TransactionFilter): Flow<PagingData<Transaction>> {
        return Pager(
            config = PagingConfig(
                pageSize = 30,
                enablePlaceholders = false,
                initialLoadSize = 60,
            ),
            pagingSourceFactory = {
                transactionDao.pagingFiltered(
                    walletId = filter.walletId,
                    type = filter.type,
                    category = filter.category?.name,
                    searchQuery = filter.searchQuery.trim(),
                    startDateMillis = filter.startDateMillis,
                    endDateMillis = filter.endDateMillis,
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun observeTransactionsInRange(from: Long, to: Long): Flow<List<Transaction>> =
        transactionDao.observeInRange(from, to).map { list -> list.map { it.toDomain() } }

    override suspend fun getTransactionById(id: Long): Transaction? =
        transactionDao.getById(id)?.toDomain()

    override suspend fun recordTransactionAtomically(transaction: Transaction): Long {
        return database.withTransaction {
            val wallet = walletDao.getById(transaction.walletId)
                ?: throw DomainException.InvalidTransactionException("Dompet tidak ditemukan.")
            if (wallet.isArchived) {
                throw DomainException.InvalidTransactionException("Dompet yang diarsipkan tidak dapat menerima transaksi baru.")
            }

            val newBalance = when (transaction.type) {
                TransactionType.INCOME -> wallet.balance + transaction.amount
                TransactionType.EXPENSE -> {
                    if (wallet.balance < transaction.amount) {
                        throw DomainException.InsufficientBalanceException()
                    }
                    wallet.balance - transaction.amount
                }
                else -> throw DomainException.InvalidTransactionException("Tipe transaksi tidak valid untuk pencatatan pemasukan/pengeluaran.")
            }

            val insertedId = transactionDao.insert(transaction.toEntity())
            walletDao.updateBalance(wallet.id, newBalance)
            insertedId
        }
    }

    override suspend fun transferBetweenWalletsAtomically(transfer: Transaction): Long {
        return database.withTransaction {
            val toWalletId = transfer.toWalletId
                ?: throw DomainException.InvalidTransactionException("Dompet tujuan wajib diisi untuk transfer.")
            if (transfer.walletId == toWalletId) {
                throw DomainException.InvalidTransactionException("Dompet asal dan dompet tujuan tidak boleh sama.")
            }

            val fromWallet = walletDao.getById(transfer.walletId)
                ?: throw DomainException.InvalidTransactionException("Dompet asal tidak ditemukan.")
            val toWallet = walletDao.getById(toWalletId)
                ?: throw DomainException.InvalidTransactionException("Dompet tujuan tidak ditemukan.")

            if (fromWallet.isArchived || toWallet.isArchived) {
                throw DomainException.InvalidTransactionException("Transfer tidak dapat dilakukan pada dompet yang sudah diarsipkan.")
            }

            val totalOutflow = transfer.amount + transfer.fee
            if (fromWallet.balance < totalOutflow) {
                throw DomainException.InsufficientBalanceException(
                    "Saldo dompet asal tidak cukup untuk nominal transfer beserta biaya admin."
                )
            }

            val txEntity = transfer.copy(
                type = TransactionType.TRANSFER,
                category = null,
            ).toEntity()

            val insertedId = transactionDao.insert(txEntity)
            walletDao.updateBalance(fromWallet.id, fromWallet.balance - totalOutflow)
            walletDao.updateBalance(toWallet.id, toWallet.balance + transfer.amount)
            insertedId
        }
    }

    override suspend fun editTransactionAtomically(updatedTransaction: Transaction) {
        database.withTransaction {
            val existing = transactionDao.getById(updatedTransaction.id)
                ?: throw DomainException.InvalidTransactionException("Transaksi yang akan diedit tidak ditemukan.")

            if (existing.type == TransactionType.INVESTMENT_BUY ||
                existing.type == TransactionType.INVESTMENT_SELL ||
                updatedTransaction.isInvestmentTransaction
            ) {
                throw DomainException.ImmutableInvestmentTransactionException()
            }

            // 1. Batalkan efek transaksi lama pada dompet terkait
            reverseTransactionBalanceEffect(existing)

            // 2. Terapkan efek transaksi baru pada dompet terkait (membaca saldo terbaru pasca-reverse)
            applyNewTransactionBalanceEffect(updatedTransaction)

            // 3. Simpan perubahan baris transaksi
            transactionDao.update(updatedTransaction.toEntity())
        }
    }

    override suspend fun deleteTransactionAtomically(transactionId: Long) {
        database.withTransaction {
            val existing = transactionDao.getById(transactionId)
                ?: throw DomainException.InvalidTransactionException("Transaksi tidak ditemukan.")

            if (existing.type == TransactionType.INVESTMENT_BUY ||
                existing.type == TransactionType.INVESTMENT_SELL
            ) {
                throw DomainException.ImmutableInvestmentTransactionException()
            }

            reverseTransactionBalanceEffect(existing)
            transactionDao.delete(existing)
        }
    }

    private suspend fun reverseTransactionBalanceEffect(tx: TransactionEntity) {
        val fromWallet = walletDao.getById(tx.walletId)
            ?: throw DomainException.InvalidTransactionException("Dompet asal transaksi tidak ditemukan.")

        when (tx.type) {
            TransactionType.INCOME -> {
                val reverted = fromWallet.balance - tx.amount
                if (reverted < BigDecimal.ZERO) {
                    throw DomainException.InsufficientBalanceException(
                        "Penghapusan/perubahan pemasukan ini akan membuat saldo dompet menjadi negatif."
                    )
                }
                walletDao.updateBalance(fromWallet.id, reverted)
            }
            TransactionType.EXPENSE -> {
                walletDao.updateBalance(fromWallet.id, fromWallet.balance + tx.amount)
            }
            TransactionType.TRANSFER -> {
                val toWalletId = tx.toWalletId
                    ?: throw DomainException.InvalidTransactionException("Data dompet tujuan transfer tidak valid.")
                val toWallet = walletDao.getById(toWalletId)
                    ?: throw DomainException.InvalidTransactionException("Dompet tujuan transfer tidak ditemukan.")

                if (toWallet.balance < tx.amount) {
                    throw DomainException.InsufficientBalanceException(
                        "Saldo dompet tujuan tidak mencukupi untuk membatalkan transfer ini."
                    )
                }
                walletDao.updateBalance(fromWallet.id, fromWallet.balance + tx.amount + tx.fee)
                walletDao.updateBalance(toWallet.id, toWallet.balance - tx.amount)
            }
            TransactionType.INVESTMENT_BUY, TransactionType.INVESTMENT_SELL -> {
                throw DomainException.ImmutableInvestmentTransactionException()
            }
        }
    }

    private suspend fun applyNewTransactionBalanceEffect(tx: Transaction) {
        val fromWallet = walletDao.getById(tx.walletId)
            ?: throw DomainException.InvalidTransactionException("Dompet asal tidak ditemukan.")
        if (fromWallet.isArchived) {
            throw DomainException.InvalidTransactionException("Dompet yang diarsipkan tidak dapat digunakan.")
        }

        when (tx.type) {
            TransactionType.INCOME -> {
                walletDao.updateBalance(fromWallet.id, fromWallet.balance + tx.amount)
            }
            TransactionType.EXPENSE -> {
                if (fromWallet.balance < tx.amount) {
                    throw DomainException.InsufficientBalanceException()
                }
                walletDao.updateBalance(fromWallet.id, fromWallet.balance - tx.amount)
            }
            TransactionType.TRANSFER -> {
                val toWalletId = tx.toWalletId
                    ?: throw DomainException.InvalidTransactionException("Dompet tujuan wajib diisi untuk transfer.")
                if (tx.walletId == toWalletId) {
                    throw DomainException.InvalidTransactionException("Dompet asal dan tujuan tidak boleh sama.")
                }
                val toWallet = walletDao.getById(toWalletId)
                    ?: throw DomainException.InvalidTransactionException("Dompet tujuan tidak ditemukan.")
                if (toWallet.isArchived) {
                    throw DomainException.InvalidTransactionException("Dompet tujuan sudah diarsipkan.")
                }
                val totalOutflow = tx.amount + tx.fee
                if (fromWallet.balance < totalOutflow) {
                    throw DomainException.InsufficientBalanceException()
                }
                walletDao.updateBalance(fromWallet.id, fromWallet.balance - totalOutflow)
                walletDao.updateBalance(toWallet.id, toWallet.balance + tx.amount)
            }
            TransactionType.INVESTMENT_BUY, TransactionType.INVESTMENT_SELL -> {
                throw DomainException.ImmutableInvestmentTransactionException()
            }
        }
    }
}
