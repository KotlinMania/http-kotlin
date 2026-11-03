package io.github.kotlinmania.http

import io.github.kotlinmania.bytes.Bytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ByteStrOrderingParityTest {
    @Test
    fun supplementaryScalarsSortAfterTheBmpPrivateUseRange() {
        assertTrue(ByteStr.from("\uD800\uDC00") > ByteStr.from("\uE000"))
        assertTrue(ByteStr.from("\uDBFF\uDFFF") > ByteStr.from("\uFFFF"))
    }

    @Test
    fun sortingUsesUtf8ByteOrder() {
        val values = listOf("\uD800\uDC00", "a", "\uE000", "😀", "é", "").map { ByteStr.from(it) }
        assertEquals(listOf("", "a", "é", "\uE000", "\uD800\uDC00", "😀"), values.sorted().map { it.asStr() })
    }

    @Test
    fun checkedAndStaticConstructorsPreserveTheSameOrder() {
        val high = ByteStr.fromUtf8(Bytes.from("\uD800\uDC00".encodeToByteArray())).getOrThrow()
        val low = ByteStr.fromStatic("\uE000")
        assertTrue(high > low)
        assertTrue(low < high)
    }

    @Test
    fun prefixesAsciiAndEqualValuesKeepTheirOrder() {
        for ((left, right) in listOf("" to "a", "x" to "xy", "z" to "é", "😀" to "😀a")) {
            assertTrue(ByteStr.from(left) < ByteStr.from(right))
        }
        assertEquals(0, ByteStr.fromStatic("é").compareTo(ByteStr.from("é")))
    }
}
