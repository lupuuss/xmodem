@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package xmodem.protocol

import platform.windows.CBR_9600
import platform.windows.COMMTIMEOUTS
import platform.windows.NOPARITY
import platform.windows.ONESTOPBIT
import xmodem.ASCII
import xmodem.checksum.Checksum
import xmodem.com.ComPort

object Xmodem {

    fun setupAndOpenCom(comPort: ComPort, timeoutEditor: (COMMTIMEOUTS.() -> Unit)?) {
        try {

            comPort.editTimeouts(timeoutEditor ?: {})

            comPort.editDCB {
                BaudRate = CBR_9600.toUInt()
                ByteSize = 8u
                Parity = NOPARITY.toUByte()
                StopBits = ONESTOPBIT.toUByte()
            }

            comPort.open()
            comPort.fullPurge()

        } catch (e: Exception) {

            comPort.close()
            throw XmodemIOException(e)
        }
    }

    const val initByteCrc16 = ASCII.C
    const val initByteSum8 = ASCII.NAK
    const val dataSize = 128

    class Config(
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
                    initByte = initByteCrc16
                    packetSize = 133
                }
                Checksum.Type.SUM8 -> {
                    checksum = Checksum.getSum8()
                    initByte = initByteSum8
                    packetSize = 132
                }
            }
        }

        override fun toString(): String {
            return "Config {checksum: $checksumType, initByte: $initByte," +
                    " packetSize: $packetSize, headerByte: $headerByte}"
        }

        companion object {

            fun getBasedOnInitByte(initByte: Byte?) = when (initByte) {
                ASCII.C -> Config(Checksum.Type.CRC16)
                ASCII.NAK -> Config(Checksum.Type.SUM8)
                else -> null
            }
        }
    }
}