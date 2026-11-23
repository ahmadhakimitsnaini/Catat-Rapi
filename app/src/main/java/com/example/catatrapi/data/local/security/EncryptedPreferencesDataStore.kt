package com.example.catatrapi.data.local.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.catatrapi.domain.model.FinancialSettings
import com.example.catatrapi.domain.repository.FinancialPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Implementasi NFR-S3: DataStore Preferences dengan seluruh nilai dienkripsi via [CryptoCipher] (Android Keystore).
 */
@Singleton
class EncryptedPreferencesDataStore(
    private val dataStore: DataStore<Preferences>,
    private val cryptoCipher: CryptoCipher,
) : FinancialPreferencesRepository {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        cryptoManager: KeystoreCryptoManager,
    ) : this(
        dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(DATASTORE_FILE_NAME) }
        ),
        cryptoCipher = cryptoManager,
    )

    override fun observeSettings(): Flow<FinancialSettings> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { prefs -> prefs.toFinancialSettings() }
            .distinctUntilChanged()

    override suspend fun getSettings(): FinancialSettings =
        observeSettings().first()

    override suspend fun updateMinimumBuffer(amount: BigDecimal) {
        val sanitized = amount.max(BigDecimal.ZERO)
        dataStore.edit { prefs ->
            prefs[KEY_MIN_BUFFER] = cryptoCipher.encryptString(sanitized.toPlainString())
        }
    }

    override suspend fun updateSweepBufferOverride(amount: BigDecimal?) {
        dataStore.edit { prefs ->
            if (amount == null) {
                prefs.remove(KEY_SWEEP_BUFFER_OVERRIDE)
            } else {
                val sanitized = amount.max(BigDecimal.ZERO)
                prefs[KEY_SWEEP_BUFFER_OVERRIDE] = cryptoCipher.encryptString(sanitized.toPlainString())
            }
        }
    }

    override suspend fun updateDailyExpenseLimitOverride(amount: BigDecimal?) {
        dataStore.edit { prefs ->
            if (amount == null || amount.signum() <= 0) {
                prefs.remove(KEY_DAILY_LIMIT_OVERRIDE)
            } else {
                prefs[KEY_DAILY_LIMIT_OVERRIDE] = cryptoCipher.encryptString(amount.toPlainString())
            }
        }
    }

    override suspend fun updateBiometricLock(enabled: Boolean, timeoutSeconds: Long) {
        val sanitizedTimeout = timeoutSeconds.coerceAtLeast(0L)
        dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = cryptoCipher.encryptString(enabled.toString())
            prefs[KEY_LOCK_TIMEOUT_SECONDS] = cryptoCipher.encryptString(sanitizedTimeout.toString())
        }
    }

    private fun Preferences.toFinancialSettings(): FinancialSettings {
        val minBuffer = decryptBigDecimal(this[KEY_MIN_BUFFER]) ?: BigDecimal.ZERO
        val sweepBuffer = decryptBigDecimal(this[KEY_SWEEP_BUFFER_OVERRIDE])
        val dailyLimit = decryptBigDecimal(this[KEY_DAILY_LIMIT_OVERRIDE])?.takeIf { it.signum() > 0 }
        val biometricEnabled = this[KEY_BIOMETRIC_ENABLED]
            ?.let { cryptoCipher.decryptStringOrNull(it) }
            ?.toBooleanStrictOrNull()
            ?: false
        val timeoutSeconds = this[KEY_LOCK_TIMEOUT_SECONDS]
            ?.let { cryptoCipher.decryptStringOrNull(it) }
            ?.toLongOrNull()
            ?.coerceAtLeast(0L)
            ?: FinancialSettings.DEFAULT_LOCK_TIMEOUT_SECONDS

        return FinancialSettings(
            minimumBuffer = minBuffer,
            sweepBufferOverride = sweepBuffer,
            dailyExpenseLimitOverride = dailyLimit,
            biometricLockEnabled = biometricEnabled,
            lockTimeoutSeconds = timeoutSeconds,
        )
    }

    private fun decryptBigDecimal(encoded: String?): BigDecimal? =
        encoded
            ?.let { cryptoCipher.decryptStringOrNull(it) }
            ?.let { plain -> runCatching { BigDecimal(plain) }.getOrNull() }

    companion object {
        const val DATASTORE_FILE_NAME = "catatrapi_encrypted_prefs"
        private val KEY_MIN_BUFFER = stringPreferencesKey("enc_min_buffer")
        private val KEY_SWEEP_BUFFER_OVERRIDE = stringPreferencesKey("enc_sweep_buffer_override")
        private val KEY_DAILY_LIMIT_OVERRIDE = stringPreferencesKey("enc_daily_limit_override")
        private val KEY_BIOMETRIC_ENABLED = stringPreferencesKey("enc_biometric_enabled")
        private val KEY_LOCK_TIMEOUT_SECONDS = stringPreferencesKey("enc_lock_timeout_seconds")
    }
}
