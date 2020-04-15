package xmodem.protocol.receiver

sealed class State(val isFinishing: Boolean, val name: String) {

    override fun toString(): String {
        return name
    }

    object NoInit : State(false, "NoInit")
    open class AcceptPacket(name: String = "AcceptPacket") : State(false, name)
    class AcceptPacketDuplicate : AcceptPacket("AcceptPacketDuplicate")
    data class RejectPacket(val cause: String) : State(false, "RejectPacket")
    data class Cancel(val cause: String) : State(true, "Cancel")
    object EOT : State(true, "EOT")
}