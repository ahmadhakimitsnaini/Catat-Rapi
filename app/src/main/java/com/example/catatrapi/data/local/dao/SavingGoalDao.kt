package com.example.catatrapi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.catatrapi.data.local.entity.GoalContributionEntity
import com.example.catatrapi.data.local.entity.SavingGoalEntity
import com.example.catatrapi.domain.model.GoalStatus
import kotlinx.coroutines.flow.Flow

/**
 * Kolom nominal disimpan sebagai TEXT, jadi DAO ini sengaja TIDAK memakai SUM().
 * Penjumlahan S_j dan G dilakukan di Kotlin dengan BigDecimal.
 */
@Dao
interface SavingGoalDao {

    @Insert
    suspend fun insertGoal(goal: SavingGoalEntity): Long

    @Insert
    suspend fun insertContribution(contribution: GoalContributionEntity): Long

    @Query("UPDATE saving_goals SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: GoalStatus)

    @Query("SELECT * FROM saving_goals WHERE id = :id")
    suspend fun getGoalById(id: Long): SavingGoalEntity?

    @Query("SELECT * FROM saving_goals WHERE id = :id")
    fun observeGoalById(id: Long): Flow<SavingGoalEntity?>

    @Query("SELECT * FROM saving_goals WHERE is_archived = 0 ORDER BY priority ASC, deadline ASC")
    fun observeActiveGoals(): Flow<List<SavingGoalEntity>>

    @Query("SELECT * FROM goal_contributions WHERE goal_id = :goalId ORDER BY created_at DESC, id DESC")
    suspend fun getContributions(goalId: Long): List<GoalContributionEntity>

    @Query("SELECT * FROM goal_contributions WHERE goal_id = :goalId ORDER BY created_at DESC, id DESC")
    fun observeContributions(goalId: Long): Flow<List<GoalContributionEntity>>

    /** Semua kontribusi milik target aktif (tidak diarsipkan). Dasar perhitungan G = sum S_j. */
    @Query(
        """
        SELECT c.* FROM goal_contributions c
        INNER JOIN saving_goals g ON g.id = c.goal_id
        WHERE g.is_archived = 0
        """
    )
    suspend fun getActiveGoalContributions(): List<GoalContributionEntity>

    @Query(
        """
        SELECT c.* FROM goal_contributions c
        INNER JOIN saving_goals g ON g.id = c.goal_id
        WHERE g.is_archived = 0
        """
    )
    fun observeActiveGoalContributions(): Flow<List<GoalContributionEntity>>
}