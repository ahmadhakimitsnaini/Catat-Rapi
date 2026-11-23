package com.example.catatrapi.data.local.db

import android.content.Context
import java.io.File
import java.io.FileInputStream
import net.zetetic.database.sqlcipher.SQLiteDatabase

/**
 * NFR-S2: Mendeteksi dan memigrasikan berkas database SQLite plaintext (dari Sprint 1–3)
 * menjadi database terenkripsi SQLCipher secara atomik tanpa kehilangan data.
 */
object DatabaseEncryptionMigrator {

    private val SQLITE_PLAINTEXT_HEADER: ByteArray =
        "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    /**
     * Mengembalikan `true` bila berkas [file] ada dan diawali dengan 16-byte magic header SQLite plaintext.
     */
    fun isPlaintextSqliteFile(file: File): Boolean {
        if (!file.exists() || !file.isFile || file.length() < SQLITE_PLAINTEXT_HEADER.size) {
            return false
        }
        val header = ByteArray(SQLITE_PLAINTEXT_HEADER.size)
        val bytesRead = runCatching {
            FileInputStream(file).use { input -> input.read(header) }
        }.getOrDefault(-1)

        if (bytesRead != SQLITE_PLAINTEXT_HEADER.size) return false
        return header.contentEquals(SQLITE_PLAINTEXT_HEADER)
    }

    /**
     * Jika `catatrapi.db` masih berformat SQLite plaintext, ekspor seluruh tabel dan `user_version`
     * ke berkas baru terenkripsi SQLCipher lalu gantikan berkas lama secara atomik.
     */
    fun migrateIfPlaintext(context: Context, dbName: String, passphrase: ByteArray) {
        val dbFile = context.getDatabasePath(dbName)
        if (!isPlaintextSqliteFile(dbFile)) return

        // 1. Flush WAL ke berkas database utama menggunakan SQLite bawaan Android
        runCatching {
            android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READWRITE,
            ).use { frameworkDb ->
                frameworkDb.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
            }
        }

        val parentDir = dbFile.parentFile ?: context.filesDir
        val tempEncryptedFile = File(parentDir, "$dbName.sqlcipher.tmp")
        if (tempEncryptedFile.exists()) {
            tempEncryptedFile.delete()
        }

        // 2. Buka database plaintext lewat SQLCipher dan ekspor ke berkas terenkripsi
        val plainDb = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            ByteArray(0),
            null,
            SQLiteDatabase.OPEN_READWRITE,
            null,
        )
        try {
            val userVersion = plainDb.version
            plainDb.rawExecSQL(
                "ATTACH DATABASE ? AS encrypted KEY ?",
                tempEncryptedFile.absolutePath,
                passphrase,
            )
            plainDb.rawExecSQL("SELECT sqlcipher_export('encrypted')")
            plainDb.rawExecSQL("PRAGMA encrypted.user_version = $userVersion")
            plainDb.rawExecSQL("DETACH DATABASE encrypted")
        } finally {
            plainDb.close()
        }

        // 3. Bersihkan berkas jurnal/WAL lama dan tukar berkas database secara atomik
        File(parentDir, "$dbName-wal").delete()
        File(parentDir, "$dbName-shm").delete()
        File(parentDir, "$dbName-journal").delete()

        val backupFile = File(parentDir, "$dbName.plaintext.bak")
        if (backupFile.exists()) backupFile.delete()

        check(dbFile.renameTo(backupFile)) {
            "Gagal memindahkan database plaintext lama ke cadangan sementara."
        }
        if (tempEncryptedFile.renameTo(dbFile)) {
            backupFile.delete()
        } else {
            // Pulihkan bila rename gagal
            backupFile.renameTo(dbFile)
            error("Gagal menggantikan berkas database dengan versi terenkripsi SQLCipher.")
        }
    }
}
