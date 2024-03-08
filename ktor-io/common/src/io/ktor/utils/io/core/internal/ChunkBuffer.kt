package io.ktor.utils.io.core.internal

import io.ktor.utils.io.*

@Suppress("DEPRECATION")
@Deprecated(IO_DEPRECATION_MESSAGE, replaceWith = ReplaceWith("Buffer", "kotlinx.io"))
public typealias ChunkBuffer = kotlinx.io.Buffer

public val ChunkBuffer.writeRemaining: Int
    get() {
        TODO("Not yet implemented")
    }
