package com.example.catatrapi.domain.repository

import androidx.paging.PagingData
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionFilter
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observePagedTransactions(filter: TransactionFilter): Flow<PagingData<Transaction>>
    fun observeTransactionsInRange(from: Long, to: Long): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun recordTransactionAtomically(transaction: Transaction): Long
    suspend fun transferBetweenWalletsAtomically(transfer: Transaction): Long
    suspend fun editTransactionAtomically(updatedTransaction: Transaction)
    suspend fun deleteTransactionAtomically(transactionId: Long)
}
