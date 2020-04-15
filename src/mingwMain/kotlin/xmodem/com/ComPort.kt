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

    private var applyCustomSettings = false

    private val prefix = "\\\\.\\"
    private val name = if (name.startsWith(prefix)) name else "$prefix$name"
    private val tag = "[Port: $name]"

    private var handle: HANDLE? = null

    private val timeouts = nativeHeap.alloc<COMMTIMEOUTS>()

    private val dcb: DCB = nativeHeap.alloc()

    fun editDCB(editor: DCB.() -> Unit) {
        applyCustomSettings = true
        dcb.apply(editor)
    }

    fun editTimeouts(editor: COMMTIMEOUTS.() -> Unit) {
        timeouts.apply(editor)
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
            throw ComOpenFailedException(name)
        }

        if (SetCommTimeouts(handle, timeouts.ptr) != TRUE) {
            throw ComSettingsApplyFailedException(name)
        }

        if (applyCustomSettings && SetCommState(handle, dcb.ptr) != TRUE) {
            throw ComSettingsApplyFailedException(name)
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
        nativeHeap.free(dcb)
        nativeHeap.free(timeouts)
    }
}
