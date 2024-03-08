/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import kotlinx.coroutines.*

public fun ByteChannel(autoFlush: Boolean = false): ByteChannel = ByteChannel()

public fun ByteChannel.attachJob(job: Job) {}

public fun ByteChannel.attachJob(job: ChannelJob) {
    attachJob(job.job)
}

public fun ByteChannel(block: (Throwable?) -> Throwable?): ByteChannel = ByteChannel()
