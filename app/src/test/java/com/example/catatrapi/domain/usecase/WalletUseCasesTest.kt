package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.DomainException
import com.example.catatrapi.domain.model.Wallet
import com.example.catatrapi.domain.model.WalletType
import com.example.catatrapi.domain.repository.WalletRepository
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletUseCasesTest {

    @Test
    fun `create wallet validates non-blank name and non-negative balance`() = runTest {
        val repo = InMemoryWalletRepository()
        val createWallet = CreateWalletUseCase(repo)
        val getTotal = GetTotalWalletBalanceUseCase(repo)

        val id1 = createWallet("BCA", WalletType.BANK, BigDecimal("2500000"))
        val id2 = createWallet("GoPay", WalletType.EWALLET, BigDecimal("500000"))

        assertEquals(1L, id1)
        assertEquals(2L, id2)
        assertEquals(BigDecimal("3000000"), getTotal().first())
    }

    @Test(expected = DomainException.InvalidTransactionException::class)
    fun `create wallet rejects blank name`() = runTest {
        val repo = InMemoryWalletRepository()
        val createWallet = CreateWalletUseCase(repo)
        createWallet("   ", WalletType.CASH, BigDecimal("100000"))
    }

    @Test(expected = DomainException.WalletArchiveBlockedByGoalException::class)
    fun `archive wallet blocked when remaining active balance is less than allocated goals`() = runTest {
        val repo = InMemoryWalletRepository(allocatedGoals = BigDecimal("2000000"))
        val createWallet = CreateWalletUseCase(repo)
        val archiveWallet = ArchiveWalletUseCase(repo)

        val w1 = createWallet("Dompet A", WalletType.BANK, BigDecimal("1500000"))
        createWallet("Dompet B", WalletType.CASH, BigDecimal("1000000"))

        // Total aktif W = 2.500.000, G = 2.000.000.
        // Mengarsipkan Dompet A (1.500.000) menyisakan 1.000.000 < 2.000.000 -> wajib ditolak!
        archiveWallet(w1)
    }

    @Test
    fun `archive wallet succeeds when remaining active balance covers allocated goals and pauses recurring`() = runTest {
        val repo = InMemoryWalletRepository(allocatedGoals = BigDecimal("1000000"))
        val createWallet = CreateWalletUseCase(repo)
        val archiveWallet = ArchiveWalletUseCase(repo)
        val getTotal = GetTotalWalletBalanceUseCase(repo)

        val w1 = createWallet("Dompet A", WalletType.BANK, BigDecimal("1500000"))
        val w2 = createWallet("Dompet B", WalletType.CASH, BigDecimal("800000"))

        // Mengarsipkan Dompet B (800.000) menyisakan 1.500.000 >= 1.000.000 -> sukses
        archiveWallet(w2)

        assertEquals(BigDecimal("1500000"), getTotal().first())
        assertTrue(repo.pausedRecurringWalletIds.contains(w2))
        assertEquals(listOf(w1), repo.getActiveWallets().map { it.id })
    }

    private class InMemoryWalletRepository(
        private val allocatedGoals: BigDecimal = BigDecimal.ZERO,
    ) : WalletRepository {
        private val walletsFlow = MutableStateFlow<List<Wallet>>(emptyList())
        val pausedRecurringWalletIds = mutableListOf<Long>()

        override fun observeActiveWallets(): Flow<List<Wallet>> =
            walletsFlow.map { list -> list.filter { !it.isArchived } }

        override fun observeWalletById(id: Long): Flow<Wallet?> =
            walletsFlow.map { list -> list.firstOrNull { it.id == id } }

        override fun observeHasAnyWallet(): Flow<Boolean> =
            walletsFlow.map { it.isNotEmpty() }

        override suspend fun getActiveWallets(): List<Wallet> =
            walletsFlow.value.filter { !it.isArchived }

        override suspend fun getWalletById(id: Long): Wallet? =
            walletsFlow.value.firstOrNull { it.id == id }

        override suspend fun createWallet(wallet: Wallet): Long {
            val nextId = (walletsFlow.value.maxOfOrNull { it.id } ?: 0L) + 1L
            walletsFlow.value = walletsFlow.value + wallet.copy(id = nextId)
            return nextId
        }

        override suspend fun updateWallet(wallet: Wallet) {
            walletsFlow.value = walletsFlow.value.map { if (it.id == wallet.id) wallet else it }
        }

        override suspend fun archiveWalletAtomically(walletId: Long) {
            val target = getWalletById(walletId)
                ?: throw DomainException.InvalidTransactionException("Dompet tidak ditemukan.")
            val totalW = getActiveWallets().fold(BigDecimal.ZERO) { acc, w -> acc + w.balance }
            if (totalW - target.balance < allocatedGoals) {
                throw DomainException.WalletArchiveBlockedByGoalException()
            }
            walletsFlow.value = walletsFlow.value.map {
                if (it.id == walletId) it.copy(isArchived = true) else it
            }
            pausedRecurringWalletIds.add(walletId)
        }
    }
}
