package xmodem.protocol

import xmodem.protocol.receiver.State

abstract class XmodemException(msg: String?, cause: Throwable? = null) : Exception(msg, cause)

class XmodemIOException(cause: Throwable) : XmodemException(cause.message, cause)

class XmodemCancelException(state: State.Cancel) : XmodemException("Transmission canceled! State: $state", null)
