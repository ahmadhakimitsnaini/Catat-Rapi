package com.example.catatrapi

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.catatrapi.domain.repository.FinancialPreferencesRepository
import com.example.catatrapi.ui.auth.BiometricAuthManager
import com.example.catatrapi.ui.auth.LockScreen
import com.example.catatrapi.ui.navigation.CatatRapiAppRoot
import com.example.catatrapi.ui.theme.CatatRapiTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var financialPreferencesRepository: FinancialPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // NFR-S4: Lindungi layar finansial dari screenshot dan tampilan recent apps
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        setContent {
            CatatRapiTheme {
                val settings by financialPreferencesRepository.observeSettings()
                    .collectAsStateWithLifecycle(initialValue = null)

                var isUnlocked by rememberSaveable { mutableStateOf(false) }
                var lastBackgroundAt by rememberSaveable { mutableStateOf<Long?>(null) }
                var authError by remember { mutableStateOf<String?>(null) }

                val currentSettings = settings
                val isLockRequired = currentSettings != null &&
                        currentSettings.biometricLockEnabled &&
                        !isUnlocked

                fun triggerPrompt() {
                    authError = null
                    BiometricAuthManager.authenticate(
                        activity = this@MainActivity,
                        onSuccess = {
                            isUnlocked = true
                            authError = null
                        },
                        onError = { message ->
                            authError = message
                        },
                    )
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, currentSettings?.biometricLockEnabled, currentSettings?.lockTimeoutSeconds) {
                    val observer = LifecycleEventObserver { _, event ->
                        val activeSettings = currentSettings ?: return@LifecycleEventObserver
                        when (event) {
                            Lifecycle.Event.ON_STOP -> {
                                if (!isChangingConfigurations) {
                                    lastBackgroundAt = System.currentTimeMillis()
                                }
                            }
                            Lifecycle.Event.ON_START -> {
                                val bgTime = lastBackgroundAt
                                if (bgTime != null && BiometricAuthManager.shouldLockAfterBackground(
                                        isEnabled = activeSettings.biometricLockEnabled,
                                        lastBackgroundMillis = bgTime,
                                        nowMillis = System.currentTimeMillis(),
                                        timeoutSeconds = activeSettings.lockTimeoutSeconds,
                                    )
                                ) {
                                    isUnlocked = false
                                }
                                lastBackgroundAt = null
                            }
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                LaunchedEffect(isLockRequired) {
                    if (isLockRequired) {
                        triggerPrompt()
                    }
                }

                Surface(color = MaterialTheme.colorScheme.background) {
                    if (isLockRequired) {
                        LockScreen(
                            errorMessage = authError,
                            onUnlockClick = { triggerPrompt() },
                        )
                    } else {
                        CatatRapiAppRoot()
                    }
                }
            }
        }
    }
}
