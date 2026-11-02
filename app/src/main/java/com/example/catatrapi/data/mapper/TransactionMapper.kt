package com.example.catatrapi.data.mapper

import com.example.catatrapi.data.local.entity.TransactionEntity
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionCategory

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    walletId = walletId,
    toWalletId = toWalletId,
    type = type,
    amount = amount,
    fee = fee,
    category = TransactionCategory.fromKeyOrNull(category),
    note = note,
    occurredAt = occurredAt,
    createdAt = createdAt,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    walletId = walletId,
    toWalletId = toWalletId,
    type = type,
    amount = amount,
    fee = fee,
    category = category?.name,
    note = note,
    occurredAt = occurredAt,
    createdAt = createdAt,
)
