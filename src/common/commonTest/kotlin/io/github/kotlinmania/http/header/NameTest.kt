// port-lint: tests src/header/name.rs
package io.github.kotlinmania.http.header

import io.github.kotlinmania.http.ByteStr
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NameTest {
    @Test fun testParseStandardHeaders() {
        for (standard in StandardHeader.entries) {
            val expected = HeaderName.from(standard)
            assertEquals(expected, HeaderName.fromBytes(standard.asStr().encodeToByteArray()).getOrThrow())
            assertEquals(expected, HeaderName.fromBytes(standard.asStr().uppercase().encodeToByteArray()).getOrThrow())
        }
    }

    @Test fun testStandardHeadersIntoBytes() {
        for (standard in StandardHeader.entries) {
            val name = standard.asStr()
            val expected = HeaderName.from(standard)
            for (text in listOf(name, name.uppercase())) {
                val actual = HeaderName.fromBytes(text.encodeToByteArray()).getOrThrow()
                assertContentEquals(name.encodeToByteArray(), actual.intoBytes().asRef())
                assertEquals(expected, actual)
            }
        }
    }

    // Compile-time Send/Sync trait bounds have no corresponding Kotlin type-system guarantee.

    @Test fun testParseInvalidHeaders() {
        for (size in 0 until 128) assertTrue(HeaderName.fromBytes(ByteArray(size) { 1 }).isFailure())
    }

    @Test fun testInvalidNameLengths() {
        assertTrue(HeaderName.fromBytes(byteArrayOf()).isFailure())
        val long = "a".repeat(maxHeaderNameLength)
        assertTrue(HeaderName.fromStatic(long).eq(long))
        assertTrue(HeaderName.fromBytes(long.encodeToByteArray()).isSuccess())
        assertTrue(HeaderName.fromBytes((long + "a").encodeToByteArray()).isFailure())
    }

    @Test fun testStaticInvalidNameLengths() {
        assertFailsWith<IllegalArgumentException> { HeaderName.fromStatic("a".repeat(maxHeaderNameLength + 1)) }
    }

    @Test fun testFromHdrName() {
        assertEquals(HeaderNameRepr.Standard(StandardHeader.Vary), HeaderName.from(HdrName.from(StandardHeader.Vary)).inner)
        for ((text, lower) in listOf("hello-world" to true, "Hello-World" to false)) {
            val name = HeaderName.from(HdrName.custom(text.encodeToByteArray(), lower))
            assertEquals(HeaderNameRepr.Custom(Custom(ByteStr.fromStatic("hello-world"))), name.inner)
        }
    }

    @Test fun testEqHdrName() {
        val standard = HeaderName.from(StandardHeader.Vary)
        val borrowed = HdrName.from(StandardHeader.Vary)
        assertTrue(standard.eq(borrowed))
        val custom = HeaderName.fromStatic("vaary")
        assertFalse(custom.eq(borrowed))
        for ((text, lower) in listOf("vaary" to true, "vaary" to false, "VAARY" to false)) {
            val other = HdrName.custom(text.encodeToByteArray(), lower)
            assertTrue(custom.eq(other))
            assertFalse(standard.eq(other))
            assertEquals(custom.hashCode(), other.hashCode())
        }
    }

    @Test fun testFromStaticStd() {
        assertEquals(HeaderName.from(StandardHeader.Vary), HeaderName.fromStatic("vary"))
        assertNotEquals(HeaderName.from(StandardHeader.Vary), HeaderName.fromStatic("vaary"))
    }

    @Test fun testFromStaticStdUppercase() {
        assertFailsWith<IllegalArgumentException> { HeaderName.fromStatic("Vary") }
    }

    @Test fun testFromStaticStdSymbol() {
        assertFailsWith<IllegalArgumentException> { HeaderName.fromStatic("vary{}") }
    }

    @Test fun testFromStaticCustomShort() {
        assertEquals(HeaderNameRepr.Custom(Custom(ByteStr.fromStatic("customheader"))), HeaderName.fromStatic("customheader").inner)
    }

    @Test fun testFromStaticCustomShortUppercase() {
        assertFailsWith<IllegalArgumentException> { HeaderName.fromStatic("custom header") }
    }

    @Test fun testFromStaticCustomShortSymbol() {
        assertFailsWith<IllegalArgumentException> { HeaderName.fromStatic("CustomHeader") }
    }

    @Test fun testFromStaticCustomLong() {
        val text = "longer-than-63--thisheaderislongerthansixtythreecharactersandthushandleddifferent"
        assertEquals(HeaderNameRepr.Custom(Custom(ByteStr.fromStatic(text))), HeaderName.fromStatic(text).inner)
    }

    @Test fun testFromStaticCustomLongUppercase() {
        assertFailsWith<IllegalArgumentException> {
            HeaderName.fromStatic("Longer-Than-63--ThisHeaderIsLongerThanSixtyThreeCharactersAndThusHandledDifferent")
        }
    }

    @Test fun testFromStaticCustomLongSymbol() {
        assertFailsWith<IllegalArgumentException> {
            HeaderName.fromStatic("longer-than-63--thisheader{}{}{}{}islongerthansixtythreecharactersandthushandleddifferent")
        }
    }

    @Test fun testFromStaticCustomSingleChar() {
        assertEquals(HeaderNameRepr.Custom(Custom(ByteStr.fromStatic("a"))), HeaderName.fromStatic("a").inner)
    }

    @Test fun testFromStaticEmpty() {
        assertFailsWith<IllegalArgumentException> { HeaderName.fromStatic("") }
    }

    @Test fun testAllTokens() {
        HeaderName.fromStatic("!#$%&'*+-.^_`|~0123456789abcdefghijklmnopqrstuvwxyz")
    }

    @Test fun testFromLowercase() {
        for (size in listOf(10, 100)) {
            for (byte in listOf(0, 65, 1, 255)) {
                assertTrue(HeaderName.fromLowercase(ByteArray(size) { byte.toByte() }).isFailure())
            }
        }
    }

    @Test fun ownedNamesDoNotChangeWithTheirInput() {
        for (size in listOf(64, 65)) {
            val input = ByteArray(size) { 'A'.code.toByte() }
            val name = HeaderName.fromBytes(input).getOrThrow()
            input.fill('B'.code.toByte())
            assertEquals("a".repeat(size), name.asStr())
            assertEquals(name, HeaderName.from(name))
        }
    }

    @Test fun allByteValuesFollowTheUpstreamTables() {
        val tokens = "!#$%&'*+-.^_`|~0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        for (value in 0..255) {
            val actual = HeaderName.fromBytes(byteArrayOf(value.toByte()))
            assertEquals(value.toChar() in tokens, actual.isSuccess(), "byte $value")
            if (actual.isSuccess()) assertEquals(value.toChar().lowercaseChar().toString(), actual.getOrThrow().asStr())
        }
        assertTrue(HeaderName.fromLowercase(byteArrayOf(34)).isSuccess())
        assertTrue(HeaderName.fromBytes(byteArrayOf(34)).isFailure())
    }

    @Test fun concreteFailurePayloadIsAvailable() {
        val result = HeaderName.fromStr("bad name")
        assertTrue(result.isFailure())
        assertEquals(null, result.value)
        val error = assertNotNull(result.error)
        assertEquals("invalid HTTP header name", error.message)
        assertEquals("InvalidHeaderName", error.debugString())
        assertFailsWith<IllegalArgumentException> { result.getOrThrow() }
    }

    @Test fun borrowedCallbacksAndStringViews() {
        assertEquals("x-custom", HdrName.fromBytes("X-CUSTOM".encodeToByteArray()) { HeaderName.from(it).asStr() }.getOrThrow())
        assertTrue(HdrName.fromBytes(byteArrayOf()) { HeaderName.from(it) }.isFailure)
        assertEquals("host", HdrName.fromStatic("HOST") { HeaderName.from(it).asStr() })
        assertFailsWith<IllegalArgumentException> { HdrName.fromStatic("") { HeaderName.from(it) } }
        assertEquals("host", HOST.asRef())
        assertEquals("host", HOST.borrow())
        assertEquals("host", HOST.toString())
        assertEquals("\"host\"", HOST.debugString())
        assertTrue(HOST.eq("HOST"))
        assertFalse(HOST.eq("hoſt"))
    }
}
