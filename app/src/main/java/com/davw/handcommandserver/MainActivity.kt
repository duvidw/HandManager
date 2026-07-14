package com.davw.handcommandserver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider

enum class AppScreen {
    Main,
    DirectCommands
}

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: BleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[BleViewModel::class.java]

        setContent {
            val context = LocalContext.current
            val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                )
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionsMap ->
                val areGranted = permissionsMap.values.all { it }
                if (areGranted) {
                    viewModel.startServer()
                }
            }
            MaterialTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.Main) }
                LaunchedEffect(Unit) {
                    val allPermissionsGranted = permissionsToRequest.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (!allPermissionsGranted) {
                        permissionLauncher.launch(permissionsToRequest)
                    } else {
                        viewModel.startServer()
                    }
                }
                LaunchedEffect(currentScreen) {
                    viewModel.notifyScreenChanged(currentScreen)
                }

                when (currentScreen) {
                    AppScreen.Main -> BleScreen(
                        viewModel = viewModel,
                        onNextScreen = { currentScreen = AppScreen.DirectCommands }
                    )

                    AppScreen.DirectCommands -> DirectCommandsScreen(
                        viewModel = viewModel,
                        onPrevScreen = { currentScreen = AppScreen.Main }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        // Shut down BLE server and release all resources when the activity is fully destroyed
        if (isFinishing) {
            println("MainActivity is finishing, disconnecting and closing BLE server.")
            viewModel.disconnectAndCloseServer()
        }
        println("MainActivity onDestroy called.")
        super.onDestroy()
    }
}
