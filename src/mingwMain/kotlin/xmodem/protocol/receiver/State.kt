package xmodem.protocol.receiver

sealed class State(open val isFinishing: Boolean, val name: String) {

    override fun toString(): String {
        return name
    }

    class NoInit(val cause: State? = null) : State(false, "NoInit") {
        override val isFinishing: Boolean get() = cause?.isFinishing ?: super.isFinishing

        override fun toString(): String {
            return "$name(${cause ?: ""})"
        }
    }

    object ExpectedPacket : State(false, "ExpectedPacket")

    object Timeout : State(false, "Timeout")

    open class AcceptPacket(name: String = "AcceptPacket") : State(false, name)

    class AcceptPacketDuplicate : AcceptPacket("AcceptPacketDuplicate")

    data class RejectPacket(val cause: String) : State(false, "RejectPacket")

    data class Cancel(val cause: String) : State(true, "Cancel")

    object EOT : State(true, "EOT")
}