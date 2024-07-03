/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

package io.ktor.client.engine.cio

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.collections.*
import kotlinx.coroutines.sync.*
import java.util.concurrent.atomic.AtomicInteger

public val CIO_CONNECTIONS_IN_FACTORY: AtomicInteger = AtomicInteger(0)

internal class ConnectionFactory(
    private val selector: SelectorManager,
    private val connectionsLimit: Int,
    private val addressConnectionsLimit: Int
) {
    private val limit = Semaphore(connectionsLimit)
    private val addressLimit = ConcurrentMap<InetSocketAddress, Semaphore>()

    suspend fun connect(
        address: InetSocketAddress,
        configuration: SocketOptions.TCPClientSocketOptions.() -> Unit = {}
    ): Socket {
        limit.acquire()
        CIO_CONNECTIONS_IN_FACTORY.incrementAndGet()
        return try {
            val addressSemaphore = addressLimit.computeIfAbsent(address) { Semaphore(addressConnectionsLimit) }
            addressSemaphore.acquire()

            try {
                aSocket(selector).tcpNoDelay().tcp().connect(address, configuration)
            } catch (cause: Throwable) {
                // a failure or cancellation
                addressSemaphore.release()
                throw cause
            }
        } catch (cause: Throwable) {
            CIO_CONNECTIONS_IN_FACTORY.decrementAndGet()
            limit.release()
            throw cause
        }
    }

    fun release(address: InetSocketAddress) {
        CIO_CONNECTIONS_IN_FACTORY.decrementAndGet()
        addressLimit[address]!!.release()
        limit.release()
    }
}
