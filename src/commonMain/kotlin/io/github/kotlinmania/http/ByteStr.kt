// port-lint: source byte_str.rs
package io.github.kotlinmania.http

import io.github.kotlinmania.bytes.Bytes

private typealias Target = String

internal class ByteStr private constructor(
    // Invariant: bytes contains valid UTF-8
    private val bytes: Bytes,
) : Comparable<ByteStr> {
    companion object {
        fun new(): ByteStr {
            return ByteStr(
                // Invariant: the empty slice is trivially valid UTF-8.
                Bytes.new(),
            )
        }

        fun fromStatic(value: String): ByteStr {
            // Invariant: value is a String so contains valid UTF-8.
            return ByteStr(Bytes.fromStatic(value))
        }

        /**
         * ## Panics
         * In a debug build this will panic if `bytes` is not valid UTF-8.
         *
         * ## Safety
         * `bytes` must contain valid UTF-8. In a release build it is undefined
         * behavior to call this with `bytes` that is not valid UTF-8.
         */
        fun fromUtf8Unchecked(bytes: Bytes): ByteStr {
            bytes.asRef().decodeToString(throwOnInvalidSequence = true)
            // Invariant: assumed by the safety requirements of this function.
            return ByteStr(bytes)
        }

        fun fromUtf8(bytes: Bytes): Result<ByteStr> {
            return runCatching {
                bytes.asRef().decodeToString(throwOnInvalidSequence = true)
                // Invariant: just checked is utf8
                ByteStr(bytes)
            }
        }

        fun from(src: String): ByteStr {
            return ByteStr(
                // Invariant: src is a String so contains valid UTF-8.
                Bytes.from(src),
            )
        }
    }

    fun deref(): Target {
        val b = bytes.asRef()
        // Safety: the invariant of `bytes` is that it contains valid UTF-8.
        return b.decodeToString()
    }

    fun asStr(): String {
        return deref()
    }

    fun intoBytes(): Bytes {
        return bytes
    }

    override fun toString(): String {
        return asStr()
    }

    override fun compareTo(other: ByteStr): Int {
        return asStr().compareTo(other.asStr())
    }

    override fun equals(other: Any?): Boolean {
        return other is ByteStr && bytes == other.bytes
    }

    override fun hashCode(): Int {
        return bytes.hashCode()
    }
}
