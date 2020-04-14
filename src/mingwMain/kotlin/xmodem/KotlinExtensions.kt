@file:Suppress("EXPERIMENTAL_API_USAGE")

package xmodem

object ASCII {
    const val SOH: Byte = 0x01
    const val EOT: Byte = 0x04
    const val ACK: Byte = 0x06
    const val NAK: Byte = 0x15
    const val CAN: Byte = 0x18
    const val C: Byte = 0x43
}

infix fun Short.shl(rhs: Int): Short = (this.toInt() shl rhs).toShort()

infix fun Short.shr(rhs: Int): Short = (this.toInt() shr rhs).toShort()

fun Short.splitInHalf(): ByteArray = byteArrayOf((this shr 8).toByte(), this.toByte())

fun Byte.asHex(): String = this.toUByte().asHex()

fun UByte.asHex(): String {

    val words = "0123456789ABCDEF"

    return charArrayOf('0', 'x', words[this.toInt() / 16], words[this.toInt() % 16]).concatToString()
}