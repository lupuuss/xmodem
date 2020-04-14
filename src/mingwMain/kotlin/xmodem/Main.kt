package xmodem

import xmodem.checksum.Checksum
import xmodem.protocol.XmodemReceiver

fun main() {
    val receiver = XmodemReceiver("COM3", Checksum.Type.CRC16)

    receiver.receive()
}