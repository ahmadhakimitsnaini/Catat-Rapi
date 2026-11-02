package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.DomainException
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.domain.repository.TransactionRepository
import java.math.BigDecimal
import javax.inject.Inject

/**
 * FT-06: Mengedit transaksi secara atomik dan menghitung ulang saldo dompet terkait.
 * Memblokir pengeditan langsung untuk tipe INVESTMENT_BUY dan INVESTMENT_SELL dari layar Cashflow.
 */
class EditTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(updatedTransaction: Transaction) {
        if (updatedTransaction.id <= 0L) {
            throw DomainException.InvalidTransactionException("ID transaksi tidak valid.")
        }
        if (updatedTransaction.isInvestmentTransaction) {
            throw DomainException.ImmutableInvestmentTransactionException()
        }
        if (updatedTransaction.amount <= BigDecimal.ZERO) {
            throw DomainException.InvalidTransactionException("Nominal transaksi harus lebih dari Rp0.")
        }
        if (updatedTransaction.fee < BigDecimal.ZERO) {
            throw DomainException.InvalidTransactionException("Biaya admin tidak boleh negatif.")
        }

        when (updatedTransaction.type) {
            TransactionType.INCOME, TransactionType.EXPENSE -> {
                val category = updatedTransaction.category
                    ?: throw DomainException.InvalidTransactionException("Kategori transaksi wajib dipilih.")
                if (category.applicableType != updatedTransaction.type) {
                    throw DomainException.InvalidTransactionException(
                        "Kategori ${category.labelId} tidak sesuai dengan tipe transaksi ${updatedTransaction.type}."
                    )
                }
            }
            TransactionType.TRANSFER -> {
                val toId = updatedTransaction.toWalletId
                    ?: throw DomainException.InvalidTransactionException("Dompet tujuan wajib dipilih untuk transfer.")
                if (updatedTransaction.walletId == toId) {
                    throw DomainException.InvalidTransactionException("Dompet asal dan tujuan tidak boleh sama.")
                }
            }
            TransactionType.INVESTMENT_BUY, TransactionType.INVESTMENT_SELL -> {
                throw DomainException.ImmutableInvestmentTransactionException()
            }
        }

        transactionRepository.editTransactionAtomically(
            updatedTransaction.copy(
                note = updatedTransaction.note?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
    }
}
