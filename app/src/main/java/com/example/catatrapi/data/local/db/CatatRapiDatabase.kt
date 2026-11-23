package com.example.catatrapi.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.catatrapi.data.local.converter.BigDecimalTypeConverter
import com.example.catatrapi.data.local.dao.CategoryBudgetDao
import com.example.catatrapi.data.local.dao.SavingGoalDao
import com.example.catatrapi.data.local.dao.TransactionDao
import com.example.catatrapi.data.local.dao.WalletDao
import com.example.catatrapi.data.local.entity.CategoryBudgetEntity
import com.example.catatrapi.data.local.entity.GoalContributionEntity
import com.example.catatrapi.data.local.entity.SavingGoalEntity
import com.example.catatrapi.data.local.entity.TransactionEntity
import com.example.catatrapi.data.local.entity.WalletEntity

@Database(
    entities = [
        WalletEntity::class,
        TransactionEntity::class,
        SavingGoalEntity::class,
        GoalContributionEntity::class,
        CategoryBudgetEntity::class,
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(BigDecimalTypeConverter::class)
abstract class CatatRapiDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun transactionDao(): TransactionDao
    abstract fun savingGoalDao(): SavingGoalDao
    abstract fun categoryBudgetDao(): CategoryBudgetDao

    companion object { const val NAME = "catatrapi.db" }
}