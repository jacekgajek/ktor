/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.utils.io.internal.*
import io.ktor.utils.io.locks.*
import kotlinx.atomicfu.*
import kotlinx.io.*
import kotlin.concurrent.*

@InternalAPI
public val CHANNEL_MAX_SIZE: Int = 8 * 1024

private class CloseToken(val cause: IOException?)

private val CLOSED = CloseToken(null)

/**
 * Sequential (non-concurrent) byte channel implementation
 */
public class ByteChannel : ByteReadChannel, BufferedByteWriteChannel {
    private val _closedCause = atomic<CloseToken?>(null)
    private val slot = AwaitingSlot()
    private val flushBuffer: Buffer = Buffer()

    @Volatile
    private var flushBufferSize = 0

    @OptIn(InternalAPI::class)
    private val mutex = SynchronizedObject()

    private val _readBuffer = Buffer()
    private val _writeBuffer = Buffer()

    @InternalAPI
    override val readBuffer: Source
        get() {
            closedCause?.let { throw it }
            if (_readBuffer.exhausted()) moveFlushToReadBuffer()
            return _readBuffer
        }

    @InternalAPI
    override val writeBuffer: Sink
        get() {
            closedCause?.let { throw it }
            if (isClosedForWrite) throw IOException("Channel is closed for write")
            return _writeBuffer
        }

    override val closedCause: IOException?
        get() = _closedCause.value?.cause?.let {
            IOException("Channel was closed", it)
        }

    override val isClosedForWrite: Boolean
        get() = _closedCause.value != null

    override val isClosedForRead: Boolean
        get() {
            closedCause?.let { throw it }
            return isClosedForWrite && flushBufferSize == 0 && _readBuffer.exhausted()
        }

    @OptIn(InternalAPI::class)
    override suspend fun awaitContent(): Boolean {
        closedCause?.let { throw it }

        if (flushBufferSize == 0 && !isClosedForRead) slot.sleepWhile { flushBufferSize == 0 && !isClosedForRead }
        if (_readBuffer.size < CHANNEL_MAX_SIZE) moveFlushToReadBuffer()
        return _closedCause.value == null
    }

    @OptIn(InternalAPI::class)
    private fun moveFlushToReadBuffer() {
        synchronized(mutex) {
            flushBuffer.transferTo(_readBuffer)
            flushBufferSize = 0
        }

        slot.resume()
    }

    @OptIn(InternalAPI::class)
    override suspend fun flush() {
        flushWriteBuffer()
        slot.sleepWhile { flushBufferSize >= CHANNEL_MAX_SIZE }
    }

    @InternalAPI
    public override fun flushWriteBuffer() {
        if (_writeBuffer.exhausted()) return

        synchronized(mutex) {
            val count = _writeBuffer.size.toInt()
            flushBuffer.transferFrom(_writeBuffer)
            flushBufferSize += count
        }

        slot.resume()
    }

    @InternalAPI
    override fun close() {
        if (!_closedCause.compareAndSet(null, CLOSED)) return
        flushWriteBuffer()
        slot.close(null)
    }

    override suspend fun flushAndClose() {
        if (!_closedCause.compareAndSet(null, CLOSED)) return
        flush()
        slot.close(null)
    }

    override fun cancel(cause: Throwable) {
        if (_closedCause.value != null) return

        val actualCause = IOException("Channel was cancelled", cause)
        _closedCause.compareAndSet(null, CloseToken(actualCause))
        slot.close(actualCause)
    }

    override fun toString(): String = "ByteChannel[${hashCode()}, ${slot.hashCode()}]"
}
