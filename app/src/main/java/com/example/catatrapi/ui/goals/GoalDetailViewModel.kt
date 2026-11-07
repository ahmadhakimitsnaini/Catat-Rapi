package com.example.catatrapi.ui.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catatrapi.domain.model.DynamicSavingRate
import com.example.catatrapi.domain.model.GoalContribution
import com.example.catatrapi.domain.model.GoalEvaluation
import com.example.catatrapi.domain.model.GoalStatus
import com.example.catatrapi.domain.model.SavingGoal
import com.example.catatrapi.domain.model.SpendableBalance
import com.example.catatrapi.domain.model.Wallet
import com.example.catatrapi.domain.repository.SavingGoalRepository
import com.example.catatrapi.domain.repository.WalletRepository
import com.example.catatrapi.domain.usecase.CalculateDynamicSavingRateUseCase
import com.example.catatrapi.domain.usecase.CalculateSpendableBalanceUseCase
import com.example.catatrapi.domain.usecase.ContributeToGoalUseCase
import com.example.catatrapi.domain.usecase.EvaluateGoalStatusUseCase
import com.example.catatrapi.domain.usecase.WithdrawFromGoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class GoalDetailUiState(
    val isLoading: Boolean = true,
    /** Null setelah selesai memuat berarti target tidak ditemukan. */
    val goal: SavingGoal? = null,
    val savedAmount: BigDecimal = BigDecimal.ZERO,
    val rate: DynamicSavingRate? = null,
    val evaluation: GoalEvaluation? = null,
    val displayStatus: GoalStatus = GoalStatus.ON_TRACK,
    val contributions: List<GoalContribution> = emptyList(),
    val wallets: List<Wallet> = emptyList(),
    val spendable: SpendableBalance = SpendableBalance(totalWalletBalance = BigDecimal.ZERO),
    val userMessage: String? = null,
)

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    savingGoalRepository: SavingGoalRepository,
    walletRepository: WalletRepository,
    calculateSpendableBalanceUseCase: CalculateSpendableBalanceUseCase,
    private val contributeToGoalUseCase: ContributeToGoalUseCase,
    private val withdrawFromGoalUseCase: WithdrawFromGoalUseCase,
) : ViewModel() {

    private val goalId: Long = checkNotNull(savedStateHandle.get<Long>(GoalRoutes.GOAL_ID_ARG)) {
        "Argumen goalId wajib ada."
    }

    private val userMessageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GoalDetailUiState> = combine(
        savingGoalRepository.observeGoalById(goalId),
        savingGoalRepository.observeContributions(goalId),
        walletRepository.observeActiveWallets(),
        calculateSpendableBalanceUseCase(),
        userMessageFlow,
    ) { goal, contributions, wallets, spendable, message ->
        if (goal == null) {
            return@combine GoalDetailUiState(
                isLoading = false,
                wallets = wallets,
                spendable = spendable,
                userMessage = message,
            )
        }
        val now = System.currentTimeMillis()
        val saved = contributions.fold(BigDecimal.ZERO) { acc, c -> acc + c.amount }
        val rate = CalculateDynamicSavingRateUseCase.calculate(goal.targetAmount, saved, goal.deadline, now)
        val evaluation = EvaluateGoalStatusUseCase.evaluate(goal, contributions, now)

        // Target baru tanpa kontribusi dan belum lewat tenggat tidak ditampilkan "Tertunda",
        // agar konsisten dengan status awal ON_TRACK di daftar.
        val displayStatus =
            if (evaluation.status == GoalStatus.DELAYED && contributions.isEmpty() && evaluation.daysUntilDeadline >= 0) {
                GoalStatus.ON_TRACK
            } else {
                evaluation.status
            }

        GoalDetailUiState(
            isLoading = false,
            goal = goal,
            savedAmount = saved,
            rate = rate,
            evaluation = evaluation,
            displayStatus = displayStatus,
            contributions = contributions,
            wallets = wallets,
            spendable = spendable,
            userMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalDetailUiState(),
    )

    fun contribute(walletId: Long, amount: BigDecimal) {
        viewModelScope.launch {
            runCatching { contributeToGoalUseCase(goalId = goalId, walletId = walletId, amount = amount) }
                .onSuccess { userMessageFlow.value = "Setoran berhasil dicatat." }
                .onFailure { err -> userMessageFlow.value = err.message ?: "Setoran tidak dapat dicatat." }
        }
    }

    fun withdraw(amount: BigDecimal) {
        viewModelScope.launch {
            runCatching { withdrawFromGoalUseCase(goalId = goalId, walletId = null, amount = amount) }
                .onSuccess { userMessageFlow.value = "Penarikan berhasil dicatat." }
                .onFailure { err -> userMessageFlow.value = err.message ?: "Penarikan tidak dapat dicatat." }
        }
    }

    fun consumeUserMessage() {
        userMessageFlow.value = null
    }
}