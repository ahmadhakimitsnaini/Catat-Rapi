package com.example.catatrapi.data.local.db

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    @Volatile
    private var INSTANCE: CatatRapiDatabase? = null

    fun getDatabase(context: Context): CatatRapiDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                CatatRapiDatabase::class.java,
                CatatRapiDatabase.NAME
            ).build().also {
                INSTANCE = it
            }
        }
    }
}