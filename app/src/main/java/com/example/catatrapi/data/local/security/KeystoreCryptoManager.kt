package com.example.catatrapi.data.local.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kontrak enkripsi simetris AEAD (AES-256-GCM) agar mudah diuji pada unit test JVM.
 */
interface CryptoCipher {
    fun encryptString(plainText: String): String
    fun decryptStringOrNull(encoded: String): String?
    fun encryptBytes(plainBytes: ByteArray): String
    fun decryptBytesOrNull(encoded: String): ByteArray?
}

/**
 * Manajer kriptografi berbasis Android Keystore (AES-256-GCM, tanpa padding) untuk NFR-S2 & NFR-S3.
 * Setiap operasi enkripsi menghasilkan IV acak dari cipher dan menyimpannya dalam format `base64(iv):base64(ciphertext)`.
 */
@Singleton
class KeystoreCryptoManager @Inject constructor() : CryptoCipher {

    @Volatile
    private var fallbackJvmKey: SecretKey? = null

    override fun encryptString(plainText: String): String =
        encryptBytes(plainText.toByteArray(StandardCharsets.UTF_8))

    override fun decryptStringOrNull(encoded: String): String? =
        decryptBytesOrNull(encoded)?.toString(StandardCharsets.UTF_8)

    override fun encryptBytes(plainBytes: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainBytes)
        val ivBase64 = Base64.getEncoder().encodeToString(iv)
        val cipherBase64 = Base64.getEncoder().encodeToString(cipherBytes)
        return "$ivBase64$DELIMITER$cipherBase64"
    }

    override fun decryptBytesOrNull(encoded: String): ByteArray? {
        if (encoded.isBlank()) return null
        return runCatching {
            val parts = encoded.split(DELIMITER, limit = 2)
            if (parts.size != 2) return null
            val iv = Base64.getDecoder().decode(parts[0])
            val cipherBytes = Base64.getDecoder().decode(parts[1])
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            cipher.doFinal(cipherBytes)
        }.getOrNull()
    }

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        val androidKeyStore = runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        }.getOrNull()

        if (androidKeyStore != null) {
            val existing = runCatching {
                (androidKeyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            }.getOrNull()
            if (existing != null) return existing

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE,
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }

        // Fallback untuk lingkungan JVM unit test di mana AndroidKeyStore tidak tersedia.
        return fallbackJvmKey ?: KeyGenerator.getInstance("AES").apply {
            init(KEY_SIZE_BITS, SecureRandom())
        }.generateKey().also { fallbackJvmKey = it }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "catatrapi_master_aes_gcm_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val DELIMITER = ":"
    }
}
