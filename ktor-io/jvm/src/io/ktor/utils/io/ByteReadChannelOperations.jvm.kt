/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.utils.io.core.*
import kotlinx.io.*
import kotlinx.io.bytestring.*
import kotlinx.io.unsafe.*
import java.nio.*
import java.nio.channels.*

/**
 * Creates a channel for reading from the specified byte buffer.
 */
public fun ByteReadChannel(content: ByteBuffer): ByteReadChannel {
    val packet = buildPacket {
        writeFully(content)
    }

    return ByteReadChannel(packet)
}

@OptIn(InternalAPI::class)
public suspend fun ByteReadChannel.readAvailable(buffer: ByteBuffer): Int {
    if (readBuffer.exhausted()) awaitContent()
    return readBuffer.readAtMostTo(buffer)
}

public fun ByteString(buffer: ByteBuffer): ByteString {
    val array = ByteArray(buffer.remaining())
    buffer.mark()
    buffer.get(array)
    buffer.reset()
    return ByteString(array)
}

public suspend fun ByteReadChannel.copyTo(channel: ReadableByteChannel): Long {
    TODO("Not yet implemented")
}

@OptIn(InternalAPI::class)
public suspend fun ByteReadChannel.readUntilDelimiter(delimiter: ByteString, out: ByteBuffer): Int {
    val initial = out.remaining()
    while (!isClosedForRead && out.hasRemaining()) {
        if (availableForRead == 0) {
            awaitContent()
            continue
        }

        val index = readBuffer.indexOf(delimiter)
        if (index == -1L) {
            readBuffer.readAtMostTo(out)
            continue
        }

        val count = minOf(out.remaining(), index.toInt())
        val limit = out.limit()
        out.limit(minOf(out.limit(), out.position() + count))
        readBuffer.readAtMostTo(out)
        out.limit(limit)
        break
    }

    return initial - out.remaining()
}

public suspend fun ByteReadChannel.readUntilDelimiter(delimiter: ByteBuffer, out: ByteBuffer): Int {
    return readUntilDelimiter(ByteString(delimiter), out)
}

public suspend fun ByteReadChannel.skipDelimiter(delimiter: ByteBuffer) {
    skipDelimiter(ByteString(delimiter))
}

public suspend fun ByteReadChannel.skipDelimiter(delimiter: ByteString) {
    for (i in 0 until delimiter.size) {
        val byte = readByte()
        if (byte != delimiter[i]) {
            throw IllegalStateException("Delimiter is not found")
        }
    }
}

@OptIn(InternalAPI::class)
public suspend fun ByteReadChannel.readFully(buffer: ByteBuffer) {
    while (availableForRead < buffer.remaining() && awaitContent()) {}

    while (readBuffer.remaining > 0 && buffer.hasRemaining()) {
        readBuffer.readAtMostTo(buffer)
    }
}

@OptIn(InternalAPI::class, SnapshotApi::class, UnsafeIoApi::class, InternalIoApi::class)
public fun ByteReadChannel.readAvailable(block: (ByteBuffer) -> Int): Int {
    if (readBuffer.exhausted()) return 0
    var result = 0
    UnsafeBufferAccessors.readFromHead(readBuffer.buffer) { array, start, endExclusive ->
        val buffer = ByteBuffer.wrap(array, start, endExclusive - start)
        result = block(buffer)
        result
    }

    return result
}

