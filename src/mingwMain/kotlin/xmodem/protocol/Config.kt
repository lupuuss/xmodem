@file:Suppress("EXPERIMENTAL_API_USAGE")

package xmodem.protocol

import xmodem.ASCII
import xmodem.checksum.Checksum

open class BasicXmodemConfig(
    val com: String,
    val timeoutMs: UInt,
    val retries: Int
) {
    fun extend(checksum: Checksum.Type): XmodemConfig {
        return XmodemConfig(this, checksum)
    }

    fun extendOrNull(initByte: Byte?): XmodemConfig? = when (initByte) {
        ASCII.C -> extend(Checksum.Type.CRC16)
        ASCII.NAK -> extend(Checksum.Type.SUM8)
        else -> null
    }

}

class XmodemConfig(
    com: String,
    timeoutMs: UInt,
    retries: Int,
    checksumType: Checksum.Type
) : BasicXmodemConfig(com, timeoutMs, retries) {

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
        basic: BasicXmodemConfig,
        checksum: Checksum.Type
    ) : this(basic.com, basic.timeoutMs, basic.retries, checksum)
}