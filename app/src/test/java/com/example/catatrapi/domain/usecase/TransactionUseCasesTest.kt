package com.example.catatrapi.domain.usecase

import androidx.paging.PagingData
import com.example.catatrapi.domain.model.DomainException
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionCategory
import com.example.catatrapi.domain.model.TransactionFilter
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.domain.model.Wallet
import com.example.catatrapi.domain.model.WalletType
import com.example.catatrapi.domain.repository.TransactionRepository
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionUseCasesTest {

    @Test
    fun `record income and expense updates wallet balance atomically`() = runTest {
        val repo = InMemoryTransactionRepository()
        val recordUseCase = RecordTransactionUseCase(repo)

        recordUseCase(
            walletId = 1L,
            type = TransactionType.INCOME,
            amount = BigDecimal("500000"),
            category = TransactionCategory.SALARY,
        )
        assertEquals(BigDecimal("1500000"), repo.walletBalance(1L))

        recordUseCase(
            walletId = 1L,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("200000"),
            category = TransactionCategory.FOOD_BEVERAGE,
        )
        assertEquals(BigDecimal("1300000"), repo.walletBalance(1L))
    }

    @Test
    fun `transfer between wallets deducts amount plus fee from source and adds amount to destination`() = runTest {
        val repo = InMemoryTransactionRepository()
        val transferUseCase = TransferBetweenWalletsUseCase(repo)
        val summaryUseCase = CalculateMonthlyCashflowSummaryUseCase(repo)

        transferUseCase(
            fromWalletId = 1L,
            toWalletId = 2L,
            amount = BigDecimal("300000"),
            fee = BigDecimal("2500"),
            note = "Top up e-wallet",
        )

        // Dompet 1 (awal 1.000.000) berkurang 302.500 -> 697.500
        assertEquals(BigDecimal("697500"), repo.walletBalance(1L))
        // Dompet 2 (awal 500.000) bertambah 300.000 -> 800.000
        assertEquals(BigDecimal("800000"), repo.walletBalance(2L))

        // Pokok transfer (300.000) tidak masuk pemasukan/pengeluaran bulanan, namun fee (2.500) dihitung sebagai pengeluaran
        val summary = summaryUseCase(0L, Long.MAX_VALUE).first()
        assertEquals(BigDecimal.ZERO, summary.totalIncome)
        assertEquals(BigDecimal("2500"), summary.totalExpense)
    }

    @Test
    fun `edit and delete transaction recalculates wallet balance accurately`() = runTest {
        val repo = InMemoryTransactionRepository()
        val recordUseCase = RecordTransactionUseCase(repo)
        val editUseCase = EditTransactionUseCase(repo)
        val deleteUseCase = DeleteTransactionUseCase(repo)

        val txId = recordUseCase(
            walletId = 1L,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("100000"),
            category = TransactionCategory.FOOD_BEVERAGE,
        )
        assertEquals(BigDecimal("900000"), repo.walletBalance(1L))

        val saved = repo.getTransactionById(txId)!!
        editUseCase(saved.copy(amount = BigDecimal("150000")))
        assertEquals(BigDecimal("850000"), repo.walletBalance(1L))

        deleteUseCase(txId)
        assertEquals(BigDecimal("1000000"), repo.walletBalance(1L))
    }

    @Test(expected = DomainException.ImmutableInvestmentTransactionException::class)
    fun `delete investment buy transaction is blocked from cashflow`() = runTest {
        val repo = InMemoryTransactionRepository()
        val invId = repo.seedInvestmentTransaction(type = TransactionType.INVESTMENT_BUY)
        val deleteUseCase = DeleteTransactionUseCase(repo)

        deleteUseCase(invId)
    }

    @Test(expected = DomainException.ImmutableInvestmentTransactionException::class)
    fun `edit investment sell transaction is blocked from cashflow`() = runTest {
        val repo = InMemoryTransactionRepository()
        val invId = repo.seedInvestmentTransaction(type = TransactionType.INVESTMENT_SELL)
        val editUseCase = EditTransactionUseCase(repo)

        val existing = repo.getTransactionById(invId)!!
        editUseCase(existing.copy(amount = BigDecimal("999999")))
    }

    private class InMemoryTransactionRepository : TransactionRepository {
        private val wallets = mutableMapOf(
            1L to Wallet(id = 1L, name = "BCA", type = WalletType.BANK, balance = BigDecimal("1000000")),
            2L to Wallet(id = 2L, name = "GoPay", type = WalletType.EWALLET, balance = BigDecimal("500000")),
        )
        private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())

        fun walletBalance(id: Long): BigDecimal = wallets.getValue(id).balance

        fun seedInvestmentTransaction(type: TransactionType): Long {
            val nextId = (transactionsFlow.value.maxOfOrNull { it.id } ?: 0L) + 1L
            val tx = Transaction(
                id = nextId,
                walletId = 1L,
                type = type,
                amount = BigDecimal("250000"),
            )
            transactionsFlow.value = transactionsFlow.value + tx
            return nextId
        }

        override fun observePagedTransactions(filter: TransactionFilter): Flow<PagingData<Transaction>> =
            emptyFlow()

        override fun observeTransactionsInRange(from: Long, to: Long): Flow<List<Transaction>> =
            transactionsFlow

        override suspend fun getTransactionById(id: Long): Transaction? =
            transactionsFlow.value.firstOrNull { it.id == id }

        override suspend fun recordTransactionAtomically(transaction: Transaction): Long {
            val w = wallets.getValue(transaction.walletId)
            val newBal = when (transaction.type) {
                TransactionType.INCOME -> w.balance + transaction.amount
                TransactionType.EXPENSE -> {
                    if (w.balance < transaction.amount) throw DomainException.InsufficientBalanceException()
                    w.balance - transaction.amount
                }
                else -> throw DomainException.InvalidTransactionException("Invalid type")
            }
            wallets[w.id] = w.copy(balance = newBal)
            val nextId = (transactionsFlow.value.maxOfOrNull { it.id } ?: 0L) + 1L
            transactionsFlow.value = transactionsFlow.value + transaction.copy(id = nextId)
            return nextId
        }

        override suspend fun transferBetweenWalletsAtomically(transfer: Transaction): Long {
            val from = wallets.getValue(transfer.walletId)
            val to = wallets.getValue(transfer.toWalletId!!)
            val totalOut = transfer.amount + transfer.fee
            if (from.balance < totalOut) throw DomainException.InsufficientBalanceException()

            wallets[from.id] = from.copy(balance = from.balance - totalOut)
            wallets[to.id] = to.copy(balance = to.balance + transfer.amount)

            val nextId = (transactionsFlow.value.maxOfOrNull { it.id } ?: 0L) + 1L
            transactionsFlow.value = transactionsFlow.value + transfer.copy(id = nextId)
            return nextId
        }

        override suspend fun editTransactionAtomically(updatedTransaction: Transaction) {
            val old = getTransactionById(updatedTransaction.id)!!
            reverseEffect(old)
            applyEffect(updatedTransaction)
            transactionsFlow.value = transactionsFlow.value.map {
                if (it.id == updatedTransaction.id) updatedTransaction else it
            }
        }

        override suspend fun deleteTransactionAtomically(transactionId: Long) {
            val old = getTransactionById(transactionId)!!
            reverseEffect(old)
            transactionsFlow.value = transactionsFlow.value.filterNot { it.id == transactionId }
        }

        private fun reverseEffect(tx: Transaction) {
            val w = wallets.getValue(tx.walletId)
            when (tx.type) {
                TransactionType.INCOME -> wallets[w.id] = w.copy(balance = w.balance - tx.amount)
                TransactionType.EXPENSE -> wallets[w.id] = w.copy(balance = w.balance + tx.amount)
                TransactionType.TRANSFER -> {
                    val to = wallets.getValue(tx.toWalletId!!)
                    wallets[w.id] = w.copy(balance = w.balance + tx.amount + tx.fee)
                    wallets[to.id] = to.copy(balance = to.balance - tx.amount)
                }
                else -> throw DomainException.ImmutableInvestmentTransactionException()
            }
        }

        private fun applyEffect(tx: Transaction) {
            val w = wallets.getValue(tx.walletId)
            when (tx.type) {
                TransactionType.INCOME -> wallets[w.id] = w.copy(balance = w.balance + tx.amount)
                TransactionType.EXPENSE -> wallets[w.id] = w.copy(balance = w.balance - tx.amount)
                TransactionType.TRANSFER -> {
                    val to = wallets.getValue(tx.toWalletId!!)
                    wallets[w.id] = w.copy(balance = w.balance - tx.amount - tx.fee)
                    wallets[to.id] = to.copy(balance = to.balance + tx.amount)
                }
                else -> throw DomainException.ImmutableInvestmentTransactionException()
            }
        }
    }
}
