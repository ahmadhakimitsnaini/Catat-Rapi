package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.DomainException
import com.example.catatrapi.domain.model.Wallet
import com.example.catatrapi.domain.model.WalletType
import com.example.catatrapi.domain.repository.WalletRepository
import java.math.BigDecimal
import javax.inject.Inject

class CreateWalletUseCase @Inject constructor(
    private val walletRepository: WalletRepository,
) {
    suspend operator fun invoke(
        name: String,
        type: WalletType,
        initialBalance: BigDecimal,
        createdAt: Long = System.currentTimeMillis(),
    ): Long {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            throw DomainException.InvalidTransactionException("Nama dompet tidak boleh kosong.")
        }
        if (initialBalance < BigDecimal.ZERO) {
            throw DomainException.InvalidTransactionException("Saldo awal dompet tidak boleh negatif.")
        }

        return walletRepository.createWallet(
            Wallet(
                name = trimmedName,
                type = type,
                balance = initialBalance,
                isArchived = false,
                createdAt = createdAt,
            )
        )
    }
}
