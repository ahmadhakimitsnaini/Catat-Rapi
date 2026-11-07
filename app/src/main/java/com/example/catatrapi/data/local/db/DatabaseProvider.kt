package com.example.catatrapi.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    @Volatile private var INSTANCE: CatatRapiDatabase? = null

    /** Menambah tabel saving_goals dan goal_contributions. Data versi 1 tidak disentuh. */
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `saving_goals` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`target_amount` TEXT NOT NULL, " +
                        "`deadline` INTEGER NOT NULL, " +
                        "`priority` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`is_archived` INTEGER NOT NULL, " +
                        "`created_at` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `goal_contributions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`goal_id` INTEGER NOT NULL, " +
                        "`wallet_id` INTEGER, " +
                        "`amount` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`created_at` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`goal_id`) REFERENCES `saving_goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`wallet_id`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_contributions_goal_id` ON `goal_contributions` (`goal_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_contributions_wallet_id` ON `goal_contributions` (`wallet_id`)")
        }
    }

    fun getDatabase(context: Context): CatatRapiDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                CatatRapiDatabase::class.java,
                CatatRapiDatabase.NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { INSTANCE = it }
        }
    }
}