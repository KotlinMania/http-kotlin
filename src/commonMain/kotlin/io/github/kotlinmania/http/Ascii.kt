package io.github.kotlinmania.http

internal fun eqIgnoreAsciiCase(left: String, right: String): Boolean =
    eqIgnoreAsciiCase(left.encodeToByteArray(), right.encodeToByteArray())

internal fun eqIgnoreAsciiCase(left: ByteArray, right: ByteArray): Boolean =
    left.size == right.size && compareAsciiBytes(left, right) == 0

internal fun compareIgnoreAsciiCase(left: String, right: String): Int =
    compareAsciiBytes(left.encodeToByteArray(), right.encodeToByteArray())

private fun compareAsciiBytes(left: ByteArray, right: ByteArray): Int {
    for (i in 0 until minOf(left.size, right.size)) {
        val compared = asciiLowercase(left[i]).compareTo(asciiLowercase(right[i]))
        if (compared != 0) return compared
    }
    return left.size.compareTo(right.size)
}

private fun asciiLowercase(value: Byte): Int {
    val unsigned = value.toInt() and 255
    return if (unsigned in 'A'.code..'Z'.code) unsigned + ('a'.code - 'A'.code) else unsigned
}
