// port-lint: tests error.rs
package io.github.kotlinmania.http

import io.github.kotlinmania.http.header.HeaderName
import io.github.kotlinmania.http.header.HeaderValue
import io.github.kotlinmania.http.header.InvalidHeaderName
import io.github.kotlinmania.http.header.InvalidHeaderValue
import io.github.kotlinmania.http.header.MaxSizeReached
import io.github.kotlinmania.http.uri.InvalidUri
import io.github.kotlinmania.http.uri.InvalidUriParts
import io.github.kotlinmania.http.uri.Parts
import io.github.kotlinmania.http.uri.Scheme
import io.github.kotlinmania.http.uri.Uri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ErrorTest {
    @Test
    fun innerErrorIsInvalidStatusCode() {
        val statusResult = StatusCode.fromU16(6666)
        assertTrue(statusResult.isFailure, "Bad status allowed!")
        val innerErr = statusResult.exceptionOrNull() as InvalidStatusCode
        val err = Error.from(innerErr)

        assertFalse(err.getRef() is InvalidHeaderValue)
        assertTrue(err.getRef() is InvalidStatusCode)
        assertNotNull(err.getRef() as? InvalidStatusCode)

        assertFalse(err.isType<InvalidHeaderValue>())
        assertTrue(err.isType<InvalidStatusCode>())
        assertFalse(err.isError(InvalidHeaderValue::class))
        assertTrue(err.isError(InvalidStatusCode::class))
        assertNull(err.source())
        assertEquals("http::Error(InvalidStatusCode)", err.debugString())
    }

    @Test
    fun wrapsAllHttpErrorVariants() {
        val methodErr = Method.fromBytes(byteArrayOf()).exceptionOrNull() as InvalidMethod
        val wrappedMethod = Error.from(methodErr)
        assertTrue(wrappedMethod.isType<InvalidMethod>())
        assertEquals(methodErr, wrappedMethod.getRef())
        assertEquals("http::Error(InvalidMethod)", wrappedMethod.debugString())

        val uriErr =
            Uri
                .builder()
                .authority(" ")
                .build()
                .exceptionOrNull() as InvalidUri
        val wrappedUri = Error.from(uriErr)
        assertTrue(wrappedUri.isType<InvalidUri>())
        assertEquals(uriErr, wrappedUri.getRef())

        val partsErr = Uri.fromParts(Parts(scheme = Scheme.HTTP)).exceptionOrNull() as InvalidUriParts
        val wrappedParts = Error.from(partsErr)
        assertTrue(wrappedParts.isType<InvalidUriParts>())
        assertEquals(partsErr, wrappedParts.getRef())

        val nameErr = HeaderName.fromStr("invalid name").error!!
        val wrappedName = Error.from(nameErr)
        assertTrue(wrappedName.isType<InvalidHeaderName>())
        assertEquals(nameErr, wrappedName.getRef())
        assertEquals("invalid HTTP header name", wrappedName.toString())
        assertEquals("http::Error(InvalidHeaderName)", wrappedName.debugString())

        val valueErr = HeaderValue.fromBytes(byteArrayOf(0)).exceptionOrNull() as InvalidHeaderValue
        val wrappedValue = Error.from(valueErr)
        assertTrue(wrappedValue.isType<InvalidHeaderValue>())
        assertEquals(valueErr, wrappedValue.getRef())
        assertEquals("http::Error(InvalidHeaderValue)", wrappedValue.debugString())

        val maxErr =
            io.github.kotlinmania.http.header.HeaderMap
                .tryWithCapacity<HeaderValue>(30_000)
                .exceptionOrNull() as MaxSizeReached
        val wrappedMax = Error.from(maxErr)
        assertTrue(wrappedMax.isType<MaxSizeReached>())
        assertEquals(maxErr, wrappedMax.getRef())
        assertEquals("max size reached", wrappedMax.toString())
        assertEquals("http::Error(MaxSizeReached)", wrappedMax.debugString())
    }

    @Test
    fun httpResultSupportsOkErrMapAndThenAndUnwrap() {
        val okRes = HttpResult.ok(21)
        assertTrue(okRes.isSuccess)
        assertFalse(okRes.isFailure)
        assertTrue(okRes.isOk())
        assertFalse(okRes.isErr())
        assertEquals(21, okRes.ok())
        assertNull(okRes.err())
        assertEquals(21, okRes.unwrap())
        assertEquals(21, okRes.getOrThrow())
        assertEquals(21, okRes.getOrNull())
        assertNull(okRes.errorOrNull())
        assertEquals("Ok(21)", okRes.toString())

        val mapped = okRes.map { it * 2 }
        assertEquals(42, mapped.unwrap())

        val chained =
            mapped.andThen { v ->
                if (v > 40) {
                    HttpResult.ok("v=$v")
                } else {
                    HttpResult.err(Error.from(HeaderName.fromStr("bad name").error!!))
                }
            }
        assertEquals("v=42", chained.unwrap())

        val innerErr = HeaderName.fromStr("bad name").error!!
        val errObj = Error.from(innerErr)
        val errRes = HttpResult.err<Int>(errObj)
        assertFalse(errRes.isSuccess)
        assertTrue(errRes.isFailure)
        assertFalse(errRes.isOk())
        assertTrue(errRes.isErr())
        assertNull(errRes.ok())
        assertEquals(errObj, errRes.err())
        assertNull(errRes.getOrNull())
        assertEquals(errObj, errRes.errorOrNull())
        assertEquals(errRes, errRes.map { it + 1 })
        assertEquals(HttpResult.err<String>(errObj), errRes.andThen { HttpResult.ok("never") })
        assertEquals("Err(invalid HTTP header name)", errRes.toString())
    }
}
