package com.example.catatrapi.data.repository

import androidx.room.withTransaction
import com.example.catatrapi.data.local.dao.CategoryBudgetDao
import com.example.catatrapi.data.local.db.CatatRapiDatabase
import com.example.catatrapi.data.local.entity.CategoryBudgetEntity
import com.example.catatrapi.data.mapper.toDomainOrNull
import com.example.catatrapi.domain.model.CategoryBudget
import com.example.catatrapi.domain.model.DomainException
import com.example.catatrapi.domain.model.TransactionCategory
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.domain.repository.CategoryBudgetRepository
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CategoryBudgetRepositoryImpl @Inject constructor(
    private val database: CatatRapiDatabase,
    private val categoryBudgetDao: CategoryBudgetDao,
) : CategoryBudgetRepository {

    override fun observeBudgetsByMonth(yearMonth: String): Flow<List<CategoryBudget>> =
        categoryBudgetDao.observeByYearMonth(yearMonth).map { entities ->
            entities.mapNotNull { it.toDomainOrNull() }
        }

    override suspend fun getBudgetsByMonth(yearMonth: String): List<CategoryBudget> =
        categoryBudgetDao.getByYearMonth(yearMonth).mapNotNull { it.toDomainOrNull() }

    override suspend fun getBudget(
        category: TransactionCategory,
        yearMonth: String,
    ): CategoryBudget? =
        categoryBudgetDao.getByCategoryAndMonth(category.name, yearMonth)?.toDomainOrNull()

    override suspend fun upsertBudget(
        category: TransactionCategory,
        yearMonth: String,
        limitAmount: BigDecimal,
        now: Long,
    ): Long {
        if (category.applicableType != TransactionType.EXPENSE) {
            throw DomainException.InvalidCategoryBudgetException(
                "Batas anggaran hanya dapat diatur untuk kategori pengeluaran."
            )
        }
        if (limitAmount.signum() <= 0) {
            throw DomainException.InvalidCategoryBudgetException(
                "Batas anggaran harus lebih besar dari Rp0."
            )
        }
        val cleanMonth = yearMonth.trim()
        if (!YEAR_MONTH_REGEX.matches(cleanMonth)) {
            throw DomainException.InvalidCategoryBudgetException(
                "Format periode anggaran harus YYYY-MM."
            )
        }

        return database.withTransaction {
            val existing = categoryBudgetDao.getByCategoryAndMonth(category.name, cleanMonth)
            // Jika limit dinaikkan, reset flag notifikasi agar dapat dievaluasi terhadap batas baru
            val keepFlags = existing != null && limitAmount <= existing.limitAmount
            categoryBudgetDao.upsert(
                CategoryBudgetEntity(
                    id = existing?.id ?: 0L,
                    category = category.name,
                    yearMonth = cleanMonth,
                    limitAmount = limitAmount,
                    notified80 = if (keepFlags) existing.notified80 else false,
                    notified100 = if (keepFlags) existing.notified100 else false,
                    updatedAt = now,
                )
            )
        }
    }

    override suspend fun markNotificationSent(
        budgetId: Long,
        notified80: Boolean,
        notified100: Boolean,
        now: Long,
    ) {
        categoryBudgetDao.updateNotificationFlags(
            id = budgetId,
            notified80 = notified80,
            notified100 = notified100,
            updatedAt = now,
        )
    }

    override suspend fun deleteBudget(budgetId: Long) {
        categoryBudgetDao.deleteById(budgetId)
    }

    companion object {
        private val YEAR_MONTH_REGEX = Regex("^\\d{4}-\\d{2}$")
    }
}
