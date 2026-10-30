package com.example.catatrapi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.catatrapi.domain.model.TransactionType
import java.math.BigDecimal

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallet_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_wallet_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("wallet_id"),
        Index("to_wallet_id"),
        Index("occurred_at"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "wallet_id") val walletId: Long,
    @ColumnInfo(name = "to_wallet_id") val toWalletId: Long? = null,
    val type: TransactionType,
    val amount: BigDecimal,
    val fee: BigDecimal = BigDecimal.ZERO,
    val category: String? = null,
    val note: String? = null,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)