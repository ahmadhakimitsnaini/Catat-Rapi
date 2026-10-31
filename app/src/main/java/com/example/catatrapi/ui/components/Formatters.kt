package com.example.catatrapi.ui.components

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ID_LOCALE: Locale = Locale.forLanguageTag("id-ID")

object CurrencyFormatter {
    /** BigDecimal(1250000) -> "Rp1.250.000", negatif -> "-Rp1.250.000" */
    fun format(amount: BigDecimal): String {
        val symbols = DecimalFormatSymbols(ID_LOCALE).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        // DecimalFormat tidak thread-safe, jadi dibuat per pemanggilan
        val df = DecimalFormat("#,##0", symbols).apply { roundingMode = RoundingMode.HALF_UP }
        val sign = if (amount.signum() < 0) "-" else ""
        return sign + "Rp" + df.format(amount.abs())
    }
}

object DateFormatterId {
    private val fullFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", ID_LOCALE)
    private val shortFmt = DateTimeFormatter.ofPattern("d MMM yyyy", ID_LOCALE)
    private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", ID_LOCALE)

    private fun Long.toZoned() = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())

    fun full(epochMillis: Long): String = fullFmt.format(epochMillis.toZoned())   // 29 Oktober 2026
    fun short(epochMillis: Long): String = shortFmt.format(epochMillis.toZoned()) // 29 Okt 2026
    fun month(epochMillis: Long): String = monthFmt.format(epochMillis.toZoned()) // Oktober 2026
}