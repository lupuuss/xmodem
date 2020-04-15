@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package xmodem.protocol.receiver

import ru.pocketbyte.kydra.log.KydraLog
import ru.pocketbyte.kydra.log.info
import ru.pocketbyte.kydra.log.warn
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
    
    private val bufferSize = 256
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
    private var retriesCounter = 0

    private var state: State = State.NoInit
        set(value) {
            field = value
            KydraLog.info(generateTag(), "State changed to: $value")
        }

    private fun generateTag() = "[${state.name}|${retriesCounter}|$expectedPacketNumber]"

    fun receive(file: FileOutput) {

        KydraLog.info("Config {timeout: $timeoutMs, retries: $retries, initByte: ${initByte.asHex()}," +
                " Packet size: $packetSize, checksum: $checksumType, com: $com}")

        try {

            comPort.editTimeouts {
                ReadIntervalTimeout = 50u
                ReadTotalTimeoutConstant = timeoutMs
                ReadTotalTimeoutMultiplier = 1u
            }

            comPort.open()

        } catch (e: Exception) {

            comPort.close()
            throw XmodemIOException(e)
        }

        comPort.write(initByte)

        do {

            val block = clearFrame(comPort.readOrNull(bufferSize))

            if (block != null) {

                val data = parsePacket(block)

                if (data != null) {
                    file.write(data)
                }

            } else {
                KydraLog.warn(generateTag(), "Timeout!")
            }

            val answer = when (state) {
                is State.NoInit -> initByte
                is State.AcceptPacket -> ASCII.ACK
                is State.RejectPacket -> ASCII.NAK
                is State.Cancel -> ASCII.CAN
                is State.EOT -> ASCII.ACK
            }

            if (state is State.AcceptPacket || state.isFinishing) {
                retriesCounter = 0
            } else {
                retriesCounter++
            }

            if (retriesCounter > retries) {
                retriesCounter--
                state = State.Cancel("Retries limit reached! Limit: $retries")
            }

            comPort.write(answer)

        } while (!state.isFinishing)

        comPort.close()

        if (state is State.Cancel) {
            throw XmodemCancelException(state as State.Cancel)
        }

    }

    private fun clearFrame(block: ByteArray?): ByteArray? {

        if (block == null || block.size == 1  || block.size == packetSize) {
            return block
        }

        KydraLog.warn(generateTag(), "Unrecognized data received!")

        var frameStart = 0

        for (i in block.indices) {
            if (block[i] == headerByte) {
                frameStart = i
                break
            }
        }

        val frame = block.drop(frameStart)

        return when {
            frame.size == packetSize -> frame.toByteArray()

            frame.size > packetSize -> frame.dropLast(frame.size - packetSize).toByteArray()

            else -> null
        }.also {
            KydraLog.info(generateTag(), if (it == null) "Frame couldn't be recoverd!" else "Frame recovered!")
        }
    }

    private fun parsePacket(block: ByteArray): ByteArray? {

        val firstByte = block.first()

        val proposedState = when {
            firstByte == headerByte -> when {

                block.size != packetSize -> {
                    State.RejectPacket("Invalid packet length")
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
            block.size != 1 -> {
                State.RejectPacket("Bad first byte! Received: ${firstByte.asHex()} Expected header: ${headerByte.asHex()}")
            }
            firstByte == ASCII.EOT -> {
                State.EOT
            }
            firstByte == ASCII.CAN -> {
                State.Cancel("Canceled by the sender!")
            }
            else -> {
                State.RejectPacket("Bad byte! Received: ${firstByte.asHex()}")
            }
        }

        if (state !is State.NoInit || proposedState is State.AcceptPacket || proposedState is State.Cancel) {
            state = proposedState
        } else {
            KydraLog.info(generateTag(), "Rejected initialization: $proposedState")
        }

        return if (state is State.AcceptPacket && state !is State.AcceptPacketDuplicate) {

            expectedPacketNumber++
            block
                .drop(3)
                .dropLast(checksumType.byteSize)
                .dropLastWhile { it == ASCII.SUB}
                .toByteArray()
        } else {
            null
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