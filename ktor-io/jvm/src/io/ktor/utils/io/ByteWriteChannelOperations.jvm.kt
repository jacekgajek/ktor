/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.utils.io.core.*
import kotlinx.io.*
import kotlinx.io.unsafe.*
import java.nio.*

@OptIn(InternalAPI::class)
public suspend fun ByteWriteChannel.writeByteBuffer(value: ByteBuffer) {
    writeBuffer.writeByteBuffer(value)
    flush()
}

@OptIn(InternalAPI::class)
public suspend fun ByteWriteChannel.writeFully(value: ByteBuffer) {
    writeBuffer.writeByteBuffer(value)
    flush()
}

@OptIn(SnapshotApi::class, UnsafeIoApi::class, InternalAPI::class, InternalIoApi::class)
public suspend fun ByteWriteChannel.write(block: (buffer: ByteBuffer) -> Unit) {
    UnsafeBufferAccessors.writeToTail(writeBuffer.buffer, 1) { array, startIndex, endIndex ->
        val buffer = ByteBuffer.wrap(array, startIndex, endIndex - startIndex)
        block(buffer)
        return@writeToTail buffer.position() - startIndex
    }
    flush()
}

public fun ByteWriteChannel.writeAvailable(block: (ByteBuffer) -> Unit) {
    TODO("Not yet implemented")
}

@OptIn(InternalAPI::class)
public fun ByteWriteChannel.writeAvailable(buffer: ByteBuffer) {
    writeBuffer.write(buffer)
}
