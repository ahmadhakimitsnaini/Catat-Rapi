package com.example.catatrapi.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.catatrapi.data.local.converter.BigDecimalTypeConverter
import com.example.catatrapi.data.local.dao.TransactionDao
import com.example.catatrapi.data.local.dao.WalletDao
import com.example.catatrapi.data.local.entity.TransactionEntity
import com.example.catatrapi.data.local.entity.WalletEntity

@Database(
    entities = [
        WalletEntity::class,
        TransactionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(BigDecimalTypeConverter::class)
abstract class CatatRapiDatabase : RoomDatabase() {

    abstract fun walletDao(): WalletDao

    abstract fun transactionDao(): TransactionDao

    companion object {
        const val NAME = "catatrapi.db"
    }
}