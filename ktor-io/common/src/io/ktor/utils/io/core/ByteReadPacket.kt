@file:Suppress("RedundantModalityModifier", "FunctionName")

package io.ktor.utils.io.core

import io.ktor.utils.io.pool.*
import kotlinx.io.*

@Deprecated(
    "Use Source instead",
    ReplaceWith("Source", "kotlinx.io.Buffer")
)
public typealias ByteReadPacket = kotlinx.io.Source

public val ByteReadPacketEmpty: ByteReadPacket = kotlinx.io.Buffer()

public inline fun ByteReadPacket(
    array: ByteArray,
    offset: Int = 0,
    length: Int = array.size
): ByteReadPacket = kotlinx.io.Buffer().apply {
    write(array, startIndex = offset, endIndex = offset + length)
}

@OptIn(InternalIoApi::class)
public val Source.remaining: Long
    get() = buffer.size

@Suppress("UNUSED_PARAMETER")
@Deprecated(
    "Use Buffer instead",
    ReplaceWith("Buffer()", "kotlinx.io.Buffer")
)
public fun Sink(pool: ObjectPool<*>): kotlinx.io.Buffer = kotlinx.io.Buffer()

@Deprecated(
    "Use Buffer instead",
    ReplaceWith("Buffer()", "kotlinx.io.Buffer")
)
public fun Sink(): kotlinx.io.Buffer = kotlinx.io.Buffer()

@OptIn(InternalIoApi::class)
public fun ByteReadPacket.readAvailable(out: Buffer): Int {
    val result = buffer.size
    out.transferFrom(this)
    return result.toInt()
}

@OptIn(InternalIoApi::class)
public fun ByteReadPacket.copy(): ByteReadPacket = buffer.copy()

@OptIn(InternalIoApi::class)
public fun ByteReadPacket.readShortLittleEndian(): Short {
    return buffer.readShortLe()
}

@OptIn(InternalIoApi::class)
public fun ByteReadPacket.discard(count: Long = Long.MAX_VALUE): Long {
    val countToDiscard = minOf(count, remaining)
    buffer.skip(countToDiscard)
    return countToDiscard
}

public fun ByteReadPacket.forEach(block: (byte: Byte) -> Unit) {
    TODO()
}

@OptIn(InternalIoApi::class)
public fun ByteReadPacket.takeWhile(block: (Buffer) -> Boolean) {
    block(buffer)
}

public fun ByteReadPacket.readFully(out: ByteArray, offset: Int = 0, length: Int = out.size - offset) {
    readTo(out, offset, offset + length)
}

@OptIn(InternalIoApi::class, ExperimentalStdlibApi::class)
public fun <T> ByteReadPacket.preview(function: (ByteReadPacket) -> T): T {
    return buffer.peek().use(function)
}

@OptIn(InternalIoApi::class, ExperimentalStdlibApi::class)
public fun <T> BytePacketBuilder.preview(function: (ByteReadPacket) -> T): T {
    return buffer.peek().use(function)
}
