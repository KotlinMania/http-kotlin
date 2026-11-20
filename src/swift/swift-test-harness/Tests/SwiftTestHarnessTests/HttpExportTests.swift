import Testing
import Http

@Suite("Http Swift Export Tests")
struct HttpExportTests {
    @Test("Header names normalize through the exported parser")
    func headerNameNormalization() {
        let result = Http.header.HeaderName.Companion.shared.fromStr(src: "X-REQUEST-ID")
        #expect(result.isSuccess())
        #expect(result.error == nil)
        #expect(result.value?.asStr() == "x-request-id")
        #expect(Http.header.HOST.asStr() == "host")
        #expect(result.value?.eq(other: "X-Request-Id") == true)
    }

    @Test("Invalid header names retain a concrete exported error")
    func headerNameFailure() {
        let result = Http.header.HeaderName.Companion.shared.fromStr(src: "bad name")
        #expect(result.isFailure())
        #expect(result.value == nil)
        #expect(result.error?.message == "invalid HTTP header name")
        #expect(result.error?.debugString() == "InvalidHeaderName")
    }

    @Test("Standard header names retain their canonical form")
    func standardHeaderName() {
        #expect(Http.header.HOST.asStr() == "host")
        #expect(Http.header.HOST.eq(other: "HOST"))
    }

    @Test("HeaderMap supports case-insensitive lookup, multi-value append, removal, and clear")
    func headerMapOperations() {
        let map = Http.header.HeaderMap.Companion.shared.new()
        #expect(map.isEmpty())
        #expect(map.len() == 0)

        let v1 = Http.header.HeaderValue.Companion.shared.fromStatic(src: "example.com")
        let v2 = Http.header.HeaderValue.Companion.shared.fromStatic(src: "example.org")
        #expect(map.insert(key: Http.header.HOST, value: v1) == nil)
        #expect(map.append(key: "HOST", value: v2))
        #expect(map.len() == 2)
        #expect(map.keysLen() == 1)
        #expect(map.containsKey(key: "host"))
        #expect((map["Host"] as? Http.header.HeaderValue)?.eq(other: "example.com") == true)

        let all = map.getAll(key: "host").iter()
        #expect((all.next() as? Http.header.HeaderValue)?.eq(other: "example.com") == true)
        #expect((all.next() as? Http.header.HeaderValue)?.eq(other: "example.org") == true)
        #expect(all.next() == nil)

        let removed = map.remove(key: Http.header.HOST)
        #expect((removed as? Http.header.HeaderValue)?.eq(other: "example.com") == true)
        #expect(map.isEmpty())

        let _ = map.insert(key: "x-custom", value: v1)
        map.clear()
        #expect(map.isEmpty())
        #expect(map.capacity() > 0)
    }

    @Test("Error wraps InvalidHeaderName and exposes debug and display representations")
    func wrappedHttpError() {
        let invalid = Http.header.HeaderName.Companion.shared.fromStr(src: "bad name").error!
        let wrapped = Http.Error.Companion.shared.from(err: invalid)
        #expect(wrapped.message == "invalid HTTP header name")
        #expect(wrapped.debugString() == "http::Error(InvalidHeaderName)")
    }
}
