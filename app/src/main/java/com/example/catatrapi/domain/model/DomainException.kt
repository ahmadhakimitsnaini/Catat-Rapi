package com.example.catatrapi.domain.model

sealed class DomainException(override val message: String) : IllegalArgumentException(message) {
    class InsufficientBalanceException(
        message: String = "Saldo dompet tidak mencukupi untuk transaksi ini.",
    ) : DomainException(message)

    class WalletArchiveBlockedByGoalException(
        message: String = "Dompet tidak dapat diarsipkan karena sisa Total Saldo aktif akan lebih kecil dari total dana yang dialokasikan ke Target Tabungan.",
    ) : DomainException(message)

    class ImmutableInvestmentTransactionException(
        message: String = "Transaksi konversi aset investasi tidak dapat diedit atau dihapus langsung dari Cashflow. Silakan kelola melalui modul Portofolio.",
    ) : DomainException(message)

    class InvalidTransactionException(
        message: String,
    ) : DomainException(message)
}
