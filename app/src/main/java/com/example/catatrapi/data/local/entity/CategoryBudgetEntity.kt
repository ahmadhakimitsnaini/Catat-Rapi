package com.example.catatrapi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * FT-05: Entitas batas anggaran bulanan per kategori pengeluaran (`category_budgets`).
 * Memiliki indeks unik pada `(category, year_month)` sesuai NFR-P6.
 */
@Entity(
    tableName = "category_budgets",
    indices = [
        Index(value = ["category", "year_month"], unique = true),
    ],
)
data class CategoryBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    @ColumnInfo(name = "year_month") val yearMonth: String,
    @ColumnInfo(name = "limit_amount") val limitAmount: BigDecimal,
    @ColumnInfo(name = "notified_80") val notified80: Boolean = false,
    @ColumnInfo(name = "notified_100") val notified100: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
