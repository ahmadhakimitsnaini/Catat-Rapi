package com.example.catatrapi.domain.model

import java.math.BigDecimal

enum class BudgetAlertLevel {
    NORMAL,
    WARNING_80,
    EXCEEDED_100,
}

/**
 * FT-05: Model domain batas anggaran bulanan per kategori pengeluaran.
 */
data class CategoryBudget(
    val id: Long = 0,
    val category: TransactionCategory,
    val yearMonth: String,
    val limitAmount: BigDecimal,
    val notified80: Boolean = false,
    val notified100: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * Hasil evaluasi realisasi pengeluaran kategori terhadap batas anggaran bulanan.
 */
data class CategoryBudgetEvaluation(
    val budget: CategoryBudget,
    val spentAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    /** Rasio spent / limit (mis. 0.8500 untuk 85%). */
    val usageRatio: BigDecimal,
    /** Persentase bulat ke bawah (mis. 85 untuk 85%). */
    val usagePercentage: Int,
    val alertLevel: BudgetAlertLevel,
    /** True bila baru menembus >= 80% dan belum pernah dikirim notifikasi 80% maupun 100% pada periode ini. */
    val shouldTrigger80Alert: Boolean,
    /** True bila baru menembus >= 100% dan belum pernah dikirim notifikasi 100% pada periode ini. */
    val shouldTrigger100Alert: Boolean,
)
