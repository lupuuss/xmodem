@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package xmodem.files

import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.GetFileSize

class FileInput(
    private val path: String,
    private val mode: Mode
) {
    enum class Mode(val str: String) {
        Binary("rb"), Text("r")
    }

    private lateinit var file: CPointer<FILE>

    fun open() {
        val tmp = fopen(path, mode.str)

        if (tmp == null) {
            throw FileOpenException(path)
        } else {
            file = tmp
        }
    }

    fun read(n: Int): ByteArray = memScoped {
        val buffer = allocArray<ByteVar>(n)

        fread(buffer.pointed.ptr, 1u, n.toULong(), file )

        return buffer.readBytes(n)
    }

    fun size() = GetFileSize(file, null)

    fun close() {
        fclose(file)
    }

}