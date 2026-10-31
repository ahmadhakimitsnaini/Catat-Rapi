package com.example.catatrapi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.catatrapi.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Insert
    suspend fun insert(wallet: WalletEntity): Long

    @Update
    suspend fun update(wallet: WalletEntity)

    @Query("SELECT * FROM wallets WHERE is_archived = 0 ORDER BY created_at ASC")
    fun observeActive(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE id = :id")
    fun observeById(id: Long): Flow<WalletEntity?>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getById(id: Long): WalletEntity?

    @Query("UPDATE wallets SET is_archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)
}