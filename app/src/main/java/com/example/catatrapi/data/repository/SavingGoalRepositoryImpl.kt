package com.example.catatrapi.data.repository

import androidx.room.withTransaction
import com.example.catatrapi.data.local.dao.SavingGoalDao
import com.example.catatrapi.data.local.dao.WalletDao
import com.example.catatrapi.data.local.db.CatatRapiDatabase
import com.example.catatrapi.data.local.entity.GoalContributionEntity
import com.example.catatrapi.data.local.entity.SavingGoalEntity
import com.example.catatrapi.data.mapper.toDomain
import com.example.catatrapi.data.mapper.toEntity
import com.example.catatrapi.domain.model.ContributionType
import com.example.catatrapi.domain.model.DomainException
import com.example.catatrapi.domain.model.GoalContribution
import com.example.catatrapi.domain.model.GoalStatus
import com.example.catatrapi.domain.model.SavingGoal
import com.example.catatrapi.domain.model.SavingGoalProgress
import com.example.catatrapi.domain.repository.FinancialAllocationProvider
import com.example.catatrapi.domain.repository.SavingGoalRepository
import com.example.catatrapi.domain.usecase.CalculateSpendableBalanceUseCase
import com.example.catatrapi.domain.usecase.EvaluateGoalStatusUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavingGoalRepositoryImpl @Inject constructor(
    private val database: CatatRapiDatabase,
    private val savingGoalDao: SavingGoalDao,
    private val walletDao: WalletDao,
    private val allocationProvider: FinancialAllocationProvider,
) : SavingGoalRepository {

    override fun observeActiveGoalsWithProgress(): Flow<List<SavingGoalProgress>> =
        combine(
            savingGoalDao.observeActiveGoals(),
            savingGoalDao.observeActiveGoalContributions(),
        ) { goals, contributions ->
            val savedByGoal: Map<Long, BigDecimal> = contributions
                .groupBy { it.goalId }
                .mapValues { (_, list) -> list.totalAmount() }
            goals.map { goal ->
                SavingGoalProgress(
                    goal = goal.toDomain(),
                    savedAmount = savedByGoal[goal.id] ?: BigDecimal.ZERO,
                )
            }
        }

    override fun observeGoalById(id: Long): Flow<SavingGoal?> =
        savingGoalDao.observeGoalById(id).map { it?.toDomain() }

    override fun observeContributions(goalId: Long): Flow<List<GoalContribution>> =
        savingGoalDao.observeContributions(goalId).map { list -> list.map { it.toDomain() } }

    override fun observeSavedAmount(goalId: Long): Flow<BigDecimal> =
        savingGoalDao.observeContributions(goalId).map { it.totalAmount() }

    override suspend fun getGoalById(id: Long): SavingGoal? =
        savingGoalDao.getGoalById(id)?.toDomain()

    override suspend fun getSavedAmount(goalId: Long): BigDecimal =
        savingGoalDao.getContributions(goalId).totalAmount()

    override suspend fun createGoalAtomically(goal: SavingGoal): Long {
        return database.withTransaction {
            val name = goal.name.trim()
            if (name.isEmpty()) {
                throw DomainException.InvalidSavingGoalException("Nama Target Tabungan tidak boleh kosong.")
            }
            if (goal.targetAmount.signum() <= 0) {
                throw DomainException.InvalidSavingGoalException("Nominal target harus lebih besar dari nol.")
            }
            if (goal.deadline <= goal.createdAt) {
                throw DomainException.InvalidSavingGoalException("Tenggat waktu harus berada di masa depan.")
            }
            val resolvedPriority = if (goal.priority > 0) {
                goal.priority
            } else {
                (savingGoalDao.getMaxActivePriority()?.coerceAtLeast(0) ?: 0) + 1
            }
            savingGoalDao.insertGoal(
                goal.copy(
                    id = 0,
                    name = name,
                    priority = resolvedPriority,
                    status = GoalStatus.ON_TRACK,
                    isArchived = false,
                ).toEntity()
            )
        }
    }

    override suspend fun contributeToGoalAtomically(
        goalId: Long,
        walletId: Long,
        amount: BigDecimal,
        now: Long,
    ): Long {
        return database.withTransaction {
            if (amount.signum() <= 0) {
                throw DomainException.InvalidGoalContributionException("Nominal setoran harus lebih besar dari nol.")
            }
            val goal = savingGoalDao.getGoalById(goalId)
                ?: throw DomainException.SavingGoalNotFoundException()
            if (goal.isArchived) {
                throw DomainException.InvalidGoalContributionException("Target Tabungan yang diarsipkan tidak dapat menerima setoran.")
            }
            val wallet = walletDao.getById(walletId)
                ?: throw DomainException.InvalidGoalContributionException("Dompet tidak ditemukan.")
            if (wallet.isArchived) {
                throw DomainException.InvalidGoalContributionException("Dompet yang diarsipkan tidak dapat dipakai untuk setoran.")
            }

            // Spendable = W - G - B_min, dihitung di dalam transaksi.
            val totalWalletBalance = walletDao.getActiveWallets()
                .fold(BigDecimal.ZERO) { acc, w -> acc + w.balance }
            val allocatedGoals = allocationProvider.getTotalAllocatedGoals()
            val minimumBuffer = allocationProvider.observeMinimumBuffer().first()
            val spendable = CalculateSpendableBalanceUseCase
                .calculate(totalWalletBalance, allocatedGoals, minimumBuffer)
                .spendableAmount
            if (amount > spendable) {
                throw DomainException.GoalContributionExceedsSpendableException()
            }

            val contributionId = savingGoalDao.insertContribution(
                GoalContributionEntity(
                    goalId = goalId,
                    walletId = walletId,
                    amount = amount,
                    type = ContributionType.DEPOSIT,
                    createdAt = now,
                )
            )
            refreshGoalStatus(goal, now)
            contributionId
        }
    }

    override suspend fun withdrawFromGoalAtomically(
        goalId: Long,
        walletId: Long?,
        amount: BigDecimal,
        now: Long,
    ): Long {
        return database.withTransaction {
            if (amount.signum() <= 0) {
                throw DomainException.InvalidGoalContributionException("Nominal penarikan harus lebih besar dari nol.")
            }
            val goal = savingGoalDao.getGoalById(goalId)
                ?: throw DomainException.SavingGoalNotFoundException()
            if (goal.isArchived) {
                throw DomainException.InvalidGoalContributionException("Dana pada Target Tabungan yang diarsipkan tidak dapat ditarik.")
            }
            if (walletId != null) {
                val wallet = walletDao.getById(walletId)
                    ?: throw DomainException.InvalidGoalContributionException("Dompet tidak ditemukan.")
                if (wallet.isArchived) {
                    throw DomainException.InvalidGoalContributionException("Dompet yang diarsipkan tidak dapat dipakai untuk penarikan.")
                }
            }

            val savedAmount = savingGoalDao.getContributions(goalId).totalAmount()
            if (amount > savedAmount) {
                throw DomainException.GoalWithdrawExceedsSavedAmountException()
            }

            val contributionId = savingGoalDao.insertContribution(
                GoalContributionEntity(
                    goalId = goalId,
                    walletId = walletId,
                    amount = amount.negate(),
                    type = ContributionType.WITHDRAW,
                    createdAt = now,
                )
            )
            refreshGoalStatus(goal, now)
            contributionId
        }
    }

    override suspend fun reorderActiveGoalsAtomically(orderedGoalIds: List<Long>) {
        database.withTransaction {
            val activeGoals = savingGoalDao.getActiveGoals()
            if (activeGoals.isEmpty()) return@withTransaction

            val activeById = activeGoals.associateBy { it.id }
            val orderedEntities = buildList {
                orderedGoalIds.distinct().forEach { id ->
                    activeById[id]?.let { add(it) }
                }
                activeGoals.forEach { goal ->
                    if (none { it.id == goal.id }) add(goal)
                }
            }
            orderedEntities.forEachIndexed { index, entity ->
                val newPriority = index + 1
                if (entity.priority != newPriority) {
                    savingGoalDao.updatePriority(entity.id, newPriority)
                }
            }
        }
    }

    /** Hitung ulang status target berdasarkan seluruh kontribusi (termasuk yang baru di-insert). */
    private suspend fun refreshGoalStatus(goal: SavingGoalEntity, now: Long) {
        val contributions = savingGoalDao.getContributions(goal.id).map { it.toDomain() }
        val evaluation = EvaluateGoalStatusUseCase.evaluate(goal.toDomain(), contributions, now)
        savingGoalDao.updateStatus(goal.id, evaluation.status)
    }

    private fun List<GoalContributionEntity>.totalAmount(): BigDecimal =
        fold(BigDecimal.ZERO) { acc, c -> acc + c.amount }
}