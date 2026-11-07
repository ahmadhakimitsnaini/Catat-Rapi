package com.example.catatrapi.ui.goals

import com.example.catatrapi.domain.model.ContributionType
import com.example.catatrapi.domain.model.GoalStatus
import com.example.catatrapi.ui.components.DateFormatterId
import java.math.BigDecimal
import java.math.RoundingMode

internal const val MILLIS_PER_DAY_UI = 86_400_000L
internal const val MAX_DISPLAY_ETA_DAYS = 36_500L // 100 tahun, mencegah overflow saat menghitung tanggal

internal fun statusLabel(status: GoalStatus): String = when (status) {
    GoalStatus.ON_TRACK -> "Sesuai Rencana"
    GoalStatus.DELAYED -> "Tertunda"
    GoalStatus.ACHIEVED -> "Tercapai"
}

internal fun remainingDaysLabel(days: Long): String = when {
    days > 0 -> "$days hari lagi"
    days == 0L -> "Tenggat hari ini"
    else -> "Terlambat ${-days} hari"
}

internal fun etaLabel(etaDays: Long?, nowMillis: Long): String = when {
    etaDays == null -> "Tidak terdefinisi"
    etaDays == 0L -> "Sudah tercapai"
    etaDays > MAX_DISPLAY_ETA_DAYS -> "Lebih dari 100 tahun"
    else -> "$etaDays hari (sekitar ${DateFormatterId.short(nowMillis + etaDays * MILLIS_PER_DAY_UI)})"
}

internal fun contributionTypeLabel(type: ContributionType): String = when (type) {
    ContributionType.DEPOSIT -> "Setoran"
    ContributionType.WITHDRAW -> "Penarikan"
    ContributionType.AUTO_SWEEP -> "Auto-Sweep"
}

/** Pembulatan ke rupiah utuh untuk tampilan. Pembulatan ke atas agar saran menabung tidak kurang. */
internal fun BigDecimal.wholeRupiahCeiling(): BigDecimal = setScale(0, RoundingMode.CEILING)

internal fun BigDecimal.wholeRupiahHalfUp(): BigDecimal = setScale(0, RoundingMode.HALF_UP)