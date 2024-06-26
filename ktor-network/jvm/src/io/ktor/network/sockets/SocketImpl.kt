/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.sockets

import io.ktor.network.selector.*
import io.ktor.util.logging.*
import java.nio.channels.*

private val LOG = KtorSimpleLogger("io.ktor.network.sockets.SocketImpl")

internal class SocketImpl<out S : SocketChannel>(
    override val channel: S,
    selector: SelectorManager,
    socketOptions: SocketOptions.TCPClientSocketOptions? = null
) : NIOSocketImpl<S>(channel, selector, pool = null, socketOptions = socketOptions),
    Socket {
    init {
        require(!channel.isBlocking) { "Channel need to be configured as non-blocking." }
    }

    override val localAddress: SocketAddress
        get() {
            val localAddress = if (java7NetworkApisAvailable) {
                channel.localAddress
            } else {
                channel.socket().localSocketAddress
            }
            return localAddress?.toSocketAddress()
                ?: throw IllegalStateException("Channel is not yet bound")
        }

    override val remoteAddress: SocketAddress
        get() {
            val remoteAddress = if (java7NetworkApisAvailable) {
                channel.remoteAddress
            } else {
                channel.socket().remoteSocketAddress
            }
            return remoteAddress?.toSocketAddress()
                ?: throw IllegalStateException("Channel is not yet connected")
        }

    @Suppress("BlockingMethodInNonBlockingContext")
    internal suspend fun connect(target: java.net.SocketAddress): Socket {
        LOG.info("Attempt connecting to ${target.hashCode()}. $this")
        if (channel.connect(target)) {
            LOG.info("Connected fastpath with ${target.hashCode()}. $this")
            return this
        }

        wantConnect(true)
        try {
            selector.select(this, SelectInterest.CONNECT)
        } catch (cause: Throwable) {
            LOG.info("Select failed: ${target.hashCode()}. ${cause.message} \n ${cause.stackTraceToString()}. $this")
            throw cause
        }

        while (true) {
            if (channel.finishConnect()) {
                LOG.info("Finish connected slow path with ${target.hashCode()}. $this")
                // TCP has a well known self-connect problem, which client can connect to the client itself
                // without any program listen on the port.
                if (selfConnect()) {
                    LOG.info("Handling self-connect ${target.hashCode()}. $this")
                    if (java7NetworkApisAvailable) {
                        channel.close()
                    } else {
                        channel.socket().close()
                    }
                    continue
                }

                LOG.info("Connection done connect ${target.hashCode()}. $this")
                break
            }

            LOG.info("Connect failed, selecting for connect again: ${target.hashCode()}. $this")
            wantConnect(true)
            selector.select(this, SelectInterest.CONNECT)
        }

        LOG.info("Set wait connect done and exit: ${target.hashCode()}. $this")
        wantConnect(false)

        return this
    }

    private fun wantConnect(state: Boolean = true) {
        interestOp(SelectInterest.CONNECT, state)
    }

    private fun selfConnect(): Boolean {
        val localAddress = if (java7NetworkApisAvailable) {
            channel.localAddress
        } else {
            channel.socket().localSocketAddress
        }
        val remoteAddress = if (java7NetworkApisAvailable) {
            channel.remoteAddress
        } else {
            channel.socket().remoteSocketAddress
        }

        if (localAddress == null || remoteAddress == null) {
            throw IllegalStateException("localAddress and remoteAddress should not be null.")
        }

        val localInetSocketAddress = localAddress as? java.net.InetSocketAddress
        val remoteInetSocketAddress = remoteAddress as? java.net.InetSocketAddress

        val localHostAddress = localInetSocketAddress?.address?.hostAddress ?: ""
        val remoteHostAddress = remoteInetSocketAddress?.address?.hostAddress ?: ""
        val isRemoteAnyLocalAddress = remoteInetSocketAddress?.address?.isAnyLocalAddress ?: false
        val localPort = localInetSocketAddress?.port
        val remotePort = remoteInetSocketAddress?.port

        return localPort == remotePort && (isRemoteAnyLocalAddress || localHostAddress == remoteHostAddress)
    }
}
