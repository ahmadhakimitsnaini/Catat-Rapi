package com.example.catatrapi.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

@Composable
fun GoalsScreen(
    uiState: GoalsUiState,
    snackbarHostState: SnackbarHostState,
    onCreateGoal: (name: String, targetAmount: BigDecimal, deadline: Long) -> Unit,
    onOpenGoal: (goalId: Long) -> Unit,
    onConsumeMessage: () -> Unit,
) {
    var showForm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onConsumeMessage()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val segments = listOf("Goals", "Portofolio (segera)")
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            segments.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = index == 0,
                    onClick = { /* Portofolio: Sprint 5-6 */ },
                    enabled = index == 0,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = segments.size),
                ) {
                    Text(label, maxLines = 1)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Target Tabungan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            FilledTonalButton(onClick = { showForm = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Buat Target")
            }
        }

        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            uiState.items.isEmpty() -> EmptyGoalsPlaceholder(onCreateClick = { showForm = true })

            else -> LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.items, key = { it.goal.id }) { item ->
                    GoalCard(
                        item = item,
                        onClick = { onOpenGoal(item.goal.id) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }

    if (showForm) {
        GoalFormSheet(
            onDismiss = { showForm = false },
            onSave = { name, amount, deadline ->
                showForm = false
                onCreateGoal(name, amount, deadline)
            },
        )
    }
}

@Composable
private fun EmptyGoalsPlaceholder(onCreateClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Belum ada target tabungan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Buat target pertama Anda, misalnya dana darurat atau laptop baru, lalu setor secara bertahap.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onCreateClick) { Text("Buat Target") }
        }
    }
}