package com.davw.handcommandserver

import android.Manifest
import android.app.Application
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import org.json.JSONException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException

class BleViewModel(application: Application) : AndroidViewModel(application) {

    private val server = NimbleServer(application)
    private val defaultCommandCount = 8

    private val _events = MutableStateFlow<List<String>>(emptyList())
    val events = _events.asStateFlow()
    var selectedCommand by mutableStateOf<Int?>(null)
    val directCommands = mutableStateListOf<DirectCommandConfig>().apply {
        addAll(loadInitialDirectCommands())
    }

    //var motorAbsPosVals = IntArray(4) { 0}

    // 4 sliders, initial value = 50f
    val _sliders = MutableStateFlow(FloatArray(4) { 50f })
    val sliders = _sliders

    fun updateSlider(index: Int, value: Float): Boolean{
        val newArray = _sliders.value.clone()
        newArray[index] = value
        _sliders.value =newArray
        server.motorAbsPosVals[index] = value.toInt()
        return true
    }
    fun getSlider(index: Int): Float {
        return server.motorAbsPosVals[index].toFloat()
        ;return (_sliders.value[index])
    }

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
        displayStatus()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun startServer() {
        server.start(application.applicationContext)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendNotification(ty : Int) = server.sendNotification(ty)
    fun subscribe() = server.subscribe()
    fun disconnect() = server.disconnect()
    fun setMotorsPos() = server.setMotorsPos()
//    fun sendCommand(command: Byte) = server.sendCommand(command)
    fun setMotorPos(index: Int, iPos: Int){
        server.setMotorPos(index, iPos)
        displayStatus()
    }
//    fun setMotorPos(index: Int, iPos: Int){
//        server.setMotorPos(index, iPos)
//        displayStatus()
//    }
//    fun setMotorPos(index: Int, iPos: Int){
//        server.setMotorPos(index, iPos)
//        displayStatus()
//    }
    fun setMotor(index: Int){
        server.setMotor(index)
        displayStatus()
    }
    fun setMotorAbsPos(index: Int, iPos : Int)
    {
        server.motorAbsPosVals[index] = iPos
    }

    fun stopMotor()
    {
        //_commandStatus.value = "Stop Motor"
        server.setMotorSTOP()
        displayStatus()
    }
    fun forwardMotor()
    {
        //server.motorMovment = 1;
        server.setMotorFRWD()
        displayStatus()
    }
    fun backwardMotor()
    {
        //server.motorMovment = 2;
        server.setMotorBKWD()
        displayStatus()
    }
    fun absPosMotor()
    {
        //server.motorMovment = 2;
        server.absPosMotor()
        displayStatus()
    }

    fun getButMotorColor(index : Int) : Color
    {
        val mt: Int = server.motors[index-1].toInt()
        var col : Color = Color.Blue
        if (mt == 0 )
        {
           col = Color.Red
        }
        else if (mt== 1) {
            col = Color.Blue
        }
        else if (mt== 2){
            col = Color.Green
        }
        else {
            col = Color.Magenta //Cyan
        }
        return col
    }
    fun commandStatus() {
        //_commandStatus.value = "Motor Status"
    }
    fun updateCommandAction(index: Int, value: String) {
        val current = directCommands[index]
        directCommands[index] = current.copy(action = value)
    }

    fun replaceDirectCommands(loaded: List<DirectCommandConfig>) {
        if (loaded.size != directCommands.size) {
            throw IllegalArgumentException("Expected ${directCommands.size} commands, found ${loaded.size}")
        }
        loaded.forEachIndexed { index, command ->
            directCommands[index] = command
        }
    }

    fun sendText(toString: String) {
        TODO("Not yet implemented")
    }
    fun sendDirectCommand(cmnd: Int) {
        server.sendCommandNotification(cmnd.toByte())
    }
    fun sendIntCommand(cmnd: Int, iMotor: Int = 0) {
        server.sendCommandNotificationToMotor(cmnd.toByte(),iMotor)
    }
    fun sendIntCommandPos(cmnd: Int) {
        when (cmnd) {
            0 -> {
                Log.d("TAG","-0-")
                server.sendCommandNotification(cmnd.toByte())
            }
            1,2,3,4 -> {
                Log.d("TAG","cmnd: "+ cmnd.toString()+ getSlider(cmnd-1).toString())
                server.sendCommandNotificationPos(cmnd.toByte(), getSlider(cmnd-1))
            }
            5,6,7,8 -> {
                Log.d("TAG","One")
                server.sendCommandNotification(cmnd.toByte())
            }
            9,10,11,12 -> {
                Log.d("TAG","Two")
                server.sendCommandNotification(cmnd.toByte())
            }
            else -> Log.d("TAG","Other"+ cmnd.toString())
        }

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

}
