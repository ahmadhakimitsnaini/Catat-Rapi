package com.example.catatrapi.ui.auth

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * NFR-S1: Manajer autentikasi BiometricPrompt (sidik jari/wajah) dengan fallback DEVICE_CREDENTIAL (PIN/pola),
 * serta logika penguncian otomatis saat kembali dari latar belakang melewati ambang waktu.
 */
object BiometricAuthManager {

    /**
     * Menentukan apakah aplikasi harus dikunci setelah kembali dari latar belakang.
     *
     * @param isEnabled Apakah fitur kunci keamanan diaktifkan oleh pengguna.
     * @param lastBackgroundMillis Waktu terakhir aplikasi masuk ke background (`ON_STOP`), atau `null` bila baru cold start.
     * @param nowMillis Waktu saat ini (`ON_START`).
     * @param timeoutSeconds Ambang waktu (detik) sebelum terkunci otomatis. `0` berarti langsung terkunci.
     */
    fun shouldLockAfterBackground(
        isEnabled: Boolean,
        lastBackgroundMillis: Long?,
        nowMillis: Long,
        timeoutSeconds: Long,
    ): Boolean {
        if (!isEnabled) return false
        if (lastBackgroundMillis == null) return true
        val elapsedMillis = (nowMillis - lastBackgroundMillis).coerceAtLeast(0L)
        val thresholdMillis = timeoutSeconds.coerceAtLeast(0L) * 1_000L
        return elapsedMillis >= thresholdMillis
    }

    /**
     * Memeriksa apakah perangkat memiliki sensor biometrik atau kredensial perangkat (PIN/Pola/Sandi) yang siap digunakan.
     */
    fun canAuthenticate(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val authenticators = allowedAuthenticators()
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Buka Kunci CatatRapi",
        subtitle: String = "Verifikasi identitas Anda untuk mengakses data keuangan",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (!canAuthenticate(activity)) {
            // Jika perangkat tidak memiliki PIN/pola/biometrik sama sekali, izinkan masuk agar tidak terkunci permanen.
            onSuccess()
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Autentikasi tidak dikenali, silakan coba lagi.")
                }
            },
        )

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        } else {
            @Suppress("DEPRECATION")
            promptInfoBuilder.setDeviceCredentialAllowed(true)
        }

        runCatching {
            prompt.authenticate(promptInfoBuilder.build())
        }.onFailure { err ->
            onError(err.message ?: "Gagal memulai prompt autentikasi.")
        }
    }

    private fun allowedAuthenticators(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        }
}
