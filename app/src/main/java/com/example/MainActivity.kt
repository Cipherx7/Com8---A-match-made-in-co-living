package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.configureAppCheck
import com.example.data.local.AppThemeMode
import com.example.data.local.SessionDataStore
import com.example.ui.CoM8AppNavigation
import com.example.ui.theme.CoM8Theme
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureAppCheck(this, intent)
        enableEdgeToEdge()

        CoroutineScope(Dispatchers.IO).launch {
            runCatching { MobileAds.initialize(applicationContext) {} }
        }

        setContent {
            val sessionStore = remember { SessionDataStore(applicationContext) }
            val themeMode by sessionStore.themeModeFlow.collectAsStateWithLifecycle(
                initialValue = AppThemeMode.SYSTEM
            )
            val useDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            CoM8Theme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CoM8AppNavigation()
                }
            }
        }
    }
}
