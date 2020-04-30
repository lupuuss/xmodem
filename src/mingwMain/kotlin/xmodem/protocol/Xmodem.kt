@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package xmodem.protocol

import xmodem.ASCII

object Xmodem {

    const val initByteCrc16 = ASCII.C
    const val initByteSum8 = ASCII.NAK
    const val dataSize = 128
}