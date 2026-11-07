package com.example.catatrapi.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.catatrapi.ui.components.CurrencyFormatter
import com.example.catatrapi.ui.theme.finance
import java.math.BigDecimal
import kotlin.math.roundToLong

private const val SLIDER_MAX = 2_000_000f
private const val SLIDER_STEP = 50_000f

@Composable
fun ImpactSimulatorCard(
    targetAmount: BigDecimal,
    savedAmount: BigDecimal,
    velocityPerDay: BigDecimal,
    daysUntilDeadline: Long,
    nowMillis: Long,
    modifier: Modifier = Modifier,
) {
    var extra by remember { mutableFloatStateOf(0f) }
    val extraAmount = BigDecimal.valueOf(extra.roundToLong())
    val result = GoalImpactSimulation.simulate(
        targetAmount = targetAmount,
        savedAmount = savedAmount,
        velocityPerDay = velocityPerDay,
        extraMonthlySpending = extraAmount,
        daysUntilDeadline = daysUntilDeadline,
    )

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Simulasi Dampak",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Jika belanja bertambah ${CurrencyFormatter.format(extraAmount)} per bulan",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = extra,
                onValueChange = { extra = it },
                valueRange = 0f..SLIDER_MAX,
                steps = (SLIDER_MAX / SLIDER_STEP).toInt() - 1,
            )

            if (result.isAchieved) {
                Text(
                    text = "Target sudah tercapai, simulasi tidak diperlukan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SimulatorRow("ETA saat ini", etaLabel(result.baselineEtaDays, nowMillis))
                SimulatorRow("ETA setelah simulasi", etaLabel(result.simulatedEtaDays, nowMillis))

                val finance = MaterialTheme.finance
                val (message, color) = when {
                    result.simulatedEtaDays == null ->
                        "ETA tidak terdefinisi: kecepatan menabung bersih menjadi nol atau negatif." to MaterialTheme.colorScheme.error
                    result.etaDelayDays == null -> null to Color.Unspecified
                    result.etaDelayDays == 0L ->
                        "ETA tidak berubah." to MaterialTheme.colorScheme.onSurfaceVariant
                    result.etaDelayDays > MAX_DISPLAY_ETA_DAYS ->
                        "ETA mundur lebih dari 100 tahun." to finance.warning
                    else -> "ETA mundur ${result.etaDelayDays} hari." to finance.warning
                }
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                    )
                }
                if (result.baselineMeetsDeadline && !result.simulatedMeetsDeadline) {
                    Text(
                        text = "Dengan belanja ini, target diperkirakan melewati tenggat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Text(
                text = "Asumsi: belanja tambahan per bulan mengurangi kecepatan menabung sebesar X/30 per hari. " +
                        "Perkiraan, bukan jaminan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SimulatorRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}