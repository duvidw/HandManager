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
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import java.util.UUID

//class NimbleServer(private val context: Context? = null) {
class NimbleServer(private val context: Context) {

    private val serviceUUID = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb" )
    private val charUUID = UUID.fromString("0000dcba-0000-1000-8000-00805f9b34fb")
    private val CCC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")


    //////////////////////////////////////////////////////////////////////////////
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        //val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    // This is the property you asked about
    private var advertiser: BluetoothLeAdvertiser? = null

    fun startAdvertising() {
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

    private var gattServer: BluetoothGattServer? = null
    private var connectedDevice: BluetoothDevice? = null

    private fun hasBluetoothConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
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

    val motors = ByteArray(5) {0}// index 0-7 val 0 no move motor st index
                                      // val 1 move forward
                                      // val 2 move backward
    val motorsAbsPos = ByteArray(16) {0}  // index:0 241

    val motorsAction = ByteArray(16) {0}// index: 0- 242
                                            // index: 1 - motor 1 direction 0 stop, 1 forward, 2 bacward
                                            // index: 2 - motor 1 nn move encoder count
                                            // indexed: 3,4; 5,6; 7,8 as above for motors 2,3,4

    val motorsPacket = ByteArray(8) {0}  // index:0 241


    var movementType: Byte = 0;

    var motorAbsPosVals = IntArray(5) { 0}

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun start(context: Context) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        gattServer = manager.openGattServer(context, callback)

        val characteristic = BluetoothGattCharacteristic(
            charUUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        val cccDescriptor = BluetoothGattDescriptor(
            CCC_UUID,
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
                onEvent("Connection OK: ${device.address} ${device.name}")
            } else {
                onEvent("Disconnected")
                connectedDevice = null
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
            if (descriptor.uuid == CCC_UUID) {

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
        var msgText = ""

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
            return  // onEvent("No device connected")} // no need event
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
        var msgText = ""

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
            return  // onEvent("No device connected")} // no need event
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
        var msgText = ""
        when (ty) {
            CommandToHand.CALIBRATION.value -> {
                msgText = "Calibration"
                motorsPacket[0] = CommandToHand.CALIBRATION.value.toByte()
            }
            CommandToHand.SUBSCRIBE.value -> {
                msgText = "Subscribe"
                motorsPacket[0] = CommandToHand.SUBSCRIBE.value.toByte()
            }
            CommandToHand.FORWARD.value -> {
                msgText = "Forward"
                motorsPacket[0] = CommandToHand.FORWARD.value.toByte()
            }
            CommandToHand.BACKWARD.value -> {
                msgText = "Backward"
                motorsPacket[0] = CommandToHand.BACKWARD.value.toByte()
            }
            CommandToHand.STEP.value -> {
                msgText = "Step"
                motorsPacket[0] = CommandToHand.STEP.value.toByte()
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
            return  // onEvent("No device connected")} // no need event
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
        var msgText = ""
        if (21 <= ty && ty <= 28) {
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
            return  // onEvent("No device connected")} // no need event
        }
        val device = connectedDevice ?: return onEvent("No device connected")

        val characteristic = gattServer
            ?.getService(serviceUUID)
            ?.getCharacteristic(charUUID)

        characteristic?.value = motorsPacket
        notifyCharacteristic(device, characteristic)

    }
    fun sendNotification_0(ty : Int) {
        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        val device = connectedDevice ?: return onEvent("No device connected")

        //val data = ByteArray(16) { 0x01 } // 16 bytes
        //val data = byteArrayOf(5, 1, 2, 3, 4)
        var msgText = ""
        val characteristic = gattServer
            ?.getService(serviceUUID)
            ?.getCharacteristic(charUUID)
        if (ty == 1) {
            motorsAbsPos[0] = 241.toByte()
            motorsAbsPos[1] = motorAbsPosVals[0].toByte()
            motorsAbsPos[2] = motorAbsPosVals[1].toByte()
            motorsAbsPos[3] = motorAbsPosVals[2].toByte()
            motorsAbsPos[4] = motorAbsPosVals[3].toByte()
            motorsAbsPos[5] = 60


            characteristic?.value = motorsAbsPos
            notifyCharacteristic(device, characteristic)
            msgText = "ty:1->" + motorsAbsPos.joinToString(separator = ", ") { it.toString() }
        }
        else if (ty == 2)
        {
            motorsAction[0] = 242.toByte()
            //M1
            motorsAction[1] = motors[0]
            motorsAction[2] = 30.toByte()
            //M2
            motorsAction[3] = motors[1]
            motorsAction[4] = 31.toByte()
            //M3
            motorsAction[5] = motors[2]
            motorsAction[6] = 32.toByte()
            //M4
            motorsAction[7] = motors[3]
            motorsAction[8] = 33.toByte()
//            //M5
//            motorsAction[9] = motors[4]
//            motorsAction[10] = 34.toByte()

            characteristic?.value = motorsAction
            notifyCharacteristic(device, characteristic)
            msgText = "ty:2->" + motorsAction.joinToString(separator = ", ") { it.toString() }

        }
        //val msgText0 = "Sent: ${data[0]}: ${data[1]},${data[2]},${data[3]},${data[4]}"

        onEvent(msgText)
    }
    fun sendCommandNotificationPos(cmnd: Byte, iPos: Float) {
        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        val device = connectedDevice ?: return onEvent("No device connected")
        val packet = ByteArray(3)
        packet[0] = 241.toByte()
        packet[1] = cmnd
        packet[2] = iPos.toInt().toByte()
        val characteristic = gattServer
            ?.getService(serviceUUID)
            ?.getCharacteristic(charUUID)

        characteristic?.value = packet
        notifyCharacteristic(device, characteristic)

        //val msgText0 = "Sent: ${data[0]}: ${data[1]},${data[2]},${data[3]},${data[4]}"
        val msgText = "Packet: " + packet.joinToString(separator = ", ") { it.toString() }

        onEvent(msgText)
    }
    fun sendCommandNotification4MotorPos(cmnd: Byte, iPos: Float) {
        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        val device = connectedDevice ?: return onEvent("No device connected")
        val packet = ByteArray(3)
        packet[0] = 242.toByte() // command type
        // set pos data to motors 1-4
        packet[1] = cmnd
        packet[2] = iPos.toInt().toByte()
        val characteristic = gattServer
            ?.getService(serviceUUID)
            ?.getCharacteristic(charUUID)

        characteristic?.value = packet
        notifyCharacteristic(device, characteristic)

        //val msgText0 = "Sent: ${data[0]}: ${data[1]},${data[2]},${data[3]},${data[4]}"
        val msgText = "Packet: " + packet.joinToString(separator = ", ") { it.toString() }

        onEvent(msgText)
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
    fun sendCommandNotificationToMotor(cmnd: Byte, iMotor: Int = 0) {
        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        val device = connectedDevice ?: return onEvent("No device connected")
        val packet = ByteArray(3)
        packet[0] = 243.toByte()
        packet[1] = cmnd
        packet[2] = (iMotor + 1).toByte()
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
    fun sendCommand(command: Byte) {
        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        val device = connectedDevice ?: return onEvent("No device connected")

        motors[command.toInt()] = 1
        val data = ByteArray(2)
        data[0] = 2
        data[1] = command
        val characteristic = gattServer
            ?.getService(serviceUUID)
            ?.getCharacteristic(charUUID)

        characteristic?.value = data
        notifyCharacteristic(device, characteristic)

        val msgText = "command: ${data[0]}: ${data[1]}"

        onEvent(msgText)
    }

    fun subscribe() {
        onEvent("Subscribe requested")
    }

    fun disconnect() {
        if (!hasBluetoothConnectPermission()) {
            return onEvent("Missing BLUETOOTH_CONNECT permission")
        }
        connectedDevice?.let {
            cancelDeviceConnection(it)
            onEvent("Disconnect requested")
        }
    }

    fun setMotor(index: Int) {
        motors[index] = movementType
        val msg = "Motor: ${index} - ${movementType}"
        onEvent(msg)
    }
    fun setMotorPos(index: Int, iPos : Int) {
        motors[index] = movementType
        val msg = "Motor: ${index} - ${movementType} - Pos: ${iPos}"
        onEvent(msg)
    }
    // Move motors to abs position 0-100
    fun setMotorsAbsPos(index: Int, iPos: Int)
    {
        // Index: 1-4
        motorsAbsPos[index] = iPos.toByte()
    }
    // Move the motors FWD/BWD iAction encoders steps
    fun setMotorsAction(index: Int, moveType: Int, iSteps: Int)
    {
        motorsAction[index*2-1] = moveType.toByte()
        motorsAction[index*2] = iSteps.toByte()
    }
    fun setMotorsPos()
    {

    }
}

