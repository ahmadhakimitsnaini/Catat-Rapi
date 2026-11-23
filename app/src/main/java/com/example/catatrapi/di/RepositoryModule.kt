package com.example.catatrapi.di

import com.example.catatrapi.data.local.security.CryptoCipher
import com.example.catatrapi.data.local.security.EncryptedPreferencesDataStore
import com.example.catatrapi.data.local.security.KeystoreCryptoManager
import com.example.catatrapi.data.repository.CategoryBudgetRepositoryImpl
import com.example.catatrapi.data.repository.DefaultFinancialAllocationProvider
import com.example.catatrapi.data.repository.SavingGoalRepositoryImpl
import com.example.catatrapi.data.repository.TransactionRepositoryImpl
import com.example.catatrapi.data.repository.WalletRepositoryImpl
import com.example.catatrapi.domain.repository.AlertNotificationSink
import com.example.catatrapi.domain.repository.CategoryBudgetRepository
import com.example.catatrapi.domain.repository.FinancialAllocationProvider
import com.example.catatrapi.domain.repository.FinancialPreferencesRepository
import com.example.catatrapi.domain.repository.SavingGoalRepository
import com.example.catatrapi.domain.repository.TransactionRepository
import com.example.catatrapi.domain.repository.WalletRepository
import com.example.catatrapi.ui.notifications.LocalAlertNotifier
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
    @Binds @Singleton abstract fun bindCategoryBudgetRepository(impl: CategoryBudgetRepositoryImpl): CategoryBudgetRepository
    @Binds @Singleton abstract fun bindCryptoCipher(impl: KeystoreCryptoManager): CryptoCipher
    @Binds @Singleton abstract fun bindFinancialPreferencesRepository(impl: EncryptedPreferencesDataStore): FinancialPreferencesRepository
    @Binds @Singleton abstract fun bindAlertNotificationSink(impl: LocalAlertNotifier): AlertNotificationSink
}