package io.github.kotlinmania.http.uri

import io.github.kotlinmania.bytes.Bytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthoritySharedApiParityTest {
    private fun check(input: String, host: String, port: Int?) {
        val values =
            listOf(
                Authority.fromMaybeShared(Bytes.copyFromSlice(input.encodeToByteArray())),
                Authority.fromMaybeShared(input.encodeToByteArray()),
                Authority.fromMaybeShared(input),
            )
        for (result in values) {
            val value = result.getOrThrow()
            assertEquals(input, value.asStr())
            assertEquals(host, value.host())
            assertEquals(port, value.portU16())
            assertEquals(input, value.toString())
        }
    }

    @Test
    fun asciiAndPorts() {
        check("example.org", "example.org", null)
        check("example.org:80", "example.org", 80)
        check("EXAMPLE.org:+80", "EXAMPLE.org", 80)
        check("example.org:00080", "example.org", 80)
        check("example.org:-0", "example.org", null)
        check("example.org:65536", "example.org", null)
    }

    @Test
    fun userinfoAndIpv6() {
        check("user:pass@example.org:+80", "example.org", 80)
        check("a%2f:b%2f@example.com", "example.com", null)
        check("[::1]:443", "[::1]", 443)
    }

    @Test
    fun invalidSyntax() {
        for (result in listOf(Authority.fromMaybeShared("".encodeToByteArray()), Authority.fromMaybeShared(Bytes.copyFromSlice("".encodeToByteArray())), Authority.fromMaybeShared(""))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals("empty string", error.message)
        }
        for (result in listOf(Authority.fromMaybeShared("example.org/path".encodeToByteArray()), Authority.fromMaybeShared(Bytes.copyFromSlice("example.org/path".encodeToByteArray())), Authority.fromMaybeShared("example.org/path"))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals("invalid uri character", error.message)
        }
        for (result in listOf(Authority.fromMaybeShared("example.org?query".encodeToByteArray()), Authority.fromMaybeShared(Bytes.copyFromSlice("example.org?query".encodeToByteArray())), Authority.fromMaybeShared("example.org?query"))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals("invalid uri character", error.message)
        }
        for (result in listOf(Authority.fromMaybeShared("example.org#fragment".encodeToByteArray()), Authority.fromMaybeShared(Bytes.copyFromSlice("example.org#fragment".encodeToByteArray())), Authority.fromMaybeShared("example.org#fragment"))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals("invalid uri character", error.message)
        }
        for (result in listOf(Authority.fromMaybeShared("exa mple.org".encodeToByteArray()), Authority.fromMaybeShared(Bytes.copyFromSlice("exa mple.org".encodeToByteArray())), Authority.fromMaybeShared("exa mple.org"))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals("invalid uri character", error.message)
        }
        for (result in listOf(Authority.fromMaybeShared("é.example".encodeToByteArray()), Authority.fromMaybeShared(Bytes.copyFromSlice("é.example".encodeToByteArray())), Authority.fromMaybeShared("é.example"))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals("invalid uri character", error.message)
        }
        for (result in listOf(Authority.fromMaybeShared("[::1".encodeToByteArray()), Authority.fromMaybeShared(Bytes.copyFromSlice("[::1".encodeToByteArray())), Authority.fromMaybeShared("[::1"))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals("invalid authority", error.message)
        }
        for (result in listOf(Authority.fromMaybeShared("example%2f.com".encodeToByteArray()), Authority.fromMaybeShared(Bytes.copyFromSlice("example%2f.com".encodeToByteArray())), Authority.fromMaybeShared("example%2f.com"))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals("invalid authority", error.message)
        }
    }

    @Test
    fun invalidUtf8() {
        for (result in listOf(Authority.fromMaybeShared(byteArrayOf(-61, 40)), Authority.fromMaybeShared(Bytes.copyFromSlice(byteArrayOf(-61, 40))))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals(ErrorKind.InvalidUriChar, error.kind)
        }
        for (result in listOf(Authority.fromMaybeShared(byteArrayOf(-1)), Authority.fromMaybeShared(Bytes.copyFromSlice(byteArrayOf(-1))))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals(ErrorKind.InvalidUriChar, error.kind)
        }
        for (result in listOf(Authority.fromMaybeShared(byteArrayOf(101, 120, 97, 109, 112, 108, 101, 0)), Authority.fromMaybeShared(Bytes.copyFromSlice(byteArrayOf(101, 120, 97, 109, 112, 108, 101, 0))))) {
            val error = assertIs<InvalidUri>(result.exceptionOrNull())
            assertEquals(ErrorKind.InvalidUriChar, error.kind)
        }
    }

    @Test
    fun sharedHandleIndependence() {
        val original = Bytes.copyFromSlice("example.org:+80".encodeToByteArray())
        val value = Authority.fromMaybeShared(original).getOrThrow()
        original.advance(2)
        assertEquals("ample.org:+80", original.asRef().decodeToString())
        assertEquals("example.org:+80", value.asStr())
        assertEquals("example.org", value.host())
        assertEquals(80, value.portU16())
    }

    @Test
    fun byteArrayCopyIndependence() {
        val original = "example.org".encodeToByteArray()
        val value = Authority.fromMaybeShared(original).getOrThrow()
        original[0] = 'z'.code.toByte()
        assertEquals("example.org", value.asStr())
    }

    @Test
    fun builderConsumer() {
        val authority = Authority.fromMaybeShared(Bytes.copyFromSlice("example.org:+80".encodeToByteArray())).getOrThrow()
        val uri =
            Uri
                .builder()
                .scheme("http")
                .authority(authority)
                .pathAndQuery("/x")
                .build()
                .getOrThrow()
        assertEquals("http://example.org:+80/x", uri.toString())
        assertEquals(80, uri.portU16())
    }
}
