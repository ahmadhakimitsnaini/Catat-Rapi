package com.example.catatrapi.ui.cashflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.example.catatrapi.domain.model.MonthlyCashflowSummary
import com.example.catatrapi.domain.model.SpendableBalance
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionCategory
import com.example.catatrapi.domain.model.TransactionFilter
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.domain.model.Wallet
import com.example.catatrapi.domain.model.WalletType
import com.example.catatrapi.domain.repository.TransactionRepository
import com.example.catatrapi.domain.usecase.ArchiveWalletUseCase
import com.example.catatrapi.domain.usecase.CalculateMonthlyCashflowSummaryUseCase
import com.example.catatrapi.domain.usecase.CalculateSpendableBalanceUseCase
import com.example.catatrapi.domain.usecase.CreateWalletUseCase
import com.example.catatrapi.domain.usecase.DeleteTransactionUseCase
import com.example.catatrapi.domain.usecase.EditTransactionUseCase
import com.example.catatrapi.domain.usecase.GetActiveWalletsUseCase
import com.example.catatrapi.domain.usecase.RecordTransactionUseCase
import com.example.catatrapi.domain.usecase.TransferBetweenWalletsUseCase
import com.example.catatrapi.domain.usecase.UpdateWalletUseCase
import com.example.catatrapi.ui.components.DateFormatterId
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CashflowListItem {
    data class DateHeader(
        val epochDay: Long,
        val formattedDate: String,
    ) : CashflowListItem

    data class TransactionRow(
        val transaction: Transaction,
    ) : CashflowListItem
}

data class CashflowUiState(
    val wallets: List<Wallet> = emptyList(),
    val spendableBalance: SpendableBalance = SpendableBalance(BigDecimal.ZERO),
    val selectedMonth: YearMonth = YearMonth.now(),
    val monthlySummary: MonthlyCashflowSummary = MonthlyCashflowSummary(),
    val filter: TransactionFilter = TransactionFilter(),
    val userMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CashflowViewModel @Inject constructor(
    getActiveWalletsUseCase: GetActiveWalletsUseCase,
    calculateSpendableBalanceUseCase: CalculateSpendableBalanceUseCase,
    private val calculateMonthlyCashflowSummaryUseCase: CalculateMonthlyCashflowSummaryUseCase,
    private val transactionRepository: TransactionRepository,
    private val createWalletUseCase: CreateWalletUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val archiveWalletUseCase: ArchiveWalletUseCase,
    private val recordTransactionUseCase: RecordTransactionUseCase,
    private val transferBetweenWalletsUseCase: TransferBetweenWalletsUseCase,
    private val editTransactionUseCase: EditTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
) : ViewModel() {

    private val selectedMonthFlow = MutableStateFlow(YearMonth.now())
    private val filterFlow = MutableStateFlow(TransactionFilter())
    private val userMessageFlow = MutableStateFlow<String?>(null)

    private val monthlySummaryFlow: Flow<MonthlyCashflowSummary> =
        selectedMonthFlow.flatMapLatest { ym ->
            val zone = ZoneId.systemDefault()
            val startMillis = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000)
                .atZone(zone).toInstant().toEpochMilli()
            calculateMonthlyCashflowSummaryUseCase(startMillis, endMillis)
        }

    val uiState: StateFlow<CashflowUiState> = combine(
        getActiveWalletsUseCase(),
        calculateSpendableBalanceUseCase(),
        selectedMonthFlow,
        monthlySummaryFlow,
        combine(filterFlow, userMessageFlow) { f, msg -> f to msg },
    ) { wallets, spendable, month, summary, (filter, message) ->
        CashflowUiState(
            wallets = wallets,
            spendableBalance = spendable,
            selectedMonth = month,
            monthlySummary = summary,
            filter = filter,
            userMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CashflowUiState(),
    )

    val pagedTransactions: Flow<PagingData<CashflowListItem>> = combine(
        filterFlow,
        selectedMonthFlow,
    ) { filter, ym ->
        val zone = ZoneId.systemDefault()
        val startMillis = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000)
            .atZone(zone).toInstant().toEpochMilli()
        filter.copy(
            startDateMillis = startMillis,
            endDateMillis = endMillis,
        )
    }.flatMapLatest { activeFilter ->
        transactionRepository.observePagedTransactions(activeFilter).map { pagingData ->
            pagingData
                .map { tx -> CashflowListItem.TransactionRow(tx) }
                .insertSeparators { before: CashflowListItem.TransactionRow?, after: CashflowListItem.TransactionRow? ->
                    if (after == null) return@insertSeparators null
                    val afterDay = after.transaction.occurredAt.toLocalEpochDay()
                    val beforeDay = before?.transaction?.occurredAt?.toLocalEpochDay()
                    if (beforeDay == null || beforeDay != afterDay) {
                        CashflowListItem.DateHeader(
                            epochDay = afterDay,
                            formattedDate = DateFormatterId.full(after.transaction.occurredAt),
                        )
                    } else {
                        null
                    }
                }
        }
    }.cachedIn(viewModelScope)

    fun onPreviousMonth() {
        selectedMonthFlow.value = selectedMonthFlow.value.minusMonths(1)
    }

    fun onNextMonth() {
        selectedMonthFlow.value = selectedMonthFlow.value.plusMonths(1)
    }

    fun onSearchQueryChange(query: String) {
        filterFlow.value = filterFlow.value.copy(searchQuery = query)
    }

    fun onSelectWalletFilter(walletId: Long?) {
        val current = filterFlow.value.walletId
        filterFlow.value = filterFlow.value.copy(
            walletId = if (current == walletId) null else walletId
        )
    }

    fun onSelectTypeFilter(type: TransactionType?) {
        val current = filterFlow.value.type
        val nextType = if (current == type) null else type
        filterFlow.value = filterFlow.value.copy(
            type = nextType,
            category = null,
        )
    }

    fun onSelectCategoryFilter(category: TransactionCategory?) {
        val current = filterFlow.value.category
        filterFlow.value = filterFlow.value.copy(
            category = if (current == category) null else category
        )
    }

    fun clearFilters() {
        filterFlow.value = TransactionFilter()
    }

    fun consumeUserMessage() {
        userMessageFlow.value = null
    }

    fun createWallet(name: String, type: WalletType, initialBalance: BigDecimal) {
        viewModelScope.launch {
            runCatching {
                createWalletUseCase(name = name, type = type, initialBalance = initialBalance)
            }.onSuccess {
                userMessageFlow.value = "Dompet \"$name\" berhasil ditambahkan."
            }.onFailure { err ->
                userMessageFlow.value = err.message ?: "Gagal menambahkan dompet."
            }
        }
    }

    fun updateWallet(walletId: Long, name: String, type: WalletType) {
        viewModelScope.launch {
            runCatching {
                updateWalletUseCase(walletId = walletId, name = name, type = type)
            }.onSuccess {
                userMessageFlow.value = "Dompet berhasil diperbarui."
            }.onFailure { err ->
                userMessageFlow.value = err.message ?: "Gagal memperbarui dompet."
            }
        }
    }

    fun archiveWallet(walletId: Long) {
        viewModelScope.launch {
            runCatching {
                archiveWalletUseCase(walletId)
            }.onSuccess {
                if (filterFlow.value.walletId == walletId) {
                    filterFlow.value = filterFlow.value.copy(walletId = null)
                }
                userMessageFlow.value = "Dompet berhasil diarsipkan."
            }.onFailure { err ->
                userMessageFlow.value = err.message ?: "Gagal mengarsipkan dompet."
            }
        }
    }

    fun recordIncomeOrExpense(
        walletId: Long,
        type: TransactionType,
        amount: BigDecimal,
        category: TransactionCategory,
        note: String?,
    ) {
        viewModelScope.launch {
            runCatching {
                recordTransactionUseCase(
                    walletId = walletId,
                    type = type,
                    amount = amount,
                    category = category,
                    note = note,
                )
            }.onSuccess {
                userMessageFlow.value = "Transaksi berhasil disimpan."
            }.onFailure { err ->
                userMessageFlow.value = err.message ?: "Gagal menyimpan transaksi."
            }
        }
    }

    fun transferBetweenWallets(
        fromWalletId: Long,
        toWalletId: Long,
        amount: BigDecimal,
        fee: BigDecimal,
        note: String?,
    ) {
        viewModelScope.launch {
            runCatching {
                transferBetweenWalletsUseCase(
                    fromWalletId = fromWalletId,
                    toWalletId = toWalletId,
                    amount = amount,
                    fee = fee,
                    note = note,
                )
            }.onSuccess {
                userMessageFlow.value = "Transfer antar dompet berhasil."
            }.onFailure { err ->
                userMessageFlow.value = err.message ?: "Gagal melakukan transfer."
            }
        }
    }

    fun editTransaction(updated: Transaction) {
        viewModelScope.launch {
            runCatching {
                editTransactionUseCase(updated)
            }.onSuccess {
                userMessageFlow.value = "Transaksi berhasil diperbarui."
            }.onFailure { err ->
                userMessageFlow.value = err.message ?: "Gagal memperbarui transaksi."
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            runCatching {
                deleteTransactionUseCase(transaction.id)
            }.onSuccess {
                userMessageFlow.value = "Transaksi berhasil dihapus dan saldo dompet telah disesuaikan."
            }.onFailure { err ->
                userMessageFlow.value = err.message ?: "Transaksi tidak dapat dihapus."
            }
        }
    }

    private fun Long.toLocalEpochDay(): Long =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
}
