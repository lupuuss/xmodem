package xmodem.protocol

import xmodem.ASCII
import xmodem.asHex
import xmodem.checksum.Checksum

class XmodemConfig(
    val checksumType: Checksum.Type
) {

    val checksum: Checksum
    val initByte: Byte
    val packetSize: Int
    val headerByte: Byte = ASCII.SOH
    private val xmodemPolyCrc16: Short = 0x1021

    init {
        when(checksumType) {
            Checksum.Type.CRC16 -> {
                checksum = Checksum.getCrc16(xmodemPolyCrc16)
                initByte = Xmodem.initByteCrc16
                packetSize = 133
            }
            Checksum.Type.SUM8 -> {
                checksum = Checksum.getSum8()
                initByte = Xmodem.initByteSum8
                packetSize = 132
            }
        }
    }

    override fun toString(): String {
        return "XmodemConfig(checksum=$checksumType, initByte=${initByte.asHex()}," +
                " packetSize=$packetSize, headerByte=${headerByte.asHex()})"
    }

    companion object {

        fun getBasedOnInitByte(initByte: Byte?) = when (initByte) {
            ASCII.C -> XmodemConfig(Checksum.Type.CRC16)
            ASCII.NAK -> XmodemConfig(Checksum.Type.SUM8)
            else -> null
        }
    }
}