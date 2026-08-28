#if canImport(Testing)
import Testing
import Http

@Suite("Http Swift Export Tests")
struct HttpExportTests {
    @Test("Swift module loads")
    func testSwiftModuleLoads() {
        #expect(Bool(true), "Http swift module imported cleanly")
    }
}
#elseif canImport(XCTest)
import XCTest
import Http

final class HttpExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "Http swift module imported cleanly")
    }
}
#endif
