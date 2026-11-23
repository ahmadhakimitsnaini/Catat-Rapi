package com.example.catatrapi.domain.model

import java.math.BigDecimal

/**
 * Rincian alokasi surplus Auto-Sweep ke satu Target Tabungan.
 */
data class AutoSweepAllocation(
    val goalId: Long,
    val goalName: String,
    val priority: Int,
    val currentSavedAmount: BigDecimal,
    val targetAmount: BigDecimal,
    val allocatedAmount: BigDecimal,
    val willAchieveGoal: Boolean,
)

/**
 * GS-05 (Rumus 2.2.3): Rencana pratinjau Auto-Sweep Surplus sebelum dikonfirmasi pengguna.
 *
 * @property spendableBalance Saldo bebas pakai saat ini (W - G - B_min).
 * @property sweepBuffer Buffer akhir periode B_sweep (default 3 * mu atau override pengguna).
 * @property rawSurplus Nilai max(0, Spendable - B_sweep) sebelum dibatasi sisa kebutuhan target.
 * @property totalAllocatedAmount Total dana yang benar-benar dialokasikan ke target aktif (<= rawSurplus).
 * @property allocations Rincian alokasi berurutan berdasarkan prioritas tertinggi.
 */
data class AutoSweepPlan(
    val spendableBalance: BigDecimal = BigDecimal.ZERO,
    val sweepBuffer: BigDecimal = BigDecimal.ZERO,
    val rawSurplus: BigDecimal = BigDecimal.ZERO,
    val totalAllocatedAmount: BigDecimal = BigDecimal.ZERO,
    val allocations: List<AutoSweepAllocation> = emptyList(),
) {
    val canExecute: Boolean
        get() = totalAllocatedAmount.signum() > 0 && allocations.isNotEmpty()
}

/**
 * Hasil eksekusi atomik Auto-Sweep di database, memuat daftar ID kontribusi untuk mekanisme Undo 10 detik.
 */
data class AutoSweepExecutionResult(
    val contributionIds: List<Long>,
    val totalAllocatedAmount: BigDecimal,
    val allocations: List<AutoSweepAllocation>,
)
