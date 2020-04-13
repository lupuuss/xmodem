@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package xmodem.protocol

import xmodem.ASCII
import xmodem.checksum.Checksum
import xmodem.com.ComPort

class XmodemReceiver(
    com: String,
    private val checksumType: Checksum.Type,
    private val timeoutMs: UInt = 1_000u,
    private val timeoutRepeat: Int = 10,
    private val verbose: Boolean = true
) {
    private val checksum: Checksum
    private val initByte: Byte
    private val packetSize: Int
    private val headerByte: Byte

    init {
        when(checksumType) {
            Checksum.Type.CRC16 -> {
                checksum = Checksum.getCrc16(Checksum.xmodem16)
                initByte = ASCII.C
                headerByte = ASCII.C
                packetSize = 133
            }
            Checksum.Type.SUM8 -> {
                checksum = Checksum.getSum8()
                initByte = ASCII.NAK
                headerByte = ASCII.SOH
                packetSize = 132
            }
        }
    }

    private val comPort = ComPort(com)
    private var expectedPacketNumber: UByte = 1u
    private var repeatCounter = 0

    private fun checkPacketNumber(block: ByteArray): Boolean {
        val packetNumber = block[1].toUByte()
        val packetNumberFill = block[2].toUByte()

        return packetNumber == expectedPacketNumber && packetNumber + packetNumberFill == 0xFFu
    }

    fun receive() {

        try {

            comPort.editTimeouts {
                ReadIntervalTimeout = timeoutMs
                ReadTotalTimeoutConstant = 0u
                ReadTotalTimeoutMultiplier = 0u
            }

            comPort.open()

        } catch (e: Exception) {

            comPort.close()
            throw XmodemIOException(e);
        }

        var answer = initByte

        while (repeatCounter < timeoutRepeat) {

            comPort.write(answer)

            val block = comPort.readOrNull(packetSize)

            if (block != null) {
                repeatCounter = 0


            } else {
                println("Timeout $repeatCounter")
                repeatCounter++
            }

        }

    }

}