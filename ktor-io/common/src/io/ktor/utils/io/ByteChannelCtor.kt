/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import kotlinx.io.*
import kotlinx.io.Buffer
import kotlin.concurrent.*

internal class ClosedToken(val cause: Throwable?)

/**
 * Creates a channel for reading from the specified byte array. Please note that it could use [content] directly
 * or copy its bytes depending on the platform
 */
public fun ByteReadChannel(content: ByteArray, offset: Int = 0, length: Int = content.size): ByteReadChannel {
    val source = Buffer().also {
        it.write(content, startIndex = offset, endIndex = offset + length)
    }

    return ByteReadChannel(source)
}

public fun ByteReadChannel(text: String, charset: Charset = Charsets.UTF_8): ByteReadChannel =
    ByteReadChannel(text.toByteArray(charset))

public fun ByteReadChannel(source: Source): ByteReadChannel = object : ByteReadChannel {

    @Volatile
    private var closed: ClosedToken? = null

    override val closedCause: Throwable?
        get() = closed?.cause

    override val isClosedForRead: Boolean
        get() = source.exhausted()

    @InternalAPI
    override val readBuffer: Source
        get() {
            closedCause?.let { throw it }
            return source
        }

    override suspend fun awaitContent(): Boolean {
        closedCause?.let { throw it }
        return false
    }

    override fun cancel(cause: Throwable) {
        if (closed != null) return
        source.close()
        closed = ClosedToken(cause)
    }
}
