package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.DomainException
import com.example.catatrapi.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * FT-06: Menghapus transaksi secara atomik dan menghitung ulang saldo dompet terkait.
 * Memblokir penghapusan langsung untuk tipe INVESTMENT_BUY dan INVESTMENT_SELL dari layar Cashflow.
 */
class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(transactionId: Long) {
        if (transactionId <= 0L) {
            throw DomainException.InvalidTransactionException("ID transaksi tidak valid.")
        }
        val existing = transactionRepository.getTransactionById(transactionId)
            ?: throw DomainException.InvalidTransactionException("Transaksi tidak ditemukan.")

        if (existing.isInvestmentTransaction) {
            throw DomainException.ImmutableInvestmentTransactionException()
        }

        transactionRepository.deleteTransactionAtomically(transactionId)
    }
}
