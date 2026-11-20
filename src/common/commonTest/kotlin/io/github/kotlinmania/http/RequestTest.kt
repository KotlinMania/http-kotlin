// port-lint: tests request.rs
package io.github.kotlinmania.http

import io.github.kotlinmania.http.header.CONTENT_TYPE
import io.github.kotlinmania.http.header.HeaderName
import io.github.kotlinmania.http.header.HeaderValue
import io.github.kotlinmania.http.header.InvalidHeaderName
import io.github.kotlinmania.http.header.InvalidHeaderValue
import io.github.kotlinmania.http.uri.InvalidUri
import io.github.kotlinmania.http.uri.Uri
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RequestTest {
    @Test
    fun itCanMapABodyFromOneTypeToAnother() {
        val request =
            Request
                .builder()
                .body("some string")
                .unwrap()
        val mappedRequest =
            request.map { s ->
                assertEquals("some string", s)
                123u
            }
        assertEquals(123u, mappedRequest.body())
    }

    @Test
    fun defaultsAndAccessorsAndPartsRoundTrip() {
        val req = Request.default()
        assertEquals(Method.GET, req.method())
        assertEquals("/", req.uri().toString())
        assertEquals(Version.HTTP_11, req.version())
        assertTrue(req.headers().isEmpty())
        assertTrue(req.extensions().isEmpty())
        assertEquals(Unit, req.body())

        req.setMethod(Method.PUT)
        req.setUri(Uri.fromStr("https://example.com/v1").getOrThrow())
        req.setVersion(Version.HTTP_2)
        req.headersMut().insert("x-k", HeaderValue.fromStatic("v"))
        req.extensionsMut().insert(77)

        val cloned = req.clone()
        cloned.headersMut().insert("x-only-clone", HeaderValue.fromStatic("1"))
        assertFalse(req.headers().containsKey("x-only-clone"))

        val (parts, body) = req.intoParts()
        assertEquals(Unit, body)
        parts.method = Method.POST
        val rebuilt = Request.fromPartsText(parts, "hello")
        assertEquals(Method.POST, rebuilt.method())
        assertEquals("hello", rebuilt.body())
        rebuilt.setBody("world")
        assertEquals("world", rebuilt.intoBody())
        assertTrue(rebuilt.debugString().contains("Request {"))
        assertTrue(parts.debugString().contains("Parts {"))
    }

    @Test
    fun shorthandMethodConstructorsAndOverloads() {
        val getReq = Request.get("https://example.com").bodyEmpty().unwrap()
        val putReq = Request.put(Uri.fromStr("https://example.com/a").getOrThrow()).bodyEmpty().unwrap()
        val postReq = Request.post("https://example.com".encodeToByteArray()).bodyEmpty().unwrap()
        val deleteReq = Request.delete("https://example.com").bodyEmpty().unwrap()
        val optionsReq = Request.options("https://example.com").bodyEmpty().unwrap()
        val headReq = Request.head("https://example.com").bodyEmpty().unwrap()
        val connectReq = Request.connect("https://example.com").bodyEmpty().unwrap()
        val patchReq = Request.patch("https://example.com").bodyEmpty().unwrap()
        val traceReq = Request.trace("https://example.com").bodyEmpty().unwrap()
        assertEquals(Method.GET, getReq.method())
        assertEquals(Method.PUT, putReq.method())
        assertEquals(Method.POST, postReq.method())
        assertEquals(Method.DELETE, deleteReq.method())
        assertEquals(Method.OPTIONS, optionsReq.method())
        assertEquals(Method.HEAD, headReq.method())
        assertEquals(Method.CONNECT, connectReq.method())
        assertEquals(Method.PATCH, patchReq.method())
        assertEquals(Method.TRACE, traceReq.method())

        val textReq = Request.newText("abc")
        assertEquals("abc", textReq.body())
        val bytesReq = Request.newBytes(byteArrayOf(1, 2, 3))
        assertContentEquals(byteArrayOf(1, 2, 3), bytesReq.body())
    }

    @Test
    fun builderInspectorsHeadersExtensionsAndErrors() {
        val builder =
            Request
                .builder()
                .method("POST")
                .uri("https://www.rust-lang.org/")
                .version(Version.HTTP_2)
                .header("Accept", "text/html")
                .header(CONTENT_TYPE, HeaderValue.fromStatic("application/json"))
                .header(HeaderName.fromStatic("x-count"), 42)
                .header("x-long", 99L)
                .header("x-bytes", "raw".encodeToByteArray())
                .extension("My Extension")

        assertEquals(Method.POST, builder.methodRef())
        assertEquals("https://www.rust-lang.org/", builder.uriRef()?.toString())
        assertEquals(Version.HTTP_2, builder.versionRef())
        val refHeaders = checkNotNull(builder.headersRef())
        val mutHeaders = checkNotNull(builder.headersMut())
        assertEquals("text/html", refHeaders["accept"]?.toStr()?.getOrNull())
        assertEquals("42", refHeaders["x-count"]?.toStr()?.getOrNull())
        assertEquals("99", refHeaders["x-long"]?.toStr()?.getOrNull())
        assertEquals("raw", mutHeaders["x-bytes"]?.toStr()?.getOrNull())
        assertEquals("My Extension", builder.extensionsRef()?.get<String>())
        builder.extensionsMut()?.insert(5u)
        assertEquals(5u, builder.extensionsRef()?.get<UInt>())
        assertTrue(builder.debugString().contains("Builder { inner: Ok("))

        val req = builder.bodyText("ok").unwrap()
        assertEquals("ok", req.body())
        assertEquals("application/json", req.headers()["content-type"]?.toStr()?.getOrNull())

        val badMethod = Request.builder().method(byteArrayOf())
        assertNull(badMethod.methodRef())
        assertTrue(badMethod.debugString().contains("Err("))
        val badMethodErr = badMethod.bodyEmpty()
        assertTrue(badMethodErr.isErr())
        assertTrue(badMethodErr.err()!!.isType<InvalidMethod>())

        val badUri = Request.builder().uri("http://")
        assertNull(badUri.uriRef())
        assertTrue(badUri.bodyEmpty().err()!!.isType<InvalidUri>())

        val badName = Request.builder().header("bad name", "value").bodyEmpty()
        assertNotNull(badName.err())
        assertTrue(badName.err()!!.isType<InvalidHeaderName>())

        val badValue = Request.builder().header("x-ok", byteArrayOf(0)).bodyEmpty()
        assertNotNull(badValue.err())
        assertTrue(badValue.err()!!.isType<InvalidHeaderValue>())
    }
}
