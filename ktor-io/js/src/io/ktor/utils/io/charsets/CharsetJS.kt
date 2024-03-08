/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io.charsets

import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.charsets.CharsetDecoder
import io.ktor.utils.io.charsets.CharsetEncoder
import io.ktor.utils.io.charsets.Charsets
import kotlinx.io.*

/**
 * Find a charset by name.
 */
public actual fun Charsets.forName(name: String): io.ktor.utils.io.charsets.Charset = Charset.forName(name)

/**
 * Check if a charset is supported by the current platform.
 */
public actual fun Charsets.isSupported(name: String): Boolean = Charset.isSupported(name)

public actual abstract class Charset(internal val _name: String) {
    public actual abstract fun newEncoder(): CharsetEncoder
    public actual abstract fun newDecoder(): CharsetDecoder

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false

        other as io.ktor.utils.io.charsets.Charset

        if (_name != other._name) return false

        return true
    }

    override fun hashCode(): Int {
        return _name.hashCode()
    }

    override fun toString(): String {
        return _name
    }

    public companion object {
        public fun forName(name: String): io.ktor.utils.io.charsets.Charset {
            if (name == "UTF-8" || name == "utf-8" || name == "UTF8" || name == "utf8") return Charsets.UTF_8
            if (name == "ISO-8859-1" || name == "iso-8859-1" ||
                name.replace('_', '-').let { it == "iso-8859-1" || it.lowercase() == "iso-8859-1" } ||
                name == "latin1" || name == "Latin1"
            ) {
                return Charsets.ISO_8859_1
            }
            throw IllegalArgumentException("Charset $name is not supported")
        }

        public fun isSupported(charset: String): Boolean = when {
            charset == "UTF-8" || charset == "utf-8" || charset == "UTF8" || charset == "utf8" -> true
            charset == "ISO-8859-1" || charset == "iso-8859-1" || charset.replace('_', '-').let {
                it == "iso-8859-1" || it.lowercase() == "iso-8859-1"
            } || charset == "latin1" -> true

            else -> false
        }
    }
}

public actual val io.ktor.utils.io.charsets.Charset.name: String get() = _name

// -----------------------

public actual abstract class CharsetEncoder(internal val _charset: io.ktor.utils.io.charsets.Charset)
private data class CharsetEncoderImpl(private val charset: io.ktor.utils.io.charsets.Charset) : CharsetEncoder(charset)

public actual val CharsetEncoder.charset: io.ktor.utils.io.charsets.Charset get() = _charset

public actual fun CharsetEncoder.encodeToByteArray(input: CharSequence, fromIndex: Int, toIndex: Int): ByteArray =
    encodeToByteArrayImpl(input, fromIndex, toIndex)

@Suppress("DEPRECATION")
internal actual fun CharsetEncoder.encodeImpl(
    input: CharSequence,
    fromIndex: Int,
    toIndex: Int,
    dst: Sink
): Int {
    TODO()
}

// ----------------------------------------------------------------------

public actual abstract class CharsetDecoder(internal val _charset: io.ktor.utils.io.charsets.Charset)

private data class CharsetDecoderImpl(private val charset: io.ktor.utils.io.charsets.Charset) : CharsetDecoder(charset)

public actual val CharsetDecoder.charset: io.ktor.utils.io.charsets.Charset get() = _charset

internal actual fun CharsetEncoder.encodeToByteArrayImpl(
    input: CharSequence,
    fromIndex: Int,
    toIndex: Int
): ByteArray {
    var start = fromIndex
    if (start >= toIndex) return ByteArray(0)

    val dst = Buffer()
    val rc = encodeImpl(input, start, toIndex, dst)
    start += rc

    if (start == toIndex) {
        return dst.readByteArray()
    }

    encodeToImpl(dst, input, start, toIndex)
    return dst.readByteArray()
}

public actual fun CharsetDecoder.decode(input: Source, dst: Appendable, max: Int): Int {
    TODO()
}

public actual object Charsets {
    public actual val UTF_8: io.ktor.utils.io.charsets.Charset = CharsetImpl("UTF-8")
    public actual val ISO_8859_1: io.ktor.utils.io.charsets.Charset = CharsetImpl("ISO-8859-1")
}

private data class CharsetImpl(val name: String) : io.ktor.utils.io.charsets.Charset(name) {
    override fun newEncoder(): CharsetEncoder = CharsetEncoderImpl(this)
    override fun newDecoder(): CharsetDecoder = CharsetDecoderImpl(this)
}

public actual open class MalformedInputException actual constructor(message: String) : Throwable(message)
