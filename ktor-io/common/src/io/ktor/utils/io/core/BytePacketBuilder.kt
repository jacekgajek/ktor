@file:Suppress("RedundantModalityModifier")

package io.ktor.utils.io.core

import io.ktor.utils.io.*
import kotlinx.io.*

@Deprecated(
    IO_DEPRECATION_MESSAGE,
    replaceWith = ReplaceWith("Sink", "kotlinx.io.Buffer")
)
public typealias BytePacketBuilder = kotlinx.io.Sink

@OptIn(InternalIoApi::class)
public val BytePacketBuilder.size: Int get() = buffer.size.toInt()

public fun BytePacketBuilder(): BytePacketBuilder = kotlinx.io.Buffer()

public fun BytePacketBuilder.append(value: CharSequence, startIndex: Int = 0, endIndex: Int = value.length) {
    writeText(value, startIndex, endIndex)
}

@OptIn(InternalIoApi::class)
@Deprecated("Build is no longer needed", ReplaceWith("this"))
public fun BytePacketBuilder.build(): ByteReadPacket = buffer

public fun BytePacketBuilder.writeFully(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size - offset) {
    write(buffer, offset, offset + length)
}

public fun BytePacketBuilder.writePacket(packet: ByteReadPacket) {
    transferFrom(packet)
}

public fun BytePacketBuilder.writeUByte(value: UByte) {
    writeByte(value.toByte())
}
