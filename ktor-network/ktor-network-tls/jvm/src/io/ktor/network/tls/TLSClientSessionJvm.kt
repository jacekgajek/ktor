/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

package io.ktor.network.tls

import io.ktor.network.sockets.*
import io.ktor.network.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.pool.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.nio.*
import kotlin.coroutines.*

internal actual suspend fun openTLSSession(
    socket: Socket,
    input: ByteReadChannel,
    output: ByteWriteChannel,
    config: TLSConfig,
    context: CoroutineContext
): Socket {
    val handshake = TLSClientHandshake(input, output, config, context)
    try {
        handshake.negotiate()
    } catch (cause: ClosedSendChannelException) {
        throw TLSException("Negotiation failed due to end of stream", cause)
    }
    return TLSSocket(handshake.input, handshake.output, socket, context)
}

private class TLSSocket(
    private val input: ReceiveChannel<TLSRecord>,
    private val output: SendChannel<TLSRecord>,
    private val socket: Socket,
    coroutineContext: CoroutineContext,
) : CoroutineScope, Socket by socket {

    private val job = Job(coroutineContext[Job])
    override val coroutineContext: CoroutineContext = coroutineContext + job

    private val reader = writer(this.coroutineContext + CoroutineName("cio-tls-input-loop")) {
        appDataInputLoop(this.channel)
    }.channel

    private val writer = reader(this.coroutineContext + CoroutineName("cio-tls-output-loop")) {
        appDataOutputLoop(this.channel)
    }.channel

    init {
        job.complete()
        job.invokeOnCompletion {
            input.cancel()
            output.close()
        }
    }

    override fun attachForReading(): ByteReadChannel = reader

    override fun attachForWriting(): ByteWriteChannel = writer

    private suspend fun appDataInputLoop(pipe: ByteWriteChannel) {
        try {
            input.consumeEach { record ->
                val packet = record.packet
                val length = packet.remaining
                when (record.type) {
                    TLSRecordType.ApplicationData -> {
                        pipe.writePacket(record.packet)
                        pipe.flush()
                    }

                    else -> throw TLSException("Unexpected record ${record.type} ($length bytes)")
                }
            }
        } catch (cause: Throwable) {
            pipe.close(cause)
        } finally {
            pipe.flushAndClose()
        }
    }

    private suspend fun appDataOutputLoop(
        pipe: ByteReadChannel
    ): Unit = DefaultByteBufferPool.useInstance { buffer: ByteBuffer ->
        try {
            while (true) {
                buffer.clear()
                val rc = pipe.readAvailable(buffer)
                if (rc == -1) break

                buffer.flip()
                output.send(TLSRecord(TLSRecordType.ApplicationData, packet = buildPacket { writeFully(buffer) }))
            }
        } catch (_: ClosedSendChannelException) {
            // The socket was already closed, we should ignore that error.
        }
    }

    override fun dispose() {
        socket.dispose()
    }
}
