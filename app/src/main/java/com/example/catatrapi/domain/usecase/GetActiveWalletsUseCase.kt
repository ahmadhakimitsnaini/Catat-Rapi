package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.Wallet
import com.example.catatrapi.domain.repository.WalletRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetActiveWalletsUseCase @Inject constructor(
    private val walletRepository: WalletRepository,
) {
    operator fun invoke(): Flow<List<Wallet>> = walletRepository.observeActiveWallets()
}
