/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.io.*
import kotlinx.io.Buffer
import kotlin.coroutines.*
import kotlin.jvm.*

@OptIn(InternalAPI::class)
public fun ByteWriteChannel.writeByte(value: Byte) {
    writeBuffer.writeByte(value)
}

@OptIn(InternalAPI::class)
public fun ByteWriteChannel.writeShort(value: Short) {
    writeBuffer.writeShort(value)
}

@OptIn(InternalAPI::class)
public fun ByteWriteChannel.writeInt(value: Int) {
    writeBuffer.writeInt(value)
}

@OptIn(InternalAPI::class)
public suspend fun ByteWriteChannel.writeLong(value: Long) {
    writeBuffer.writeLong(value)
    flush()
}

@OptIn(InternalAPI::class)
public suspend fun ByteWriteChannel.writeByteArray(array: ByteArray) {
    writeBuffer.write(array)
    flush()
}

@OptIn(InternalAPI::class)
public suspend fun ByteWriteChannel.writeSource(source: Source) {
    writeBuffer.transferFrom(source)
    flush()
}

@OptIn(InternalAPI::class)
public suspend fun ByteWriteChannel.writeString(value: String) {
    writeBuffer.writeText(value)
    flush()
}

@OptIn(InternalAPI::class)
public suspend fun ByteWriteChannel.writeFully(value: ByteArray, offset: Int = 0, length: Int = value.size - offset) {
    writeBuffer.write(value, offset, offset + length)
    flush()
}

@OptIn(InternalAPI::class)
public suspend fun ByteWriteChannel.writeBuffer(value: Source) {
    writeBuffer.transferFrom(value)
    flush()
}

@OptIn(InternalAPI::class)
public suspend fun ByteWriteChannel.writeStringUtf8(value: String) {
    writeBuffer.writeText(value)
    flush()
}

@OptIn(InternalAPI::class)
public fun ByteWriteChannel.writePacket(copy: Buffer) {
    writeBuffer.transferFrom(copy)
}

@OptIn(InternalAPI::class)
public fun ByteWriteChannel.writePacket(copy: Source) {
    writeBuffer.transferFrom(copy)
}

public fun ByteWriteChannel.close(cause: Throwable?) {
    if (cause == null) {
        GlobalScope.launch { flushAndClose() }
    } else {
        cancel(cause)
    }
}

@JvmInline
public value class WriterScope(public val channel: ByteWriteChannel)

public interface ChannelJob {
    public val job: Job
}

public suspend fun ChannelJob.join() {
    job.join()
}

public fun ChannelJob.invokeOnCompletion(block: () -> Unit) {
    job.invokeOnCompletion { block() }
}

public fun ChannelJob.cancel(): Unit = job.cancel()

public class WriterJob internal constructor(
    public val channel: ByteReadChannel,
    public override val job: Job
) : ChannelJob

public fun CoroutineScope.writer(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    autoFlush: Boolean = false,
    block: suspend WriterScope.() -> Unit
): WriterJob = writer(coroutineContext, ByteChannel(), block)

public fun CoroutineScope.writer(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    channel: ByteChannel,
    block: suspend WriterScope.() -> Unit
): WriterJob {
    val job = launch(coroutineContext) {
        try {
            block(WriterScope(channel))
        } catch (cause: Throwable) {
            channel.cancel(cause)
        } finally {
            channel.flushAndClose()
        }
    }

    return WriterJob(channel, job)
}

public fun ByteWriteChannel.write(block: (Memory, Int, Int) -> Int): Int {
    TODO("Not yet implemented")
}

public fun ByteWriteChannel.awaitFreeSpace() {
    TODO()
}

