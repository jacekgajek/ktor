/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io.core

import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
public fun BytePacketBuilder.writeFully(buffer: CPointer<ByteVarOf<Byte>>, offset: Int, length: Long) {
    TODO("Not yet implemented")
}

@OptIn(ExperimentalForeignApi::class)
public fun BytePacketBuilder.readAvailable(buffer: CPointer<ByteVarOf<Byte>>) {
    TODO("Not yet implemented")
}
