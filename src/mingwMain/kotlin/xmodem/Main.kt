@file:Suppress("EXPERIMENTAL_API_USAGE")

package xmodem

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import ru.pocketbyte.kydra.log.*
import xmodem.checksum.Checksum
import xmodem.com.ComConfig
import xmodem.files.FileInput
import xmodem.files.FileOutput
import xmodem.log.Log
import xmodem.protocol.Xmodem
import xmodem.protocol.XmodemException
import xmodem.protocol.receiver.XmodemReceiver
import xmodem.protocol.sender.XmodemSender

abstract class XmodemTask(name: String, help: String) : CliktCommand(name = name, help = help)
{

    init {
        context {
            helpFormatter = CliktHelpFormatter(showDefaultValues = true)
        }
    }

    protected val path by argument("Path to file.")
        .validate { it.isNotEmpty() }

    protected val logLevel by option(help = "Switches between log levels.")
        .switch(*LogLevel.values().map { "--${it.name.toLowerCase()}" to it }.toTypedArray())
        .default(LogLevel.INFO)

    protected val stack by option(help = "If the flag is present, stack trace is printed.")
        .flag(default = false)

    protected val comPort by option("--port", "-p")
        .default("COM3")
        .validate {
            val regex = "COM\\d\\d?".toRegex()
            require(it.matches(regex)) {
                "Com port name must match following regex: $regex"
            }
        }

    private val rate by option(help = "Sets com port speed rate (in bps).")
        .switch(*CBR.values().map { "--${it.name.toLowerCase().substringAfter("_")}" to it}.toTypedArray())
        .default(CBR.BPS_9600)

    private val stopBits by option(help = "Sets number of stop bits.")
        .switch(*StopBits.values().map { "--stop-${it.name.toLowerCase()}" to it }.toTypedArray())
        .default(StopBits.ONE)

    private val dataBits by option()
        .int()
        .default(8)
        .validate { require(it in 5..8) { "The number of data bits must be 5 to 8 bits." } }

    private val parity by option(help = "Sets parity check mode.")
        .switch(*Parity.values().map { "--parity-${it.name.toLowerCase()}" to it }.toTypedArray())
        .default(Parity.NO)

    protected val comConfig by lazy { ComConfig(comPort, rate, dataBits.toUByte(), parity, stopBits) }

    fun initKydraLogger() {
        KydraLog.initDefault(level = logLevel)

        KydraLog.log(logLevel, "Chosen log level: $logLevel")
    }
}

class XmodemReceiveTask : XmodemTask("receive", "Receives a file via XMODEM protocol.") {


    private val retries by option(help = "Sets number of retries after repeating errors.")
        .int()
        .default(10)
        .validate { require(it > 0) { "Retries number must be positive!" } }

    private val timeout by option(help = "Sets amount of time to wait for sender in ms.")
        .int()
        .default(10_000)
        .validate { require(it > 0) { "Timeout must be positive number!" } }

    private val checksum by option(help = "Switches between checksum type.")
        .switch(
            "--crc16" to Checksum.Type.CRC16,
            "--sum8" to Checksum.Type.SUM8
        ).default(Checksum.Type.CRC16)

    override fun run() {

        initKydraLogger()

        try {

            val fileOutput = FileOutput(path, FileOutput.Mode.Binary)

            fileOutput.open()

            val config = Xmodem.Config(checksum)

            XmodemReceiver(comConfig, timeout.toUInt(), retries, config).receive(fileOutput)

            fileOutput.close()

        } catch (e: XmodemException) {

            if (stack) {
                e.printStackTrace()
            } else {
                Log.error(e.toString())
            }
        }
    }
}

class XmodemSendTask: XmodemTask("send", "Sends the passed file via XMODEM protocol.") {
    override fun run() {

        initKydraLogger()

        try {

            val file = FileInput(path, FileInput.Mode.Binary)

            file.open()

            XmodemSender(comConfig).send(file)

            file.close()

        } catch (e: Exception) {
            if (stack) {
                e.printStackTrace()
            } else {
                Log.error(e.toString())
            }
        }

    }
}

class Main : CliktCommand(
    printHelpOnEmptyArgs = true,
    name = "xmodem"
) {
    override fun run() {
        if (currentContext.invokedSubcommand is XmodemReceiveTask) {
            println(">>> XMODEM RECEIVER STARTED")
        } else if (currentContext.invokedSubcommand is XmodemSendTask) {
            println(">>> XMODEM SENDER STARTED")
        }
    }
}

fun main(args: Array<String>) = Main().subcommands(XmodemReceiveTask(), XmodemSendTask()).main(args)