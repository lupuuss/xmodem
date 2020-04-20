@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package xmodem.protocol.sender

import ru.pocketbyte.kydra.log.debug
import ru.pocketbyte.kydra.log.info
import xmodem.ASCII
import xmodem.com.ComPort
import xmodem.cycle
import xmodem.files.FileInput
import xmodem.log.Log
import xmodem.protocol.Xmodem
import xmodem.protocol.XmodemCancelException
import xmodem.repeatByte
import kotlin.math.roundToInt

enum class State {
    NoInit, Canceled, Accepted, Rejected, UnrecognizedByte, WaitingForAnswer, EOT, Finish
}

class XmodemSender(
    com: String
) {
    private val progressBarSize = 50

    private val comPort = ComPort(com)

    private var packetNumber: Byte = 1

    private var packetsCounter = 0u
    private var totalPackets = 0u

    private val animIter = cycle(".  ", ".. ", "...", ".. ", ".  ").iterator()

    private var currentPacket: ByteArray? = null

    private var state = State.NoInit

    fun send(file: FileInput) {

        Xmodem.setupAndOpenCom(comPort) {
            ReadIntervalTimeout = 10u
            ReadTotalTimeoutConstant = 200u
            ReadTotalTimeoutMultiplier = 1u
        }

        val config = waitForInitConfig()
        val fileBytes = file.size().toUInt()

        totalPackets = fileBytes / Xmodem.dataSize.toUInt() +
                if (fileBytes % Xmodem.dataSize.toUInt() == 0u) 0u else 1u

        Log.info(config.toString())
        Log.info("File size: $fileBytes bytes | Packets: $totalPackets")

        state = State.Accepted

        do {

            if (state == State.Accepted || state == State.Rejected) {

                if (currentPacket == null) {

                    val data = file.read(Xmodem.dataSize)
                    currentPacket = makePacket(data, config)
                }

                comPort.write(currentPacket!!)
                state = State.WaitingForAnswer
            }

            printStatus()

            val controlByte = comPort.readOrNull(1)?.firstOrNull()

            state = when (controlByte) {
                null -> State.WaitingForAnswer
                ASCII.ACK -> State.Accepted
                ASCII.NAK, config.initByte -> State.Rejected
                ASCII.CAN -> State.Canceled
                else -> State.UnrecognizedByte
            }

            if (state == State.Accepted) {
                packetNumber++
                packetsCounter++
                currentPacket = null
            }

            printStatus()

        } while (packetsCounter < totalPackets && state != State.Canceled)

        if (state == State.Canceled) {
            comPort.close()
            throw XmodemCancelException("Transmission canceled by receiver!")
        }

        state = State.Finish

        finishTransmission()

        comPort.close()
    }

    private fun printStatus() {
        Log.debug("[$state | $packetsCounter/$totalPackets | #$packetNumber]")
        Log.updateStatus("${progressBar()} State: $state")
    }

    private fun progressBar(): String {

        val progress = ((packetsCounter.toDouble() / totalPackets.toDouble()) * progressBarSize).roundToInt()

        val progressString = String(CharArray(progress) { '=' })
        val spaces = String(CharArray(progressBarSize - progress) { ' ' })

        return "[$progressString>$spaces] [$packetsCounter/$totalPackets]"
    }

    private fun waitForInitConfig(): Xmodem.Config {

        var config: Xmodem.Config?

        do {

            val receivedByte = comPort.readOrNull(1)?.firstOrNull()

            config = Xmodem.Config.getBasedOnInitByte(receivedByte)

            Log.updateStatus("Waiting for config initialization${animIter.next()}")

        } while (config == null)

        Log.info("Initialization finished!")
        Log.updateStatus("Initialization finished!")

        return config
    }

    private fun makePacket(rawData: ByteArray, config: Xmodem.Config): ByteArray {

        val packet = ByteArray(3 + Xmodem.dataSize + config.checksumType.byteSize)

        val data = rawData + repeatByte(ASCII.SUB, Xmodem.dataSize - rawData.size)

        packet[0] = config.headerByte
        packet[1] = packetNumber
        packet[2] = (0xFF - packetNumber).toByte()

        for (i in data.indices) {
            packet[i + 3] = data[i]
        }

        val checksumValue = config.checksum.calculate(data)

        for (i in checksumValue.indices) {
            packet[3 + Xmodem.dataSize + i] = checksumValue[i]
        }

        return packet
    }

    private fun finishTransmission() {

        do {

            if (state == State.Finish) {
                comPort.write(ASCII.EOT)
                state = State.WaitingForAnswer
            }

            val controlByte = comPort.readOrNull(1)?.firstOrNull()

            if (controlByte != null && controlByte == ASCII.ACK) {
                state = State.EOT
            } else if (controlByte != null && controlByte == ASCII.NAK) {
                state = State.Finish
            } else if (controlByte != null && controlByte == ASCII.CAN) {
                state = State.Canceled
            }

            Log.updateStatus("Finishing transmission${animIter.next()}")

        } while (state != State.EOT && state != State.Canceled)

    }

}