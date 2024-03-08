/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.util

import kotlinx.io.*
import kotlinx.io.Buffer
import kotlinx.io.unsafe.*
import java.nio.*
import java.nio.channels.*

/**
 * Read from a NIO channel into the specified [buffer]
 * Could return `0` if the channel is non-blocking or [buffer] has no free space
 * @return number of bytes read (possibly 0) or -1 if EOF
 */
@OptIn(SnapshotApi::class, UnsafeIoApi::class)
public fun ReadableByteChannel.read(buffer: Buffer): Int {
    var count = 0
    var done = false
    var eof = false
    while (!done) {
        UnsafeBufferAccessors.writeToTail(buffer, 1) { array, start, endExclusive ->
            val buffer = ByteBuffer.wrap(array, start, endExclusive - start)
            when (val rc = read(buffer)) {
                -1 -> {
                    eof = true
                    done = true
                    0
                }
                0 -> {
                    done = true
                    0
                }
                else -> {
                    count += rc
                    rc
                }
            }
        }
    }

    return if (count == 0 && eof) -1 else count
}
