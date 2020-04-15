package xmodem.protocol

abstract class XmodemException(msg: String?, cause: Throwable? = null) : Exception(msg, cause)

class XmodemSenderTimeout(time: Long) : XmodemException("Sender didn't answer in $time ms!")

class XmodemIOException(cause: Throwable) : XmodemException(null, cause)

class XmodemCancelException(state: State.Cancel) : XmodemException("Transmission canceled! State: $state", null)

class XmodemRetriesLimitReached(limit: Int) : XmodemException("Retries limit reached! Limit: $limit")