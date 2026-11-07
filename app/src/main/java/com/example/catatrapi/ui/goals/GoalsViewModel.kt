package com.example.catatrapi.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catatrapi.domain.model.GoalStatus
import com.example.catatrapi.domain.model.SavingGoal
import com.example.catatrapi.domain.model.SavingGoalProgress
import com.example.catatrapi.domain.repository.SavingGoalRepository
import com.example.catatrapi.domain.usecase.CalculateDynamicSavingRateUseCase
import com.example.catatrapi.domain.usecase.CreateSavingGoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import javax.inject.Inject

data class GoalListItem(
    val goal: SavingGoal,
    val savedAmount: BigDecimal,
    /** 0f..1f untuk progress bar. */
    val progress: Float,
    /** Selisih hari kalender ke tenggat. Negatif bila terlewat. */
    val daysRemaining: Long,
    val status: GoalStatus,
)

data class GoalsUiState(
    val items: List<GoalListItem> = emptyList(),
    val isLoading: Boolean = true,
    val userMessage: String? = null,
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    savingGoalRepository: SavingGoalRepository,
    private val createSavingGoalUseCase: CreateSavingGoalUseCase,
) : ViewModel() {

    private val userMessageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GoalsUiState> = combine(
        savingGoalRepository.observeActiveGoalsWithProgress(),
        userMessageFlow,
    ) { progressList, message ->
        val now = System.currentTimeMillis()
        GoalsUiState(
            items = progressList.map { it.toListItem(now) },
            isLoading = false,
            userMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalsUiState(),
    )

    fun createGoal(name: String, targetAmount: BigDecimal, deadline: Long) {
        viewModelScope.launch {
            runCatching { createSavingGoalUseCase(name = name, targetAmount = targetAmount, deadline = deadline) }
                .onSuccess { userMessageFlow.value = "Target \"$name\" berhasil dibuat." }
                .onFailure { err -> userMessageFlow.value = err.message ?: "Target tidak dapat dibuat." }
        }
    }

    fun consumeUserMessage() {
        userMessageFlow.value = null
    }
}

/**
 * Status di daftar dikoreksi saat ditampilkan agar tidak kedaluwarsa:
 * S >= T -> ACHIEVED; tenggat lewat dan S < T -> DELAYED; selain itu memakai status tersimpan.
 */
internal fun SavingGoalProgress.toListItem(
    now: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): GoalListItem {
    val days = CalculateDynamicSavingRateUseCase.daysBetween(now, goal.deadline, zoneId)
    val status = when {
        savedAmount >= goal.targetAmount -> GoalStatus.ACHIEVED
        days < 0 -> GoalStatus.DELAYED
        goal.status == GoalStatus.ACHIEVED -> GoalStatus.ON_TRACK
        else -> goal.status
    }
    val progress = if (goal.targetAmount.signum() <= 0) {
        0f
    } else {
        savedAmount.divide(goal.targetAmount, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
    }
    return GoalListItem(
        goal = goal,
        savedAmount = savedAmount,
        progress = progress,
        daysRemaining = days,
        status = status,
    )
}