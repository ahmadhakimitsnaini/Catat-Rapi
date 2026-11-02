package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.DomainException
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionCategory
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.domain.repository.TransactionRepository
import java.math.BigDecimal
import javax.inject.Inject

/**
 * FT-02: Mencatat Pemasukan (INCOME) atau Pengeluaran (EXPENSE) secara atomik.
 * Validasi nominal > 0, tipe INCOME/EXPENSE, kategori wajib diisi, dan dompet aktif.
 */
class RecordTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        walletId: Long,
        type: TransactionType,
        amount: BigDecimal,
        category: TransactionCategory,
        note: String? = null,
        occurredAt: Long = System.currentTimeMillis(),
        createdAt: Long = System.currentTimeMillis(),
    ): Long {
        if (type != TransactionType.INCOME && type != TransactionType.EXPENSE) {
            throw DomainException.InvalidTransactionException(
                "Gunakan RecordTransactionUseCase hanya untuk INCOME atau EXPENSE."
            )
        }
        if (amount <= BigDecimal.ZERO) {
            throw DomainException.InvalidTransactionException("Nominal transaksi harus lebih dari Rp0.")
        }
        if (category.applicableType != type) {
            throw DomainException.InvalidTransactionException(
                "Kategori ${category.labelId} tidak sesuai dengan tipe transaksi $type."
            )
        }

        val transaction = Transaction(
            walletId = walletId,
            toWalletId = null,
            type = type,
            amount = amount,
            fee = BigDecimal.ZERO,
            category = category,
            note = note?.trim()?.takeIf { it.isNotEmpty() },
            occurredAt = occurredAt,
            createdAt = createdAt,
        )
        return transactionRepository.recordTransactionAtomically(transaction)
    }
}
