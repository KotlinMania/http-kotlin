// port-lint: source header/value.rs
package io.github.kotlinmania.http.header

import io.github.kotlinmania.bytes.Bytes

/**
 * Represents an HTTP header field value.
 *
 * Values may contain opaque bytes that cannot be represented as text.
 * [toStr] returns an error when non-visible ASCII bytes are present.
 */
class HeaderValue private constructor(
    private val inner: Bytes,
    private var sensitive: Boolean,
) : Comparable<HeaderValue> {
    companion object {
        /** Constructs an ASCII value, throwing if any byte is invalid. Tab is permitted. */
        fun fromStatic(src: String): HeaderValue {
            for (byte in src.encodeToByteArray()) {
                require(isVisibleAscii(byte)) { "HeaderValue::from_static with invalid bytes" }
            }
            return HeaderValue(Bytes.fromStatic(src), false)
        }

        /** Converts a string, returning [InvalidHeaderValue] for invalid field-value bytes. */
        fun fromStr(src: String): Result<HeaderValue> = fromBytes(src.encodeToByteArray())

        /** Converts a header name to a value containing its normalized bytes. */
        fun fromName(name: HeaderName): HeaderValue = HeaderValue(name.intoBytes().clone(), false)

        /**
         * Converts bytes to a field value. Tab and bytes 32 through 255 are
         * permitted, except for byte 127 (DEL).
         */
        fun fromBytes(src: ByteArray): Result<HeaderValue> = checked(src) { Bytes.copyFromSlice(src) }

        /** Converts a shared byte buffer, retaining an independent owned handle. */
        fun fromMaybeShared(src: Bytes): Result<HeaderValue> {
            val owned = src.clone()
            return checked(owned.asRef()) { owned }
        }

        fun fromMaybeShared(src: ByteArray): Result<HeaderValue> = fromBytes(src)

        fun fromMaybeShared(src: String): Result<HeaderValue> = fromStr(src)

        fun from(name: HeaderName): HeaderValue = fromName(name)

        fun from(value: HeaderValue): HeaderValue = value.clone()

        fun tryFrom(src: String): Result<HeaderValue> = fromStr(src)

        fun tryFrom(src: ByteArray): Result<HeaderValue> = fromBytes(src)

        fun tryFrom(name: HeaderName): Result<HeaderValue> = Result.success(fromName(name))

        private inline fun checked(bytes: ByteArray, into: () -> Bytes): Result<HeaderValue> {
            for (byte in bytes) {
                if (!isValidHeaderValueByte(byte)) return Result.failure(InvalidHeaderValue.new())
            }
            return Result.success(HeaderValue(into(), false))
        }
    }

    /** Scans the bytes, returning text only when all bytes are visible ASCII or tab. */
    fun toStr(): Result<String> {
        val bytes = asBytes()
        for (byte in bytes) {
            if (!isVisibleAscii(byte)) return Result.failure(ToStrError.new())
        }
        return Result.success(bytes.decodeToString())
    }

    /** Returns the number of bytes in this value. */
    fun len(): Int = inner.len()

    fun isEmpty(): Boolean = len() == 0

    /** Returns the header bytes. */
    fun asBytes(): ByteArray = inner.asRef()

    fun asRef(): ByteArray = asBytes()

    /** Marks whether this value represents sensitive information. */
    fun setSensitive(value: Boolean) {
        sensitive = value
    }

    /**
     * Returns whether the value is marked sensitive. Components may use this
     * flag for caching or encoding decisions. Debug output masks sensitive
     * values. Sensitivity is not factored into equality or ordering.
     */
    fun isSensitive(): Boolean = sensitive

    fun clone(): HeaderValue = HeaderValue(inner.clone(), sensitive)

    fun eq(other: HeaderValue): Boolean = inner == other.inner

    fun eq(other: String): Boolean = asBytes().contentEquals(other.encodeToByteArray())

    fun eq(other: ByteArray): Boolean = asBytes().contentEquals(other)

    override fun equals(other: Any?): Boolean = other is HeaderValue && eq(other)

    override fun hashCode(): Int = inner.hashCode()

    override fun compareTo(other: HeaderValue): Int = inner.compareTo(other.inner)

    operator fun compareTo(other: String): Int = inner.compareTo(Bytes.from(other))

    operator fun compareTo(other: ByteArray): Int = inner.compareTo(Bytes.copyFromSlice(other))

    fun cmp(other: HeaderValue): Int = compareTo(other)

    fun partialCmp(other: HeaderValue): Int? = compareTo(other)

    fun partialCmp(other: String): Int? = compareTo(other)

    fun partialCmp(other: ByteArray): Int? = compareTo(other)

    /** Formats the value for debugging, masking values marked sensitive. */
    fun debugString(): String {
        if (sensitive) return "Sensitive"
        val output = StringBuilder("\"")
        val bytes = asBytes()
        var from = 0
        for (i in bytes.indices) {
            val byte = bytes[i]
            if (!isVisibleAscii(byte) || byte == '"'.code.toByte()) {
                if (from != i) output.append(bytes.decodeToString(startIndex = from, endIndex = i))
                if (byte == '"'.code.toByte()) {
                    output.append("\\\"")
                } else {
                    output.append("\\x").append((byte.toInt() and 255).toString(16))
                }
                from = i + 1
            }
        }
        output.append(bytes.decodeToString(startIndex = from))
        return output.append('"').toString()
    }

    override fun toString(): String = debugString()
}

/** An error converting a string or byte slice to a header value. */
class InvalidHeaderValue private constructor() : IllegalArgumentException("failed to parse header value") {
    companion object {
        internal fun new(): InvalidHeaderValue = InvalidHeaderValue()
    }

    fun debugString(): String = "InvalidHeaderValue"

    override fun toString(): String = message!!
}

/** A header value contains opaque bytes and cannot be represented as text. */
class ToStrError private constructor() : IllegalArgumentException("failed to convert header to a str") {
    companion object {
        internal fun new(): ToStrError = ToStrError()
    }

    fun debugString(): String = "ToStrError { _priv: () }"

    override fun toString(): String = message!!
}

private fun isVisibleAscii(byte: Byte): Boolean {
    val value = byte.toInt() and 255
    return value in 32..126 || value == 9
}

private fun isValidHeaderValueByte(byte: Byte): Boolean {
    val value = byte.toInt() and 255
    return (value >= 32 && value != 127) || value == 9
}
