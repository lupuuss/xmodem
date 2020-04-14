package xmodem.protocol

sealed class State(val isFinishing: Boolean, private val name: String) {

    override fun toString(): String {
        return name
    }

    object NoInit : State(false, "NoInit")
    object AcceptPacket : State(false, "AcceptPacket")
    data class RejectPacket(val cause: String) : State(false, "RejectPacket")
    data class Cancel(val cause: String) : State(true, "Cancel")
    object EOT : State(true, "EOT")
}