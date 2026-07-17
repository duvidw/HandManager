package com.davw.handcommandserver

import android.Manifest
import android.content.pm.PackageManager
import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.core.content.ContextCompat
import org.json.JSONException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException

class BleViewModel(application: Application) : AndroidViewModel(application) {

    /** Called by the OS when the ViewModel is permanently destroyed (app is finishing). */

    private val server = NimbleServer(application)
    private val defaultCommandCount = 8
    private var serverStarted = false

    private val _events = MutableStateFlow<List<String>>(emptyList())
    val events = _events.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()
    //var selectedCommand by mutableStateOf<Int?>(null)
    val directCommands = mutableStateListOf<DirectCommandConfig>().apply {
        addAll(loadInitialDirectCommands())
    }

    //var motorAbsPosVals = IntArray(4) { 0}

    // 4 sliders, initial value = 50f
//    val _sliders = MutableStateFlow(FloatArray(5) { 50f })
    //val sliders = _sliders

    //private val _commandStatus = MutableStateFlow("System Ready")
    //val statusText: StateFlow<String> = _commandStatus
//
    private val _statusText = MutableStateFlow("System Ready")
    val statusText: StateFlow<String> = _statusText

    fun displayStatus() {
        // Business logic...
        //_statusText.value = "Motor Stopped at ${System.currentTimeMillis()}"
        val motorString = server.motors.joinToString(separator = ", ") { it.toString() }
        _statusText.value = "Select type: ${server.movementType} Motors type: " + motorString
    }

    //
    init {
        server.onEvent = { msg ->
            _events.update { it + msg }
        }
        server.onConnectionChanged = { connected ->
            _isConnected.value = connected
        }
        displayStatus()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun startServer() {
        if (serverStarted) {
            return
        }
        server.start(application.applicationContext)
        serverStarted = true
    }
    override fun onCleared() {
        super.onCleared()
        server.shutdown()
        serverStarted = false
    }

    fun disconnectAndCloseServer() {
        server.shutdown()
        serverStarted = false
    }

    fun sendNotification(ty : Int) {
        if (!hasBluetoothConnectPermission()) {
            _events.update { it + "Missing BLUETOOTH_CONNECT permission" }
            return
        }
        server.sendNotification(ty)
    }


    fun sendDisconnectNotification() {
        // Send explicit disconnect notification to ESP32
        server.sendDisconnectNotification()
    }

    fun sendUnsubscribeAndDisconnectNotification() {
        server.sendUnsubscribeNotification()
        server.sendDisconnectNotification()
    }


    fun sendMotorsVelocity(ty : Int, motorNum: Int, value: Float) {
        if (!hasBluetoothConnectPermission()) {
            _events.update { it + "Missing BLUETOOTH_CONNECT permission" }
            return
        }
        server.sendMotorsVelocity(ty, motorNum, value)
    }

    fun sendMotorPosition(ty : Int, motorNum: Int, value: Float) {
        if (!hasBluetoothConnectPermission()) {
            _events.update { it + "Missing BLUETOOTH_CONNECT permission" }
            return
        }
        server.sendMotorPosition(ty, motorNum, value)
    }


    fun sendNotificationSpecialCommand(ty : Int) {
        if (!hasBluetoothConnectPermission()) {
            _events.update { it + "Missing BLUETOOTH_CONNECT permission" }
            return
        }
        server.sendNotificationSpecialCommand(ty)
    }
    fun setMotorAbsPos(index: Int, iPos : Int)
    {
        server.motorAbsPosVals[index] = iPos
    }


    fun updateCommandAction(index: Int, value: String) {
        val current = directCommands[index]
        directCommands[index] = current.copy(action = value)
    }
    fun sendDirectCommand(cmnd: Int) {
        server.sendCommandNotification(cmnd.toByte())
    }
    fun notifyScreenChanged(screen: AppScreen) {
        val screenId = when (screen) {
            AppScreen.Main -> 1
            AppScreen.DirectCommands -> 2
        }
        server.sendNavigationNotification(screenId)
    }
    private fun hasBluetoothConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                application.applicationContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun loadInitialDirectCommands(): List<DirectCommandConfig> {
        return try {
            val jsonText = application.assets.open("DefCommands.json").bufferedReader(Charsets.UTF_8).use {
                it.readText()
            }
            val commands = parseDirectCommands(jsonText)
            if (commands.size != defaultCommandCount) {
                fallbackDirectCommands(defaultCommandCount)
            } else {
                commands
            }
        } catch (_: IOException) {
            fallbackDirectCommands(defaultCommandCount)
        } catch (_: JSONException) {
            fallbackDirectCommands(defaultCommandCount)
        }
    }

    private fun fallbackDirectCommands(size: Int): List<DirectCommandConfig> {
        return List(size) { index ->
            DirectCommandConfig(
                name = "Cmd ${index + 1}",
                action = "Action for command ${index + 1}"
            )
        }
    }

    fun stopServer() {
        print("Stopping server...")
        server.shutdown()
    }

    fun unsubscribeThenShutdown() {
        server.unsubscribeThenShutdown()
        serverStarted = false
    }
}
