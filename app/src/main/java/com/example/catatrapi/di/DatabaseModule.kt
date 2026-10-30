package com.example.catatrapi.di

import android.content.Context
import androidx.room.Room
import com.example.catatrapi.data.local.dao.TransactionDao
import com.example.catatrapi.data.local.dao.WalletDao
import com.example.catatrapi.data.local.db.CatatRapiDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CatatRapiDatabase =
        Room.databaseBuilder(context, CatatRapiDatabase::class.java, CatatRapiDatabase.NAME)
            .build()

    @Provides
    fun provideWalletDao(db: CatatRapiDatabase): WalletDao = db.walletDao()

    @Provides
    fun provideTransactionDao(db: CatatRapiDatabase): TransactionDao = db.transactionDao()
}