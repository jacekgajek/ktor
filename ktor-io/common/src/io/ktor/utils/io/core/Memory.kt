/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io.core

public typealias Memory = ByteArray

public fun <T> withMemory(size: Int, block: (Memory) -> T): T {
    TODO()
}

public fun Memory.storeIntAt(index: Int, value: Int) {
    TODO("Not yet implemented")
}
