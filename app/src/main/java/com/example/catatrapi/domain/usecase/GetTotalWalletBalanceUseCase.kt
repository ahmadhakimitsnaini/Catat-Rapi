package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.repository.WalletRepository
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * FT-01: Menghitung Total Saldo dompet aktif: W = \sum_{i \in dompet aktif} saldo_i
 */
class GetTotalWalletBalanceUseCase @Inject constructor(
    private val walletRepository: WalletRepository,
) {
    operator fun invoke(): Flow<BigDecimal> =
        walletRepository.observeActiveWallets().map { activeWallets ->
            activeWallets.fold(BigDecimal.ZERO) { acc, wallet -> acc + wallet.balance }
        }
}
