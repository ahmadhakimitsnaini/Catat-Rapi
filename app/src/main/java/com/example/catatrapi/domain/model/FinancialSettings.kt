package com.example.catatrapi.domain.model

import java.math.BigDecimal

/**
 * Konfigurasi buffer finansial dan keamanan lokal yang disimpan terenkripsi (NFR-S1, NFR-S3).
 *
 * @property minimumBuffer Buffer minimum B_min pada rumus Spendable = W - G - B_min (default Rp0).
 * @property sweepBufferOverride Override manual untuk B_sweep pada Auto-Sweep (null = gunakan default 3 * mu).
 * @property dailyExpenseLimitOverride Batas pengeluaran harian manual untuk Impact Warning GS-04 (null = gunakan rata-rata 30 hari mu).
 * @property biometricLockEnabled Apakah penguncian aplikasi dengan BiometricPrompt / PIN perangkat aktif.
 * @property lockTimeoutSeconds Ambang waktu di latar belakang (detik) sebelum aplikasi terkunci otomatis.
 */
data class FinancialSettings(
    val minimumBuffer: BigDecimal = BigDecimal.ZERO,
    val sweepBufferOverride: BigDecimal? = null,
    val dailyExpenseLimitOverride: BigDecimal? = null,
    val biometricLockEnabled: Boolean = false,
    val lockTimeoutSeconds: Long = DEFAULT_LOCK_TIMEOUT_SECONDS,
) {
    companion object {
        const val DEFAULT_LOCK_TIMEOUT_SECONDS = 30L
    }
}
