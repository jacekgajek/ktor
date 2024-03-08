// ktlint-disable filename
package io.ktor.network.selector

import io.ktor.utils.io.errors.*
import kotlinx.atomicfu.*
import java.nio.channels.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.*

internal open class SelectableBase(override val channel: SelectableChannel) : Selectable {
    private val _isClosed = AtomicBoolean(false)

    override val suspensions = InterestSuspensionsMap()

    override val isClosed: Boolean
        get() = _isClosed.get()

    private val _interestedOps = atomic(0)

    override val interestedOps: Int get() = _interestedOps.value

    override fun interestOp(interest: SelectInterest, state: Boolean) {
        val flag = interest.flag

        while (true) {
            val before = _interestedOps.value
            val after = if (state) before or flag else before and flag.inv()
            if (_interestedOps.compareAndSet(before, after)) break
        }
    }

    override fun cancel(cause: Throwable) {
        if (!_isClosed.compareAndSet(false, true)) return

        _interestedOps.value = 0
        val exception = IOException("Selectable is cancelled", cause)
        suspensions.invokeForEachPresent {
            resumeWithException(exception)
        }
    }

    override fun close() {
        if (!_isClosed.compareAndSet(false, true)) return

        _interestedOps.value = 0
        val exception = IOException("Selectable is closed")
        suspensions.invokeForEachPresent {
            resumeWithException(exception)
        }
    }

    override fun dispose() {
        close()
    }
}
