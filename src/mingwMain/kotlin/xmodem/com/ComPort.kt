@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package xmodem.com

import kotlinx.cinterop.*
import platform.windows.*
import ru.pocketbyte.kydra.log.debug
import xmodem.asHex
import xmodem.log.Log

class ComPort(
    name: String
) {

    constructor(config: ComConfig) : this(config.port) {
        editDCB {
            BaudRate = config.cbrRate.value
            ByteSize = config.byteSize
            Parity = config.parity.winValue
            StopBits = config.stopBits.winValue
        }
    }

    private val prefix = "\\\\.\\"
    private val name = if (name.startsWith(prefix)) name else "$prefix$name"
    private val tag = "[Port: $name]"

    private var handle: HANDLE? = null

    private var dcbEditor: (DCB.() -> Unit)? = null
    private var timeoutsEditor: (COMMTIMEOUTS.() -> Unit?)? = null

    fun editDCB(editor: DCB.() -> Unit) {
        this.dcbEditor = editor
    }

    fun editTimeouts(editor: COMMTIMEOUTS.() -> Unit) {
        this.timeoutsEditor = editor
    }

    fun fullPurge() {
        val purgeResult = PurgeComm(handle, (PURGE_RXABORT or PURGE_RXCLEAR or PURGE_TXCLEAR or PURGE_TXABORT).toUInt())

        if (purgeResult != TRUE) {
            throw ComPurgeFailedException(name, GetLastError())
        }
    }

    fun open() = memScoped {

        handle = CreateFile?.invoke(
            name.wcstr.ptr,
            GENERIC_WRITE.toUInt() or  GENERIC_READ,
            0u,
            null,
            OPEN_EXISTING.toUInt(),
            0u,
            null
        )

        if (handle == INVALID_HANDLE_VALUE) {
            throw ComOpenFailedException(name, GetLastError())
        }

        val timeouts = alloc<COMMTIMEOUTS>()
        val dcb = alloc<DCB>()

        GetCommState(handle, dcb.ptr)
        GetCommTimeouts(handle, timeouts.ptr)

        timeoutsEditor?.let {

            it(timeouts)

            if (SetCommTimeouts(handle, timeouts.ptr) != TRUE) {
                throw ComSettingsApplyFailedException(name,"SetCommTimeouts", GetLastError())
            }
        }

        dcbEditor?.let {

            it(dcb)

            if (SetCommState(handle, dcb.ptr) != TRUE) {
                throw ComSettingsApplyFailedException(name, "SetCommState", GetLastError())
            }
        }
    }

    fun write(byte: Byte) = write(byteArrayOf(byte))

    fun write(bytes: ByteArray) = memScoped {

        val buffer = allocArray<ByteVar>(bytes.size)
        val dwBytesRead = alloc<UIntVar>()

        bytes.forEachIndexed { index, byte ->  buffer[index] = byte }

        WriteFile(
            handle,
            buffer.pointed.ptr,
            bytes.size.toUInt(),
            dwBytesRead.ptr,
            null
        )

        Log.debug(tag) { "Written bytes: ${bytes.map { it.asHex() }}" }
    }

    fun readOrNull(n: Int): ByteArray? = memScoped {

        val bufferSize = n.toUInt()
        val buffer = allocArray<ByteVar>(n)
        val readByte = alloc<UIntVar>()

        ReadFile(
            handle,
            buffer.pointed.ptr,
            bufferSize,
            readByte.ptr,
            null
        )

        Log.debug (tag) { "Read bytes: ${buffer.readBytes(readByte.value.toInt()).map { it.asHex() }}" }

        if (readByte.value == 0u) {
            return null
        }

        ByteArray(readByte.value.toInt()) { buffer[it] }
    }

    fun close() {

        handle?.let {
            CloseHandle(it)
        }
    }
}
