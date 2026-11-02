package com.example.catatrapi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.catatrapi.ui.navigation.CatatRapiAppRoot
import com.example.catatrapi.ui.theme.CatatRapiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CatatRapiTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CatatRapiAppRoot()
                }
            }
        }
    }
}
