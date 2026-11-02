package com.example.catatrapi.domain.model

import java.math.BigDecimal

/**
 * Representasi Rumus 2.2.1: Spendable = W - G - B_min
 * Nilai negatif (spendableAmount < 0) dipertahankan sebagai peringatan "alokasi melebihi saldo".
 */
data class SpendableBalance(
    val totalWalletBalance: BigDecimal,
    val allocatedGoalsAmount: BigDecimal = BigDecimal.ZERO,
    val minimumBuffer: BigDecimal = BigDecimal.ZERO,
    val spendableAmount: BigDecimal = totalWalletBalance - allocatedGoalsAmount - minimumBuffer,
) {
    val isOverAllocated: Boolean
        get() = spendableAmount.signum() < 0
}
