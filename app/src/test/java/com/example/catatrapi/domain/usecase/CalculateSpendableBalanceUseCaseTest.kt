package com.example.catatrapi.domain.usecase

import com.example.catatrapi.domain.model.Wallet
import com.example.catatrapi.domain.model.WalletType
import com.example.catatrapi.domain.repository.FinancialAllocationProvider
import com.example.catatrapi.domain.repository.WalletRepository
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateSpendableBalanceUseCaseTest {

    @Test
    fun `spendable positive when total wallet balance exceeds allocated goals and buffer`() = runTest {
        val walletRepo = FakeWalletRepository(
            listOf(
                Wallet(id = 1, name = "BCA", type = WalletType.BANK, balance = BigDecimal("5000000")),
                Wallet(id = 2, name = "Tunai", type = WalletType.CASH, balance = BigDecimal("1500000")),
            )
        )
        val allocationProvider = FakeAllocationProvider(
            allocatedGoals = BigDecimal("2000000"),
            minimumBuffer = BigDecimal("500000"),
        )
        val useCase = CalculateSpendableBalanceUseCase(
            getTotalWalletBalanceUseCase = GetTotalWalletBalanceUseCase(walletRepo),
            allocationProvider = allocationProvider,
        )

        val result = useCase().first()

        assertEquals(BigDecimal("6500000"), result.totalWalletBalance)
        assertEquals(BigDecimal("2000000"), result.allocatedGoalsAmount)
        assertEquals(BigDecimal("500000"), result.minimumBuffer)
        assertEquals(BigDecimal("4000000"), result.spendableAmount)
        assertFalse(result.isOverAllocated)
    }

    @Test
    fun `spendable negative is preserved and never clamped to zero when goals exceed balance`() = runTest {
        val walletRepo = FakeWalletRepository(
            listOf(
                Wallet(id = 1, name = "BCA", type = WalletType.BANK, balance = BigDecimal("1000000")),
            )
        )
        val allocationProvider = FakeAllocationProvider(
            allocatedGoals = BigDecimal("1250000"),
            minimumBuffer = BigDecimal("100000"),
        )
        val useCase = CalculateSpendableBalanceUseCase(
            getTotalWalletBalanceUseCase = GetTotalWalletBalanceUseCase(walletRepo),
            allocationProvider = allocationProvider,
        )

        val result = useCase().first()

        assertEquals(BigDecimal("-350000"), result.spendableAmount)
        assertTrue(result.isOverAllocated)
    }

    private class FakeAllocationProvider(
        allocatedGoals: BigDecimal,
        minimumBuffer: BigDecimal,
    ) : FinancialAllocationProvider {
        private val goalsFlow = MutableStateFlow(allocatedGoals)
        private val bufferFlow = MutableStateFlow(minimumBuffer)

        override fun observeTotalAllocatedGoals(): Flow<BigDecimal> = goalsFlow
        override suspend fun getTotalAllocatedGoals(): BigDecimal = goalsFlow.value
        override fun observeMinimumBuffer(): Flow<BigDecimal> = bufferFlow
        override suspend fun pauseRecurringTransactionsForWallet(walletId: Long) = Unit
    }

    private class FakeWalletRepository(initial: List<Wallet>) : WalletRepository {
        private val flow = MutableStateFlow(initial)
        override fun observeActiveWallets(): Flow<List<Wallet>> = flow
        override fun observeWalletById(id: Long): Flow<Wallet?> = MutableStateFlow(flow.value.firstOrNull { it.id == id })
        override fun observeHasAnyWallet(): Flow<Boolean> = MutableStateFlow(flow.value.isNotEmpty())
        override suspend fun getActiveWallets(): List<Wallet> = flow.value
        override suspend fun getWalletById(id: Long): Wallet? = flow.value.firstOrNull { it.id == id }
        override suspend fun createWallet(wallet: Wallet): Long = 1L
        override suspend fun updateWallet(wallet: Wallet) = Unit
        override suspend fun archiveWalletAtomically(walletId: Long) = Unit
    }
}
