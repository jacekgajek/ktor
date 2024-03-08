@file:Suppress("FunctionName")

package io.ktor.utils.io.core

import kotlinx.io.*
import kotlinx.io.Buffer
import java.nio.*

public fun ByteReadPacket(byteBuffer: ByteBuffer): Source = Buffer().also {
    it.write(byteBuffer)
}

public fun ByteReadPacket.readAvailable(buffer: ByteBuffer): Int {
    val result = buffer.remaining()
    readAtMostTo(buffer)
    return result - buffer.remaining()
}

public fun ByteReadPacket.readFully(buffer: ByteBuffer) {
    readAtMostTo(buffer)
}
