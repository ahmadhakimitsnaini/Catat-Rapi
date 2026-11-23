package com.example.catatrapi.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.ui.cashflow.CashflowScreen
import com.example.catatrapi.ui.cashflow.CashflowViewModel
import com.example.catatrapi.ui.cashflow.TransactionFormSheet
import com.example.catatrapi.ui.dashboard.DashboardScreen
import com.example.catatrapi.ui.dashboard.DashboardViewModel
import com.example.catatrapi.ui.goals.GoalDetailScreen
import com.example.catatrapi.ui.goals.GoalDetailViewModel
import com.example.catatrapi.ui.goals.GoalRoutes
import com.example.catatrapi.ui.goals.GoalsScreen
import com.example.catatrapi.ui.goals.GoalsViewModel
import com.example.catatrapi.ui.onboarding.OnboardingScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val AUTO_SWEEP_UNDO_TIMEOUT_MS = 10_000L

@Composable
fun CatatRapiAppRoot(
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val entryState by mainViewModel.entryState.collectAsStateWithLifecycle()
    var onboardingCompletedManually by remember { mutableStateOf(false) }

    when (entryState) {
        AppEntryState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        AppEntryState.NeedsOnboarding -> {
            if (onboardingCompletedManually) {
                MainScaffold()
            } else {
                OnboardingScreen(
                    onOnboardingCompleted = { onboardingCompletedManually = true }
                )
            }
        }
        AppEntryState.Ready -> {
            MainScaffold()
        }
    }
}

@Composable
fun MainScaffold(
    cashflowViewModel: CashflowViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: MainTab.DASHBOARD.route

    val cashflowUiState by cashflowViewModel.uiState.collectAsStateWithLifecycle()
    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val pagedTransactions = cashflowViewModel.pagedTransactions.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showTransactionSheet by remember { mutableStateOf(false) }
    var initialTransactionType by remember { mutableStateOf<TransactionType?>(null) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    // GS-05: Mekanisme Snackbar Undo selama 10 detik pada scope MainScaffold agar tidak putus saat berpindah tab
    fun showAutoSweepUndoSnackbar(
        message: String,
        contributionIds: List<Long>,
        onUndo: (List<Long>) -> Unit,
    ) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = withTimeoutOrNull(AUTO_SWEEP_UNDO_TIMEOUT_MS) {
                snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "Urungkan",
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite,
                )
            }
            if (result == SnackbarResult.ActionPerformed) {
                onUndo(contributionIds)
            } else {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
    }

    fun navigateToTopLevelTab(tab: MainTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val activeTab = MainTab.entries.firstOrNull {
        currentRoute == it.route || currentRoute.startsWith(it.route + "/")
    } ?: MainTab.DASHBOARD

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    val selected = activeTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navigateToTopLevelTab(tab)
                            } else if (currentRoute != tab.route) {
                                navController.popBackStack(tab.route, inclusive = false)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                            )
                        },
                        label = { Text(tab.title) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTab.showQuickTransactionFab) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingTransaction = null
                        initialTransactionType = null
                        showTransactionSheet = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("+ Transaksi") },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainTab.DASHBOARD.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(MainTab.DASHBOARD.route) {
                DashboardScreen(
                    uiState = dashboardUiState,
                    snackbarHostState = snackbarHostState,
                    onOpenGoalDetail = { goalId ->
                        navController.navigate(GoalRoutes.detail(goalId))
                    },
                    onOpenGoalsTab = { navigateToTopLevelTab(MainTab.GOALS_WEALTH) },
                    onOpenCashflowTab = { navigateToTopLevelTab(MainTab.CASHFLOW) },
                    onQuickTransferClick = {
                        editingTransaction = null
                        initialTransactionType = TransactionType.TRANSFER
                        showTransactionSheet = true
                    },
                    onEditTransactionClick = { tx ->
                        if (tx.isInvestmentTransaction) {
                            cashflowViewModel.deleteTransaction(tx)
                        } else {
                            editingTransaction = tx
                            initialTransactionType = null
                            showTransactionSheet = true
                        }
                    },
                    onExecuteAutoSweep = {
                        dashboardViewModel.executeAutoSweep { msg, ids ->
                            showAutoSweepUndoSnackbar(
                                message = msg,
                                contributionIds = ids,
                                onUndo = dashboardViewModel::undoAutoSweep,
                            )
                        }
                    },
                    onDismissImpactBanner = dashboardViewModel::dismissImpactBanner,
                    onSaveFinancialSettings = dashboardViewModel::saveFinancialSettings,
                    onConsumeMessage = dashboardViewModel::consumeUserMessage,
                )
            }

            composable(MainTab.CASHFLOW.route) {
                CashflowScreen(
                    uiState = cashflowUiState,
                    pagedItems = pagedTransactions,
                    snackbarHostState = snackbarHostState,
                    onPreviousMonth = cashflowViewModel::onPreviousMonth,
                    onNextMonth = cashflowViewModel::onNextMonth,
                    onSearchQueryChange = cashflowViewModel::onSearchQueryChange,
                    onSelectWalletFilter = cashflowViewModel::onSelectWalletFilter,
                    onSelectTypeFilter = cashflowViewModel::onSelectTypeFilter,
                    onSelectCategoryFilter = cashflowViewModel::onSelectCategoryFilter,
                    onClearFilters = cashflowViewModel::clearFilters,
                    onCreateWallet = cashflowViewModel::createWallet,
                    onUpdateWallet = cashflowViewModel::updateWallet,
                    onArchiveWallet = cashflowViewModel::archiveWallet,
                    onEditTransactionRequest = { tx ->
                        if (tx.isInvestmentTransaction) {
                            cashflowViewModel.deleteTransaction(tx)
                        } else {
                            editingTransaction = tx
                            initialTransactionType = null
                            showTransactionSheet = true
                        }
                    },
                    onDeleteTransaction = cashflowViewModel::deleteTransaction,
                    onUpsertCategoryBudget = cashflowViewModel::upsertCategoryBudget,
                    onDeleteCategoryBudget = cashflowViewModel::deleteCategoryBudget,
                    onConsumeMessage = cashflowViewModel::consumeUserMessage,
                )
            }

            composable(MainTab.GOALS_WEALTH.route) {
                val goalsViewModel: GoalsViewModel = hiltViewModel()
                val goalsUiState by goalsViewModel.uiState.collectAsStateWithLifecycle()
                GoalsScreen(
                    uiState = goalsUiState,
                    snackbarHostState = snackbarHostState,
                    onCreateGoal = goalsViewModel::createGoal,
                    onOpenGoal = { goalId -> navController.navigate(GoalRoutes.detail(goalId)) },
                    onConsumeMessage = goalsViewModel::consumeUserMessage,
                    onMoveGoalPriority = goalsViewModel::moveGoalPriority,
                )
            }

            composable(
                route = GoalRoutes.DETAIL_PATTERN,
                arguments = listOf(
                    navArgument(GoalRoutes.GOAL_ID_ARG) { type = NavType.LongType },
                ),
            ) {
                val detailViewModel: GoalDetailViewModel = hiltViewModel()
                val detailUiState by detailViewModel.uiState.collectAsStateWithLifecycle()
                GoalDetailScreen(
                    uiState = detailUiState,
                    snackbarHostState = snackbarHostState,
                    onBack = { navController.popBackStack() },
                    onContribute = detailViewModel::contribute,
                    onWithdraw = detailViewModel::withdraw,
                    onAutoSweep = {
                        detailViewModel.executeAutoSweep { msg, ids ->
                            showAutoSweepUndoSnackbar(
                                message = msg,
                                contributionIds = ids,
                                onUndo = detailViewModel::undoAutoSweep,
                            )
                        }
                    },
                    onConsumeMessage = detailViewModel::consumeUserMessage,
                )
            }

            composable(MainTab.ANALYTICS.route) {
                UpcomingModulePlaceholder(
                    title = "Analytics & Net Worth",
                    description = "Laporan visual bulanan, Savings Rate, dan tren Net Worth (Sprint 7) akan tersedia pada fase berikutnya.",
                )
            }
        }
    }

    if (showTransactionSheet) {
        TransactionFormSheet(
            wallets = cashflowUiState.wallets,
            currentSpendable = cashflowUiState.spendableBalance,
            initialWalletId = cashflowUiState.filter.walletId,
            initialType = initialTransactionType,
            editingTransaction = editingTransaction,
            onDismiss = {
                showTransactionSheet = false
                editingTransaction = null
                initialTransactionType = null
            },
            onSaveIncomeOrExpense = { walletId, type, amount, category, note ->
                showTransactionSheet = false
                initialTransactionType = null
                cashflowViewModel.recordIncomeOrExpense(
                    walletId = walletId,
                    type = type,
                    amount = amount,
                    category = category,
                    note = note,
                )
            },
            onSaveTransfer = { fromId, toId, amount, fee, note ->
                showTransactionSheet = false
                initialTransactionType = null
                cashflowViewModel.transferBetweenWallets(
                    fromWalletId = fromId,
                    toWalletId = toId,
                    amount = amount,
                    fee = fee,
                    note = note,
                )
            },
            onSaveEdit = { updated ->
                showTransactionSheet = false
                editingTransaction = null
                initialTransactionType = null
                cashflowViewModel.editTransaction(updated)
            },
        )
    }
}

@Composable
private fun UpcomingModulePlaceholder(
    title: String,
    description: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
