// port-lint: tests tests/header_map.rs
package io.github.kotlinmania.http.header

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeaderMapTest {
    @Test
    fun smoke() {
        val headers = HeaderMap.new()

        assertNull(headers.get("hello"))

        val name = HeaderName.fromStr("hello").getOrThrow()

        when (val e = headers.entry(name)) {
            is Entry.Vacant -> e.entry.insert(HeaderValue.fromStr("world").getOrThrow())
            is Entry.Occupied -> error("expected vacant entry")
        }

        assertNotNull(headers.get("hello"))

        when (val e = headers.entry(name)) {
            is Entry.Occupied -> {
                assertTrue(e.entry.get().eq("world"))

                // Push another value
                e.entry.append(HeaderValue.fromStr("zomg").getOrThrow())

                val iter = e.entry.iter()
                assertTrue(iter.next()!!.eq("world"))
                assertTrue(iter.next()!!.eq("zomg"))
                assertNull(iter.next())
            }
            is Entry.Vacant -> error("expected occupied entry")
        }
    }

    @Test
    fun reserveOverCapacity() {
        // See https://github.com/hyperium/http/issues/352
        val headers = HeaderMap.withCapacityTyped<Int>(32)
        assertFailsWith<IllegalStateException> {
            headers.reserve(50_000) // over MAX_SIZE
        }
    }

    @Test
    fun withCapacityMax() {
        // The largest capacity such that (cap + cap / 3) < MAX_SIZE.
        HeaderMap.withCapacityTyped<Int>(24_576)
    }

    @Test
    fun withCapacityOverflow() {
        assertFailsWith<IllegalStateException> {
            HeaderMap.withCapacityTyped<Int>(24_577)
        }
    }

    @Test
    fun reserveOverflow() {
        // See https://github.com/hyperium/http/issues/352
        val headers = HeaderMap.withCapacityTyped<Int>(0)
        assertFailsWith<IllegalStateException> {
            headers.reserve(Int.MAX_VALUE) // next_power_of_two overflows
        }
    }

    @Test
    fun reserve() {
        val headers = HeaderMap.newTyped<Int>()
        assertEquals(0, headers.capacity())

        val requestedCap = 8
        headers.reserve(requestedCap)

        val reservedCap = headers.capacity()
        assertTrue(
            reservedCap >= requestedCap,
            "requested $requestedCap capacity, but it reserved only $reservedCap entries",
        )

        for (i in 0 until requestedCap) {
            val name = HeaderName.fromStr("h$i").getOrThrow()
            headers.insert(name, i)
        }

        assertEquals(reservedCap, headers.capacity(), "unexpected reallocation")
    }

    @Test
    fun drain() {
        val headers = HeaderMap.new()

        // Insert a single value
        val name = HeaderName.fromStr("hello").getOrThrow()
        headers.insert(name, HeaderValue.fromStr("world").getOrThrow())

        run {
            val iter = headers.drain()
            val (drainedName, drainedVal) = iter.next()!!
            assertEquals("hello", drainedName!!.asStr())
            assertTrue(drainedVal.eq("world"))
            assertNull(iter.next())
        }

        assertTrue(headers.isEmpty())

        // Insert two sequential values
        headers.insert(HeaderName.fromStr("hello").getOrThrow(), HeaderValue.fromStr("world").getOrThrow())
        headers.insert(HeaderName.fromStr("zomg").getOrThrow(), HeaderValue.fromStr("bar").getOrThrow())
        headers.append(HeaderName.fromStr("hello").getOrThrow(), HeaderValue.fromStr("world2").getOrThrow())

        // Drain...
        val iter = headers.drain()

        val (name1, value1) = iter.next()!!
        assertEquals("hello", name1!!.asStr())
        assertTrue(value1.eq("world"))

        val (name2, value2) = iter.next()!!
        assertNull(name2)
        assertTrue(value2.eq("world2"))

        val (name3, value3) = iter.next()!!
        assertEquals("zomg", name3!!.asStr())
        assertTrue(value3.eq("bar"))

        assertNull(iter.next())
    }

    @Test
    fun drainDropImmediately() {
        // Kotlin GC reclaims abandoned iterators without manual Drop; verify sizeHint and cleared map.
        val headers = HeaderMap.new()
        headers.insert("hello", HeaderValue.fromStr("world").getOrThrow())
        headers.insert("zomg", HeaderValue.fromStr("bar").getOrThrow())
        headers.append("hello", HeaderValue.fromStr("world2").getOrThrow())

        val iter = headers.drain()
        assertEquals(SizeHint(2, 3), iter.sizeHint())
        assertTrue(headers.isEmpty())
    }

    @Test
    fun drainForget() {
        // Kotlin has no mem::forget destructor skip; detached drain state still leaves map empty.
        val headers = HeaderMap.new()
        headers.insert("hello", HeaderValue.fromStr("world").getOrThrow())
        headers.insert("zomg", HeaderValue.fromStr("bar").getOrThrow())

        assertEquals(2, headers.len())

        val iter = headers.drain()
        assertEquals(SizeHint(2, 2), iter.sizeHint())
        assertNotNull(iter.next())

        assertEquals(0, headers.len())
    }

    @Test
    fun drainEntry() {
        val headers = HeaderMap.new()

        headers.insert(HeaderName.fromStr("hello").getOrThrow(), HeaderValue.fromStr("world").getOrThrow())
        headers.insert(HeaderName.fromStr("zomg").getOrThrow(), HeaderValue.fromStr("foo").getOrThrow())
        headers.append(HeaderName.fromStr("hello").getOrThrow(), HeaderValue.fromStr("world2").getOrThrow())
        headers.insert(HeaderName.fromStr("more").getOrThrow(), HeaderValue.fromStr("words").getOrThrow())
        headers.append(HeaderName.fromStr("more").getOrThrow(), HeaderValue.fromStr("insertions").getOrThrow())
        assertEquals(5, headers.len())

        // Using insertMult
        run {
            val e =
                when (val entry = headers.entry("hello")) {
                    is Entry.Occupied -> entry.entry
                    is Entry.Vacant -> error("expected occupied")
                }

            val vals = e.insertMult(HeaderValue.fromStr("wat").getOrThrow()).toList()
            assertEquals(2, vals.size)
            assertTrue(vals[0].eq("world"))
            assertTrue(vals[1].eq("world2"))
        }

        assertEquals(5 - 2 + 1, headers.len())
    }

    @Test
    fun insertMultThreeOrMoreExtras() {
        val headers = HeaderMap.new()
        headers.append("hello", HeaderValue.fromStatic("v1"))
        headers.append("hello", HeaderValue.fromStatic("v2"))
        headers.append("hello", HeaderValue.fromStatic("v3"))
        headers.append("hello", HeaderValue.fromStatic("v4"))
        headers.insert("other", HeaderValue.fromStatic("keep"))

        val e = (headers.entry("hello") as Entry.Occupied).entry
        val drained = e.insertMult(HeaderValue.fromStatic("replaced")).toList()
        assertEquals(listOf("v1", "v2", "v3", "v4"), drained.map { it.toStr().getOrThrow() })
        assertEquals(listOf("replaced"), headers.getAll("hello").map { it.toStr().getOrThrow() })
        assertEquals(2, headers.len())
        assertTrue(headers.get("other")!!.eq("keep"))
    }

    @Test
    fun eq() {
        val a = HeaderMap.new()
        val b = HeaderMap.new()

        assertEquals(a, b)

        a.insert(HeaderName.fromStr("hello").getOrThrow(), HeaderValue.fromStr("world").getOrThrow())
        assertNotEquals(a, b)

        b.insert(HeaderName.fromStr("hello").getOrThrow(), HeaderValue.fromStr("world").getOrThrow())
        assertEquals(a, b)

        a.insert(HeaderName.fromStr("foo").getOrThrow(), HeaderValue.fromStr("bar").getOrThrow())
        a.append(HeaderName.fromStr("foo").getOrThrow(), HeaderValue.fromStr("baz").getOrThrow())
        assertNotEquals(a, b)

        b.insert(HeaderName.fromStr("foo").getOrThrow(), HeaderValue.fromStr("bar").getOrThrow())
        assertNotEquals(a, b)

        b.append(HeaderName.fromStr("foo").getOrThrow(), HeaderValue.fromStr("baz").getOrThrow())
        assertEquals(a, b)

        a.append(HeaderName.fromStr("a").getOrThrow(), HeaderValue.fromStr("a").getOrThrow())
        a.append(HeaderName.fromStr("a").getOrThrow(), HeaderValue.fromStr("b").getOrThrow())
        b.append(HeaderName.fromStr("a").getOrThrow(), HeaderValue.fromStr("b").getOrThrow())
        b.append(HeaderName.fromStr("a").getOrThrow(), HeaderValue.fromStr("a").getOrThrow())

        assertNotEquals(a, b)
    }

    @Test
    fun intoHeaderName() {
        val m = HeaderMap.new()
        m.insert(HOST, HeaderValue.fromStr("localhost").getOrThrow())
        m.insert(ACCEPT, HeaderValue.fromStr("*/*").getOrThrow())
        m.insert("connection", HeaderValue.fromStr("keep-alive").getOrThrow())

        m.append(LOCATION, HeaderValue.fromStr("/").getOrThrow())
        m.append(VIA, HeaderValue.fromStr("bob").getOrThrow())
        m.append("transfer-encoding", HeaderValue.fromStr("chunked").getOrThrow())
        m.insert(IntoHeaderName.of("x-wrapped"), HeaderValue.fromStr("ok").getOrThrow())

        assertEquals(7, m.len())
    }

    @Test
    fun asHeaderName() {
        val m = HeaderMap.new()
        val v = HeaderValue.fromStr("localhost").getOrThrow()
        m.insert(HOST, v.clone())

        assertEquals(v, m.get("host"))
        assertEquals(v, m.get(HOST))
        assertEquals(v, m.get(AsHeaderName.of("host")))
        assertEquals(v, m.get(AsHeaderName.of(HOST)))
        assertEquals(v, m.get("HOST"))
    }

    @Test
    fun insertAllStdHeaders() {
        val m = HeaderMap.new()

        for ((i, hdr) in STD.withIndex()) {
            m.insert(hdr.clone(), HeaderValue.fromStr(hdr.asStr()).getOrThrow())

            for (j in 0 until (i + 1)) {
                assertTrue(m.index(STD[j]).eq(STD[j].asStr()))
            }

            if (i != 0) {
                for (j in (i + 1) until STD.size) {
                    assertNull(m.get(STD[j]), "contained ${STD[j].asStr()}; j=$j")
                }
            }
        }
    }

    @Test
    fun insert79CustomStdHeaders() {
        val h = HeaderMap.new()
        val hdrs = customStd(79)

        for ((i, hdr) in hdrs.withIndex()) {
            h.insert(hdr.clone(), HeaderValue.fromStr(hdr.asStr()).getOrThrow())

            for (j in 0 until (i + 1)) {
                assertTrue(h.index(hdrs[j]).eq(hdrs[j].asStr()))
            }

            for (j in (i + 1) until hdrs.size) {
                assertNull(h.get(hdrs[j]))
            }
        }
    }

    @Test
    fun appendMultipleValues() {
        val map = HeaderMap.new()

        map.append(CONTENT_TYPE, HeaderValue.fromStr("json").getOrThrow())
        map.append(CONTENT_TYPE, HeaderValue.fromStr("html").getOrThrow())
        map.append(CONTENT_TYPE, HeaderValue.fromStr("xml").getOrThrow())

        val vals = map.getAll(CONTENT_TYPE).iter().toList()
        assertEquals(listOf("json", "html", "xml"), vals.map { it.toStr().getOrThrow() })
    }

    @Test
    fun getInvalid() {
        val headers = HeaderMap.new()
        headers.insert("foo", HeaderValue.fromStr("bar").getOrThrow())
        assertNull(headers.get("Evil\r\nKey"))
    }

    @Test
    fun insertInvalid() {
        val headers = HeaderMap.new()
        assertFailsWith<IllegalArgumentException> {
            headers.insert("evil\r\nfoo", HeaderValue.fromStr("bar").getOrThrow())
        }
    }

    @Test
    fun valueHtab() {
        // RFC 7230 Section 3.2: field-content = field-vchar [ 1*( SP / HTAB ) field-vchar ]
        HeaderValue.fromStatic("hello\tworld")
        HeaderValue.fromStr("hello\tworld").getOrThrow()
    }

    @Test
    fun removeMultipleA() {
        val headers = HeaderMap.new()
        headers.insert(VIA, HeaderValue.fromStr("1.1 example.com").getOrThrow())
        headers.insert(SET_COOKIE, HeaderValue.fromStr("cookie_1=value 1").getOrThrow())
        headers.append(SET_COOKIE, HeaderValue.fromStr("cookie_2=value 2").getOrThrow())
        headers.append(VIA, HeaderValue.fromStr("1.1 other.com").getOrThrow())
        headers.append(SET_COOKIE, HeaderValue.fromStr("cookie_3=value 3").getOrThrow())
        headers.insert(VARY, HeaderValue.fromStr("*").getOrThrow())

        assertEquals(6, headers.len())

        val cookie = headers.remove(SET_COOKIE)
        assertEquals(HeaderValue.fromStr("cookie_1=value 1").getOrThrow(), cookie)
        assertEquals(3, headers.len())

        val via = headers.remove(VIA)
        assertEquals(HeaderValue.fromStr("1.1 example.com").getOrThrow(), via)
        assertEquals(1, headers.len())

        val vary = headers.remove(VARY)
        assertEquals(HeaderValue.fromStr("*").getOrThrow(), vary)
        assertEquals(0, headers.len())
    }

    @Test
    fun removeMultipleB() {
        val headers = HeaderMap.new()
        headers.insert(VIA, HeaderValue.fromStr("1.1 example.com").getOrThrow())
        headers.insert(SET_COOKIE, HeaderValue.fromStr("cookie_1=value 1").getOrThrow())
        headers.append(SET_COOKIE, HeaderValue.fromStr("cookie_2=value 2").getOrThrow())
        headers.append(VIA, HeaderValue.fromStr("1.1 other.com").getOrThrow())
        headers.append(SET_COOKIE, HeaderValue.fromStr("cookie_3=value 3").getOrThrow())
        headers.insert(VARY, HeaderValue.fromStr("*").getOrThrow())

        assertEquals(6, headers.len())

        val vary = headers.remove(VARY)
        assertEquals(HeaderValue.fromStr("*").getOrThrow(), vary)
        assertEquals(5, headers.len())

        val via = headers.remove(VIA)
        assertEquals(HeaderValue.fromStr("1.1 example.com").getOrThrow(), via)
        assertEquals(3, headers.len())

        val cookie = headers.remove(SET_COOKIE)
        assertEquals(HeaderValue.fromStr("cookie_1=value 1").getOrThrow(), cookie)
        assertEquals(0, headers.len())
    }

    @Test
    fun removeEntryMulti0() {
        val headers = HeaderMap.new()
        val cookies = removeAllValues(headers, SET_COOKIE)
        assertEquals(0, cookies.size)
        assertEquals(0, headers.len())
    }

    @Test
    fun removeEntryMulti0Others() {
        val headers = HeaderMap.new()
        headers.insert(VIA, HeaderValue.fromStr("1.1 example.com").getOrThrow())
        headers.append(VIA, HeaderValue.fromStr("1.1 other.com").getOrThrow())

        val cookies = removeAllValues(headers, SET_COOKIE)
        assertEquals(0, cookies.size)
        assertEquals(2, headers.len())
    }

    @Test
    fun removeEntryMulti1() {
        val headers = HeaderMap.new()
        headers.insert(SET_COOKIE, HeaderValue.fromStr("cookie_1=value 1").getOrThrow())

        val cookies = removeAllValues(headers, SET_COOKIE)
        assertEquals(1, cookies.size)
        assertEquals(0, headers.len())
    }

    @Test
    fun removeEntryMulti1Other() {
        val headers = HeaderMap.new()
        headers.insert(SET_COOKIE, HeaderValue.fromStr("cookie_1=value 1").getOrThrow())
        headers.insert(VIA, HeaderValue.fromStr("1.1 example.com").getOrThrow())

        val cookies = removeAllValues(headers, SET_COOKIE)
        assertEquals(1, cookies.size)
        assertEquals(1, headers.len())

        val vias = removeAllValues(headers, VIA)
        assertEquals(1, vias.size)
        assertEquals(0, headers.len())
    }

    @Test
    fun removeEntryMulti2() {
        val headers = HeaderMap.new()
        headers.insert(SET_COOKIE, HeaderValue.fromStr("cookie_1=value 1").getOrThrow())
        headers.append(SET_COOKIE, HeaderValue.fromStr("cookie_2=value 2").getOrThrow())

        val cookies = removeAllValues(headers, SET_COOKIE)
        assertEquals(2, cookies.size)
        assertEquals(0, headers.len())
    }

    @Test
    fun removeEntryMulti3() {
        val headers = HeaderMap.new()
        headers.insert(SET_COOKIE, HeaderValue.fromStr("cookie_1=value 1").getOrThrow())
        headers.append(SET_COOKIE, HeaderValue.fromStr("cookie_2=value 2").getOrThrow())
        headers.append(SET_COOKIE, HeaderValue.fromStr("cookie_3=value 3").getOrThrow())

        val cookies = removeAllValues(headers, SET_COOKIE)
        assertEquals(3, cookies.size)
        assertEquals(0, headers.len())
    }

    @Test
    fun removeEntryMulti3Others() {
        val headers = HeaderMap.new()
        headers.insert(VIA, HeaderValue.fromStr("1.1 example.com").getOrThrow())
        headers.insert(SET_COOKIE, HeaderValue.fromStr("cookie_1=value 1").getOrThrow())
        headers.append(SET_COOKIE, HeaderValue.fromStr("cookie_2=value 2").getOrThrow())
        headers.append(VIA, HeaderValue.fromStr("1.1 other.com").getOrThrow())
        headers.append(SET_COOKIE, HeaderValue.fromStr("cookie_3=value 3").getOrThrow())
        headers.insert(VARY, HeaderValue.fromStr("*").getOrThrow())

        val cookies = removeAllValues(headers, SET_COOKIE)
        assertEquals(3, cookies.size)
        assertEquals(3, headers.len())

        val vias = removeAllValues(headers, VIA)
        assertEquals(2, vias.size)
        assertEquals(1, headers.len())

        val varies = removeAllValues(headers, VARY)
        assertEquals(1, varies.size)
        assertEquals(0, headers.len())
    }

    @Test
    fun removeEntry3OthersA() {
        val headers = HeaderMap.new()
        headers.insert(VIA, HeaderValue.fromStr("1.1 example.com").getOrThrow())
        headers.insert(SET_COOKIE, HeaderValue.fromStr("cookie_1=value 1").getOrThrow())
        headers.append(SET_COOKIE, HeaderValue.fromStr("cookie_2=value 2").getOrThrow())
        headers.append(VIA, HeaderValue.fromStr("1.1 other.com").getOrThrow())
        headers.append(SET_COOKIE, HeaderValue.fromStr("cookie_3=value 3").getOrThrow())
        headers.insert(VARY, HeaderValue.fromStr("*").getOrThrow())

        assertEquals(6, headers.len())

        val cookie = removeValues(headers, SET_COOKIE)
        assertEquals(HeaderValue.fromStr("cookie_1=value 1").getOrThrow(), cookie)
        assertEquals(3, headers.len())

        val via = removeValues(headers, VIA)
        assertEquals(HeaderValue.fromStr("1.1 example.com").getOrThrow(), via)
        assertEquals(1, headers.len())

        val vary = removeValues(headers, VARY)
        assertEquals(HeaderValue.fromStr("*").getOrThrow(), vary)
        assertEquals(0, headers.len())
    }

    @Test
    fun removeEntry3OthersB() {
        val headers = HeaderMap.new()
        headers.insert(VIA, HeaderValue.fromStr("1.1 example.com").getOrThrow())
        headers.insert(SET_COOKIE, HeaderValue.fromStr("cookie_1=value 1").getOrThrow())
        headers.append(SET_COOKIE, HeaderValue.fromStr("cookie_2=value 2").getOrThrow())
        headers.append(VIA, HeaderValue.fromStr("1.1 other.com").getOrThrow())
        headers.append(SET_COOKIE, HeaderValue.fromStr("cookie_3=value 3").getOrThrow())
        headers.insert(VARY, HeaderValue.fromStr("*").getOrThrow())

        assertEquals(6, headers.len())

        val vary = removeValues(headers, VARY)
        assertEquals(HeaderValue.fromStr("*").getOrThrow(), vary)
        assertEquals(5, headers.len())

        val via = removeValues(headers, VIA)
        assertEquals(HeaderValue.fromStr("1.1 example.com").getOrThrow(), via)
        assertEquals(3, headers.len())

        val cookie = removeValues(headers, SET_COOKIE)
        assertEquals(HeaderValue.fromStr("cookie_1=value 1").getOrThrow(), cookie)
        assertEquals(0, headers.len())
    }

    @Test
    fun ensureMiriSharedReadonlyNotViolated() {
        val headers = HeaderMap.new()
        headers.insert(
            HeaderName.fromStatic("chunky-trailer"),
            HeaderValue.fromStatic("header data"),
        )
        val first = headers.iter().next()
        assertNotNull(first)
        assertEquals("chunky-trailer", first.name.asStr())
    }

    @Test
    fun skipDuplicatesDuringKeyIteration() {
        val map = HeaderMap.new()
        map.tryAppend("a", HeaderValue.fromStatic("a")).getOrThrow()
        map.tryAppend("a", HeaderValue.fromStatic("b")).getOrThrow()
        assertEquals(map.keysLen(), map.keys().count())
    }

    @Test
    fun inlineMapBoundsAndDoubleEndedValueIterators() {
        // Rust Send/Sync compile-time trait bounds have no Kotlin type-system equivalent.
        val map = HeaderMap.new()
        map.insert("x-test", HeaderValue.fromStatic("first"))
        map.append("x-test", HeaderValue.fromStatic("second"))
        map.append("x-test", HeaderValue.fromStatic("third"))

        val vi = map.getAll("x-test").iter()
        assertTrue(vi.next()!!.eq("first"))
        assertTrue(vi.nextBack()!!.eq("third"))
        assertTrue(vi.nextBack()!!.eq("second"))
        assertNull(vi.next())
        assertNull(vi.nextBack())

        val extended = HeaderMap.new()
        extended.extend(map.clone().intoIter().toList())
        assertEquals(map, extended)
    }

    private fun removeAllValues(
        headers: HeaderMap<HeaderValue>,
        key: HeaderName,
    ): List<HeaderValue> =
        when (val entry = headers.entry(key)) {
            is Entry.Occupied ->
                entry.entry
                    .removeEntryMult()
                    .values
                    .toList()
            is Entry.Vacant -> emptyList()
        }

    private fun removeValues(
        headers: HeaderMap<HeaderValue>,
        key: HeaderName,
    ): HeaderValue? =
        when (val entry = headers.entry(key)) {
            is Entry.Occupied -> entry.entry.removeEntry().value
            is Entry.Vacant -> null
        }

    private fun customStd(n: Int): List<HeaderName> =
        (0 until n).map { i ->
            val s = "${STD[i % STD.size].asStr()}-$i"
            HeaderName.fromStr(s).getOrThrow()
        }

    companion object {
        private val STD: List<HeaderName> =
            listOf(
                ACCEPT,
                ACCEPT_CHARSET,
                ACCEPT_ENCODING,
                ACCEPT_LANGUAGE,
                ACCEPT_RANGES,
                ACCESS_CONTROL_ALLOW_CREDENTIALS,
                ACCESS_CONTROL_ALLOW_HEADERS,
                ACCESS_CONTROL_ALLOW_METHODS,
                ACCESS_CONTROL_ALLOW_ORIGIN,
                ACCESS_CONTROL_EXPOSE_HEADERS,
                ACCESS_CONTROL_MAX_AGE,
                ACCESS_CONTROL_REQUEST_HEADERS,
                ACCESS_CONTROL_REQUEST_METHOD,
                AGE,
                ALLOW,
                ALT_SVC,
                AUTHORIZATION,
                CACHE_CONTROL,
                CACHE_STATUS,
                CDN_CACHE_CONTROL,
                CONNECTION,
                CONTENT_DISPOSITION,
                CONTENT_ENCODING,
                CONTENT_LANGUAGE,
                CONTENT_LENGTH,
                CONTENT_LOCATION,
                CONTENT_RANGE,
                CONTENT_SECURITY_POLICY,
                CONTENT_SECURITY_POLICY_REPORT_ONLY,
                CONTENT_TYPE,
                COOKIE,
                DNT,
                DATE,
                ETAG,
                EXPECT,
                EXPIRES,
                FORWARDED,
                FROM,
                HOST,
                IF_MATCH,
                IF_MODIFIED_SINCE,
                IF_NONE_MATCH,
                IF_RANGE,
                IF_UNMODIFIED_SINCE,
                LAST_MODIFIED,
                LINK,
                LOCATION,
                MAX_FORWARDS,
                ORIGIN,
                PRAGMA,
                PROXY_AUTHENTICATE,
                PROXY_AUTHORIZATION,
                PUBLIC_KEY_PINS,
                PUBLIC_KEY_PINS_REPORT_ONLY,
                RANGE,
                REFERER,
                REFERRER_POLICY,
                RETRY_AFTER,
                SERVER,
                SET_COOKIE,
                STRICT_TRANSPORT_SECURITY,
                TE,
                TRAILER,
                TRANSFER_ENCODING,
                USER_AGENT,
                UPGRADE,
                UPGRADE_INSECURE_REQUESTS,
                VARY,
                VIA,
                WARNING,
                WWW_AUTHENTICATE,
                X_CONTENT_TYPE_OPTIONS,
                X_DNS_PREFETCH_CONTROL,
                X_FRAME_OPTIONS,
                X_XSS_PROTECTION,
            )
    }
}
