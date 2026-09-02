// port-lint: tests uri/path.rs
package io.github.kotlinmania.http.uri

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PathAndQueryTest {
    @Test
    fun equalToSelfOfSamePath() {
        val p1: PathAndQuery = PathAndQuery.fromStr("/hello/world&foo=bar").getOrThrow()
        val p2: PathAndQuery = PathAndQuery.fromStr("/hello/world&foo=bar").getOrThrow()
        assertEquals(p1, p2)
        assertEquals(p2, p1)
    }

    @Test
    fun notEqualToSelfOfDifferentPath() {
        val p1: PathAndQuery = PathAndQuery.fromStr("/hello/world&foo=bar").getOrThrow()
        val p2: PathAndQuery = PathAndQuery.fromStr("/world&foo=bar").getOrThrow()
        assertNotEquals(p1, p2)
        assertNotEquals(p2, p1)
    }

    @Test
    fun equatesWithAStr() {
        val pathAndQuery: PathAndQuery = PathAndQuery.fromStr("/hello/world&foo=bar").getOrThrow()
        assertTrue(pathAndQuery.eq("/hello/world&foo=bar"))
        assertEquals("/hello/world&foo=bar", pathAndQuery.asStr())
    }

    @Test
    fun notEqualWithAStrOfADifferentPath() {
        val pathAndQuery: PathAndQuery = PathAndQuery.fromStr("/hello/world&foo=bar").getOrThrow()
        assertFalse(pathAndQuery.eq("/hello&foo=bar"))
    }

    @Test
    fun equatesWithAString() {
        val pathAndQuery: PathAndQuery = PathAndQuery.fromStr("/hello/world&foo=bar").getOrThrow()
        assertTrue(pathAndQuery.eq("/hello/world&foo=bar"))
    }

    @Test
    fun notEqualWithAStringOfADifferentPath() {
        val pathAndQuery: PathAndQuery = PathAndQuery.fromStr("/hello/world&foo=bar").getOrThrow()
        assertFalse(pathAndQuery.eq("/hello&foo=bar"))
    }

    @Test
    fun comparesToSelf() {
        val p1: PathAndQuery = PathAndQuery.fromStr("/a/world&foo=bar").getOrThrow()
        val p2: PathAndQuery = PathAndQuery.fromStr("/b/world&foo=bar").getOrThrow()
        assertTrue(p1 < p2)
        assertTrue(p2 > p1)
    }

    @Test
    fun comparesWithAStr() {
        val pathAndQuery: PathAndQuery = PathAndQuery.fromStr("/b/world&foo=bar").getOrThrow()
        assertTrue(pathAndQuery.compareTo("/c/world&foo=bar") < 0)
        assertTrue(pathAndQuery.compareTo("/a/world&foo=bar") > 0)
    }

    @Test
    fun ignoresValidPercentEncodings() {
        assertEquals("/a%20b", pq("/a%20b?r=1").path())
        assertEquals("qr=%31", pq("/a/b?qr=%31").query())
    }

    @Test
    fun ignoresInvalidPercentEncodings() {
        assertEquals("/a%%b", pq("/a%%b?r=1").path())
        assertEquals("/aaa%", pq("/aaa%").path())
        assertEquals("/aaa%", pq("/aaa%?r=1").path())
        assertEquals("/aa%2", pq("/aa%2").path())
        assertEquals("/aa%2", pq("/aa%2?r=1").path())
        assertEquals("qr=%3", pq("/a/b?qr=%3").query())
    }

    @Test
    fun allowUtf8InPath() {
        assertEquals("/🍕", pq("/🍕").path())
    }

    @Test
    fun allowUtf8InQuery() {
        assertEquals("pizza=🍕", pq("/test?pizza=🍕").query())
    }

    @Test
    fun rejectsInvalidUtf8InPath() {
        assertTrue(PathAndQuery.tryFrom(byteArrayOf('/'.code.toByte(), 0xFF.toByte())).isFailure)
    }

    @Test
    fun rejectsInvalidUtf8InQuery() {
        assertTrue(PathAndQuery.tryFrom(byteArrayOf('/'.code.toByte(), 'a'.code.toByte(), '?'.code.toByte(), 0xFF.toByte())).isFailure)
    }

    @Test
    fun jsonIsFine() {
        assertEquals(
            """/{"bread":"baguette"}""",
            pq("""/{"bread":"baguette"}""").path(),
        )
    }

    private fun pq(s: String): PathAndQuery =
        PathAndQuery.fromStr(s).getOrThrow()
}
