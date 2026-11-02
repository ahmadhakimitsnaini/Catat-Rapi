package com.example.catatrapi.domain.model

import java.math.BigDecimal

data class Wallet(
    val id: Long = 0,
    val name: String,
    val type: WalletType,
    val balance: BigDecimal,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
