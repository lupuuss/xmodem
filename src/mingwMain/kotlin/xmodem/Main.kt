package xmodem

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.enum
import ru.pocketbyte.kydra.log.KydraLog
import ru.pocketbyte.kydra.log.LogLevel
import ru.pocketbyte.kydra.log.error
import ru.pocketbyte.kydra.log.initDefault
import xmodem.checksum.Checksum
import xmodem.files.FileOutput
import xmodem.protocol.XmodemException
import xmodem.protocol.XmodemReceiver

class Main : CliktCommand(
    printHelpOnEmptyArgs = true
) {

    init {
        context {
            helpFormatter = CliktHelpFormatter(showDefaultValues = true)
        }
    }

    private val logLevel by option(help = "Switches between log levels.")
        .switch(*LogLevel.values().map { "--${it.name.toLowerCase()}" to it }.toTypedArray())
        .default(LogLevel.INFO)

    private val comPort by option("--port", "-p")
        .default("COM3")
        .validate {
            require(it.matches("COM\\d\\d?".toRegex()))
        }

    private val checksum by option(help = "Switches between checksum type.")
        .switch(
            "--crc16" to Checksum.Type.CRC16,
            "--sum8" to Checksum.Type.SUM8
        ).default(Checksum.Type.CRC16)

    private val receiver by option()
        .switch("--receiver" to true, "--sender" to false)
        .default(true)

    private val ioPath by option()
        .prompt("File path: ")
        .validate { it.isNotEmpty() }

    override fun run() {

        KydraLog.initDefault(logLevel)

        try {

            val fileOutput = FileOutput(ioPath)

            fileOutput.open()

            if (receiver) {
                XmodemReceiver(comPort, checksum).receive(fileOutput)
            }

            fileOutput.close()

        } catch (e: XmodemException) {
            KydraLog.error(e)
        }
    }
}

fun main(args: Array<String>) = Main().main(args)