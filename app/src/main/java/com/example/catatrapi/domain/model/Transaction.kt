package com.example.catatrapi.domain.model

import java.math.BigDecimal

data class Transaction(
    val id: Long = 0,
    val walletId: Long,
    val toWalletId: Long? = null,
    val type: TransactionType,
    val amount: BigDecimal,
    val fee: BigDecimal = BigDecimal.ZERO,
    val category: TransactionCategory? = null,
    val note: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isInvestmentTransaction: Boolean
        get() = type == TransactionType.INVESTMENT_BUY || type == TransactionType.INVESTMENT_SELL
}
