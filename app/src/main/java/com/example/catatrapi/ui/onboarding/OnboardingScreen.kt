package com.example.catatrapi.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.catatrapi.domain.model.WalletType
import com.example.catatrapi.ui.components.CurrencyFormatter
import java.math.BigDecimal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingCompleted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) {
            onOnboardingCompleted()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            when (state.step) {
                OnboardingStep.CREATE_FIRST_WALLET -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Selamat Datang di CatatRapi",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Langkah 1 dari 2 • Buat dompet pertama Anda agar setiap rupiah tercatat sebagai bebas pakai, dialokasikan, atau diinvestasikan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = state.walletName,
                            onValueChange = viewModel::onWalletNameChange,
                            label = { Text("Nama Dompet") },
                            placeholder = { Text("Contoh: BCA, Dompet Tunai, GoPay") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text(
                            text = "Tipe Dompet",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            WalletType.entries.forEach { type ->
                                FilterChip(
                                    selected = state.walletType == type,
                                    onClick = { viewModel.onWalletTypeChange(type) },
                                    label = { Text(type.toReadableLabel()) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (type) {
                                                WalletType.CASH -> Icons.Default.AccountBalanceWallet
                                                WalletType.BANK -> Icons.Default.AccountBalance
                                                WalletType.EWALLET -> Icons.Default.Smartphone
                                            },
                                            contentDescription = null,
                                        )
                                    },
                                )
                            }
                        }

                        val parsedBalance = state.initialBalanceInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        OutlinedTextField(
                            value = state.initialBalanceInput,
                            onValueChange = viewModel::onInitialBalanceChange,
                            label = { Text("Saldo Awal (Rp)") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            supportingText = {
                                Text("Pratinjau: ${CurrencyFormatter.format(parsedBalance)}")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        state.errorMessage?.let { msg ->
                            Text(
                                text = msg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    Button(
                        onClick = viewModel::submitFirstWallet,
                        enabled = !state.isSubmitting && state.walletName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    ) {
                        Text("Simpan Dompet & Lanjut")
                    }
                }

                OnboardingStep.OPTIONAL_FIRST_GOAL -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Target Tabungan Pertama (Opsional)",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Text(
                            text = "Langkah 2 dari 2 • Di CatatRapi, dana yang disisihkan ke Target Tabungan akan otomatis mengurangi Spendable Balance (Saldo Bebas Pakai) tanpa mengurangi Total Saldo rekening Anda.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedTextField(
                                    value = state.goalNameInput,
                                    onValueChange = viewModel::onGoalNameChange,
                                    label = { Text("Nama Target (Opsional)") },
                                    placeholder = { Text("Contoh: Dana Darurat, DP Motor") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                val parsedGoal = state.goalTargetInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                OutlinedTextField(
                                    value = state.goalTargetInput,
                                    onValueChange = viewModel::onGoalTargetChange,
                                    label = { Text("Nominal Target (Rp)") },
                                    placeholder = { Text("Contoh: 15000000") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    supportingText = {
                                        if (parsedGoal > BigDecimal.ZERO) {
                                            Text("Target: ${CurrencyFormatter.format(parsedGoal)} (Dapat dikelola di tab Goals & Wealth)")
                                        } else {
                                            Text("Anda juga dapat membuat target kapan saja di tab Goals & Wealth.")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    ) {
                        Button(
                            onClick = viewModel::finishOnboarding,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Masuk ke Dashboard")
                        }
                        OutlinedButton(
                            onClick = viewModel::finishOnboarding,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Lewati untuk Sekarang")
                        }
                    }
                }
            }
        }
    }
}

fun WalletType.toReadableLabel(): String = when (this) {
    WalletType.CASH -> "Kas Tunai"
    WalletType.BANK -> "Rekening Bank"
    WalletType.EWALLET -> "E-Wallet"
}
