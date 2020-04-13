package xmodem.protocol

abstract class XmodemException(msg: String?, cause: Throwable? = null) : Exception(msg, cause)

class XmodemSenderTimeout(time: Long) : XmodemException("Sender didn't answer in $time ms!")

class XmodemIOException(cause: Throwable) : XmodemException(null, cause)