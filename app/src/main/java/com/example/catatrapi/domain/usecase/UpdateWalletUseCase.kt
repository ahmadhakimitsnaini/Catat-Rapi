package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.DomainException
import com.example.catatrapi.domain.model.WalletType
import com.example.catatrapi.domain.repository.WalletRepository
import javax.inject.Inject

class UpdateWalletUseCase @Inject constructor(
    private val walletRepository: WalletRepository,
) {
    suspend operator fun invoke(
        walletId: Long,
        name: String,
        type: WalletType,
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            throw DomainException.InvalidTransactionException("Nama dompet tidak boleh kosong.")
        }

        val existing = walletRepository.getWalletById(walletId)
            ?: throw DomainException.InvalidTransactionException("Dompet tidak ditemukan.")

        if (existing.isArchived) {
            throw DomainException.InvalidTransactionException("Dompet yang sudah diarsipkan tidak dapat diubah.")
        }

        walletRepository.updateWallet(
            existing.copy(
                name = trimmedName,
                type = type,
            )
        )
    }
}
