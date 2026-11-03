#if canImport(Testing)
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

    @Test("Swift module loads")
    func testSwiftModuleLoads() {
        #expect(Bool(true), "Http swift module imported cleanly")
    }
}
#elseif canImport(XCTest)
import XCTest
import Http

final class HttpExportTests: XCTestCase {
    func testHeaderNameNormalization() {
        let result = Http.header.HeaderName.Companion.shared.fromStr(src: "X-REQUEST-ID")
        XCTAssertTrue(result.isSuccess())
        XCTAssertNil(result.error)
        XCTAssertEqual(result.value?.asStr(), "x-request-id")
        XCTAssertEqual(Http.header.HOST.asStr(), "host")
    }

    func testHeaderNameFailure() {
        let result = Http.header.HeaderName.Companion.shared.fromStr(src: "bad name")
        XCTAssertTrue(result.isFailure())
        XCTAssertNil(result.value)
        XCTAssertEqual(result.error?.message, "invalid HTTP header name")
    }

    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "Http swift module imported cleanly")
    }
}
#endif
