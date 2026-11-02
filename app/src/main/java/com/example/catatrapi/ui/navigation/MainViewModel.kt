package com.example.catatrapi.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catatrapi.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface AppEntryState {
    data object Loading : AppEntryState
    data object NeedsOnboarding : AppEntryState
    data object Ready : AppEntryState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    walletRepository: WalletRepository,
) : ViewModel() {

    val entryState: StateFlow<AppEntryState> = walletRepository.observeHasAnyWallet()
        .map { hasWallet ->
            if (hasWallet) AppEntryState.Ready else AppEntryState.NeedsOnboarding
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppEntryState.Loading,
        )
}
