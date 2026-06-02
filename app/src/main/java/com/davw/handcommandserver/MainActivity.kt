package com.davw.handcommandserver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider

enum class AppScreen {
    Main,
    DirectCommands
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this)[BleViewModel::class.java]

        setContent {
            MaterialTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.Main) }

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
}