@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package xmodem.protocol

import xmodem.ASCII
import xmodem.checksum.Checksum
import xmodem.com.ComPort

class XmodemReceiver(
    com: String,
    private val checksumType: Checksum.Type,
    private val timeoutMs: UInt = 10_000u,
    private val repeat: Int = 10,
    private val verbose: Boolean = true
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
    private var repeatCounter = 0

    private var state: State = State.NoInit
        set(value) {
            field = value

            if (verbose) {
                println(state)
            }
        }

    fun receive() {

        try {

            comPort.editTimeouts {
                ReadIntervalTimeout = 100u
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

            val block = comPort.readOrNull(packetSize)

            if (block != null) {

                parsePacket(block)

            } else {
                println("Timeout!")
            }

            val answer = when (state) {
                State.NoInit -> initByte
                State.AcceptPacket -> ASCII.ACK
                is State.RejectPacket -> ASCII.NAK
                is State.Cancel -> ASCII.CAN
                State.EOT -> ASCII.ACK
            }

            if (state is State.AcceptPacket) {
                repeatCounter = 0
            } else {
                repeatCounter++
                println("Repeats [$repeatCounter/$repeat]")
            }

            comPort.write(answer)

        } while (!state.isFinishing && repeatCounter < repeat)

    }

    private fun parsePacket(block: ByteArray): ByteArray? {

        val firstByte = block.first()

        val proposedState = when {
            firstByte == ASCII.EOT -> {
                State.EOT
            }
            firstByte == ASCII.CAN -> {
                State.Cancel("Canceled by the sender!")
            }
            firstByte == headerByte && block.size != packetSize -> {
                State.RejectPacket("Invalid packet length")
            }
            firstByte == headerByte && !checkPacketNumber(block) -> {
                State.RejectPacket("Bad number! Expected: $expectedPacketNumber Received: ${block[1]}|${block[2]}")
            }
            firstByte == headerByte && !checksum.verify(block.drop(3).toByteArray()) -> {
                State.RejectPacket("Bad checksum!")
            }
            firstByte == headerByte -> {
                State.AcceptPacket
            }
            else -> {
                State.RejectPacket("Bad first byte! Received: $firstByte Expected header: $headerByte")
            }
        }

        if (state !is State.NoInit || proposedState is State.AcceptPacket) {
            state = proposedState
        } else if (verbose) {
            println("Rejected init cause: $proposedState")
        }

        return if (state is State.AcceptPacket) {

            expectedPacketNumber++
            block
                .drop(3)
                .dropLast(checksumType.byteSize)
                .toByteArray()
        } else {
            null
        }
    }

    private fun checkPacketNumber(block: ByteArray): Boolean {
        val packetNumber = block[1].toUByte()
        val packetNumberFill = block[2].toUByte()

        val packetNumberDiff = expectedPacketNumber.toInt() - packetNumber.toInt()

        return packetNumberDiff in -1..0 && packetNumber + packetNumberFill == 0xFFu
    }


}