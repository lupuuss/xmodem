@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package xmodem.files

import kotlinx.cinterop.*
import platform.posix.*

class FileOutput(
    private val path: String
) {

    private lateinit var file: CPointer<FILE>

    fun open() {
        val tmp = fopen(path, "w")

        if (tmp == null) {
            throw FileOpenException(path)
        } else {
            file = tmp
        }
    }

    fun write(bytes: ByteArray) = memScoped {

        val bytesPtr = bytes.toCValues().ptr

        fwrite(bytesPtr, 1u, bytes.size.toULong(), file)
    }

    fun close() {
        fclose(file)
    }
}