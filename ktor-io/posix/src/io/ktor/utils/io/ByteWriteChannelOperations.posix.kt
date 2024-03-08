/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
public fun ByteWriteChannel.writeFully(value: CPointer<ByteVarOf<Byte>>, offset: Int, length: Int) {
    TODO("Not yet implemented")
}
