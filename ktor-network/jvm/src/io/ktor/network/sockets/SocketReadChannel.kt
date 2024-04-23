/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.sockets

import io.ktor.network.selector.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.io.*
import java.net.*
import java.nio.channels.*

internal class SocketReadChannel(
    private val channel: ReadableByteChannel,
    private val selectable: Selectable,
    private val selector: SelectorManager,
    socketOptions: SocketOptions.TCPClientSocketOptions?,
    private val onClose: (Throwable?) -> Unit
) : ByteReadChannel {
    @Volatile
    private var closed: ClosedToken? = null

    private val buffer = Buffer()

    override val closedCause: Throwable?
        get() = closed?.cause

    override val isClosedForRead: Boolean
        get() = closed != null && buffer.exhausted()

    init {
        selectable.interestOp(SelectInterest.READ, true)
    }

    @InternalAPI
    override val readBuffer: Source get() = buffer

    private val timeout = socketOptions?.socketTimeout ?: INFINITE_TIMEOUT_MS

    override suspend fun awaitContent(): Boolean {
        if (closed != null) {
            closedCause?.let { throw IOException("Channel is cancelled", closedCause) }
        }

        var count = 0
        while (count == 0) {
            count = channel.read(buffer)
            if (count == -1) {
                closeFromNetwork()
                return false
            }
            if (count == 0) {
                if (timeout == INFINITE_TIMEOUT_MS) {
                    selector.select(selectable, SelectInterest.READ)
                } else {
                    val result = withTimeoutOrNull(timeout) {
                        selector.select(selectable, SelectInterest.READ)
                    }

                    if (result == null) {
                        cancel(SocketTimeoutException("Read timed out"))
                        throw SocketTimeoutException("Read timed out")
                    }
                }
            }
        }

        return closed == null
    }

    private fun closeFromNetwork() {
        closed = CLOSED_OK
        selectable.interestOp(SelectInterest.READ, false)
        onClose(null)
    }

    override fun cancel(cause: Throwable) {
        buffer.close()
        selectable.interestOp(SelectInterest.READ, false)

        if (closed != null) return
        val closeException = IOException("Channel was cancelled", cause)
        closed = ClosedToken(closeException)
        onClose(closeException)
    }
}
