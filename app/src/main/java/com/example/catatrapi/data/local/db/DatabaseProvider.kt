package com.example.catatrapi.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.catatrapi.data.local.security.DatabasePassphraseManager
import com.example.catatrapi.data.local.security.KeystoreCryptoManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

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

    /** Sprint 4 (FT-05): Menambah tabel category_budgets dengan indeks unik (category, year_month). */
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `category_budgets` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "`year_month` TEXT NOT NULL, " +
                        "`limit_amount` TEXT NOT NULL, " +
                        "`notified_80` INTEGER NOT NULL, " +
                        "`notified_100` INTEGER NOT NULL, " +
                        "`updated_at` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_category_budgets_category_year_month` " +
                        "ON `category_budgets` (`category`, `year_month`)"
            )
        }
    }

    fun getDatabase(context: Context): CatatRapiDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildEncryptedDatabase(context.applicationContext).also { INSTANCE = it }
        }
    }

    private fun buildEncryptedDatabase(appContext: Context): CatatRapiDatabase {
        System.loadLibrary("sqlcipher")
        val passphraseManager = DatabasePassphraseManager(KeystoreCryptoManager())
        val passphrase = passphraseManager.getOrCreatePassphrase(appContext)
        DatabaseEncryptionMigrator.migrateIfPlaintext(
            context = appContext,
            dbName = CatatRapiDatabase.NAME,
            passphrase = passphrase,
        )
        // SupportOpenHelperFactory menyalin/memegang referensi byte passphrase untuk membuka koneksi SQLCipher
        val factory = SupportOpenHelperFactory(passphrase)
        return Room.databaseBuilder(
            appContext,
            CatatRapiDatabase::class.java,
            CatatRapiDatabase.NAME,
        )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }
}
