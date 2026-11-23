package com.example.catatrapi.domain.repository

import com.example.catatrapi.domain.model.FinancialSettings
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

/**
 * Kontrak penyimpanan preferensi finansial dan keamanan terenkripsi (NFR-S3).
 */
interface FinancialPreferencesRepository {
    fun observeSettings(): Flow<FinancialSettings>
    suspend fun getSettings(): FinancialSettings
    suspend fun updateMinimumBuffer(amount: BigDecimal)
    suspend fun updateSweepBufferOverride(amount: BigDecimal?)
    suspend fun updateDailyExpenseLimitOverride(amount: BigDecimal?)
    suspend fun updateBiometricLock(enabled: Boolean, timeoutSeconds: Long)
}
