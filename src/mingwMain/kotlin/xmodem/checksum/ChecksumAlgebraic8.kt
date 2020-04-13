package xmodem.checksum

class ChecksumAlgebraic8 : Checksum {

    override fun calculate(bytes: ByteArray): ByteArray = byteArrayOf(bytes.sum().toByte())

    override fun verify(bytes: ByteArray, checksum: ByteArray): Boolean {

        return calculate(bytes).contentEquals(checksum)
    }

    override fun verify(bytes: ByteArray): Boolean {
       return calculate(bytes.dropLast(1).toByteArray()).contentEquals(byteArrayOf(bytes.last()))
    }
}