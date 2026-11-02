package com.example.catatrapi.domain.model

data class TransactionFilter(
    val searchQuery: String = "",
    val walletId: Long? = null,
    val type: TransactionType? = null,
    val category: TransactionCategory? = null,
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
)
