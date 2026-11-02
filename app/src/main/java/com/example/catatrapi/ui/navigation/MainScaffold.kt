package com.example.catatrapi.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.ui.cashflow.CashflowScreen
import com.example.catatrapi.ui.cashflow.CashflowUiState
import com.example.catatrapi.ui.cashflow.CashflowViewModel
import com.example.catatrapi.ui.cashflow.TransactionFormSheet
import com.example.catatrapi.ui.components.CurrencyFormatter
import com.example.catatrapi.ui.onboarding.OnboardingScreen

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
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: MainTab.CASHFLOW.route

    val cashflowUiState by cashflowViewModel.uiState.collectAsStateWithLifecycle()
    val pagedTransactions = cashflowViewModel.pagedTransactions.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }

    var showTransactionSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    val activeTab = MainTab.entries.firstOrNull { it.route == currentRoute } ?: MainTab.CASHFLOW

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
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
            startDestination = MainTab.CASHFLOW.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(MainTab.DASHBOARD.route) {
                DashboardPreviewTab(
                    uiState = cashflowUiState,
                    onGoToCashflow = {
                        navController.navigate(MainTab.CASHFLOW.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
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
                            cashflowViewModel.deleteTransaction(tx) // Memicu pesan ImmutableInvestmentTransactionException
                        } else {
                            editingTransaction = tx
                            showTransactionSheet = true
                        }
                    },
                    onDeleteTransaction = cashflowViewModel::deleteTransaction,
                    onConsumeMessage = cashflowViewModel::consumeUserMessage,
                )
            }

            composable(MainTab.GOALS_WEALTH.route) {
                UpcomingModulePlaceholder(
                    title = "Goals & Wealth",
                    description = "Segmen Tabungan Target (Sprint 3) dan Portofolio Investasi (Sprint 5–6) akan tersedia pada fase berikutnya.",
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
            editingTransaction = editingTransaction,
            onDismiss = {
                showTransactionSheet = false
                editingTransaction = null
            },
            onSaveIncomeOrExpense = { walletId, type, amount, category, note ->
                showTransactionSheet = false
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
                cashflowViewModel.editTransaction(updated)
            },
        )
    }
}

@Composable
private fun DashboardPreviewTab(
    uiState: CashflowUiState,
    onGoToCashflow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Beranda CatatRapi",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Total Saldo Dompet (W)",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = CurrencyFormatter.format(uiState.spendableBalance.totalWalletBalance),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Spendable Balance",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = CurrencyFormatter.format(uiState.spendableBalance.spendableAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.spendableBalance.isOverAllocated) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                        )
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Kelola Dompet & Riwayat Arus Kas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Anda memiliki ${uiState.wallets.size} dompet aktif. Gunakan tombol '+ Transaksi' di kanan bawah untuk mencatat pemasukan, pengeluaran, atau transfer dalam 3 ketuk.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onGoToCashflow) {
                    Text("Buka Tab Cashflow")
                }
            }
        }
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
