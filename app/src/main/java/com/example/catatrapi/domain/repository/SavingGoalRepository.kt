package com.example.catatrapi.domain.repository

import com.example.catatrapi.domain.model.GoalContribution
import com.example.catatrapi.domain.model.SavingGoal
import com.example.catatrapi.domain.model.SavingGoalProgress
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface SavingGoalRepository {
    fun observeActiveGoalsWithProgress(): Flow<List<SavingGoalProgress>>
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
}