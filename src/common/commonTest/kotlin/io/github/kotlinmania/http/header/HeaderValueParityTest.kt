package io.github.kotlinmania.http.header

import io.github.kotlinmania.bytes.Bytes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HeaderValueParityTest {
    @Test
    fun allBytesHaveTheSourceValidationAndTextContract() {
        var accepted = 0
        var text = 0
        for (byte in 0..255) {
            val raw = byteArrayOf(byte.toByte())
            val result: Result<HeaderValue> = HeaderValue.fromBytes(raw)
            assertEquals(byte == 9 || byte in 32..126 || byte >= 128, result.isSuccess)
            if (result.isSuccess) {
                accepted++
                val value = result.getOrThrow()
                assertContentEquals(raw, value.asBytes())
                assertEquals(byte == 9 || byte in 32..126, value.toStr().isSuccess)
                if (value.toStr().isSuccess) text++ else assertIs<ToStrError>(value.toStr().exceptionOrNull())
            } else {
                assertIs<InvalidHeaderValue>(result.exceptionOrNull())
            }
        }
        assertEquals(224, accepted)
        assertEquals(96, text)
    }

    @Test
    fun emptyAndAsciiValuesHaveTextAndLength() {
        for (input in listOf("", "hello", " \t~")) {
            val value = HeaderValue.fromStatic(input)
            assertEquals(input, value.toStr().getOrThrow())
            assertEquals(input.length, value.len())
            assertEquals(input.isEmpty(), value.isEmpty())
            assertFalse(value.isSensitive())
        }
    }

    @Test
    fun unicodeStringInputIsOpaqueButNotText() {
        val value = HeaderValue.fromStr("é").getOrThrow()
        assertContentEquals("é".encodeToByteArray(), value.asBytes())
        assertIs<ToStrError>(value.toStr().exceptionOrNull())
        assertEquals("\"\\xc3\\xa9\"", value.debugString())
    }

    @Test
    fun staticConstructorRejectsNonvisibleCharacters() {
        for (input in listOf("\n", "\r", "\u0000", "\u007F", "é")) {
            assertFailsWith<IllegalArgumentException> { HeaderValue.fromStatic(input) }
        }
    }

    @Test
    fun headerNameConversionPreservesNormalizedBytes() {
        val name = HeaderName.fromStr("X-Custom").getOrThrow()
        assertEquals("x-custom", HeaderValue.fromName(name).toStr().getOrThrow())
        assertEquals("content-type", HeaderValue.from(HeaderName.fromStatic("content-type")).toStr().getOrThrow())
    }

    @Test
    fun cloningKeepsIndependentSensitivityAndValueIdentity() {
        val value = HeaderValue.fromStatic("secret")
        value.setSensitive(true)
        val copy = HeaderValue.from(value)
        assertTrue(copy.isSensitive())
        copy.setSensitive(false)
        assertTrue(value.isSensitive())
        assertFalse(copy.isSensitive())
        assertEquals(value, copy)
        assertEquals(value.hashCode(), copy.hashCode())
        assertEquals(0, value.compareTo(copy))
        assertEquals("Sensitive", value.debugString())
        assertEquals("secret", copy.toStr().getOrThrow())
    }

    @Test
    fun inputAndReturnedMutableArraysCannotChangeTheValue() {
        val input = "hello".encodeToByteArray()
        val value = HeaderValue.fromBytes(input).getOrThrow()
        input[0] = 0
        val returned = value.asBytes()
        returned[0] = 0
        assertEquals("hello", value.toStr().getOrThrow())
        assertContentEquals("hello".encodeToByteArray(), value.asRef())
    }

    @Test
    fun sharedBytesKeepAnIndependentOwnedCursor() {
        val input = Bytes.from("hello")
        val value = HeaderValue.fromMaybeShared(input).getOrThrow()
        input.advance(1)
        assertEquals("hello", value.toStr().getOrThrow())
    }

    @Test
    fun checkedConstructorAdaptersMatch() {
        assertTrue(HeaderValue.fromMaybeShared("hello").getOrThrow().eq("hello"))
        assertTrue(HeaderValue.fromMaybeShared("hello".encodeToByteArray()).getOrThrow().eq("hello"))
        assertTrue(HeaderValue.tryFrom("hello").getOrThrow().eq("hello"))
        assertTrue(HeaderValue.tryFrom("hello".encodeToByteArray()).getOrThrow().eq("hello"))
        assertTrue(HeaderValue.tryFrom(HeaderName.fromStatic("accept")).getOrThrow().eq("accept"))
    }

    @Test
    fun debugEscapingIsExact() {
        val cases =
            listOf(
                "hello" to "\"hello\"",
                "hello \"world\"" to "\"hello \\\"world\\\"\"",
                "\u7FFFhello" to "\"\\xe7\\xbf\\xbfhello\"",
                "\t\\" to ("\"" + "\t\\" + "\""),
            )
        for ((input, expected) in cases) {
            assertEquals(expected, HeaderValue.fromStr(input).getOrThrow().debugString())
        }
        assertEquals("\"\\x80\\xff\"", HeaderValue.fromBytes(byteArrayOf(-128, -1)).getOrThrow().debugString())
    }

    @Test
    fun equalityAndOrderingUseCaseSensitiveUnsignedBytes() {
        val value = HeaderValue.fromBytes(byteArrayOf(-1)).getOrThrow()
        assertTrue(value.compareTo(byteArrayOf(-128)) > 0)
        assertTrue(value.compareTo("é") > 0)
        assertTrue(value.eq(byteArrayOf(-1)))
        val lower = HeaderValue.fromStatic("a")
        assertFalse(lower.eq("A"))
        assertTrue(lower.compareTo("A") > 0)
        assertTrue(lower.cmp(HeaderValue.fromStatic("A")) > 0)
        assertTrue(lower.partialCmp("A")!! > 0)
    }

    @Test
    fun errorMessagesAndDebugMatchSource() {
        val invalid = assertIs<InvalidHeaderValue>(HeaderValue.fromBytes(byteArrayOf(10)).exceptionOrNull())
        assertEquals("failed to parse header value", invalid.toString())
        assertEquals("InvalidHeaderValue", invalid.debugString())
        val nontext = HeaderValue.fromBytes(byteArrayOf(-1)).getOrThrow()
        val err = assertIs<ToStrError>(nontext.toStr().exceptionOrNull())
        assertEquals("failed to convert header to a str", err.toString())
        assertEquals("ToStrError { _priv: () }", err.debugString())
    }
}
