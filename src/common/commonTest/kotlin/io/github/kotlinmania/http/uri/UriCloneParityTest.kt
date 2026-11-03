package io.github.kotlinmania.http.uri

import io.github.kotlinmania.bytes.Bytes
import io.github.kotlinmania.http.ByteStr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class UriCloneParityTest {
    @Test
    fun schemes() {
        for (text in listOf("http", "https", "custom+V1")) {
            val original = Scheme.fromStr(text).getOrThrow()
            val clone = original.clone()
            assertEquals(original, clone)
            assertNotSame(original, clone)
            assertEquals(original.hashCode(), clone.hashCode())
            assertEquals(text, clone.asStr())
        }
    }

    @Test
    fun authorities() {
        for (text in listOf("example.org:+80", "user:pass@example.org", "[::1]:443")) {
            val original = Authority.fromStr(text).getOrThrow()
            val clone = original.clone()
            assertEquals(original, clone)
            assertNotSame(original, clone)
            assertEquals(original.host(), clone.host())
            assertEquals(original.portU16(), clone.portU16())
            assertEquals(original.hashCode(), clone.hashCode())
            assertEquals(text, clone.asStr())
        }
    }

    @Test
    fun paths() {
        for (text in listOf("", "/", "?q", "/é?q=🍕#fragment")) {
            val original = PathAndQuery.fromStr(text).getOrThrow()
            val clone = original.clone()
            assertEquals(original, clone)
            assertNotSame(original, clone)
            assertEquals(original.asStr(), clone.asStr())
            assertEquals(original.path(), clone.path())
            assertEquals(original.query(), clone.query())
            assertEquals(original.hashCode(), clone.hashCode())
            assertEquals(original.toString(), clone.toString())
        }
    }

    @Test
    fun uriForms() {
        for (original in listOf(Uri.default(), Uri.fromStr("example.org:80").getOrThrow(), Uri.fromStr("*").getOrThrow(), Uri.fromStr("/a?b").getOrThrow(), Uri.fromStr("http://example.org:+80/é?q=🍕").getOrThrow())) {
            val clone = original.clone()
            assertEquals(original, clone)
            assertNotSame(original, clone)
            assertEquals(original.path(), clone.path())
            assertEquals(original.query(), clone.query())
            assertEquals(original.host(), clone.host())
            assertEquals(original.portU16(), clone.portU16())
            assertEquals(original.hashCode(), clone.hashCode())
            assertEquals(original.toString(), clone.toString())
        }
    }

    @Test
    fun sharedInputCursor() {
        val input = Bytes.copyFromSlice("http://example.org:+80/a?b".encodeToByteArray())
        val original = Uri.fromMaybeShared(input).getOrThrow()
        val clone = original.clone()
        input.advance(7)
        assertEquals("example.org:+80/a?b", input.asRef().decodeToString())
        assertEquals("http://example.org:+80/a?b", original.toString())
        assertEquals("http://example.org:+80/a?b", clone.toString())
    }

    @Test
    fun partsRoundTrip() {
        val original = Uri.fromStr("http://example.org:+80/a?b").getOrThrow()
        val rebuilt = Uri.fromParts(original.clone().intoParts()).getOrThrow()
        assertEquals(original, rebuilt)
        assertEquals(80, rebuilt.portU16())
    }

    @Test
    fun byteStrCanonicalHandleControl() {
        val input = Bytes.copyFromSlice("abc".encodeToByteArray())
        val original = ByteStr.fromUtf8(input).getOrThrow()
        val clone = original.clone()
        input.advance(1)
        assertEquals("bc", original.asStr())
        assertEquals("abc", clone.asStr())
    }
}
