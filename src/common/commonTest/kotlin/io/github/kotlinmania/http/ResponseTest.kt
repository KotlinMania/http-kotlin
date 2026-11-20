// port-lint: tests response.rs
package io.github.kotlinmania.http

import io.github.kotlinmania.http.header.CONTENT_LENGTH
import io.github.kotlinmania.http.header.HeaderName
import io.github.kotlinmania.http.header.HeaderValue
import io.github.kotlinmania.http.header.InvalidHeaderName
import io.github.kotlinmania.http.header.InvalidHeaderValue
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResponseTest {
    @Test
    fun itCanMapABodyFromOneTypeToAnother() {
        val response =
            Response
                .builder()
                .body("some string")
                .unwrap()
        val mappedResponse =
            response.map { s ->
                assertEquals("some string", s)
                123u
            }
        assertEquals(123u, mappedResponse.body())
    }

    @Test
    fun defaultsAccessorsCloneAndPartsRoundTrip() {
        val response = Response.default()
        assertEquals(StatusCode.OK, response.status())
        assertEquals(Version.HTTP_11, response.version())
        assertTrue(response.headers().isEmpty())
        assertTrue(response.extensions().isEmpty())
        assertEquals(Unit, response.body())

        response.setStatus(StatusCode.CREATED)
        response.setVersion(Version.HTTP_2)
        response.headersMut().insert("content-type", HeaderValue.fromStatic("text/plain"))
        response.extensionsMut().insert("ext")

        val cloned = response.clone()
        cloned.headersMut().insert("x-cloned", HeaderValue.fromStatic("1"))
        assertFalse(response.headers().containsKey("x-cloned"))

        val (parts, body) = response.intoParts()
        assertEquals(Unit, body)
        parts.status = StatusCode.BAD_REQUEST
        val rebuilt = Response.fromPartsText(parts, "hello world")
        assertEquals(StatusCode.BAD_REQUEST, rebuilt.status())
        assertEquals("hello world", rebuilt.body())
        rebuilt.setBody("updated")
        assertEquals("updated", rebuilt.intoBody())
        assertTrue(rebuilt.debugString().contains("Response {"))
        assertTrue(parts.debugString().contains("Parts {"))

        val textRes = Response.newText("hi")
        assertEquals("hi", textRes.body())
        val bytesRes = Response.newBytes(byteArrayOf(7, 8))
        assertContentEquals(byteArrayOf(7, 8), bytesRes.body())
    }

    @Test
    fun builderStatusHeadersExtensionsAndErrorPropagation() {
        val builder =
            Response
                .builder()
                .status(201)
                .status("202")
                .status("204".encodeToByteArray())
                .version(Version.HTTP_2)
                .header("content-length", 0)
                .header(CONTENT_LENGTH, 0L)
                .header(HeaderName.fromStatic("x-id"), "42")
                .header("x-bytes", "bytes".encodeToByteArray())
                .extension("My Extension")

        val refHeaders = checkNotNull(builder.headersRef())
        val mutHeaders = checkNotNull(builder.headersMut())
        assertEquals("0", refHeaders["content-length"]?.toStr()?.getOrNull())
        assertEquals("42", refHeaders["x-id"]?.toStr()?.getOrNull())
        assertEquals("bytes", mutHeaders["x-bytes"]?.toStr()?.getOrNull())
        assertEquals("My Extension", builder.extensionsRef()?.get<String>())
        builder.extensionsMut()?.insert(5u)
        assertEquals(5u, builder.extensionsRef()?.get<UInt>())
        assertTrue(builder.debugString().contains("Builder { inner: Ok("))

        val res = builder.bodyText("done").unwrap()
        assertEquals(StatusCode.NO_CONTENT, res.status())
        assertEquals(Version.HTTP_2, res.version())
        assertEquals("done", res.body())

        val badStatus = Response.builder().status(9999)
        assertNull(badStatus.headersRef())
        assertNull(badStatus.headersMut())
        assertNull(badStatus.extensionsRef())
        assertNull(badStatus.extensionsMut())
        assertTrue(badStatus.debugString().contains("Err("))
        val badStatusErr = badStatus.bodyEmpty()
        assertTrue(badStatusErr.isErr())
        assertTrue(badStatusErr.err()!!.isType<InvalidStatusCode>())

        val badName = Response.builder().header("bad name", "v").bodyBytes(byteArrayOf())
        assertNotNull(badName.err())
        assertTrue(badName.err()!!.isType<InvalidHeaderName>())

        val badValue = Response.builder().header("x-ok", "\n").bodyEmpty()
        assertNotNull(badValue.err())
        assertTrue(badValue.err()!!.isType<InvalidHeaderValue>())
    }
}
