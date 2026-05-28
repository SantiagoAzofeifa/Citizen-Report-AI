package com.example.citizenreportai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.citizenreportai.data.repository.RealAuthRepository
import com.example.citizenreportai.ui.navigation.AppNavigation
import com.example.citizenreportai.ui.theme.CitizenReportAITheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fire-and-forget warmup so Render wakes up while the UI loads.
        lifecycleScope.launch(Dispatchers.IO) {
            RealAuthRepository(applicationContext).warmUp()
        }

        setContent {
            CitizenReportAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
