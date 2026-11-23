package com.example.catatrapi.data.local.db

import com.example.catatrapi.data.local.security.DatabasePassphraseManager
import com.example.catatrapi.data.local.security.KeystoreCryptoManager
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseSecurityUnitTest {

    @Test
    fun `DatabasePassphraseManager generates 32-byte key once and reloads identical key`() {
        val tempDir = Files.createTempDirectory("catatrapi_db_key_test").toFile()
        try {
            val crypto = KeystoreCryptoManager()
            val manager = DatabasePassphraseManager(crypto)

            val firstKey = manager.getOrCreatePassphraseInDir(tempDir)
            val secondKey = manager.getOrCreatePassphraseInDir(tempDir)

            assertEquals(DatabasePassphraseManager.PASSPHRASE_LENGTH_BYTES, firstKey.size)
            assertArrayEquals(firstKey, secondKey)

            val storedFile = File(tempDir, DatabasePassphraseManager.KEY_FILE_NAME)
            assertTrue(storedFile.exists())
            assertTrue(storedFile.readText().contains(":"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `DatabaseEncryptionMigrator accurately detects plaintext SQLite header vs encrypted bytes`() {
        val tempDir = Files.createTempDirectory("catatrapi_header_test").toFile()
        try {
            val plainFile = File(tempDir, "plain.db")
            plainFile.writeBytes("SQLite format 3\u0000some_table_data_here".toByteArray(Charsets.US_ASCII))
            assertTrue(DatabaseEncryptionMigrator.isPlaintextSqliteFile(plainFile))

            val encryptedFile = File(tempDir, "encrypted.db")
            encryptedFile.writeBytes(ByteArray(64) { index -> (index * 19 + 7).toByte() })
            assertFalse(DatabaseEncryptionMigrator.isPlaintextSqliteFile(encryptedFile))

            val shortFile = File(tempDir, "short.db")
            shortFile.writeBytes("SQLite".toByteArray(Charsets.US_ASCII))
            assertFalse(DatabaseEncryptionMigrator.isPlaintextSqliteFile(shortFile))

            val missingFile = File(tempDir, "missing.db")
            assertFalse(DatabaseEncryptionMigrator.isPlaintextSqliteFile(missingFile))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
