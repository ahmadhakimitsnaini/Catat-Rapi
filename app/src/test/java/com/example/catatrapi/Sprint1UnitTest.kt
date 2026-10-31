package com.example.catatrapi

import com.example.catatrapi.data.local.converter.BigDecimalTypeConverter
import com.example.catatrapi.ui.components.CurrencyFormatter
import com.example.catatrapi.ui.components.DateFormatterId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Sprint1UnitTest {

    private val converter = BigDecimalTypeConverter()

    @Test
    fun `BigDecimalTypeConverter converts without floating point precision loss`() {
        val original = BigDecimal("123456789.12345678")
        val text = converter.fromBigDecimal(original)
        assertEquals("123456789.12345678", text)
        assertEquals(original, converter.toBigDecimal(text))
        assertNull(converter.fromBigDecimal(null))
        assertNull(converter.toBigDecimal(null))
    }

    @Test
    fun `CurrencyFormatter formats positive zero and negative Rupiah values`() {
        assertEquals("Rp1.250.000", CurrencyFormatter.format(BigDecimal("1250000")))
        assertEquals("Rp0", CurrencyFormatter.format(BigDecimal.ZERO))
        assertEquals("-Rp50.000", CurrencyFormatter.format(BigDecimal("-50000")))
    }

    @Test
    fun `DateFormatterId formats Indonesian dates properly`() {
        val epochMillis = LocalDate.of(2026, 10, 29)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        assertEquals("29 Oktober 2026", DateFormatterId.full(epochMillis))
        assertEquals("Oktober 2026", DateFormatterId.month(epochMillis))
    }
}
