@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package xmodem.protocol.receiver

import ru.pocketbyte.kydra.log.debug
import ru.pocketbyte.kydra.log.info
import xmodem.log.Log
import xmodem.ASCII
import xmodem.asHex
import xmodem.com.ComPort
import xmodem.files.FileOutput
import xmodem.protocol.XmodemCancelException
import xmodem.protocol.Xmodem

class XmodemReceiver(
    com: String,
    private val timeoutMs: UInt,
    private val retries: Int,
    private val config: Xmodem.Config
) {

    private val comPort = ComPort(com)
    private var expectedPacketNumber: UByte = 1u
    private var totalPacketCount: Int = 0
    private var retriesCounter = 0

    private var state: State = State.NoInit()
        set(value) {
            field = if (field is State.NoInit && value !is State.AcceptPacket && value !is State.ExpectedPacket) {
                State.NoInit(value)
            } else {
                value
            }

            Log.debug(generateTag(), "Status changed: $field")
        }

    private fun generateTag() = "[$state|${retriesCounter}|$expectedPacketNumber]"

    fun receive(file: FileOutput) {

        Log.info(config.toString())
        Log.info("Receiver config: {Retries limit: $retries, timeout: $timeoutMs ms}")

        Xmodem.setupAndOpenCom(comPort) {
            ReadIntervalTimeout = 10u
            ReadTotalTimeoutConstant = timeoutMs
            ReadTotalTimeoutMultiplier = 1u
        }

        comPort.write(config.initByte)

        printStatus()

        do {

            val controlByte = comPort.readOrNull(1)?.firstOrNull()

            state = checkControlByte(controlByte)
            printStatus()

            if (state is State.ExpectedPacket) {

                val packet = readPacketFromComPort()

                state = checkPacket(packet)
                printStatus()

                if (state is State.AcceptPacket && state !is State.AcceptPacketDuplicate) {
                    expectedPacketNumber++
                    totalPacketCount++
                    val data = readDataFromPacket(packet)
                    file.write(data)
                }
            }

            val answer = when (state) {
                is State.NoInit -> config.initByte
                is State.AcceptPacket -> ASCII.ACK
                is State.Cancel -> ASCII.CAN
                is State.EOT -> ASCII.ACK
                else -> ASCII.NAK
            }

            comPort.write(answer)

            handleRetriesCount()
            printStatus()

        } while (!state.isFinishing)

        Log.updateStatus("[End of transmission, Total packets: $totalPacketCount, State: $state]")

        comPort.close()

        if (state is State.Cancel) {
            throw XmodemCancelException(state as State.Cancel)
        }

    }

    private fun printStatus() {
        Log.updateStatus("[Total packets:  $totalPacketCount, Expected packet number: $expectedPacketNumber," +
                " Retries: $retriesCounter, State: $state]")

    }

    private fun checkControlByte(byte: Byte?) = when (byte) {
        config.headerByte -> {
            State.ExpectedPacket
        }
        ASCII.EOT -> {
            State.EOT
        }
        ASCII.CAN -> {
            State.Cancel("Canceled by the sender!")
        }
        null -> {
            State.Timeout
        }
        else -> {
            State.RejectPacket("Bad byte! Received: ${byte.asHex()}")
        }
    }

    private fun readPacketFromComPort(): ByteArray {
        var requiredBytesCount = config.packetSize - 1
        val packetBuilder = mutableListOf(config.headerByte)

        while (requiredBytesCount > 0) {

            val part = comPort.readOrNull(requiredBytesCount)

            if (part != null) {
                packetBuilder.addAll(part.toList())
                requiredBytesCount -= part.size
            }
        }

        return packetBuilder.toByteArray()
    }

    private fun handleRetriesCount() {

        if (state is State.AcceptPacket || state.isFinishing) {
            retriesCounter = 0
        } else {
            retriesCounter++
        }

        if (retriesCounter > retries) {
            retriesCounter = 0
            state = State.Cancel("Retries limit reached! Limit: ${retries}")
        }
    }

    private fun readDataFromPacket(packet: ByteArray): ByteArray {
        return packet.drop(3)
            .dropLast(2)
            .dropLastWhile {
                it == ASCII.SUB
            }.toByteArray()
    }

    private fun checkPacket(block: ByteArray?): State {

        return when {
            block == null -> {
                State.Timeout
            }
            block.size != config.packetSize -> {
                State.RejectPacket("Invalid packet length! Expected: ${config.packetSize} Received: ${block.size}")
            }
            !checkPacketNumber(block) -> {
                State.RejectPacket("Bit error in packet number! Received: [${block[1].asHex()}|${block[2].asHex()}]")
            }
            getPacketNumberDiff(block) == -1 -> {
                State.AcceptPacketDuplicate()
            }
            getPacketNumberDiff(block) != 0 -> {
                State.Cancel("Transmission out of sync!")
            }
            !config.checksum.verify(block.drop(3).toByteArray()) -> {
                State.RejectPacket("Bad checksum!")
            }
            else -> {
                State.AcceptPacket()
            }
        }
    }

    private fun checkPacketNumber(block: ByteArray): Boolean {
        val packetNumber = block[1].toUByte()
        val packetNumberFill = block[2].toUByte()

        return packetNumber + packetNumberFill == 0xFFu
    }

    private fun getPacketNumberDiff(block: ByteArray): Int {
        val packetNumber = block[1].toUByte()

        return expectedPacketNumber.toInt() - packetNumber.toInt()
    }

}