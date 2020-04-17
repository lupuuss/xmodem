package xmodem.protocol

import xmodem.protocol.receiver.State

abstract class XmodemException(msg: String?, cause: Throwable? = null) : Exception(msg, cause)

class XmodemIOException(cause: Throwable) : XmodemException(cause.message, cause)

class XmodemCancelException(msg: String) : XmodemException(msg, null) {

    constructor(state: State.Cancel) : this("Transmission canceled! State: $state")
}
