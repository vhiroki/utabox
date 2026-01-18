package com.vhiroki.utabox

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import com.vhiroki.utabox.util.VideoStorageHelper

class MainActivity : ComponentActivity() {

    private lateinit var videoStorageHelper: VideoStorageHelper

    // Track permission state to trigger recomposition
    private val permissionGranted = mutableStateOf(false)

    // Callback to invoke after folder is selected
    private var onFolderSelectedCallback: (() -> Unit)? = null

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            android.util.Log.d("MainActivity", "Selected folder: $it")
            videoStorageHelper.savePersistedFolderUri(it)
            // Trigger recomposition to update video source description
            permissionGranted.value = !permissionGranted.value
            // Call the callback to reload songs
            onFolderSelectedCallback?.invoke()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted.value = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        videoStorageHelper = VideoStorageHelper(this)

        // Request permission on startup
        requestMediaPermission()

        enableEdgeToEdge()
        setContent {
            UtaBoxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
val navController = rememberNavController()

                    // Read permissionGranted to trigger recomposition when it changes
                    @Suppress("UNUSED_VARIABLE")
                    val permissionState = permissionGranted.value

                    UtaBoxNavHost(
                        navController = navController,
                        onRequestFolderPicker = { onFolderSelected ->
                            onFolderSelectedCallback = onFolderSelected
                            folderPickerLauncher.launch(null)
                        }
                    )
                }
            }
        }
    }

    private fun requestMediaPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                permissionGranted.value = true
            }
            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }
}