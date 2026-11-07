package com.example.catatrapi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.catatrapi.domain.model.GoalStatus
import java.math.BigDecimal

@Entity(tableName = "saving_goals")
data class SavingGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "target_amount") val targetAmount: BigDecimal,
    val deadline: Long,
    val priority: Int,
    val status: GoalStatus,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)