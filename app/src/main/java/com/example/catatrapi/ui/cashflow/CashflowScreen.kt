package com.example.catatrapi.ui.cashflow

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.catatrapi.domain.model.MonthlyCashflowSummary
import com.example.catatrapi.domain.model.SpendableBalance
import com.example.catatrapi.domain.model.Transaction
import com.example.catatrapi.domain.model.TransactionCategory
import com.example.catatrapi.domain.model.TransactionFilter
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.domain.model.Wallet
import com.example.catatrapi.domain.model.WalletType
import com.example.catatrapi.ui.components.CurrencyFormatter
import com.example.catatrapi.ui.components.DateFormatterId
import com.example.catatrapi.ui.onboarding.toReadableLabel
import com.example.catatrapi.ui.theme.finance
import java.math.BigDecimal
import java.time.YearMonth
import java.time.ZoneId

@Composable
fun CashflowScreen(
    uiState: CashflowUiState,
    pagedItems: LazyPagingItems<CashflowListItem>,
    snackbarHostState: SnackbarHostState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectWalletFilter: (Long?) -> Unit,
    onSelectTypeFilter: (TransactionType?) -> Unit,
    onSelectCategoryFilter: (TransactionCategory?) -> Unit,
    onClearFilters: () -> Unit,
    onCreateWallet: (String, WalletType, BigDecimal) -> Unit,
    onUpdateWallet: (Long, String, WalletType) -> Unit,
    onArchiveWallet: (Long) -> Unit,
    onEditTransactionRequest: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onConsumeMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showWalletDialog by remember { mutableStateOf(false) }
    var editingWallet by remember { mutableStateOf<Wallet?>(null) }
    var archivingWallet by remember { mutableStateOf<Wallet?>(null) }
    var deletingTransaction by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onConsumeMessage()
        }
    }

    val walletMap = remember(uiState.wallets) {
        uiState.wallets.associateBy { it.id }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Kartu Total Saldo & Spendable Balance + Carousel Dompet (FT-01, FT-03)
        item {
            WalletCarouselHeader(
                wallets = uiState.wallets,
                spendableBalance = uiState.spendableBalance,
                selectedWalletId = uiState.filter.walletId,
                onSelectWallet = onSelectWalletFilter,
                onAddWalletClick = {
                    editingWallet = null
                    showWalletDialog = true
                },
                onEditWalletClick = { wallet ->
                    editingWallet = wallet
                    showWalletDialog = true
                },
                onArchiveWalletClick = { wallet ->
                    archivingWallet = wallet
                },
            )
        }

        // 2. Selektor Bulan & Ringkasan Periode Bulanan
        item {
            MonthlyCashflowSummarySection(
                selectedMonth = uiState.selectedMonth,
                summary = uiState.monthlySummary,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // 3. Pencarian & Filter (Dompet, Tipe, Kategori)
        item {
            TransactionSearchAndFilterSection(
                filter = uiState.filter,
                onSearchQueryChange = onSearchQueryChange,
                onSelectTypeFilter = onSelectTypeFilter,
                onSelectCategoryFilter = onSelectCategoryFilter,
                onClearFilters = onClearFilters,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // 4. Daftar Transaksi Paging 3 Dikelompokkan per Tanggal (FT-02, FT-06, NFR-P5)
        if (pagedItems.itemCount == 0) {
            item {
                EmptyTransactionPlaceholder(
                    hasActiveFilter = uiState.filter != TransactionFilter(),
                    onClearFilters = onClearFilters,
                )
            }
        } else {
            items(
                count = pagedItems.itemCount,
                key = { index ->
                    when (val item = pagedItems.peek(index)) {
                        is CashflowListItem.DateHeader -> "header_${item.epochDay}"
                        is CashflowListItem.TransactionRow -> "tx_${item.transaction.id}"
                        null -> "placeholder_$index"
                    }
                }
            ) { index ->
                when (val item = pagedItems[index]) {
                    is CashflowListItem.DateHeader -> {
                        DateGroupHeader(
                            dateText = item.formattedDate,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    is CashflowListItem.TransactionRow -> {
                        SwipeableTransactionItem(
                            transaction = item.transaction,
                            walletName = walletMap[item.transaction.walletId]?.name ?: "Dompet #${item.transaction.walletId}",
                            toWalletName = item.transaction.toWalletId?.let { id ->
                                walletMap[id]?.name ?: "Dompet #$id"
                            },
                            onEdit = { onEditTransactionRequest(item.transaction) },
                            onDelete = {
                                if (item.transaction.isInvestmentTransaction) {
                                    onDeleteTransaction(item.transaction)
                                } else {
                                    deletingTransaction = item.transaction
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    null -> Unit
                }
            }
        }
    }

    if (showWalletDialog) {
        WalletFormDialog(
            editingWallet = editingWallet,
            onDismiss = { showWalletDialog = false },
            onConfirmCreate = { name, type, balance ->
                showWalletDialog = false
                onCreateWallet(name, type, balance)
            },
            onConfirmUpdate = { id, name, type ->
                showWalletDialog = false
                onUpdateWallet(id, name, type)
            },
        )
    }

    archivingWallet?.let { wallet ->
        AlertDialog(
            onDismissRequest = { archivingWallet = null },
            title = { Text("Arsipkan Dompet \"${wallet.name}\"?") },
            text = {
                Text(
                    "Dompet yang diarsipkan tidak akan dihitung ke dalam Total Saldo (W) dan jadwal transaksi berulang terkait akan dijeda, namun seluruh riwayat transaksinya tetap tersimpan."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetId = wallet.id
                        archivingWallet = null
                        onArchiveWallet(targetId)
                    }
                ) {
                    Text("Arsipkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { archivingWallet = null }) {
                    Text("Batal")
                }
            },
        )
    }

    deletingTransaction?.let { tx ->
        AlertDialog(
            onDismissRequest = { deletingTransaction = null },
            title = { Text("Hapus Transaksi?") },
            text = {
                Text(
                    "Transaksi sebesar ${CurrencyFormatter.format(tx.amount)} akan dihapus dan saldo dompet terkait akan dihitung ulang secara atomik."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = tx
                        deletingTransaction = null
                        onDeleteTransaction(target)
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTransaction = null }) {
                    Text("Batal")
                }
            },
        )
    }
}

@Composable
private fun WalletCarouselHeader(
    wallets: List<Wallet>,
    spendableBalance: SpendableBalance,
    selectedWalletId: Long?,
    onSelectWallet: (Long?) -> Unit,
    onAddWalletClick: () -> Unit,
    onEditWalletClick: (Wallet) -> Unit,
    onArchiveWalletClick: (Wallet) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Kartu Total Saldo & Spendable Balance berdampingan (FT-03)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total Saldo Aktif (W)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                        Text(
                            text = CurrencyFormatter.format(spendableBalance.totalWalletBalance),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = "Spendable (Bebas Pakai)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                        Text(
                            text = CurrencyFormatter.format(spendableBalance.spendableAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (spendableBalance.isOverAllocated) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                        )
                    }
                }

                if (spendableBalance.isOverAllocated) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Peringatan: Alokasi melebihi saldo bebas pakai!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        // Carousel Dompet
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Dompet Aktif (${wallets.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (selectedWalletId != null) {
                TextButton(onClick = { onSelectWallet(null) }) {
                    Text("Tampilkan Semua Dompet")
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(wallets, key = { it.id }) { wallet ->
                val isSelected = selectedWalletId == wallet.id
                var menuExpanded by remember { mutableStateOf(false) }

                OutlinedCard(
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    modifier = Modifier
                        .width(200.dp)
                        .clickable { onSelectWallet(wallet.id) },
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = when (wallet.type) {
                                        WalletType.CASH -> Icons.Default.AccountBalanceWallet
                                        WalletType.BANK -> Icons.Default.AccountBalance
                                        WalletType.EWALLET -> Icons.Default.Smartphone
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = wallet.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Opsi Dompet",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Dompet") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onEditWalletClick(wallet)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Arsipkan Dompet") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Archive, contentDescription = null)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onArchiveWalletClick(wallet)
                                        },
                                    )
                                }
                            }
                        }

                        Text(
                            text = wallet.type.toReadableLabel(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = CurrencyFormatter.format(wallet.balance),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Tombol Tambah Dompet di ujung Carousel
            item {
                OutlinedCard(
                    modifier = Modifier
                        .width(140.dp)
                        .height(98.dp)
                        .clickable { onAddWalletClick() },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Dompet",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tambah Dompet",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyCashflowSummarySection(
    selectedMonth: YearMonth,
    summary: MonthlyCashflowSummary,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthEpochMillis = remember(selectedMonth) {
        selectedMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Bulan Sebelumnya",
                    )
                }
                Text(
                    text = DateFormatterId.month(monthEpochMillis),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onNextMonth) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Bulan Berikutnya",
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pemasukan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = CurrencyFormatter.format(summary.totalIncome),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.finance.income,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Pengeluaran",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = CurrencyFormatter.format(summary.totalExpense),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.finance.expense,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = "Selisih Bersih",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = CurrencyFormatter.format(summary.netCashflow),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.netCashflow >= BigDecimal.ZERO) {
                            MaterialTheme.finance.income
                        } else {
                            MaterialTheme.finance.expense
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionSearchAndFilterSection(
    filter: TransactionFilter,
    onSearchQueryChange: (String) -> Unit,
    onSelectTypeFilter: (TransactionType?) -> Unit,
    onSelectCategoryFilter: (TransactionCategory?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = filter.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Cari catatan atau kategori transaksi...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (filter.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Bersihkan pencarian")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // Filter Tipe & Kategori
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val filterableTypes = listOf(
                TransactionType.EXPENSE to "Pengeluaran",
                TransactionType.INCOME to "Pemasukan",
                TransactionType.TRANSFER to "Transfer",
                TransactionType.INVESTMENT_BUY to "Beli Aset",
                TransactionType.INVESTMENT_SELL to "Jual Aset",
            )
            items(filterableTypes) { (type, label) ->
                FilterChip(
                    selected = filter.type == type,
                    onClick = { onSelectTypeFilter(type) },
                    label = { Text(label) },
                )
            }

            val categories = if (filter.type == TransactionType.INCOME || filter.type == TransactionType.EXPENSE) {
                TransactionCategory.forType(filter.type)
            } else {
                TransactionCategory.entries
            }

            items(categories) { category ->
                FilterChip(
                    selected = filter.category == category,
                    onClick = { onSelectCategoryFilter(category) },
                    label = { Text(category.labelId) },
                )
            }
        }
    }
}

@Composable
private fun DateGroupHeader(
    dateText: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = dateText,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTransactionItem(
    transaction: Transaction,
    walletName: String,
    toWalletName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val alignment = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }
            val bgColor = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondaryContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment,
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Transaksi",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                } else if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Transaksi",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        },
    ) {
        TransactionRowCard(
            transaction = transaction,
            walletName = walletName,
            toWalletName = toWalletName,
            onClick = onEdit,
        )
    }
}

@Composable
private fun TransactionRowCard(
    transaction: Transaction,
    walletName: String,
    toWalletName: String?,
    onClick: () -> Unit,
) {
    val (amountPrefix, amountColor) = when (transaction.type) {
        TransactionType.INCOME, TransactionType.INVESTMENT_SELL -> "+" to MaterialTheme.finance.income
        TransactionType.EXPENSE, TransactionType.INVESTMENT_BUY -> "-" to MaterialTheme.finance.expense
        TransactionType.TRANSFER -> "" to MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when (transaction.type) {
                        TransactionType.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
                        TransactionType.EXPENSE -> Icons.AutoMirrored.Filled.TrendingDown
                        TransactionType.TRANSFER -> Icons.AutoMirrored.Filled.CompareArrows
                        TransactionType.INVESTMENT_BUY, TransactionType.INVESTMENT_SELL -> Icons.AutoMirrored.Filled.ShowChart
                    },
                    contentDescription = null,
                    tint = amountColor,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = when (transaction.type) {
                            TransactionType.TRANSFER -> "Transfer Antar Dompet"
                            TransactionType.INVESTMENT_BUY -> "Beli Aset Investasi"
                            TransactionType.INVESTMENT_SELL -> "Jual Aset Investasi"
                            else -> transaction.category?.labelId ?: "Transaksi"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )

                    if (transaction.isInvestmentTransaction) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = "Konversi Aset",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                val walletDescription = if (transaction.type == TransactionType.TRANSFER && toWalletName != null) {
                    "$walletName → $toWalletName"
                } else {
                    walletName
                }

                Text(
                    text = buildString {
                        append(walletDescription)
                        transaction.note?.let {
                            append(" • ")
                            append(it)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${CurrencyFormatter.format(transaction.amount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                )
                if (transaction.type == TransactionType.TRANSFER && transaction.fee > BigDecimal.ZERO) {
                    Text(
                        text = "+ Admin ${CurrencyFormatter.format(transaction.fee)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.finance.expense,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionPlaceholder(
    hasActiveFilter: Boolean,
    onClearFilters: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (hasActiveFilter) {
                "Tidak ada transaksi yang cocok dengan filter."
            } else {
                "Belum ada transaksi pada bulan ini."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hasActiveFilter) {
            TextButton(onClick = onClearFilters) {
                Text("Reset Filter")
            }
        }
    }
}
