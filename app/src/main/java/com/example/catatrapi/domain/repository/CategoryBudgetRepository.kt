package com.example.catatrapi.domain.repository

import com.example.catatrapi.domain.model.CategoryBudget
import com.example.catatrapi.domain.model.TransactionCategory
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

interface CategoryBudgetRepository {
    fun observeBudgetsByMonth(yearMonth: String): Flow<List<CategoryBudget>>
    suspend fun getBudgetsByMonth(yearMonth: String): List<CategoryBudget>
    suspend fun getBudget(category: TransactionCategory, yearMonth: String): CategoryBudget?
    suspend fun upsertBudget(
        category: TransactionCategory,
        yearMonth: String,
        limitAmount: BigDecimal,
        now: Long = System.currentTimeMillis(),
    ): Long
    suspend fun markNotificationSent(
        budgetId: Long,
        notified80: Boolean,
        notified100: Boolean,
        now: Long = System.currentTimeMillis(),
    )
    suspend fun deleteBudget(budgetId: Long)
}
