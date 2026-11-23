package com.example.catatrapi

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.catatrapi.data.local.db.CatatRapiDatabase
import com.example.catatrapi.data.local.entity.TransactionEntity
import com.example.catatrapi.data.local.entity.WalletEntity
import com.example.catatrapi.domain.model.TransactionType
import com.example.catatrapi.domain.model.WalletType
import java.math.BigDecimal
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Quality Gate 1: Verifikasi NFR-P2 (muat layar <= 500 ms untuk 10.000 transaksi)
 * dan NFR-P3 (penyimpanan transaksi atomik < 200 ms) pada Room Database.
 */
@RunWith(AndroidJUnit4::class)
class PerformanceBenchmarkTest {

    private lateinit var database: CatatRapiDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CatatRapiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun nfrP2_and_nfrP3_atomicInsertAndLoadWith10000Transactions() = runBlocking {
        val walletDao = database.walletDao()
        val txDao = database.transactionDao()
        val now = 1_760_000_000_000L

        val walletId = walletDao.insert(
            WalletEntity(
                name = "BCA Utama",
                type = WalletType.BANK,
                balance = BigDecimal("500000000"),
                createdAt = now,
            )
        )

        // NFR-P3: Uji penyimpanan 1 transaksi atomik lintas tabel (< 200 ms)
        val singleAtomicWriteMs = measureTimeMillis {
            database.withTransaction {
                val w = walletDao.getById(walletId)!!
                txDao.insert(
                    TransactionEntity(
                        walletId = walletId,
                        type = TransactionType.EXPENSE,
                        amount = BigDecimal("25000"),
                        category = "FOOD_BEVERAGE",
                        occurredAt = now,
                        createdAt = now,
                    )
                )
                walletDao.updateBalance(walletId, w.balance - BigDecimal("25000"))
            }
        }
        assertTrue(
            "NFR-P3 gagal: penyimpanan atomik memakan ${singleAtomicWriteMs} ms (target < 200 ms)",
            singleAtomicWriteMs < 200L,
        )

        // Seed hingga 10.000 transaksi dalam satu batch transaksi
        database.withTransaction {
            for (i in 2..10_000) {
                txDao.insert(
                    TransactionEntity(
                        walletId = walletId,
                        type = TransactionType.EXPENSE,
                        amount = BigDecimal("10000"),
                        category = "FOOD_BEVERAGE",
                        occurredAt = now - i * 1_000L,
                        createdAt = now - i * 1_000L,
                    )
                )
            }
        }

        // NFR-P2: Uji muat data Dashboard (5 transaksi terbaru + dompet aktif) dari 10.000 baris <= 500 ms
        val dashboardLoadMs = measureTimeMillis {
            val activeWallets = walletDao.observeActive().first()
            val recent5 = txDao.observeRecent(5).first()
            assertEquals(1, activeWallets.size)
            assertEquals(5, recent5.size)
        }
        assertTrue(
            "NFR-P2 gagal: muat layar dari 10.000 transaksi memakan ${dashboardLoadMs} ms (target <= 500 ms)",
            dashboardLoadMs <= 500L,
        )
    }
}
