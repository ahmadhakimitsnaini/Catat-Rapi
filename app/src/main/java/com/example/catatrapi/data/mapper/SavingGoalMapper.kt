package com.example.catatrapi.data.mapper

import com.example.catatrapi.data.local.entity.GoalContributionEntity
import com.example.catatrapi.data.local.entity.SavingGoalEntity
import com.example.catatrapi.domain.model.GoalContribution
import com.example.catatrapi.domain.model.SavingGoal

fun SavingGoalEntity.toDomain(): SavingGoal = SavingGoal(
    id = id,
    name = name,
    targetAmount = targetAmount,
    deadline = deadline,
    priority = priority,
    status = status,
    isArchived = isArchived,
    createdAt = createdAt,
)

fun SavingGoal.toEntity(): SavingGoalEntity = SavingGoalEntity(
    id = id,
    name = name,
    targetAmount = targetAmount,
    deadline = deadline,
    priority = priority,
    status = status,
    isArchived = isArchived,
    createdAt = createdAt,
)

fun GoalContributionEntity.toDomain(): GoalContribution = GoalContribution(
    id = id,
    goalId = goalId,
    walletId = walletId,
    amount = amount,
    type = type,
    createdAt = createdAt,
)

fun GoalContribution.toEntity(): GoalContributionEntity = GoalContributionEntity(
    id = id,
    goalId = goalId,
    walletId = walletId,
    amount = amount,
    type = type,
    createdAt = createdAt,
)