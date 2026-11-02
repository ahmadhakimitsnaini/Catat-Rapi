package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.MonthlyCashflowSummary
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.domain.repository.TransactionRepository
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Menghitung ringkasan Pemasukan & Pengeluaran pada rentang waktu tertentu.
 * Aturan FT-04:
 * - Pokok transfer (TRANSFER.amount) TIDAK dihitung sebagai pemasukan/pengeluaran bulanan.
 * - Biaya admin transfer (TRANSFER.fee > 0) DIHITUNG sebagai pengeluaran (ADMIN_FEE).
 * - Transaksi konversi aset (INVESTMENT_BUY / INVESTMENT_SELL) tidak dihitung sebagai pengeluaran/pemasukan konsumtif.
 */
class CalculateMonthlyCashflowSummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(fromMillis: Long, toMillis: Long): Flow<MonthlyCashflowSummary> =
        transactionRepository.observeTransactionsInRange(fromMillis, toMillis).map { transactions ->
            summarize(transactions)
        }

    companion object {
        fun summarize(transactions: List<Transaction>): MonthlyCashflowSummary {
            var income = BigDecimal.ZERO
            var expense = BigDecimal.ZERO

            for (tx in transactions) {
                when (tx.type) {
                    TransactionType.INCOME -> {
                        income += tx.amount
                    }
                    TransactionType.EXPENSE -> {
                        expense += tx.amount
                    }
                    TransactionType.TRANSFER -> {
                        if (tx.fee > BigDecimal.ZERO) {
                            expense += tx.fee
                        }
                    }
                    TransactionType.INVESTMENT_BUY, TransactionType.INVESTMENT_SELL -> {
                        // Konversi aset bukan pengeluaran/pemasukan bulanan melainkan perpindahan kas <-> portofolio
                    }
                }
            }

            return MonthlyCashflowSummary(
                totalIncome = income,
                totalExpense = expense,
            )
        }
    }
}
