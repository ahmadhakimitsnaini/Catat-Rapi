package com.example.catatrapi.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catatrapi.domain.model.AutoSweepPlan
import com.example.catatrapi.domain.model.FinancialSettings
import com.example.catatrapi.domain.model.GoalContribution
import com.example.catatrapi.domain.model.GoalStatus
import com.example.catatrapi.domain.model.ImpactWarningReport
import com.example.catatrapi.domain.model.SavingGoal
import com.example.catatrapi.domain.model.SavingGoalProgress
import com.example.catatrapi.domain.model.SpendableBalance
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.Wallet
import com.example.catatrapi.domain.repository.FinancialPreferencesRepository
import com.example.catatrapi.domain.repository.SavingGoalRepository
import com.example.catatrapi.domain.repository.TransactionRepository
import com.example.catatrapi.domain.usecase.AutoSweepSurplusUseCase
import com.example.catatrapi.domain.usecase.CalculateImpactWarningUseCase
import com.example.catatrapi.domain.usecase.CalculateSpendableBalanceUseCase
import com.example.catatrapi.domain.usecase.EvaluateGoalStatusUseCase
import com.example.catatrapi.domain.usecase.GetActiveWalletsUseCase
import com.example.catatrapi.ui.components.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardGoalSummary(
    val goal: SavingGoal,
    val savedAmount: BigDecimal,
    val progress: Float,
    val status: GoalStatus,
    val etaDays: Long?,
    val daysUntilDeadline: Long,
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val wallets: List<Wallet> = emptyList(),
    val spendableBalance: SpendableBalance = SpendableBalance(BigDecimal.ZERO),
    val topGoals: List<DashboardGoalSummary> = emptyList(),
    val totalActiveGoalCount: Int = 0,
    val impactWarningReport: ImpactWarningReport = ImpactWarningReport(),
    val isImpactBannerDismissed: Boolean = false,
    val autoSweepPlan: AutoSweepPlan = AutoSweepPlan(),
    val recentTransactions: List<Transaction> = emptyList(),
    val financialSettings: FinancialSettings = FinancialSettings(),
    val userMessage: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    getActiveWalletsUseCase: GetActiveWalletsUseCase,
    calculateSpendableBalanceUseCase: CalculateSpendableBalanceUseCase,
    savingGoalRepository: SavingGoalRepository,
    transactionRepository: TransactionRepository,
    calculateImpactWarningUseCase: CalculateImpactWarningUseCase,
    private val autoSweepSurplusUseCase: AutoSweepSurplusUseCase,
    private val financialPreferencesRepository: FinancialPreferencesRepository,
) : ViewModel() {

    private val impactDismissedFlow = MutableStateFlow(false)
    private val userMessageFlow = MutableStateFlow<String?>(null)

    private val goalsAndContributionsFlow = combine(
        savingGoalRepository.observeActiveGoalsWithProgress(),
        savingGoalRepository.observeActiveGoalContributions(),
    ) { goals, contributions ->
        goals to contributions
    }

    private val coreFinancialFlow = combine(
        getActiveWalletsUseCase(),
        calculateSpendableBalanceUseCase(),
        goalsAndContributionsFlow,
        calculateImpactWarningUseCase.observe(),
        autoSweepSurplusUseCase.observePlan(),
    ) { wallets, spendable, (goals, contributions), impactReport, sweepPlan ->
        CoreDashboardData(
            wallets = wallets,
            spendable = spendable,
            goals = goals,
            contributions = contributions,
            impactReport = impactReport,
            sweepPlan = sweepPlan,
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        coreFinancialFlow,
        transactionRepository.observeRecentTransactions(limit = MAX_RECENT_TRANSACTIONS),
        financialPreferencesRepository.observeSettings(),
        impactDismissedFlow,
        userMessageFlow,
    ) { core, recentTxs, settings, isBannerDismissed, message ->
        val now = System.currentTimeMillis()
        val topGoals = mapTopGoals(
            goalsWithProgress = core.goals,
            allContributions = core.contributions,
            now = now,
        )
        DashboardUiState(
            isLoading = false,
            wallets = core.wallets,
            spendableBalance = core.spendable,
            topGoals = topGoals,
            totalActiveGoalCount = core.goals.size,
            impactWarningReport = core.impactReport,
            isImpactBannerDismissed = isBannerDismissed,
            autoSweepPlan = core.sweepPlan,
            recentTransactions = recentTxs,
            financialSettings = settings,
            userMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    fun dismissImpactBanner() {
        impactDismissedFlow.value = true
    }

    fun executeAutoSweep(
        onShowUndoSnackbar: (message: String, contributionIds: List<Long>) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                autoSweepSurplusUseCase.execute()
            }.onSuccess { result ->
                val formatted = CurrencyFormatter.format(result.totalAllocatedAmount)
                onShowUndoSnackbar(
                    "Auto-Sweep $formatted berhasil dialokasikan ke ${result.allocations.size} target.",
                    result.contributionIds,
                )
            }.onFailure { err ->
                userMessageFlow.value = err.message ?: "Gagal menjalankan Auto-Sweep."
            }
        }
    }

    fun undoAutoSweep(contributionIds: List<Long>) {
        if (contributionIds.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                autoSweepSurplusUseCase.undo(contributionIds)
            }.onSuccess {
                userMessageFlow.value = "Alokasi Auto-Sweep berhasil dibatalkan dan dana dikembalikan ke Spendable."
            }.onFailure { err ->
                userMessageFlow.value = err.message ?: "Gagal membatalkan Auto-Sweep."
            }
        }
    }

    fun saveFinancialSettings(
        minimumBuffer: BigDecimal,
        sweepBufferOverride: BigDecimal?,
        dailyExpenseLimitOverride: BigDecimal?,
        biometricLockEnabled: Boolean,
        lockTimeoutSeconds: Long,
    ) {
        viewModelScope.launch {
            runCatching {
                financialPreferencesRepository.updateMinimumBuffer(minimumBuffer)
                financialPreferencesRepository.updateSweepBufferOverride(sweepBufferOverride)
                financialPreferencesRepository.updateDailyExpenseLimitOverride(dailyExpenseLimitOverride)
                financialPreferencesRepository.updateBiometricLock(
                    enabled = biometricLockEnabled,
                    timeoutSeconds = lockTimeoutSeconds,
                )
            }.onSuccess {
                userMessageFlow.value = "Pengaturan buffer & keamanan berhasil disimpan."
            }.onFailure { err ->
                userMessageFlow.value = err.message ?: "Gagal menyimpan pengaturan."
            }
        }
    }

    fun consumeUserMessage() {
        userMessageFlow.value = null
    }

    private data class CoreDashboardData(
        val wallets: List<Wallet>,
        val spendable: SpendableBalance,
        val goals: List<SavingGoalProgress>,
        val contributions: List<GoalContribution>,
        val impactReport: ImpactWarningReport,
        val sweepPlan: AutoSweepPlan,
    )

    companion object {
        const val MAX_TOP_GOALS = 3
        const val MAX_RECENT_TRANSACTIONS = 5

        internal fun mapTopGoals(
            goalsWithProgress: List<SavingGoalProgress>,
            allContributions: List<GoalContribution>,
            now: Long,
        ): List<DashboardGoalSummary> {
            val contributionsByGoal = allContributions.groupBy { it.goalId }
            return goalsWithProgress
                .sortedWith(
                    compareBy<SavingGoalProgress>(
                        { it.goal.priority },
                        { it.goal.deadline },
                        { it.goal.id },
                    )
                )
                .take(MAX_TOP_GOALS)
                .map { item ->
                    val goalContribs = contributionsByGoal[item.goal.id] ?: emptyList()
                    val eval = EvaluateGoalStatusUseCase.evaluate(item.goal, goalContribs, now)
                    val displayStatus = if (eval.status == GoalStatus.DELAYED &&
                        goalContribs.isEmpty() &&
                        eval.daysUntilDeadline >= 0
                    ) {
                        GoalStatus.ON_TRACK
                    } else {
                        eval.status
                    }
                    val progress = if (item.goal.targetAmount.signum() <= 0) {
                        0f
                    } else {
                        item.savedAmount.divide(item.goal.targetAmount, 4, RoundingMode.HALF_UP)
                            .toFloat()
                            .coerceIn(0f, 1f)
                    }
                    DashboardGoalSummary(
                        goal = item.goal,
                        savedAmount = item.savedAmount,
                        progress = progress,
                        status = displayStatus,
                        etaDays = eval.etaDays,
                        daysUntilDeadline = eval.daysUntilDeadline,
                    )
                }
        }
    }
}
