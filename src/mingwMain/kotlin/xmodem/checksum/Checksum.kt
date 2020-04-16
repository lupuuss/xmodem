package xmodem.checksum

interface Checksum {

    enum class Type(val byteSize: Int) {
        CRC16(2), SUM8(1);
    }

    fun calculate(bytes: ByteArray): ByteArray

    fun verify(bytes: ByteArray, checksum: ByteArray): Boolean

    fun verify(bytes: ByteArray): Boolean

    companion object {

        fun getCrc16(poly: Short) = ChecksumCrc16(poly)

        fun getSum8() = ChecksumAlgebraic8()
    }
}