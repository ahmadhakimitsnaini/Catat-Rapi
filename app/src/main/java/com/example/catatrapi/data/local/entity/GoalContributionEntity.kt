package com.example.catatrapi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.catatrapi.domain.model.ContributionType
import java.math.BigDecimal

@Entity(
    tableName = "goal_contributions",
    foreignKeys = [
        ForeignKey(
            entity = SavingGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallet_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["goal_id"]),
        Index(value = ["wallet_id"]),
    ],
)
data class GoalContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "goal_id") val goalId: Long,
    @ColumnInfo(name = "wallet_id") val walletId: Long?,
    val amount: BigDecimal,
    val type: ContributionType,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)