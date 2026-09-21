package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.service.AvisoTaskMonitorService
import com.example.ui.AvisoBrowserScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AvisoNotificationHelper
import com.example.viewmodel.AvisoViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AvisoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure notification channels are set up
        AvisoNotificationHelper.createNotificationChannels(this)

        handleIntent(intent)
        viewModel.refreshPermissions(this)

        setContent {
            MyApplicationTheme {
                // Request Notification permission on Android 13+ (API 33+)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        // Start background service if not already started
                        if (!AvisoTaskMonitorService.isServiceRunning) {
                            AvisoTaskMonitorService.start(this, 2)
                        }
                    }
                    viewModel.syncServiceState()
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        if (!AvisoTaskMonitorService.isServiceRunning) {
                            AvisoTaskMonitorService.start(this@MainActivity, 2)
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    AvisoBrowserScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val openUrl = intent?.getStringExtra("OPEN_URL")
        if (!openUrl.isNullOrBlank()) {
            viewModel.navigateTo(openUrl)
        }
    }
}
