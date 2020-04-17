@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package xmodem.files

import kotlinx.cinterop.*
import platform.posix.*

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

        val allRead = fread(buffer.pointed.ptr, 1u, n.toULong(), file)

        return buffer.readBytes(allRead.toInt())
    }

    fun size(): Int {

        fseek(file, 0, SEEK_END)
        val size = ftell(file)
        rewind(file)

        return size
    }

    fun close() {
        fclose(file)
    }

}