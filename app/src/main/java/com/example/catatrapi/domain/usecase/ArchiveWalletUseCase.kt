package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.repository.WalletRepository
import javax.inject.Inject

/**
 * FT-01 & GS-02: Mengarsipkan dompet secara atomik.
 * Menolak pengarsipan bila W - balance_dompet < G (total alokasi target aktif),
 * serta otomatis menjeda RecurringTransactions terkait.
 */
class ArchiveWalletUseCase @Inject constructor(
    private val walletRepository: WalletRepository,
) {
    suspend operator fun invoke(walletId: Long) {
        walletRepository.archiveWalletAtomically(walletId)
    }
}
