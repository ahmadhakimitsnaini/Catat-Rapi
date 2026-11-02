package com.example.catatrapi.domain.model

enum class TransactionCategory(
    val labelId: String,
    val applicableType: TransactionType,
) {
    // Kategori Pemasukan (INCOME)
    SALARY("Gaji", TransactionType.INCOME),
    BONUS("Bonus & THR", TransactionType.INCOME),
    FREELANCE("Hasil Sampingan", TransactionType.INCOME),
    INVESTMENT_RETURN("Hasil Investasi", TransactionType.INCOME),
    OTHER_INCOME("Pemasukan Lainnya", TransactionType.INCOME),

    // Kategori Pengeluaran (EXPENSE)
    FOOD_BEVERAGE("Makan & Minum", TransactionType.EXPENSE),
    TRANSPORTATION("Transportasi", TransactionType.EXPENSE),
    BILLS_UTILITIES("Tagihan & Utilitas", TransactionType.EXPENSE),
    SHOPPING("Belanja", TransactionType.EXPENSE),
    ENTERTAINMENT("Hiburan", TransactionType.EXPENSE),
    HEALTH("Kesehatan", TransactionType.EXPENSE),
    EDUCATION("Pendidikan", TransactionType.EXPENSE),
    SUBSCRIPTION("Langganan", TransactionType.EXPENSE),
    ADMIN_FEE("Biaya Admin", TransactionType.EXPENSE),
    OTHER_EXPENSE("Pengeluaran Lainnya", TransactionType.EXPENSE);

    companion object {
        fun forType(type: TransactionType): List<TransactionCategory> =
            entries.filter { it.applicableType == type }

        fun fromKeyOrNull(key: String?): TransactionCategory? =
            key?.let { k -> entries.firstOrNull { it.name == k } }
    }
}
