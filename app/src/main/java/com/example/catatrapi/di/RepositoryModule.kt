package com.example.catatrapi.di

import com.example.catatrapi.data.repository.DefaultFinancialAllocationProvider
import com.example.catatrapi.data.repository.SavingGoalRepositoryImpl
import com.example.catatrapi.data.repository.TransactionRepositoryImpl
import com.example.catatrapi.data.repository.WalletRepositoryImpl
import com.example.catatrapi.domain.repository.FinancialAllocationProvider
import com.example.catatrapi.domain.repository.SavingGoalRepository
import com.example.catatrapi.domain.repository.TransactionRepository
import com.example.catatrapi.domain.repository.WalletRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindWalletRepository(impl: WalletRepositoryImpl): WalletRepository
    @Binds @Singleton abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
    @Binds @Singleton abstract fun bindFinancialAllocationProvider(impl: DefaultFinancialAllocationProvider): FinancialAllocationProvider
    @Binds @Singleton abstract fun bindSavingGoalRepository(impl: SavingGoalRepositoryImpl): SavingGoalRepository
}