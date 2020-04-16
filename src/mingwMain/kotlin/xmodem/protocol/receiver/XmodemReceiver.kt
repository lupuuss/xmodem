@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package xmodem.protocol.receiver

import ru.pocketbyte.kydra.log.debug
import ru.pocketbyte.kydra.log.info
import xmodem.log.Log
import xmodem.ASCII
import xmodem.asHex
import xmodem.checksum.Checksum
import xmodem.com.ComPort
import xmodem.files.FileOutput
import xmodem.protocol.XmodemCancelException
import xmodem.protocol.XmodemIOException

class XmodemReceiver(
    private val com: String,
    private val checksumType: Checksum.Type,
    private val timeoutMs: UInt,
    private val retries: Int
) {

    private val checksum: Checksum
    private val initByte: Byte
    private val packetSize: Int
    private val headerByte: Byte = ASCII.SOH

    init {
        when(checksumType) {
            Checksum.Type.CRC16 -> {
                checksum = Checksum.getCrc16(Checksum.xmodem16)
                initByte = ASCII.C
                packetSize = 133
            }
            Checksum.Type.SUM8 -> {
                checksum = Checksum.getSum8()
                initByte = ASCII.NAK
                packetSize = 132
            }
        }
    }

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

        Log.info("Config {timeout: $timeoutMs, retries: $retries, initByte: ${initByte.asHex()}," +
                " Packet size: $packetSize, checksum: $checksumType, com: $com}")

        try {

            comPort.editTimeouts {
                ReadIntervalTimeout = 10u
                ReadTotalTimeoutConstant = timeoutMs
                ReadTotalTimeoutMultiplier = 1u
            }

            comPort.open()

        } catch (e: Exception) {

            comPort.close()
            throw XmodemIOException(e)
        }

        comPort.write(initByte)

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
                is State.NoInit -> initByte
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

    private fun readPacketFromComPort(): ByteArray {
        var requiredBytesCount = packetSize - 1
        val packetBuilder = mutableListOf(headerByte)

        while (requiredBytesCount > 0) {

            val part = comPort.readOrNull(requiredBytesCount)

            if (part != null) {
                packetBuilder.addAll(part.toList())
                requiredBytesCount -= part.size
            }
        }

        return packetBuilder.toByteArray()
    }

    private fun printStatus() {
        Log.updateStatus("[Total packets:  $totalPacketCount, Expected packet number: $expectedPacketNumber," +
                " Retries: $retriesCounter, State: $state]")

    }

    private fun handleRetriesCount() {

        if (state is State.AcceptPacket || state.isFinishing) {
            retriesCounter = 0
        } else {
            retriesCounter++
        }

        if (retriesCounter > retries) {
            retriesCounter = 0
            state = State.Cancel("Retries limit reached! Limit: $retries")
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
            block.size != packetSize -> {
                State.RejectPacket("Invalid packet length! Expected: $packetSize Received: ${block.size}")
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
            !checksum.verify(block.drop(3).toByteArray()) -> {
                State.RejectPacket("Bad checksum!")
            }
            else -> {
                State.AcceptPacket()
            }
        }
    }

    private fun checkControlByte(byte: Byte?) = when (byte) {
            headerByte -> {
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