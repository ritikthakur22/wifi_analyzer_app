package com.rtwifianalyzer.app

import android.Manifest
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.rtwifianalyzer.app.data.SettingsStore
import com.rtwifianalyzer.app.ui.AnalyzerViewModel
import com.rtwifianalyzer.app.ui.WifiAnalyzerApp
import com.rtwifianalyzer.app.ui.theme.WifiAnalyzerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: AnalyzerViewModel by viewModels()
    @Inject lateinit var settings: SettingsStore
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
        val permission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.scan() }
        setContent { val theme by settings.theme.collectAsStateWithLifecycle(initialValue = com.rtwifianalyzer.app.domain.AppTheme.SYSTEM); WifiAnalyzerTheme(theme) { WifiAnalyzerApp(vm, theme, { permission.launch(if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)) }, { t -> lifecycleScope.launch { settings.setTheme(t) } }) } }
    }
}
