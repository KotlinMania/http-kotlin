import Testing
import Http

@Suite("Http Header Round Trip Tests")
struct HttpHeaderRoundTripTests {
    @Test("Normalized custom headers compare equal when parsed again")
    func normalizedHeaderRoundTrip() {
        let first = Http.header.HeaderName.Companion.shared.fromStr(src: "X-TRACE-ID")
        #expect(first.isSuccess())
        #expect(first.value?.asStr() == "x-trace-id")
        let second = Http.header.HeaderName.Companion.shared.fromStr(src: "x-trace-id")
        #expect(second.isSuccess())
        #expect(first.value?.asStr() == second.value?.asStr())
    }
}
