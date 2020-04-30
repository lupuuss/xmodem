@file:Suppress("EXPERIMENTAL_API_USAGE")

package xmodem.com

import xmodem.CBR
import xmodem.Parity
import xmodem.StopBits

data class ComConfig(
    val port: String,
    val cbrRate: CBR,
    val byteSize: UByte,
    val parity: Parity,
    val stopBits: StopBits
)