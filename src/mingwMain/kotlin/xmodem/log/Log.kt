package xmodem.log

import ru.pocketbyte.kydra.log.KydraLog
import ru.pocketbyte.kydra.log.LogLevel

import ru.pocketbyte.kydra.log.LoggerWrapper

@ThreadLocal
object Log : LoggerWrapper(KydraLog.logger) {

    private var status: String = "Status: -"

    override fun log(level: LogLevel, tag: String?, function: () -> String) {
        cleanStatus()
        super.log(level, tag, function)
        printStatus()
    }

    override fun log(level: LogLevel, tag: String?, message: String) {
        cleanStatus()
        super.log(level, tag, message)
        printStatus()
    }

    override fun log(level: LogLevel, tag: String?, exception: Throwable) {
        cleanStatus()
        super.log(level, tag, exception)
        printStatus()
    }

    private fun cleanStatus() {
        val spaces = generateSequence { ' ' }.take(status.length).joinToString(separator = "")
        print("\r$spaces\r")
    }

    private fun printStatus() {
        print(status)
    }

    fun updateStatus(status: String) {

        cleanStatus()
        this.status = "Status: $status"
        printStatus()
    }
}