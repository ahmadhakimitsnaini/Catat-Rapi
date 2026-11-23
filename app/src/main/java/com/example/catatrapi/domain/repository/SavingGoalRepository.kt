package com.example.catatrapi.domain.repository

import com.example.catatrapi.domain.model.AutoSweepExecutionResult
import com.example.catatrapi.domain.model.GoalContribution
import com.example.catatrapi.domain.model.SavingGoal
import com.example.catatrapi.domain.model.SavingGoalProgress
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface SavingGoalRepository {
    fun observeActiveGoalsWithProgress(): Flow<List<SavingGoalProgress>>
    fun observeActiveGoalContributions(): Flow<List<GoalContribution>>
    suspend fun getActiveGoalsWithProgress(): List<SavingGoalProgress>
    suspend fun getActiveGoalContributions(): List<GoalContribution>
    fun observeGoalById(id: Long): Flow<SavingGoal?>
    fun observeContributions(goalId: Long): Flow<List<GoalContribution>>
    fun observeSavedAmount(goalId: Long): Flow<BigDecimal>
    suspend fun getGoalById(id: Long): SavingGoal?
    suspend fun getSavedAmount(goalId: Long): BigDecimal

    /** GS-01. [goal].createdAt dipakai sebagai "sekarang" untuk validasi tenggat. Mengembalikan id target baru. */
    suspend fun createGoalAtomically(goal: SavingGoal): Long

    /** GS-02. Total Saldo (W) tidak berubah. Mengembalikan id kontribusi. */
    suspend fun contributeToGoalAtomically(goalId: Long, walletId: Long, amount: BigDecimal, now: Long): Long

    /** GS-06. Mengembalikan id kontribusi (bernilai negatif). */
    suspend fun withdrawFromGoalAtomically(goalId: Long, walletId: Long?, amount: BigDecimal, now: Long): Long

    /** GS-07. Mengatur ulang urutan prioritas target aktif (1 = tertinggi) secara atomik. */
    suspend fun reorderActiveGoalsAtomically(orderedGoalIds: List<Long>)

    /** GS-05. Mengalokasikan surplus Spendable - [sweepBuffer] secara atomik ke target aktif sesuai prioritas. */
    suspend fun autoSweepSurplusAtomically(
        sweepBuffer: BigDecimal,
        now: Long = System.currentTimeMillis(),
    ): AutoSweepExecutionResult

    /** GS-05. Membatalkan (Undo) batch kontribusi Auto-Sweep secara atomik dan menghitung ulang status target. */
    suspend fun undoAutoSweepAtomically(
        contributionIds: List<Long>,
        now: Long = System.currentTimeMillis(),
    )
}