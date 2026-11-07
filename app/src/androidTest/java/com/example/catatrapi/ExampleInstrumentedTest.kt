package com.example.catatrapi.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CatatRapiDatabase::class.java,
    )

    @Test
    fun migrate1To2_keepsWalletData_andCreatesGoalTables() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO wallets (name, type, balance, is_archived, created_at) " +
                        "VALUES ('Dompet Uji', 'CASH', '150000', 0, 1700000000000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseProvider.MIGRATION_1_2)

        db.query("SELECT name, balance FROM wallets").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals("Dompet Uji", c.getString(0))
            assertEquals("150000", c.getString(1))
        }
        db.query("SELECT COUNT(*) FROM saving_goals").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM goal_contributions").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}