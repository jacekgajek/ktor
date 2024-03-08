/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.sockets

import io.ktor.network.selector.*
import io.ktor.utils.io.*
import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import java.nio.channels.*
import kotlin.coroutines.*

internal abstract class NIOSocketImpl<out S>(
    override val channel: S,
    val selector: SelectorManager,
    private val socketOptions: SocketOptions.TCPClientSocketOptions? = null
) : ReadWriteSocket, SelectableBase(channel), CoroutineScope
    where S : java.nio.channels.ByteChannel, S : SelectableChannel {

    private val closeFlag = atomic(false)

    override val socketContext: CompletableJob = Job().apply {
        invokeOnCompletion {
            close()
        }
    }

    override val coroutineContext: CoroutineContext get() = socketContext

    @Volatile
    private var isClosedForReading = false

    @Volatile
    private var isClosedForWriting = false

    override fun attachForReading(): ByteReadChannel {
        return SocketReadChannel(channel, this, selector, socketOptions) {
            if (it != null) cancel(it)
            else closeForRead()
        }
    }

    override fun attachForWriting(): ByteWriteChannel = SocketWriteChannel(channel, this, selector, socketOptions) {
        if (it != null) cancel(it)
        else closeForWrite()
    }

    private fun closeForRead() {
        isClosedForReading = true
        if (isClosedForWriting) {
            close()
        }
    }

    private fun closeForWrite() {
        isClosedForWriting = true
        if (isClosedForReading) {
            close()
        }
    }

    override fun dispose() {
        close()
    }

    override fun cancel(cause: Throwable) {
        if (!closeFlag.compareAndSet(false, true)) return
        channel.close()
        socketContext.completeExceptionally(cause)

        super.cancel(cause)
    }

    override fun close() {
        if (!closeFlag.compareAndSet(false, true)) return
        channel.close()
        socketContext.complete()
    }
}
