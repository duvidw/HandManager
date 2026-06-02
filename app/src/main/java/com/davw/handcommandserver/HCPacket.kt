package com.davw.handcommandserver

data class BlePacket32(
    val bytes: ByteArray
) {
    init {
        require(bytes.size == 32) { "Packet must be exactly 32 bytes" }
    }
}


data class HCPacket (
    val cmdType: Byte = 0,
    val let: Byte = 0,
    val buff: ByteArray

) {
    init {
        require(buff.size == 32) { "Packet must be exactly 32 bytes" }
    }
}