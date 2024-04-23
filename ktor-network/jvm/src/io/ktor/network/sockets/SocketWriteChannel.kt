/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.sockets

import io.ktor.network.selector.*
import io.ktor.utils.io.*
import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import kotlinx.io.*
import java.nio.channels.*

internal class ClosedToken(val cause: Throwable?)

internal val CLOSED_OK = ClosedToken(null)

internal class SocketWriteChannel(
    private val nioChannel: WritableByteChannel,
    private val selectable: Selectable,
    private val selector: SelectorManager,
    private val socketOptions: SocketOptions.TCPClientSocketOptions?,
    private val onClose: (Throwable?) -> Unit
) : ByteWriteChannel {
    private val _closed = atomic<ClosedToken?>(null)
    private val buffer = Buffer()

    override val isClosedForWrite: Boolean
        get() = _closed.value != null

    override val closedCause: Throwable?
        get() = _closed.value?.cause

    @InternalAPI
    override val writeBuffer: Sink
        get() = buffer

    init {
        selectable.interestOp(SelectInterest.WRITE, true)
    }

    private val writeTimeout: Long = socketOptions?.socketTimeout ?: INFINITE_TIMEOUT_MS

    override suspend fun flush() {
        while (!buffer.exhausted()) {
            val count = nioChannel.write(buffer)

            if (count == 0L) {
                if (writeTimeout == INFINITE_TIMEOUT_MS) {
                    selector.select(selectable, SelectInterest.WRITE)
                } else {
                    val result = withTimeoutOrNull(writeTimeout) {
                        selector.select(selectable, SelectInterest.WRITE)
                    }

                    if (result == null) {
                        cancel(SocketTimeoutException("Write timed out"))
                        throw SocketTimeoutException("Write timed out")
                    }
                }
            }
        }
    }

    override suspend fun flushAndClose() {
        flush()

        if (!_closed.compareAndSet(null, CLOSED_OK)) return
        selectable.interestOp(SelectInterest.WRITE, false)
        onClose(null)
    }

    override fun cancel(cause: Throwable) {
        val actualCause = IOException("Channel was cancelled", cause)

        val token = ClosedToken(actualCause)
        if (_closed.compareAndSet(null, token)) {
            selectable.interestOp(SelectInterest.WRITE, false)
            onClose(actualCause)
        }
    }
}

