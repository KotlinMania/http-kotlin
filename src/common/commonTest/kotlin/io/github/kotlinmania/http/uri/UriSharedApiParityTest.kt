package io.github.kotlinmania.http.uri

import io.github.kotlinmania.bytes.Bytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UriSharedApiParityTest {
    private fun check(input: String, display: String, path: String, query: String?, host: String?, port: Int?) {
        val values =
            listOf(
                Uri.fromMaybeShared(Bytes.copyFromSlice(input.encodeToByteArray())),
                Uri.fromMaybeShared(input.encodeToByteArray()),
                Uri.fromMaybeShared(input),
            )
        for (result in values) {
            val value = result.getOrThrow()
            assertEquals(display, value.toString())
            assertEquals(path, value.path())
            assertEquals(query, value.query())
            assertEquals(host, value.host())
            assertEquals(port, value.portU16())
        }
    }

    @Test
    fun uriForms() {
        check("/", "/", "/", null, null, null)
        check("*", "*", "*", null, null, null)
        check("example.org:80", "example.org:80", "", null, "example.org", 80)
        check("/a?b", "/a?b", "/a", "b", null, null)
        check("http://example.org:+80/a?b", "http://example.org:+80/a?b", "/a", "b", "example.org", 80)
    }

    @Test
    fun utf8AndFragments() {
        check("/é?q=🍕", "/é?q=🍕", "/é", "q=🍕", null, null)
        check("http://example.org/é?q=🍕#ignored", "http://example.org/é?q=🍕", "/é", "q=🍕", "example.org", null)
    }

    @Test
    fun invalidSyntax() {
        for (result in listOf(Uri.fromMaybeShared("".encodeToByteArray()), Uri.fromMaybeShared(Bytes.copyFromSlice("".encodeToByteArray())), Uri.fromMaybeShared(""))) {
            assertIs<InvalidUri>(result.exceptionOrNull())
        }
        for (result in listOf(Uri.fromMaybeShared("http:///x".encodeToByteArray()), Uri.fromMaybeShared(Bytes.copyFromSlice("http:///x".encodeToByteArray())), Uri.fromMaybeShared("http:///x"))) {
            assertIs<InvalidUri>(result.exceptionOrNull())
        }
        for (result in listOf(Uri.fromMaybeShared("http://exa mple.org/x".encodeToByteArray()), Uri.fromMaybeShared(Bytes.copyFromSlice("http://exa mple.org/x".encodeToByteArray())), Uri.fromMaybeShared("http://exa mple.org/x"))) {
            assertIs<InvalidUri>(result.exceptionOrNull())
        }
        for (result in listOf(Uri.fromMaybeShared("/a b".encodeToByteArray()), Uri.fromMaybeShared(Bytes.copyFromSlice("/a b".encodeToByteArray())), Uri.fromMaybeShared("/a b"))) {
            assertIs<InvalidUri>(result.exceptionOrNull())
        }
        for (result in listOf(Uri.fromMaybeShared("http://[::1/x".encodeToByteArray()), Uri.fromMaybeShared(Bytes.copyFromSlice("http://[::1/x".encodeToByteArray())), Uri.fromMaybeShared("http://[::1/x"))) {
            assertIs<InvalidUri>(result.exceptionOrNull())
        }
    }

    @Test
    fun invalidUtf8() {
        for (result in listOf(Uri.fromMaybeShared(byteArrayOf(47, -61, 40)), Uri.fromMaybeShared(Bytes.copyFromSlice(byteArrayOf(47, -61, 40))))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals(ErrorKind.InvalidUriChar, error.kind)
        }
        for (result in listOf(Uri.fromMaybeShared(byteArrayOf(47, 63, -1)), Uri.fromMaybeShared(Bytes.copyFromSlice(byteArrayOf(47, 63, -1))))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals(ErrorKind.InvalidUriChar, error.kind)
        }
    }

    @Test
    fun byteLengthBoundary() {
        for (valid in listOf("/" + "a".repeat(65533), "/" + "é".repeat(32766) + "a")) {
            assertEquals(valid, Uri.fromMaybeShared(valid).getOrThrow().path())
        }
        for (invalid in listOf("/" + "a".repeat(65534), "/" + "é".repeat(32767), "/#" + "a".repeat(65533))) {
            for (result in listOf(Uri.fromMaybeShared(invalid), Uri.fromMaybeShared(invalid.encodeToByteArray()), Uri.fromMaybeShared(Bytes.copyFromSlice(invalid.encodeToByteArray())))) {
                val error = assertIs<InvalidUri>(result.exceptionOrNull())
                assertEquals(ErrorKind.TooLong, error.kind)
                assertEquals("uri too long", error.message)
            }
        }
    }

    @Test
    fun sharedHandleIndependence() {
        val original = Bytes.copyFromSlice("http://example.org:+80/a?b".encodeToByteArray())
        val value = Uri.fromMaybeShared(original).getOrThrow()
        original.advance(7)
        assertEquals("example.org:+80/a?b", original.asRef().decodeToString())
        assertEquals("http://example.org:+80/a?b", value.toString())
        assertEquals("example.org", value.host())
        assertEquals(80, value.portU16())
    }

    @Test
    fun byteArrayCopyIndependence() {
        val original = "http://example.org/a".encodeToByteArray()
        val value = Uri.fromMaybeShared(original).getOrThrow()
        original[0] = 'x'.code.toByte()
        assertEquals("http://example.org/a", value.toString())
    }

    @Test
    fun partsRoundTrip() {
        val value = Uri.fromMaybeShared("http://example.org:+80/a?b").getOrThrow()
        val rebuilt = Uri.fromParts(value.intoParts()).getOrThrow()
        assertEquals("http://example.org:+80/a?b", rebuilt.toString())
        assertEquals(80, rebuilt.portU16())
    }
}
