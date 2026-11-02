package com.example.catatrapi.data.mapper

import com.example.catatrapi.data.local.entity.WalletEntity
import com.example.catatrapi.domain.model.Wallet

fun WalletEntity.toDomain(): Wallet = Wallet(
    id = id,
    name = name,
    type = type,
    balance = balance,
    isArchived = isArchived,
    createdAt = createdAt,
)

fun Wallet.toEntity(): WalletEntity = WalletEntity(
    id = id,
    name = name,
    type = type,
    balance = balance,
    isArchived = isArchived,
    createdAt = createdAt,
)
