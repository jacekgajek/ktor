/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.selector

import kotlinx.atomicfu.*
import kotlinx.coroutines.*

public class InterestSuspensionsMap {
    private val readHandler = atomic<CancellableContinuation<Unit>?>(null)
    private val writeHandler = atomic<CancellableContinuation<Unit>?>(null)
    private val connectHandler = atomic<CancellableContinuation<Unit>?>(null)
    private val acceptHandler = atomic<CancellableContinuation<Unit>?>(null)

    public fun addSuspension(interest: SelectInterest, continuation: CancellableContinuation<Unit>) {
        val result = when (interest) {
            SelectInterest.READ -> readHandler.getAndSet(continuation)
            SelectInterest.WRITE -> writeHandler.getAndSet(continuation)
            SelectInterest.ACCEPT -> acceptHandler.getAndSet(continuation)
            SelectInterest.CONNECT -> connectHandler.getAndSet(continuation)
        }

        if (result != null && !result.isCancelled && !result.isCompleted) {
            throw IllegalStateException("Handler for ${interest.name} is already registered")
        }
    }

    @Suppress("LoopToCallChain")
    public inline fun invokeForEachPresent(readyOps: Int, block: CancellableContinuation<Unit>.() -> Unit) {
        val flags = SelectInterest.flags

        for (ordinal in flags.indices) {
            if (flags[ordinal] and readyOps != 0) {
                removeSuspension(flags[ordinal])?.block()
            }
        }
    }

    public inline fun invokeForEachPresent(block: CancellableContinuation<Unit>.(SelectInterest) -> Unit) {
        for (interest in SelectInterest.AllInterests) {
            removeSuspension(interest)?.run { block(interest) }
        }
    }

    public fun removeSuspension(interest: SelectInterest): CancellableContinuation<Unit>? = when (interest) {
        SelectInterest.READ -> readHandler.getAndSet(null)
        SelectInterest.WRITE -> writeHandler.getAndSet(null)
        SelectInterest.ACCEPT -> acceptHandler.getAndSet(null)
        SelectInterest.CONNECT -> connectHandler.getAndSet(null)
    }

    public fun removeSuspension(interestOrdinal: Int): CancellableContinuation<Unit>? = when (interestOrdinal) {
        SelectInterest.READ.flag -> readHandler.getAndSet(null)
        SelectInterest.WRITE.flag -> writeHandler.getAndSet(null)
        SelectInterest.ACCEPT.flag -> acceptHandler.getAndSet(null)
        SelectInterest.CONNECT.flag -> connectHandler.getAndSet(null)
        else -> throw IllegalArgumentException("Invalid interest ordinal $interestOrdinal")
    }

    override fun toString(): String {
        return "R ${readHandler.value} W ${writeHandler.value} C ${connectHandler.value} A ${acceptHandler.value}"
    }
}
