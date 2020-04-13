package xmodem.checksum

import xmodem.shl
import xmodem.shr
import xmodem.splitInHalf
import kotlin.experimental.and
import kotlin.experimental.xor

class ChecksumCrc16(private val poly: Short) : Checksum {

    private fun calculateCrc(input: ByteArray, append: ByteArray? = null): ByteArray {

        var crc: Short = 0

        val bytes = if (append != null) input + append else input

        for (byte in bytes) {

            crc = crc xor (byte.toShort() shl 8)

            for (i in 0..7) {

                crc = if (crc and 0x8000.toShort() != 0.toShort()) {

                    (crc shl 1) xor poly

                } else {
                    crc shl 1
                }
            }
        }

        return crc.splitInHalf()
    }

    override fun calculate(bytes: ByteArray): ByteArray {

        return calculateCrc(bytes)
    }

    override fun verify(bytes: ByteArray, checksum: ByteArray): Boolean {

        return calculateCrc(bytes, checksum).contentEquals(byteArrayOf(0, 0))
    }

    override fun verify(bytes: ByteArray): Boolean {
        return calculateCrc(bytes).contentEquals(byteArrayOf(0, 0))
    }
}