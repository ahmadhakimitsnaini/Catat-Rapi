package com.example.catatrapi.data.local

import androidx.room.TypeConverter
import com.example.catatrapi.domain.model.WalletType
import java.math.BigDecimal

class WalletConverters {

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) }
    }

    @TypeConverter
    fun fromWalletType(value: WalletType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toWalletType(value: String?): WalletType? {
        return value?.let { WalletType.valueOf(it) }
    }
}