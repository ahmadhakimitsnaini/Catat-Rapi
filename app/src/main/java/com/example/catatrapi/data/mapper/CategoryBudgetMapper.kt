package com.example.catatrapi.data.mapper

import com.example.catatrapi.data.local.entity.CategoryBudgetEntity
import com.example.catatrapi.domain.model.CategoryBudget
import com.example.catatrapi.domain.model.TransactionCategory

fun CategoryBudgetEntity.toDomainOrNull(): CategoryBudget? {
    val cat = TransactionCategory.fromKeyOrNull(category) ?: return null
    return CategoryBudget(
        id = id,
        category = cat,
        yearMonth = yearMonth,
        limitAmount = limitAmount,
        notified80 = notified80,
        notified100 = notified100,
        updatedAt = updatedAt,
    )
}

fun CategoryBudget.toEntity(): CategoryBudgetEntity =
    CategoryBudgetEntity(
        id = id,
        category = category.name,
        yearMonth = yearMonth,
        limitAmount = limitAmount,
        notified80 = notified80,
        notified100 = notified100,
        updatedAt = updatedAt,
    )
