package com.example.catatrapi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class FinanceColors(
    val income: Color,   // hijau: pemasukan / untung
    val expense: Color,  // merah: pengeluaran / rugi
    val warning: Color,  // kuning: peringatan
)

private val LightFinance = FinanceColors(income = Green40, expense = Red40, warning = Yellow40)
private val DarkFinance = FinanceColors(income = Green80, expense = Red80, warning = Yellow80)

val LocalFinanceColors = staticCompositionLocalOf { LightFinance }

private val LightScheme = lightColorScheme(
    primary = Green40,
    error = Red40,
)

private val DarkScheme = darkColorScheme(
    primary = Green80,
    error = Red80,
)

@Composable
fun CatatRapiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalFinanceColors provides if (darkTheme) DarkFinance else LightFinance
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

// Pemakaian di composable: MaterialTheme.finance.income
val MaterialTheme.finance: FinanceColors
    @Composable get() = LocalFinanceColors.current