package com.example.catatrapi.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catatrapi.domain.model.WalletType
import com.example.catatrapi.domain.usecase.CreateWalletUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep {
    CREATE_FIRST_WALLET,
    OPTIONAL_FIRST_GOAL,
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.CREATE_FIRST_WALLET,
    val walletName: String = "Dompet Utama",
    val walletType: WalletType = WalletType.BANK,
    val initialBalanceInput: String = "",
    val goalNameInput: String = "",
    val goalTargetInput: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isCompleted: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val createWalletUseCase: CreateWalletUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onWalletNameChange(name: String) {
        _uiState.update { it.copy(walletName = name, errorMessage = null) }
    }

    fun onWalletTypeChange(type: WalletType) {
        _uiState.update { it.copy(walletType = type, errorMessage = null) }
    }

    fun onInitialBalanceChange(raw: String) {
        val digitsOnly = raw.filter { it.isDigit() }
        _uiState.update { it.copy(initialBalanceInput = digitsOnly, errorMessage = null) }
    }

    fun onGoalNameChange(name: String) {
        _uiState.update { it.copy(goalNameInput = name) }
    }

    fun onGoalTargetChange(raw: String) {
        val digitsOnly = raw.filter { it.isDigit() }
        _uiState.update { it.copy(goalTargetInput = digitsOnly) }
    }

    fun submitFirstWallet() {
        val current = _uiState.value
        val trimmedName = current.walletName.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Nama dompet pertama wajib diisi.") }
            return
        }

        val balance = current.initialBalanceInput.takeIf { it.isNotEmpty() }
            ?.toBigDecimalOrNull() ?: BigDecimal.ZERO

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            runCatching {
                createWalletUseCase(
                    name = trimmedName,
                    type = current.walletType,
                    initialBalance = balance,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        step = OnboardingStep.OPTIONAL_FIRST_GOAL,
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = err.message ?: "Gagal membuat dompet pertama.",
                    )
                }
            }
        }
    }

    fun finishOnboarding() {
        _uiState.update { it.copy(isCompleted = true) }
    }
}
