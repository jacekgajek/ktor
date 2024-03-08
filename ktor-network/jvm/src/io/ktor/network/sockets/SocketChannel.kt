/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.sockets

import kotlinx.io.*
import kotlinx.io.Buffer
import kotlinx.io.unsafe.*
import java.nio.*
import java.nio.channels.*

@OptIn(SnapshotApi::class, UnsafeIoApi::class)
internal fun WritableByteChannel.write(buffer: Buffer): Long {
    check(this is GatheringByteChannel)
    val tmp = arrayOfNulls<ByteBuffer>(1000)
    var count = 0L
    UnsafeBufferAccessors.readFully(buffer, tmp) { array, startIndex, endIndex ->
        count = write(array, startIndex, endIndex - startIndex)
        count
    }
    return count
}
