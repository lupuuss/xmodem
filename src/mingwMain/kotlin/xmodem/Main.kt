package xmodem

import ru.pocketbyte.kydra.log.KydraLog
import ru.pocketbyte.kydra.log.LogLevel
import ru.pocketbyte.kydra.log.error
import ru.pocketbyte.kydra.log.initDefault
import xmodem.checksum.Checksum
import xmodem.protocol.XmodemException
import xmodem.protocol.XmodemReceiver

fun main() {

    KydraLog.initDefault(LogLevel.WARNING)

    try {
        val receiver = XmodemReceiver("COM3", Checksum.Type.CRC16)
        receiver.receive()
    } catch (e: XmodemException) {
        KydraLog.error(e)
    }
}