/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.engine.apache

import io.ktor.client.request.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.io.*
import kotlinx.io.unsafe.*
import org.apache.http.*
import org.apache.http.nio.*
import org.apache.http.nio.protocol.*
import org.apache.http.protocol.*
import java.nio.*
import kotlin.coroutines.*

internal class ApacheResponseConsumer(
    val parentContext: CoroutineContext,
    private val requestData: HttpRequestData
) : HttpAsyncResponseConsumer<Unit> {
    private val interestController = InterestControllerHolder()

    private val responseDeferred = CompletableDeferred<HttpResponse>()
    private val channel = ByteChannel()
    val responseChannel: ByteReadChannel = channel

    @OptIn(InternalAPI::class, SnapshotApi::class, UnsafeIoApi::class, InternalIoApi::class)
    override fun consumeContent(decoder: ContentDecoder, ioctrl: IOControl) {
        var eof = false
        UnsafeBufferAccessors.writeToTail(channel.writeBuffer.buffer, 1) { array, startIndex, endIndex ->
            val buffer = ByteBuffer.wrap(array, startIndex, endIndex - startIndex)
            val written = decoder.read(buffer)
            if (written == -1) {
                eof = true
                0
            } else {
                written
            }
        }

        channel.flushWriteBuffer()
        if (eof) {
            runBlocking {
                channel.flushAndClose()
            }
        }
    }

    override fun failed(cause: Exception) {
        val mappedCause = mapCause(cause, requestData)
        responseDeferred.completeExceptionally(mappedCause)
        responseChannel.cancel(mappedCause)
    }

    override fun cancel(): Boolean {
        return true
    }

    override fun close() {
        runBlocking {
            channel.flushAndClose()
        }
    }

    override fun getException(): Exception? = channel.closedCause as? Exception

    override fun getResult() {
    }

    override fun isDone(): Boolean = channel.isClosedForWrite

    override fun responseCompleted(context: HttpContext) {
    }

    override fun responseReceived(response: HttpResponse) {
        responseDeferred.complete(response)
    }

    suspend fun waitForResponse(): HttpResponse = responseDeferred.await()
}
