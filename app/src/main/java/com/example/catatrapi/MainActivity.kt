package com.example.catatrapi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.catatrapi.ui.components.CurrencyFormatter
import com.example.catatrapi.ui.components.DateFormatterId
import com.example.catatrapi.ui.theme.CatatRapiTheme
import java.math.BigDecimal
import androidx.lifecycle.lifecycleScope
import com.example.catatrapi.data.local.db.DatabaseProvider
import com.example.catatrapi.data.local.entity.WalletEntity
import com.example.catatrapi.domain.model.WalletType
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CatatRapiTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "CatatRapi"
                        )

                        Text(
                            text = CurrencyFormatter.format(
                                BigDecimal("1250000")
                            )
                        )

                        Text(
                            text = CurrencyFormatter.format(
                                BigDecimal("-50000")
                            ),
                            color = MaterialTheme.colorScheme.error
                        )

                        Text(
                            text = DateFormatterId.full(
                                System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
        lifecycleScope.launch {
            val database = DatabaseProvider.getDatabase(applicationContext)

            database.walletDao().insert(
                WalletEntity(
                    name = "Dompet Tes",
                    type = WalletType.CASH,
                    balance = BigDecimal("100000"),
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
}