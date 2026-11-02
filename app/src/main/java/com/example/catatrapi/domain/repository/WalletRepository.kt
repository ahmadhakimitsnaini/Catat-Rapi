package com.example.catatrapi.domain.repository

import com.example.catatrapi.domain.model.Wallet
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    fun observeActiveWallets(): Flow<List<Wallet>>
    fun observeWalletById(id: Long): Flow<Wallet?>
    fun observeHasAnyWallet(): Flow<Boolean>
    suspend fun getActiveWallets(): List<Wallet>
    suspend fun getWalletById(id: Long): Wallet?
    suspend fun createWallet(wallet: Wallet): Long
    suspend fun updateWallet(wallet: Wallet)
    suspend fun archiveWalletAtomically(walletId: Long)
}
