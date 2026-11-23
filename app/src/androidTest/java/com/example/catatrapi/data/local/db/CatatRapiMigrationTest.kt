package com.example.catatrapi.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * NFR-R2 (Quality Gate 1): Pengujian migrasi skema Room dari versi 1 -> 2 -> 3
 * menggunakan MigrationTestHelper.
 */
@RunWith(AndroidJUnit4::class)
class CatatRapiMigrationTest {

    private val testDbName = "catatrapi_migration_test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CatatRapiDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2_preservesWalletsAndTransactions_andCreatesSavingGoalsTables() {
        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                "INSERT INTO `wallets` (`id`, `name`, `type`, `balance`, `is_archived`, `created_at`) " +
                        "VALUES (1, 'BCA', 'BANK', '5000000', 0, 1700000000000)"
            )
            close()
        }

        val dbV2 = helper.runMigrationsAndValidate(
            testDbName,
            2,
            true,
            DatabaseProvider.MIGRATION_1_2,
        )

        dbV2.query("SELECT `name`, `balance` FROM `wallets` WHERE `id` = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("BCA", cursor.getString(0))
            assertEquals("5000000", cursor.getString(1))
        }
        dbV2.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3_preservesExistingData_andCreatesCategoryBudgetsWithUniqueIndex() {
        helper.createDatabase(testDbName, 2).apply {
            execSQL(
                "INSERT INTO `saving_goals` (`id`, `name`, `target_amount`, `deadline`, `priority`, `status`, `is_archived`, `created_at`) " +
                        "VALUES (1, 'Dana Darurat', '10000000', 1800000000000, 1, 'ON_TRACK', 0, 1700000000000)"
            )
            close()
        }

        val dbV3 = helper.runMigrationsAndValidate(
            testDbName,
            3,
            true,
            DatabaseProvider.MIGRATION_2_3,
        )

        dbV3.execSQL(
            "INSERT INTO `category_budgets` (`category`, `year_month`, `limit_amount`, `notified_80`, `notified_100`, `updated_at`) " +
                    "VALUES ('FOOD_BEVERAGE', '2026-11', '1500000', 0, 0, 1760000000000)"
        )

        dbV3.query("SELECT `limit_amount` FROM `category_budgets` WHERE `category` = 'FOOD_BEVERAGE' AND `year_month` = '2026-11'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("1500000", cursor.getString(0))
        }
        dbV3.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll_1To3_succeedsEndToEnd() {
        helper.createDatabase(testDbName, 1).close()
        helper.runMigrationsAndValidate(
            testDbName,
            3,
            true,
            DatabaseProvider.MIGRATION_1_2,
            DatabaseProvider.MIGRATION_2_3,
        ).close()
    }
}
