package com.davw.handcommandserver

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import java.util.UUID

//class NimbleServer(private val context: Context? = null) {
class NimbleServer(private val context: Context) {

    private val serviceUUID = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb" )
    private val charUUID = UUID.fromString("0000dcba-0000-1000-8000-00805f9b34fb")
    private val ccccUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private val mainHandler = Handler(Looper.getMainLooper())


    //////////////////////////////////////////////////////////////////////////////
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        //val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    // This is the property you asked about
    private var advertiser: BluetoothLeAdvertiser? = null

    @SuppressLint("MissingPermission")
    fun startAdvertising() {
        if (!hasBluetoothAdvertisePermission()) {
            onEvent("Missing BLUETOOTH_ADVERTISE permission")
            return
        }

        runCatching {
            advertiser?.stopAdvertising(advertiseCallback)
        }

        // Initialize it here to ensure Bluetooth is actually ON
        advertiser = bluetoothAdapter?.bluetoothLeAdvertiser

        if (advertiser == null) {
            Log.e("NimbleServer", "Device does not support BLE Advertising or Bluetooth is OFF")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb" )))
            .build()

        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i("NimbleServer", "Server is now discoverable by Arduino!")
        }
    }
    //////////////////////////////////////////////////////////////////////////////
    var onEvent: (String) -> Unit = {}
    var onConnectionChanged: (Boolean) -> Unit = {}

    private var gattServer: BluetoothGattServer? = null
    private var connectedDevice: BluetoothDevice? = null

    private fun hasBluetoothConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBluetoothAdvertisePermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
    }
    @SuppressLint("MissingPermission")
    private fun notifyCharacteristicFIX(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic?,
        packet: ByteArray
    ) {
        characteristic ?: return onEvent("Characteristic unavailable")
        val server = gattServer ?: return onEvent("GATT server unavailable")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: pass payload directly (no characteristic.value assignment)
            server.notifyCharacteristicChanged(device, characteristic, false, packet)
        } else {
            // Older APIs
            characteristic.value = packet
            server.notifyCharacteristicChanged(device, characteristic, false)
        }
    }
    @SuppressLint("MissingPermission")
    private fun notifyCharacteristic(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic?
    ) {
        characteristic ?: return onEvent("Characteristic unavailable")
        gattServer?.notifyCharacteristicChanged(device, characteristic, false)
    }

    @SuppressLint("MissingPermission")
    private fun cancelDeviceConnection(device: BluetoothDevice) {
        gattServer?.cancelConnection(device)
    }

//    private val serviceUUID = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb" )
//    private val charUUID = UUID.fromString("0000abce-0000-1000-8000-00805f9b34fb")
//    private val CCC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    val motors = ByteArray(5) // {0}// index 0-7 val 0 no move motor st index
                                      // val 1 move forward
                                      // val 2 move backward
//    val motorsAbsPos = ByteArray(16) // {0}  // index:0 241

//    val motorsAction = ByteArray(16) // {0}// index: 0- 242
                                            // index: 1 - motor 1 direction 0 stop, 1 forward, 2 bacward
                                            // index: 2 - motor 1 nn move encoder count
                                            // indexed: 3,4; 5,6; 7,8 as above for motors 2,3,4

    val motorsPacket = ByteArray(8) // {0}  // index:0 241


    var movementType: Byte = 0x0

    var motorAbsPosVals = IntArray(5) // { 0}

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun start(context: Context) {
        if (gattServer != null) {
            return
        }

        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        gattServer = manager.openGattServer(context, callback)

        val characteristic = BluetoothGattCharacteristic(
            charUUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        val cccDescriptor = BluetoothGattDescriptor(
            ccccUUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )

        val service = BluetoothGattService(serviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        service.addCharacteristic(characteristic)
        characteristic.addDescriptor(cccDescriptor)

        gattServer?.addService(service)

        startAdvertising()
        onEvent("Server started")
    }

    private val callback = object : BluetoothGattServerCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device
                onConnectionChanged(true)
                onEvent("Connection OK: ${device.address}") // ${device.name}") // place for the device client name
            } else {
                onEvent("Disconnected")
                connectedDevice = null
                onConnectionChanged(false)
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (descriptor.uuid == ccccUUID) {

                val enable = value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
                        value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)

                if (enable) {
                    onEvent("ESP32 subscribed")
                } else {
                    onEvent("ESP32 unsubscribed")
                }

                gattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    null
                )

            } else {
                gattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0,
                    null
                )
            }
        }
    }
    // Make this function suite to all enumatated commands in enum class CommandToHand
    fun sendMotorsVelocity(ty : Int, motorNum: Int, value: Float) {
        var msgConnect = "  "
        var connectionOK = true
        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        if (connectedDevice == null) {
            connectionOK = false
            msgConnect = "* "
        }
        var msgText: String

        if (ty == CommandToHand.VELOCITY.value) {
            msgText = "Velcity"
            motorsPacket[0] = CommandToHand.VELOCITY.value.toByte()
            motorsPacket[1] = motorNum.toByte()
            motorsPacket[2] = value.toInt().toByte()
        } else {
            msgText = "Command: Not motor velocity: $ty"
        }
        msgText = msgConnect + msgText + ": " + motorsPacket.joinToString(separator = ", ") { it.toString() }
        onEvent(msgText)

        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        if (!connectionOK) {
            return  // onEvent("No device connected") // no need event
        }
        val device = connectedDevice ?: return onEvent("No device connected")

        val characteristic = gattServer
            ?.getService(serviceUUID)
            ?.getCharacteristic(charUUID)

        characteristic?.value = motorsPacket
        notifyCharacteristic(device, characteristic)

    }
    fun sendMotorPosition(ty : Int, motorNum: Int, value: Float) {
        var msgConnect = "  "
        var connectionOK = true
        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        if (connectedDevice == null) {
            connectionOK = false
            msgConnect = "* "
        }
        var msgText : String

        if (ty == CommandToHand.POSITION.value) {
            msgText = "Position"
            motorsPacket[0] = CommandToHand.POSITION.value.toByte()
            motorsPacket[1] = motorNum.toByte()
            motorsPacket[2] = value.toInt().toByte()
        } else {
            msgText = "Command: Not motor position: $ty"
        }
        msgText = msgConnect + msgText + ": " + motorsPacket.joinToString(separator = ", ") { it.toString() }
        onEvent(msgText)

        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        if (!connectionOK) {
            return  // onEvent("No device connected") // no need event
        }
        val device = connectedDevice ?: return onEvent("No device connected")

        val characteristic = gattServer
            ?.getService(serviceUUID)
            ?.getCharacteristic(charUUID)

        characteristic?.value = motorsPacket
        notifyCharacteristic(device, characteristic)

    }
    fun sendNotification(ty : Int) {
        // Test for connection
        var msgConnect = "  "
        var connectionOK = true
        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        if (connectedDevice == null) {
            connectionOK = false
            msgConnect = "* "
        }
        var msgText: String
        when (ty) {
            CommandToHand.CALIBRATION.value -> {
                msgText = "Calibration"
                motorsPacket[0] = CommandToHand.CALIBRATION.value.toByte()
            }
            CommandToHand.STEP.value -> {
                msgText = "Step"
                motorsPacket[0] = CommandToHand.STEP.value.toByte()
            }
            CommandToHand.FORWARD.value -> {
                msgText = "Forward"
                motorsPacket[0] = CommandToHand.FORWARD.value.toByte()
            }
            CommandToHand.BACKWARD.value -> {
                msgText = "Backward"
                motorsPacket[0] = CommandToHand.BACKWARD.value.toByte()
            }
            CommandToHand.DISCONNECT.value -> {
                // disconnect the device
                //disconnect()
                println("Requested hard disconnect and server restart")

                msgText = "Disconnect"
                //motorsPacket[0] = CommandToHand.DISCONNECT.value.toByte()
                sendDisconnectNotification()
            }
            CommandToHand.STOP.value -> {
                msgText = "Stop"
                motorsPacket[0] = CommandToHand.STOP.value.toByte()
            }
            CommandToHand.POSITION.value -> {
                msgText = "Position"
                motorsPacket[0] = CommandToHand.POSITION.value.toByte()
            }
            else -> msgText = "Unknown Command"
        }
        msgText = msgConnect + msgText + ": " + motorsPacket.joinToString(separator = ", ") { it.toString() }
        onEvent(msgText)

        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        if (!connectionOK) {
            return  // onEvent("No device connected") // no need event
        }
        val device = connectedDevice ?: return onEvent("No device connected")

        val characteristic = gattServer
            ?.getService(serviceUUID)
            ?.getCharacteristic(charUUID)

        characteristic?.value = motorsPacket
        notifyCharacteristic(device, characteristic)

    }

    fun sendDisconnectNotification() {
        // Test for connection
        var msgConnect = "  "
        var connectionOK = true
        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        if (connectedDevice == null) {
            connectionOK = false
            msgConnect = "* "
        }
        var msgText: String
        msgText = "Disconnect"
        motorsPacket[0] = CommandToHand.DISCONNECT.value.toByte()
        msgText = msgConnect + msgText + ": " + motorsPacket.joinToString(separator = ", ") { it.toString() }
        onEvent(msgText)

        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        if (!connectionOK) {
            return  // onEvent("No device connected") // no need event
        }
        val device = connectedDevice ?: return onEvent("No device connected")
        val characteristic = gattServer
            ?.getService(serviceUUID)
            ?.getCharacteristic(charUUID)

        characteristic?.value = motorsPacket
        notifyCharacteristic(device, characteristic)
    }


    fun unsubscribeThenShutdown() {
        sendUnsubscribeNotification()
        sendDisconnectNotification()
        shutdown()
    }


    fun sendUnsubscribeNotification() {
        // App-level unsubscribe request before disconnecting.
        var msgConnect = "  "
        var connectionOK = true
        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        if (connectedDevice == null) {
            connectionOK = false
            msgConnect = "* "
        }

        var msgText = "Unsubscribe"
        motorsPacket[0] = CommandToHand.SUBSCRIBE.value.toByte()
        motorsPacket[1] = 0
        msgText = msgConnect + msgText + ": " + motorsPacket.joinToString(separator = ", ") { it.toString() }
        onEvent(msgText)

        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        if (!connectionOK) {
            return
        }

        val device = connectedDevice ?: return onEvent("No device connected")
        val characteristic = gattServer
            ?.getService(serviceUUID)
            ?.getCharacteristic(charUUID)

        characteristic?.value = motorsPacket
        notifyCharacteristic(device, characteristic)
    }

    fun sendNotificationSpecialCommand(ty : Int) {
        // Test for connection
        var msgConnect = "  "
        var connectionOK = true
        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        if (connectedDevice == null) {
            connectionOK = false
            msgConnect = "* "
        }
        var msgText : String
        if (ty in 21..28) {
            msgText = "Special: $ty"
            motorsPacket[0] = ty.toByte()
        } else {
            msgText = "Unknown Special Command: $ty"
        }
        msgText = msgConnect + msgText + ": " + motorsPacket.joinToString(separator = ", ") { it.toString() }
        onEvent(msgText)

        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        if (!connectionOK) {
            return  // onEvent("No device connected") // no need event
        }
        val device = connectedDevice ?: return onEvent("No device connected")

        val characteristic = gattServer
            ?.getService(serviceUUID)
            ?.getCharacteristic(charUUID)

        //characteristic?.value = motorsPacket
        notifyCharacteristicFIX(device, characteristic, motorsPacket)

    }
    fun sendCommandNotification(cmnd: Byte, iVal: Int = 0) {
        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        val device = connectedDevice ?: return onEvent("No device connected")
        val packet = ByteArray(3)
        packet[0] = 240.toByte()
        packet[1] = cmnd
        packet[2] = iVal.toByte()
        val characteristic = gattServer
            ?.getService(serviceUUID)
            ?.getCharacteristic(charUUID)

        characteristic?.value = packet
        notifyCharacteristic(device, characteristic)

        //val msgText0 = "Sent: ${data[0]}: ${data[1]},${data[2]},${data[3]},${data[4]}"
        val msgText = "Packet: " + packet.joinToString(separator = ", ") { it.toString() }

        onEvent(msgText)
    }
    fun sendNavigationNotification(screenId: Int) {
        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        val device = connectedDevice ?: return onEvent("No device connected")
        val packet = ByteArray(3)
        packet[0] = 244.toByte()
        packet[1] = screenId.toByte()
        packet[2] = 0
        val characteristic = gattServer
            ?.getService(serviceUUID)
            ?.getCharacteristic(charUUID)

        characteristic?.value = packet
        notifyCharacteristic(device, characteristic)

        val msgText = "Screen packet: " + packet.joinToString(separator = ", ") { it.toString() }
        onEvent(msgText)
    }
    fun setMotorFRWD() {
        movementType = 1
        onEvent("setMotorFRWD")
    }
    fun setMotorBKWD() {
        movementType = 2
        onEvent("setMotorBKWD")
    }
    fun absPosMotor() {
        movementType = 3
        onEvent("setPosMotor")
    }
    fun setMotorSTOP() {
        movementType = 0
        onEvent("setMotorSTOP")
    }
    @SuppressLint("MissingPermission")
    private fun restartGattServerForNextConnection() {
        if (!hasBluetoothConnectPermission() || !hasBluetoothAdvertisePermission()) {
            onEvent("Missing Bluetooth permission to restart server")
            return
        }

        runCatching {
            advertiser?.stopAdvertising(advertiseCallback)
        }.onFailure {
            onEvent("Stop advertise failed: ${it.message}")
        }
        advertiser = null

        runCatching {
            gattServer?.close()
        }.onFailure {
            onEvent("Gatt close failed: ${it.message}")
        }
        gattServer = null

        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            start(context)
            onEvent("Server restarted and ready for new connection")
        }, 300)
    }

    @SuppressLint("MissingPermission")
    fun shutdown() {
        mainHandler.removeCallbacksAndMessages(null)
        connectedDevice?.let { device ->
            runCatching {
                cancelDeviceConnection(device)
            }.onFailure {
                onEvent("Disconnect failed: ${it.message}")
            }
        }
        connectedDevice = null

        runCatching {
            advertiser?.stopAdvertising(advertiseCallback)
        }.onFailure {
            onEvent("Stop advertise failed: ${it.message}")
        }
        advertiser = null

        runCatching {
            gattServer?.close()
        }.onFailure {
            onEvent("Gatt close failed: ${it.message}")
        }
        gattServer = null
        onEvent("Server shutdown")
    }
}

