package com.example.catatrapi.data.local.security

import android.content.Context
import java.io.File
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NFR-S2: Mengelola kunci (passphrase) 32-byte untuk SQLCipher yang dienkripsi menggunakan Android Keystore ([CryptoCipher])
 * dan disimpan di `noBackupFilesDir` agar tidak pernah ikut tercadangkan ke cloud.
 */
@Singleton
class DatabasePassphraseManager @Inject constructor(
    private val cryptoCipher: CryptoCipher,
) {

    fun getOrCreatePassphrase(context: Context): ByteArray =
        getOrCreatePassphraseInDir(context.noBackupFilesDir)

    @Synchronized
    internal fun getOrCreatePassphraseInDir(directory: File): ByteArray {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val keyFile = File(directory, KEY_FILE_NAME)
        if (keyFile.exists()) {
            val encoded = runCatching { keyFile.readText(Charsets.UTF_8).trim() }.getOrNull()
            val decrypted = encoded?.let { cryptoCipher.decryptBytesOrNull(it) }
            if (decrypted != null && decrypted.size == PASSPHRASE_LENGTH_BYTES) {
                return decrypted
            }
        }

        val generated = ByteArray(PASSPHRASE_LENGTH_BYTES).also { bytes ->
            SecureRandom().nextBytes(bytes)
        }
        val encrypted = cryptoCipher.encryptBytes(generated)
        keyFile.writeText(encrypted, Charsets.UTF_8)
        return generated
    }

    companion object {
        const val KEY_FILE_NAME = "catatrapi_sqlcipher_key.enc"
        const val PASSPHRASE_LENGTH_BYTES = 32
    }
}
