package com.example.catatrapi.data.local.security

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.catatrapi.domain.model.FinancialSettings
import com.example.catatrapi.ui.auth.BiometricAuthManager
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EncryptedPreferencesDataStoreTest {

    @Test
    fun `KeystoreCryptoManager encrypts with random IV and decrypts roundtrip`() {
        val crypto = KeystoreCryptoManager()
        val secret = "1250000"

        val enc1 = crypto.encryptString(secret)
        val enc2 = crypto.encryptString(secret)

        // IV acak memastikan dua ciphertext untuk plaintext sama selalu berbeda
        assertNotEquals(enc1, enc2)
        assertFalse(enc1.contains(secret))
        assertEquals(secret, crypto.decryptStringOrNull(enc1))
        assertEquals(secret, crypto.decryptStringOrNull(enc2))
        assertNull(crypto.decryptStringOrNull("corrupted:payload"))
    }

    @Test
    fun `KeystoreCryptoManager encryptBytes and decryptBytesOrNull preserve 32-byte key`() {
        val crypto = KeystoreCryptoManager()
        val rawKey = ByteArray(32) { index -> (index * 7 + 3).toByte() }

        val encoded = crypto.encryptBytes(rawKey)
        val decoded = crypto.decryptBytesOrNull(encoded)

        assertArrayEquals(rawKey, decoded)
    }

    @Test
    fun `EncryptedPreferencesDataStore persists and decrypts buffers and security settings`() = runTest {
        val tempDir = Files.createTempDirectory("catatrapi_prefs_test").toFile()
        try {
            val file = File(tempDir, "test_prefs.preferences_pb")
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
            val dataStore = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file },
            )
            val repo = EncryptedPreferencesDataStore(dataStore, KeystoreCryptoManager())

            // Default awal
            val initial = repo.getSettings()
            assertEquals(0, BigDecimal.ZERO.compareTo(initial.minimumBuffer))
            assertNull(initial.sweepBufferOverride)
            assertNull(initial.dailyExpenseLimitOverride)
            assertFalse(initial.biometricLockEnabled)
            assertEquals(FinancialSettings.DEFAULT_LOCK_TIMEOUT_SECONDS, initial.lockTimeoutSeconds)

            // Update semua pengaturan
            repo.updateMinimumBuffer(BigDecimal("500000"))
            repo.updateSweepBufferOverride(BigDecimal("300000"))
            repo.updateDailyExpenseLimitOverride(BigDecimal("150000"))
            repo.updateBiometricLock(enabled = true, timeoutSeconds = 60L)

            val updated = repo.getSettings()
            assertEquals(0, BigDecimal("500000").compareTo(updated.minimumBuffer))
            assertEquals(0, BigDecimal("300000").compareTo(updated.sweepBufferOverride))
            assertEquals(0, BigDecimal("150000").compareTo(updated.dailyExpenseLimitOverride))
            assertTrue(updated.biometricLockEnabled)
            assertEquals(60L, updated.lockTimeoutSeconds)

            // Reset override ke null
            repo.updateSweepBufferOverride(null)
            repo.updateDailyExpenseLimitOverride(null)
            val afterReset = repo.getSettings()
            assertNull(afterReset.sweepBufferOverride)
            assertNull(afterReset.dailyExpenseLimitOverride)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `BiometricAuthManager shouldLockAfterBackground evaluates timeout accurately`() {
        // Nonaktif -> tidak pernah mengunci
        assertFalse(
            BiometricAuthManager.shouldLockAfterBackground(
                isEnabled = false,
                lastBackgroundMillis = 1_000L,
                nowMillis = 999_000L,
                timeoutSeconds = 30L,
            )
        )

        // Cold start (lastBackgroundMillis == null) saat aktif -> wajib kunci
        assertTrue(
            BiometricAuthManager.shouldLockAfterBackground(
                isEnabled = true,
                lastBackgroundMillis = null,
                nowMillis = 10_000L,
                timeoutSeconds = 30L,
            )
        )

        // Di background 15 detik, timeout 30 detik -> belum terkunci
        assertFalse(
            BiometricAuthManager.shouldLockAfterBackground(
                isEnabled = true,
                lastBackgroundMillis = 10_000L,
                nowMillis = 25_000L,
                timeoutSeconds = 30L,
            )
        )

        // Di background tepat 30 detik, timeout 30 detik -> terkunci
        assertTrue(
            BiometricAuthManager.shouldLockAfterBackground(
                isEnabled = true,
                lastBackgroundMillis = 10_000L,
                nowMillis = 40_000L,
                timeoutSeconds = 30L,
            )
        )
    }
}
