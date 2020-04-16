@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package xmodem.protocol

import platform.windows.CBR_9600
import platform.windows.NOPARITY
import platform.windows.ONESTOPBIT
import xmodem.ASCII
import xmodem.checksum.Checksum
import xmodem.com.ComPort

object Xmodem {

    fun setupAndOpenCom(comPort: ComPort, config: BasicConfig) {
        try {

            comPort.editTimeouts {
                ReadIntervalTimeout = 10u
                ReadTotalTimeoutConstant = config.timeoutMs
                ReadTotalTimeoutMultiplier = 1u
            }

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

    open class BasicConfig(
        val com: String,
        val timeoutMs: UInt,
        val retries: Int
    ) {
        fun extend(checksum: Checksum.Type): Config {
            return Config(this, checksum)
        }

        fun extendOrNull(initByte: Byte?): Config? = when (initByte) {
            ASCII.C -> extend(Checksum.Type.CRC16)
            ASCII.NAK -> extend(Checksum.Type.SUM8)
            else -> null
        }
    }

    class Config(
        com: String,
        timeoutMs: UInt,
        retries: Int,
        checksumType: Checksum.Type
    ) : BasicConfig(com, timeoutMs, retries) {

        val checksum: Checksum
        val initByte: Byte
        val packetSize: Int
        val headerByte: Byte = ASCII.SOH
        private val xmodemPolyCrc16: Short = 0x1021

        init {
            when(checksumType) {
                Checksum.Type.CRC16 -> {
                    checksum = Checksum.getCrc16(xmodemPolyCrc16)
                    initByte = ASCII.C
                    packetSize = 133
                }
                Checksum.Type.SUM8 -> {
                    checksum = Checksum.getSum8()
                    initByte = ASCII.NAK
                    packetSize = 132
                }
            }
        }

        constructor(
            basic: BasicConfig,
            checksum: Checksum.Type
        ) : this(basic.com, basic.timeoutMs, basic.retries, checksum)
    }
}