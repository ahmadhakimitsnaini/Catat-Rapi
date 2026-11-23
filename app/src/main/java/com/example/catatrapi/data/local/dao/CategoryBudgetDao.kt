package com.example.catatrapi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.catatrapi.data.local.entity.CategoryBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: CategoryBudgetEntity): Long

    @Query("SELECT * FROM category_budgets WHERE year_month = :yearMonth ORDER BY category ASC")
    fun observeByYearMonth(yearMonth: String): Flow<List<CategoryBudgetEntity>>

    @Query("SELECT * FROM category_budgets WHERE year_month = :yearMonth ORDER BY category ASC")
    suspend fun getByYearMonth(yearMonth: String): List<CategoryBudgetEntity>

    @Query("SELECT * FROM category_budgets WHERE category = :category AND year_month = :yearMonth LIMIT 1")
    suspend fun getByCategoryAndMonth(category: String, yearMonth: String): CategoryBudgetEntity?

    @Query(
        "UPDATE category_budgets " +
                "SET notified_80 = :notified80, notified_100 = :notified100, updated_at = :updatedAt " +
                "WHERE id = :id"
    )
    suspend fun updateNotificationFlags(
        id: Long,
        notified80: Boolean,
        notified100: Boolean,
        updatedAt: Long,
    )

    @Query("DELETE FROM category_budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM category_budgets WHERE category = :category AND year_month = :yearMonth")
    suspend fun deleteByCategoryAndMonth(category: String, yearMonth: String)
}
