package io.github.kotlinmania.http.uri

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PathUnicodeParityTest {
    @Test
    fun utf8PathAndQueryUseByteBoundaries() {
        for ((text, path, query) in listOf(Triple("/é?q=1", "/é", "q=1"), Triple("/é😀?q=é😀", "/é😀", "q=é😀"))) {
            val value = PathAndQuery.fromStr(text).getOrThrow()
            assertEquals(path, value.path())
            assertEquals(query, value.query())
            assertEquals(text, value.asStr())
            assertEquals(path.encodeToByteArray().size, value.query)
        }
    }

    @Test
    fun supplementaryPathSupportsAnEmptyQuery() {
        for (value in listOf(PathAndQuery.fromStr("/😀?").getOrThrow(), PathAndQuery.fromStatic("/😀?"))) {
            assertEquals("/😀", value.path())
            assertEquals("", value.query())
        }
    }

    @Test
    fun fragmentsAreRemovedWithoutChangingUnicodeParts() {
        val value = PathAndQuery.fromStr("/é?q=☃#ignored").getOrThrow()
        assertEquals("/é", value.path())
        assertEquals("q=☃", value.query())
        assertEquals("/é?q=☃", value.asStr())
    }

    @Test
    fun uriDelegatesPathAndQueryWithoutUtf16OffsetErrors() {
        val uri = Uri.fromStr("http://example/é😀?q=☃").getOrThrow()
        assertEquals("/é😀", uri.path())
        assertEquals("q=☃", uri.query())
    }

    @Test
    fun orderingUsesUnsignedUtf8Bytes() {
        val high = PathAndQuery.fromStr("/\uD800\uDC00").getOrThrow()
        val low = PathAndQuery.fromStr("/\uE000").getOrThrow()
        assertTrue(high > low)
        assertTrue(high.compareTo("/\uE000") > 0)
        val queryHigh = PathAndQuery.fromStr("/?q=\uD800\uDC00").getOrThrow()
        val queryLow = PathAndQuery.fromStr("/?q=\uE000").getOrThrow()
        assertTrue(queryHigh > queryLow)
    }

    @Test
    fun emptyAndCaseSensitiveOrderingKeepSourceBehavior() {
        val empty = PathAndQuery.fromStatic("")
        val slash = PathAndQuery.fromStatic("/")
        assertEquals("/", empty.path())
        assertNull(empty.query())
        assertEquals(0, empty.compareTo(slash))
        assertNotEquals(empty, slash)
        assertTrue(PathAndQuery.fromStatic("/A").compareTo("/a") < 0)
        val unicode = PathAndQuery.fromStr("/😀").getOrThrow()
        assertEquals("/😀", unicode.path())
        assertNull(unicode.query())
    }

    @Test
    fun invalidUtf8StillReturnsInvalidUri() {
        for (bytes in listOf(byteArrayOf(47, -1, 63, 120), byteArrayOf(47, 120, 63, -1))) {
            assertIs<InvalidUri>(PathAndQuery.tryFrom(bytes).exceptionOrNull())
        }
    }

    @Test
    fun queryOffsetKeepsUpstreamUnsignedNarrowing() {
        for (pos in listOf(65534, 65535, 65536)) {
            val text = "/" + "a".repeat(pos - 1) + "?q"
            for (value in listOf(PathAndQuery.fromStr(text).getOrThrow(), PathAndQuery.fromStatic(text))) {
                when (pos) {
                    65534 -> {
                        assertEquals(65534, value.path().length)
                        assertEquals("q", value.query())
                    }

                    65535 -> {
                        assertEquals(65537, value.path().length)
                        assertNull(value.query())
                    }

                    else -> {
                        assertEquals("/", value.path())
                        assertEquals(text.substring(1), value.query())
                    }
                }
            }
        }
    }
}
