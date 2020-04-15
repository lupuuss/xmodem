package xmodem.protocol

abstract class XmodemException(msg: String?, cause: Throwable? = null) : Exception(msg, cause)

class XmodemIOException(cause: Throwable) : XmodemException(cause.message, cause)

class XmodemCancelException(state: State.Cancel) : XmodemException("Transmission canceled! State: $state", null)
