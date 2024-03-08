/*
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package io.ktor.utils.io.charsets

import kotlinx.cinterop.*
import kotlinx.io.*
import platform.iconv.*
import platform.posix.*

public actual object Charsets {
    public actual val UTF_8: Charset = CharsetIconv("UTF-8")
    public actual val ISO_8859_1: Charset = CharsetIconv("ISO-8859-1")
    internal val UTF_16: Charset = CharsetIconv(platformUtf16)
}

@OptIn(ExperimentalForeignApi::class)
private class CharsetIconv(name: String) : Charset(name) {
    init {
        val v = iconv_open(name, "UTF-8")
        checkErrors(v, name)
        iconv_close(v)
    }

    override fun newEncoder(): CharsetEncoder = CharsetEncoderImpl(this)
    override fun newDecoder(): CharsetDecoder = CharsetDecoderImpl(this)
}

internal actual fun findCharset(name: String): Charset {
    if (name == "UTF-8" || name == "utf-8" || name == "UTF8" || name == "utf8") return Charsets.UTF_8
    if (name == "ISO-8859-1" || name == "iso-8859-1" || name == "ISO_8859_1") return Charsets.ISO_8859_1
    if (name == "UTF-16" || name == "utf-16" || name == "UTF16" || name == "utf16") return Charsets.UTF_16

    return CharsetIconv(name)
}

internal fun iconvCharsetName(name: String) = when (name) {
    "UTF-16" -> platformUtf16
    else -> name
}

@OptIn(ExperimentalForeignApi::class)
private val negativePointer = (-1L).toCPointer<IntVar>()

@OptIn(ExperimentalForeignApi::class)
internal fun checkErrors(iconvOpenResults: COpaquePointer?, charset: String) {
    if (iconvOpenResults == null || iconvOpenResults === negativePointer) {
        throw IllegalArgumentException("Failed to open iconv for charset $charset with error code ${posix_errno()}")
    }
}

@Suppress("DEPRECATION")
@OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
internal actual fun CharsetEncoder.encodeImpl(input: CharSequence, fromIndex: Int, toIndex: Int, dst: Sink): Int {
    TODO()
}

@Suppress("DEPRECATION")
@OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
public actual fun CharsetDecoder.decode(input: Source, dst: Appendable, max: Int): Int {
    TODO()
}

@Suppress("DEPRECATION")
internal actual fun CharsetEncoder.encodeToByteArrayImpl(
    input: CharSequence,
    fromIndex: Int,
    toIndex: Int
): ByteArray {
    TODO()
}
