package com.vhiroki.utabox

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.vhiroki.utabox.ui.navigation.UtaBoxNavHost
import com.vhiroki.utabox.ui.theme.UtaBoxTheme

class MainActivity : ComponentActivity() {

    // Track permission grant to trigger recomposition
    private val reloadCount = mutableStateOf(0)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        reloadCount.value++ // Trigger reload on permission grant
    }

    // Launcher for "All Files Access" settings screen (Android 11+)
    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        reloadCount.value++ // Trigger reload when returning from settings
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permission on startup
        requestStoragePermission()

        enableEdgeToEdge()
        setContent {
            UtaBoxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Read reloadCount to trigger recomposition when permission is granted
                    val reloadTrigger = reloadCount.value

                    UtaBoxNavHost(
                        navController = navController,
                        reloadTrigger = reloadTrigger
                    )
                }
            }
        }
    }

    private fun requestStoragePermission() {
        when {
            // Android 11+ (API 30+): Need MANAGE_EXTERNAL_STORAGE for full access
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (!Environment.isExternalStorageManager()) {
                    // Open system settings for "All Files Access"
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    manageStorageLauncher.launch(intent)
                }
            }
            // Android 10 and below: Use traditional permissions
            else -> {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_VIDEO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(permission)
                }
            }
        }
    }
}