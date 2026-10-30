package com.example.catatrapi.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.catatrapi.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    // Daftar transaksi dengan Paging 3 (NFR-P5)
    @Query("SELECT * FROM transactions ORDER BY occurred_at DESC, id DESC")
    fun pagingAll(): PagingSource<Int, TransactionEntity>

    @Query(
        "SELECT * FROM transactions " +
                "WHERE wallet_id = :walletId OR to_wallet_id = :walletId " +
                "ORDER BY occurred_at DESC, id DESC"
    )
    fun pagingByWallet(walletId: Long): PagingSource<Int, TransactionEntity>

    @Query(
        "SELECT * FROM transactions " +
                "WHERE occurred_at BETWEEN :from AND :to ORDER BY occurred_at DESC"
    )
    fun observeInRange(from: Long, to: Long): Flow<List<TransactionEntity>>
}