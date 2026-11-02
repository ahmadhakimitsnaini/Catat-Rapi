package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.DomainException
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.domain.repository.TransactionRepository
import java.math.BigDecimal
import javax.inject.Inject

/**
 * FT-04: Transfer dana antar dompet secara atomik.
 * Validasi dompet asal != tujuan, amount > 0, fee >= 0, dan saldo asal >= amount + fee.
 */
class TransferBetweenWalletsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        fromWalletId: Long,
        toWalletId: Long,
        amount: BigDecimal,
        fee: BigDecimal = BigDecimal.ZERO,
        note: String? = null,
        occurredAt: Long = System.currentTimeMillis(),
        createdAt: Long = System.currentTimeMillis(),
    ): Long {
        if (fromWalletId == toWalletId) {
            throw DomainException.InvalidTransactionException("Dompet asal dan dompet tujuan tidak boleh sama.")
        }
        if (amount <= BigDecimal.ZERO) {
            throw DomainException.InvalidTransactionException("Nominal transfer harus lebih dari Rp0.")
        }
        if (fee < BigDecimal.ZERO) {
            throw DomainException.InvalidTransactionException("Biaya admin transfer tidak boleh negatif.")
        }

        val transfer = Transaction(
            walletId = fromWalletId,
            toWalletId = toWalletId,
            type = TransactionType.TRANSFER,
            amount = amount,
            fee = fee,
            category = null,
            note = note?.trim()?.takeIf { it.isNotEmpty() },
            occurredAt = occurredAt,
            createdAt = createdAt,
        )
        return transactionRepository.transferBetweenWalletsAtomically(transfer)
    }
}
