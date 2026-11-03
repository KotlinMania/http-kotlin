package io.github.kotlinmania.http.uri

import io.github.kotlinmania.bytes.Bytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PathSharedApiParityTest {
    private fun check(input: String, repr: String, path: String, query: String?, display: String) {
        val values =
            listOf(
                PathAndQuery.fromMaybeShared(Bytes.copyFromSlice(input.encodeToByteArray())),
                PathAndQuery.fromMaybeShared(input.encodeToByteArray()),
                PathAndQuery.fromMaybeShared(input),
            )
        for (result in values) {
            val value = result.getOrThrow()
            assertEquals(repr, value.asStr())
            assertEquals(path, value.path())
            assertEquals(query, value.query())
            assertEquals(display, value.toString())
        }
    }

    @Test
    fun textForms() {
        check("/a?b", "/a?b", "/a", "b", "/a?b")
        check("", "/", "/", null, "/")
        check("?q", "?q", "/", "q", "/?q")
        check("/?", "/?", "/", "", "/?")
        check("*", "*", "*", null, "*")
        check("abc", "abc", "abc", null, "/abc")
    }

    @Test
    fun utf8AndFragments() {
        check("/é?q=🍕", "/é?q=🍕", "/é", "q=🍕", "/é?q=🍕")
        check("/a?b#ignored", "/a?b", "/a", "b", "/a?b")
        check("/a#ignored\u0000", "/a", "/a", null, "/a")
    }

    @Test
    fun invalidAscii() {
        for (result in listOf(PathAndQuery.fromMaybeShared("/a b".encodeToByteArray()), PathAndQuery.fromMaybeShared(Bytes.copyFromSlice("/a b".encodeToByteArray())), PathAndQuery.fromMaybeShared("/a b"))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals(ErrorKind.InvalidUriChar, error.kind)
            assertEquals("invalid uri character", error.message)
        }
        for (result in listOf(PathAndQuery.fromMaybeShared("/a?b\n".encodeToByteArray()), PathAndQuery.fromMaybeShared(Bytes.copyFromSlice("/a?b\n".encodeToByteArray())), PathAndQuery.fromMaybeShared("/a?b\n"))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals(ErrorKind.InvalidUriChar, error.kind)
            assertEquals("invalid uri character", error.message)
        }
        for (result in listOf(PathAndQuery.fromMaybeShared("\u0000".encodeToByteArray()), PathAndQuery.fromMaybeShared(Bytes.copyFromSlice("\u0000".encodeToByteArray())), PathAndQuery.fromMaybeShared("\u0000"))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals(ErrorKind.InvalidUriChar, error.kind)
            assertEquals("invalid uri character", error.message)
        }
        for (result in listOf(PathAndQuery.fromMaybeShared("\n".encodeToByteArray()), PathAndQuery.fromMaybeShared(Bytes.copyFromSlice("\n".encodeToByteArray())), PathAndQuery.fromMaybeShared("\n"))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals(ErrorKind.InvalidUriChar, error.kind)
            assertEquals("invalid uri character", error.message)
        }
        for (result in listOf(PathAndQuery.fromMaybeShared("/x?y\t".encodeToByteArray()), PathAndQuery.fromMaybeShared(Bytes.copyFromSlice("/x?y\t".encodeToByteArray())), PathAndQuery.fromMaybeShared("/x?y\t"))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals(ErrorKind.InvalidUriChar, error.kind)
            assertEquals("invalid uri character", error.message)
        }
    }

    @Test
    fun invalidUtf8() {
        for (result in listOf(PathAndQuery.fromMaybeShared(byteArrayOf(47, -61, 40)), PathAndQuery.fromMaybeShared(Bytes.copyFromSlice(byteArrayOf(47, -61, 40))))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals(ErrorKind.InvalidUriChar, error.kind)
        }
        for (result in listOf(PathAndQuery.fromMaybeShared(byteArrayOf(47, 63, -64, -128)), PathAndQuery.fromMaybeShared(Bytes.copyFromSlice(byteArrayOf(47, 63, -64, -128))))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals(ErrorKind.InvalidUriChar, error.kind)
        }
        for (result in listOf(PathAndQuery.fromMaybeShared(byteArrayOf(47, -19, -96, -128)), PathAndQuery.fromMaybeShared(Bytes.copyFromSlice(byteArrayOf(47, -19, -96, -128))))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals(ErrorKind.InvalidUriChar, error.kind)
        }
    }

    @Test
    fun sharedHandleIndependence() {
        val original = Bytes.copyFromSlice("/a?b#fragment".encodeToByteArray())
        val value = PathAndQuery.fromMaybeShared(original).getOrThrow()
        original.advance(2)
        assertEquals("?b#fragment", original.asRef().decodeToString())
        assertEquals("/a?b", value.asStr())
        assertEquals("/a", value.path())
        assertEquals("b", value.query())
    }

    @Test
    fun byteArrayCopyIndependence() {
        val original = "/a?b".encodeToByteArray()
        val value = PathAndQuery.fromMaybeShared(original).getOrThrow()
        original[1] = 'z'.code.toByte()
        assertEquals("/a?b", value.asStr())
    }

    @Test
    fun uriBuilderConsumer() {
        val path = PathAndQuery.fromMaybeShared("/é?q=🍕#fragment").getOrThrow()
        val uri =
            Uri
                .builder()
                .scheme("http")
                .authority("example.org:+80")
                .pathAndQuery(path)
                .build()
                .getOrThrow()
        assertEquals("http://example.org:+80/é?q=🍕", uri.toString())
        assertEquals(80, uri.portU16())
    }
}
