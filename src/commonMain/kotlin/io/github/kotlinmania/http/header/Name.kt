// port-lint: source header/name.rs
package io.github.kotlinmania.http.header

import io.github.kotlinmania.bytes.Bytes
import io.github.kotlinmania.http.ByteStr

/** An HTTP header field name. Custom names are normalized to lowercase. */
class HeaderName private constructor(
    internal val inner: HeaderNameRepr<Custom>,
) {
    companion object {
        /** Converts bytes to a normalized HTTP header name. */
        fun fromBytes(src: ByteArray): HeaderNameResult = fromTable(src, headerChars, false)

        /** Accepts the lowercase names required by HTTP/2 and HTTP/3. */
        fun fromLowercase(src: ByteArray): HeaderNameResult = fromTable(src, headerCharsH2, true)

        private fun fromTable(src: ByteArray, table: ByteArray, lowercase: Boolean): HeaderNameResult {
            val parsed = parseHdr(src, table) ?: return HeaderNameResult.err(InvalidHeaderName.new())
            return when (val repr = parsed.inner) {
                is HeaderNameRepr.Standard -> HeaderNameResult.ok(from(repr.header))
                is HeaderNameRepr.Custom -> {
                    val borrowed = repr.value
                    val normalized = ByteArray(borrowed.buf.size)
                    for (i in borrowed.buf.indices) {
                        val byte = borrowed.buf[i]
                        val mapped = table[byte.toInt() and 255]
                        if (mapped == 0.toByte()) return HeaderNameResult.err(InvalidHeaderName.new())
                        normalized[i] = if (lowercase) byte else mapped
                    }
                    HeaderNameResult.ok(
                        HeaderName(
                            HeaderNameRepr.Custom(
                                Custom(
                                    ByteStr.fromUtf8Unchecked(Bytes.copyFromSlice(normalized)),
                                ),
                            ),
                        ),
                    )
                }
            }
        }

        /** Parses a lowercase constant name, rejecting invalid bytes. */
        fun fromStatic(src: String): HeaderName {
            val bytes = src.encodeToByteArray()
            StandardHeader.fromBytes(bytes)?.let { return from(it) }
            require(
                bytes.isNotEmpty() &&
                    bytes.size <= maxHeaderNameLength &&
                    bytes.all { headerCharsH2[it.toInt() and 255] != 0.toByte() },
            ) {
                "HeaderName.fromStatic with invalid bytes"
            }
            return HeaderName(HeaderNameRepr.Custom(Custom(ByteStr.fromStatic(src))))
        }

        fun fromStr(src: String): HeaderNameResult = fromBytes(src.encodeToByteArray())

        fun tryFrom(src: String): HeaderNameResult = fromStr(src)

        fun tryFrom(src: ByteArray): HeaderNameResult = fromBytes(src)

        fun from(src: HeaderName): HeaderName = HeaderName(src.inner)

        internal fun from(src: StandardHeader): HeaderName = HeaderName(HeaderNameRepr.Standard(src))

        internal fun from(src: HdrName): HeaderName =
            when (val repr = src.inner) {
                is HeaderNameRepr.Standard -> from(repr.header)
                is HeaderNameRepr.Custom -> {
                    val borrowed = repr.value
                    val bytes =
                        if (borrowed.lower) {
                            borrowed.buf.copyOf()
                        } else {
                            ByteArray(borrowed.buf.size) { headerChars[borrowed.buf[it].toInt() and 255] }
                        }
                    HeaderName(HeaderNameRepr.Custom(Custom(ByteStr.fromUtf8Unchecked(Bytes.copyFromSlice(bytes)))))
                }
            }
    }

    /** Returns the lowercase string representation. */
    fun asStr(): String =
        when (val repr = inner) {
            is HeaderNameRepr.Standard -> repr.header.asStr()
            is HeaderNameRepr.Custom -> repr.value.value.asStr()
        }

    fun asRef(): String = asStr()

    fun asBytes(): ByteArray = asStr().encodeToByteArray()

    fun borrow(): String = asStr()

    fun clone(): HeaderName = from(this)

    internal fun intoBytes(): Bytes =
        when (val repr = inner) {
            is HeaderNameRepr.Standard -> Bytes.fromStatic(repr.header.asStr())
            is HeaderNameRepr.Custom -> repr.value.value.intoBytes()
        }

    fun eq(other: HeaderName): Boolean = inner == other.inner

    fun eq(other: String): Boolean = eqIgnoreAsciiCase(asBytes(), other.encodeToByteArray())

    internal fun eq(other: HdrName): Boolean =
        when (val a = inner) {
            is HeaderNameRepr.Standard -> (other.inner as? HeaderNameRepr.Standard)?.header == a.header
            is HeaderNameRepr.Custom ->
                when (val b = other.inner) {
                    is HeaderNameRepr.Standard -> false
                    is HeaderNameRepr.Custom ->
                        if (b.value.lower) {
                            a.value.value
                                .intoBytes()
                                .asRef()
                                .contentEquals(b.value.buf)
                        } else {
                            eqIgnoreAsciiCase(
                                a.value.value
                                    .intoBytes()
                                    .asRef(),
                                b.value.buf,
                            )
                        }
                }
        }

    override fun equals(other: Any?): Boolean = other is HeaderName && eq(other)

    override fun hashCode(): Int =
        when (val repr = inner) {
            is HeaderNameRepr.Standard -> repr.header.hashCode()
            is HeaderNameRepr.Custom ->
                repr.value.value
                    .intoBytes()
                    .asRef()
                    .contentHashCode()
        }

    override fun toString(): String = asStr()

    fun debugString(): String = "\"" + asStr().replace("\"", "\\\"") + "\""
}

internal class HdrName(
    internal val inner: HeaderNameRepr<MaybeLower>,
) {
    companion object {
        internal fun custom(buf: ByteArray, lower: Boolean): HdrName = HdrName(HeaderNameRepr.Custom(MaybeLower(buf, lower)))

        internal fun from(header: StandardHeader): HdrName = HdrName(HeaderNameRepr.Standard(header))

        // generic by design: the caller selects the callback output, as in the upstream borrowed-name API.
        internal fun <U> fromBytes(hdr: ByteArray, operation: (HdrName) -> U): Result<U> {
            val parsed =
                parseHdr(hdr, headerChars)
                    ?: return Result.failure(IllegalArgumentException("invalid HTTP header name"))
            return Result.success(operation(parsed))
        }

        internal fun <U> fromStatic(hdr: String, operation: (HdrName) -> U): U {
            val parsed = requireNotNull(parseHdr(hdr.encodeToByteArray(), headerChars)) { "static str is invalid name" }
            return operation(parsed)
        }
    }

    override fun hashCode(): Int =
        when (val repr = inner) {
            is HeaderNameRepr.Standard -> repr.header.hashCode()
            is HeaderNameRepr.Custom -> {
                val borrowed = repr.value
                if (borrowed.lower) {
                    borrowed.buf.contentHashCode()
                } else {
                    ByteArray(borrowed.buf.size) { headerChars[borrowed.buf[it].toInt() and 255] }.contentHashCode()
                }
            }
        }
}

// generic by design: the owned and borrowed representations carry different concrete payloads.
internal sealed class HeaderNameRepr<out T> {
    data class Standard(
        val header: StandardHeader,
    ) : HeaderNameRepr<Nothing>()

    data class Custom<T>(
        val value: T,
    ) : HeaderNameRepr<T>()
}

internal data class Custom(
    val value: ByteStr,
)

internal class MaybeLower(
    val buf: ByteArray,
    val lower: Boolean,
)

/** A possible error when converting a header name. */
class InvalidHeaderName private constructor() {
    val message: String get() = "invalid HTTP header name"

    override fun toString(): String = message

    fun debugString(): String = "InvalidHeaderName"

    companion object {
        internal fun new(): InvalidHeaderName = InvalidHeaderName()
    }
}

/** A parsed header name or its concrete error. */
class HeaderNameResult private constructor(
    val value: HeaderName?,
    val error: InvalidHeaderName?,
) {
    init {
        require((value == null) != (error == null))
    }

    fun isSuccess(): Boolean = value != null

    fun isFailure(): Boolean = error != null

    fun getOrNull(): HeaderName? = value

    fun getOrThrow(): HeaderName = value ?: throw IllegalArgumentException(error?.message)

    companion object {
        internal fun ok(value: HeaderName): HeaderNameResult = HeaderNameResult(value, null)

        internal fun err(error: InvalidHeaderName): HeaderNameResult = HeaderNameResult(null, error)
    }
}

internal enum class StandardHeader(
    private val text: String,
) {
    Accept("accept"),
    AcceptCharset("accept-charset"),
    AcceptEncoding("accept-encoding"),
    AcceptLanguage("accept-language"),
    AcceptRanges("accept-ranges"),
    AccessControlAllowCredentials("access-control-allow-credentials"),
    AccessControlAllowHeaders("access-control-allow-headers"),
    AccessControlAllowMethods("access-control-allow-methods"),
    AccessControlAllowOrigin("access-control-allow-origin"),
    AccessControlExposeHeaders("access-control-expose-headers"),
    AccessControlMaxAge("access-control-max-age"),
    AccessControlRequestHeaders("access-control-request-headers"),
    AccessControlRequestMethod("access-control-request-method"),
    Age("age"),
    Allow("allow"),
    AltSvc("alt-svc"),
    Authorization("authorization"),
    CacheControl("cache-control"),
    CacheStatus("cache-status"),
    CdnCacheControl("cdn-cache-control"),
    Connection("connection"),
    ContentDisposition("content-disposition"),
    ContentEncoding("content-encoding"),
    ContentLanguage("content-language"),
    ContentLength("content-length"),
    ContentLocation("content-location"),
    ContentRange("content-range"),
    ContentSecurityPolicy("content-security-policy"),
    ContentSecurityPolicyReportOnly("content-security-policy-report-only"),
    ContentType("content-type"),
    Cookie("cookie"),
    Dnt("dnt"),
    Date("date"),
    Etag("etag"),
    Expect("expect"),
    Expires("expires"),
    Forwarded("forwarded"),
    From("from"),
    Host("host"),
    IfMatch("if-match"),
    IfModifiedSince("if-modified-since"),
    IfNoneMatch("if-none-match"),
    IfRange("if-range"),
    IfUnmodifiedSince("if-unmodified-since"),
    LastModified("last-modified"),
    Link("link"),
    Location("location"),
    MaxForwards("max-forwards"),
    Origin("origin"),
    Pragma("pragma"),
    ProxyAuthenticate("proxy-authenticate"),
    ProxyAuthorization("proxy-authorization"),
    PublicKeyPins("public-key-pins"),
    PublicKeyPinsReportOnly("public-key-pins-report-only"),
    Range("range"),
    Referer("referer"),
    ReferrerPolicy("referrer-policy"),
    Refresh("refresh"),
    RetryAfter("retry-after"),
    SecWebSocketAccept("sec-websocket-accept"),
    SecWebSocketExtensions("sec-websocket-extensions"),
    SecWebSocketKey("sec-websocket-key"),
    SecWebSocketProtocol("sec-websocket-protocol"),
    SecWebSocketVersion("sec-websocket-version"),
    Server("server"),
    SetCookie("set-cookie"),
    StrictTransportSecurity("strict-transport-security"),
    Te("te"),
    Trailer("trailer"),
    TransferEncoding("transfer-encoding"),
    UserAgent("user-agent"),
    Upgrade("upgrade"),
    UpgradeInsecureRequests("upgrade-insecure-requests"),
    Vary("vary"),
    Via("via"),
    Warning("warning"),
    WwwAuthenticate("www-authenticate"),
    XContentTypeOptions("x-content-type-options"),
    XDnsPrefetchControl("x-dns-prefetch-control"),
    XFrameOptions("x-frame-options"),
    XXssProtection("x-xss-protection"),
    ;

    fun asStr(): String = text

    companion object {
        fun fromBytes(bytes: ByteArray): StandardHeader? =
            when (bytes.decodeToString()) {
                "accept" -> Accept
                "accept-charset" -> AcceptCharset
                "accept-encoding" -> AcceptEncoding
                "accept-language" -> AcceptLanguage
                "accept-ranges" -> AcceptRanges
                "access-control-allow-credentials" -> AccessControlAllowCredentials
                "access-control-allow-headers" -> AccessControlAllowHeaders
                "access-control-allow-methods" -> AccessControlAllowMethods
                "access-control-allow-origin" -> AccessControlAllowOrigin
                "access-control-expose-headers" -> AccessControlExposeHeaders
                "access-control-max-age" -> AccessControlMaxAge
                "access-control-request-headers" -> AccessControlRequestHeaders
                "access-control-request-method" -> AccessControlRequestMethod
                "age" -> Age
                "allow" -> Allow
                "alt-svc" -> AltSvc
                "authorization" -> Authorization
                "cache-control" -> CacheControl
                "cache-status" -> CacheStatus
                "cdn-cache-control" -> CdnCacheControl
                "connection" -> Connection
                "content-disposition" -> ContentDisposition
                "content-encoding" -> ContentEncoding
                "content-language" -> ContentLanguage
                "content-length" -> ContentLength
                "content-location" -> ContentLocation
                "content-range" -> ContentRange
                "content-security-policy" -> ContentSecurityPolicy
                "content-security-policy-report-only" -> ContentSecurityPolicyReportOnly
                "content-type" -> ContentType
                "cookie" -> Cookie
                "dnt" -> Dnt
                "date" -> Date
                "etag" -> Etag
                "expect" -> Expect
                "expires" -> Expires
                "forwarded" -> Forwarded
                "from" -> From
                "host" -> Host
                "if-match" -> IfMatch
                "if-modified-since" -> IfModifiedSince
                "if-none-match" -> IfNoneMatch
                "if-range" -> IfRange
                "if-unmodified-since" -> IfUnmodifiedSince
                "last-modified" -> LastModified
                "link" -> Link
                "location" -> Location
                "max-forwards" -> MaxForwards
                "origin" -> Origin
                "pragma" -> Pragma
                "proxy-authenticate" -> ProxyAuthenticate
                "proxy-authorization" -> ProxyAuthorization
                "public-key-pins" -> PublicKeyPins
                "public-key-pins-report-only" -> PublicKeyPinsReportOnly
                "range" -> Range
                "referer" -> Referer
                "referrer-policy" -> ReferrerPolicy
                "refresh" -> Refresh
                "retry-after" -> RetryAfter
                "sec-websocket-accept" -> SecWebSocketAccept
                "sec-websocket-extensions" -> SecWebSocketExtensions
                "sec-websocket-key" -> SecWebSocketKey
                "sec-websocket-protocol" -> SecWebSocketProtocol
                "sec-websocket-version" -> SecWebSocketVersion
                "server" -> Server
                "set-cookie" -> SetCookie
                "strict-transport-security" -> StrictTransportSecurity
                "te" -> Te
                "trailer" -> Trailer
                "transfer-encoding" -> TransferEncoding
                "user-agent" -> UserAgent
                "upgrade" -> Upgrade
                "upgrade-insecure-requests" -> UpgradeInsecureRequests
                "vary" -> Vary
                "via" -> Via
                "warning" -> Warning
                "www-authenticate" -> WwwAuthenticate
                "x-content-type-options" -> XContentTypeOptions
                "x-dns-prefetch-control" -> XDnsPrefetchControl
                "x-frame-options" -> XFrameOptions
                "x-xss-protection" -> XXssProtection
                else -> null
            }
    }
}

/**
 * Advertises which content types the client is able to understand.
 *
 * The Accept request HTTP header advertises which content types, expressed
 * as MIME types, the client is able to understand. Using content
 * negotiation, the server then selects one of the proposals, uses it and
 * informs the client of its choice with the Content-Type response header.
 * Browsers set adequate values for this header depending of the context
 * where the request is done: when fetching a CSS stylesheet a different
 * value is set for the request than when fetching an image, video or a
 * script.
 */
val ACCEPT: HeaderName = HeaderName.from(StandardHeader.Accept)

/**
 * Advertises which character set the client is able to understand.
 *
 * The Accept-Charset request HTTP header advertises which character set
 * the client is able to understand. Using content negotiation, the server
 * then selects one of the proposals, uses it and informs the client of its
 * choice within the Content-Type response header. Browsers usually don't
 * set this header as the default value for each content type is usually
 * correct and transmitting it would allow easier fingerprinting.
 *
 * If the server cannot serve any matching character set, it can
 * theoretically send back a 406 (Not Acceptable) error code. But, for a
 * better user experience, this is rarely done and the more common way is
 * to ignore the Accept-Charset header in this case.
 */
val ACCEPT_CHARSET: HeaderName = HeaderName.from(StandardHeader.AcceptCharset)

/**
 * Advertises which content encoding the client is able to understand.
 *
 * The Accept-Encoding request HTTP header advertises which content
 * encoding, usually a compression algorithm, the client is able to
 * understand. Using content negotiation, the server selects one of the
 * proposals, uses it and informs the client of its choice with the
 * Content-Encoding response header.
 *
 * Even if both the client and the server supports the same compression
 * algorithms, the server may choose not to compress the body of a
 * response, if the identity value is also acceptable. Two common cases
 * lead to this:
 *
 * * The data to be sent is already compressed and a second compression
 * won't lead to smaller data to be transmitted. This may the case with
 * some image formats;
 *
 * * The server is overloaded and cannot afford the computational overhead
 * induced by the compression requirement. Typically, Microsoft recommends
 * not to compress if a server use more than 80 % of its computational
 * power.
 *
 * As long as the identity value, meaning no compression, is not explicitly
 * forbidden, by an identity;q=0 or a *;q=0 without another explicitly set
 * value for identity, the server must never send back a 406 Not Acceptable
 * error.
 */
val ACCEPT_ENCODING: HeaderName = HeaderName.from(StandardHeader.AcceptEncoding)

/**
 * Advertises which languages the client is able to understand.
 *
 * The Accept-Language request HTTP header advertises which languages the
 * client is able to understand, and which locale variant is preferred.
 * Using content negotiation, the server then selects one of the proposals,
 * uses it and informs the client of its choice with the Content-Language
 * response header. Browsers set adequate values for this header according
 * their user interface language and even if a user can change it, this
 * happens rarely (and is frown upon as it leads to fingerprinting).
 *
 * This header is a hint to be used when the server has no way of
 * determining the language via another way, like a specific URL, that is
 * controlled by an explicit user decision. It is recommended that the
 * server never overrides an explicit decision. The content of the
 * Accept-Language is often out of the control of the user (like when
 * traveling and using an Internet Cafe in a different country); the user
 * may also want to visit a page in another language than the locale of
 * their user interface.
 *
 * If the server cannot serve any matching language, it can theoretically
 * send back a 406 (Not Acceptable) error code. But, for a better user
 * experience, this is rarely done and more common way is to ignore the
 * Accept-Language header in this case.
 */
val ACCEPT_LANGUAGE: HeaderName = HeaderName.from(StandardHeader.AcceptLanguage)

/**
 * Marker used by the server to advertise partial request support.
 *
 * The Accept-Ranges response HTTP header is a marker used by the server to
 * advertise its support of partial requests. The value of this field
 * indicates the unit that can be used to define a range.
 *
 * In presence of an Accept-Ranges header, the browser may try to resume an
 * interrupted download, rather than to start it from the start again.
 */
val ACCEPT_RANGES: HeaderName = HeaderName.from(StandardHeader.AcceptRanges)

/**
 * Preflight response indicating if the response to the request can be
 * exposed to the page.
 *
 * The Access-Control-Allow-Credentials response header indicates whether
 * or not the response to the request can be exposed to the page. It can be
 * exposed when the true value is returned; it can't in other cases.
 *
 * Credentials are cookies, authorization headers or TLS client
 * certificates.
 *
 * When used as part of a response to a preflight request, this indicates
 * whether or not the actual request can be made using credentials. Note
 * that simple GET requests are not preflighted, and so if a request is
 * made for a resource with credentials, if this header is not returned
 * with the resource, the response is ignored by the browser and not
 * returned to web content.
 *
 * The Access-Control-Allow-Credentials header works in conjunction with
 * the XMLHttpRequest.withCredentials property or with the credentials
 * option in the Request() constructor of the Fetch API. Credentials must
 * be set on both sides (the Access-Control-Allow-Credentials header and in
 * the XHR or Fetch request) in order for the CORS request with credentials
 * to succeed.
 */
val ACCESS_CONTROL_ALLOW_CREDENTIALS: HeaderName = HeaderName.from(StandardHeader.AccessControlAllowCredentials)

/**
 * Preflight response indicating permitted HTTP headers.
 *
 * The Access-Control-Allow-Headers response header is used in response to
 * a preflight request to indicate which HTTP headers will be available via
 * Access-Control-Expose-Headers when making the actual request.
 *
 * The simple headers, Accept, Accept-Language, Content-Language,
 * Content-Type (but only with a MIME type of its parsed value (ignoring
 * parameters) of either application/x-www-form-urlencoded,
 * multipart/form-data, or text/plain), are always available and don't need
 * to be listed by this header.
 *
 * This header is required if the request has an
 * Access-Control-Request-Headers header.
 */
val ACCESS_CONTROL_ALLOW_HEADERS: HeaderName = HeaderName.from(StandardHeader.AccessControlAllowHeaders)

/**
 * Preflight header response indicating permitted access methods.
 *
 * The Access-Control-Allow-Methods response header specifies the method or
 * methods allowed when accessing the resource in response to a preflight
 * request.
 */
val ACCESS_CONTROL_ALLOW_METHODS: HeaderName = HeaderName.from(StandardHeader.AccessControlAllowMethods)

/**
 * Indicates whether the response can be shared with resources with the
 * given origin.
 */
val ACCESS_CONTROL_ALLOW_ORIGIN: HeaderName = HeaderName.from(StandardHeader.AccessControlAllowOrigin)

/**
 * Indicates which headers can be exposed as part of the response by
 * listing their names.
 */
val ACCESS_CONTROL_EXPOSE_HEADERS: HeaderName = HeaderName.from(StandardHeader.AccessControlExposeHeaders)

/**
 * Indicates how long the results of a preflight request can be cached.
 */
val ACCESS_CONTROL_MAX_AGE: HeaderName = HeaderName.from(StandardHeader.AccessControlMaxAge)

/**
 * Informs the server which HTTP headers will be used when an actual
 * request is made.
 */
val ACCESS_CONTROL_REQUEST_HEADERS: HeaderName = HeaderName.from(StandardHeader.AccessControlRequestHeaders)

/**
 * Informs the server know which HTTP method will be used when the actual
 * request is made.
 */
val ACCESS_CONTROL_REQUEST_METHOD: HeaderName = HeaderName.from(StandardHeader.AccessControlRequestMethod)

/**
 * Indicates the time in seconds the object has been in a proxy cache.
 *
 * The Age header is usually close to zero. If it is Age: 0, it was
 * probably just fetched from the origin server; otherwise It is usually
 * calculated as a difference between the proxy's current date and the Date
 * general header included in the HTTP response.
 */
val AGE: HeaderName = HeaderName.from(StandardHeader.Age)

/**
 * Lists the set of methods support by a resource.
 *
 * This header must be sent if the server responds with a 405 Method Not
 * Allowed status code to indicate which request methods can be used. An
 * empty Allow header indicates that the resource allows no request
 * methods, which might occur temporarily for a given resource, for
 * example.
 */
val ALLOW: HeaderName = HeaderName.from(StandardHeader.Allow)

/**
 * Advertises the availability of alternate services to clients.
 */
val ALT_SVC: HeaderName = HeaderName.from(StandardHeader.AltSvc)

/**
 * Contains the credentials to authenticate a user agent with a server.
 *
 * Usually this header is included after the server has responded with a
 * 401 Unauthorized status and the WWW-Authenticate header.
 */
val AUTHORIZATION: HeaderName = HeaderName.from(StandardHeader.Authorization)

/**
 * Specifies directives for caching mechanisms in both requests and
 * responses.
 *
 * Caching directives are unidirectional, meaning that a given directive in
 * a request is not implying that the same directive is to be given in the
 * response.
 */
val CACHE_CONTROL: HeaderName = HeaderName.from(StandardHeader.CacheControl)

/**
 * Indicates how caches have handled a response and its corresponding request.
 *
 * See [RFC 9211](https://www.rfc-editor.org/rfc/rfc9211.html).
 */
val CACHE_STATUS: HeaderName = HeaderName.from(StandardHeader.CacheStatus)

/**
 * Specifies directives that allow origin servers to control the behavior of CDN caches
 * interposed between them and clients separately from other caches that might handle the
 * response.
 *
 * See [RFC 9213](https://www.rfc-editor.org/rfc/rfc9213.html).
 */
val CDN_CACHE_CONTROL: HeaderName = HeaderName.from(StandardHeader.CdnCacheControl)

/**
 * Controls whether or not the network connection stays open after the
 * current transaction finishes.
 *
 * If the value sent is keep-alive, the connection is persistent and not
 * closed, allowing for subsequent requests to the same server to be done.
 *
 * Except for the standard hop-by-hop headers (Keep-Alive,
 * Transfer-Encoding, TE, Connection, Trailer, Upgrade, Proxy-Authorization
 * and Proxy-Authenticate), any hop-by-hop headers used by the message must
 * be listed in the Connection header, so that the first proxy knows he has
 * to consume them and not to forward them further. Standard hop-by-hop
 * headers can be listed too (it is often the case of Keep-Alive, but this
 * is not mandatory.
 */
val CONNECTION: HeaderName = HeaderName.from(StandardHeader.Connection)

/**
 * Indicates if the content is expected to be displayed inline.
 *
 * In a regular HTTP response, the Content-Disposition response header is a
 * header indicating if the content is expected to be displayed inline in
 * the browser, that is, as a Web page or as part of a Web page, or as an
 * attachment, that is downloaded and saved locally.
 *
 * In a multipart/form-data body, the HTTP Content-Disposition general
 * header is a header that can be used on the subpart of a multipart body
 * to give information about the field it applies to. The subpart is
 * delimited by the boundary defined in the Content-Type header. Used on
 * the body itself, Content-Disposition has no effect.
 *
 * The Content-Disposition header is defined in the larger context of MIME
 * messages for e-mail, but only a subset of the possible parameters apply
 * to HTTP forms and POST requests. Only the value form-data, as well as
 * the optional directive name and filename, can be used in the HTTP
 * context.
 */
val CONTENT_DISPOSITION: HeaderName = HeaderName.from(StandardHeader.ContentDisposition)

/**
 * Used to compress the media-type.
 *
 * When present, its value indicates what additional content encoding has
 * been applied to the entity-body. It lets the client know, how to decode
 * in order to obtain the media-type referenced by the Content-Type header.
 *
 * It is recommended to compress data as much as possible and therefore to
 * use this field, but some types of resources, like jpeg images, are
 * already compressed.  Sometimes using additional compression doesn't
 * reduce payload size and can even make the payload longer.
 */
val CONTENT_ENCODING: HeaderName = HeaderName.from(StandardHeader.ContentEncoding)

/**
 * Used to describe the languages intended for the audience.
 *
 * This header allows a user to differentiate according to the users' own
 * preferred language. For example, if "Content-Language: de-DE" is set, it
 * says that the document is intended for German language speakers
 * (however, it doesn't indicate the document is written in German. For
 * example, it might be written in English as part of a language course for
 * German speakers).
 *
 * If no Content-Language is specified, the default is that the content is
 * intended for all language audiences. Multiple language tags are also
 * possible, as well as applying the Content-Language header to various
 * media types and not only to textual documents.
 */
val CONTENT_LANGUAGE: HeaderName = HeaderName.from(StandardHeader.ContentLanguage)

/**
 * Indicates the size of the entity-body.
 *
 * The header value must be a decimal indicating the number of octets sent
 * to the recipient.
 */
val CONTENT_LENGTH: HeaderName = HeaderName.from(StandardHeader.ContentLength)

/**
 * Indicates an alternate location for the returned data.
 *
 * The principal use case is to indicate the URL of the resource
 * transmitted as the result of content negotiation.
 *
 * Location and Content-Location are different: Location indicates the
 * target of a redirection (or the URL of a newly created document), while
 * Content-Location indicates the direct URL to use to access the resource,
 * without the need of further content negotiation. Location is a header
 * associated with the response, while Content-Location is associated with
 * the entity returned.
 */
val CONTENT_LOCATION: HeaderName = HeaderName.from(StandardHeader.ContentLocation)

/**
 * Indicates where in a full body message a partial message belongs.
 */
val CONTENT_RANGE: HeaderName = HeaderName.from(StandardHeader.ContentRange)

/**
 * Allows controlling resources the user agent is allowed to load for a
 * given page.
 *
 * With a few exceptions, policies mostly involve specifying server origins
 * and script endpoints. This helps guard against cross-site scripting
 * attacks (XSS).
 */
val CONTENT_SECURITY_POLICY: HeaderName = HeaderName.from(StandardHeader.ContentSecurityPolicy)

/**
 * Allows experimenting with policies by monitoring their effects.
 *
 * The HTTP Content-Security-Policy-Report-Only response header allows web
 * developers to experiment with policies by monitoring (but not enforcing)
 * their effects. These violation reports consist of JSON documents sent
 * via an HTTP POST request to the specified URI.
 */
val CONTENT_SECURITY_POLICY_REPORT_ONLY: HeaderName = HeaderName.from(StandardHeader.ContentSecurityPolicyReportOnly)

/**
 * Used to indicate the media type of the resource.
 *
 * In responses, a Content-Type header tells the client what the content
 * type of the returned content actually is. Browsers will do MIME sniffing
 * in some cases and will not necessarily follow the value of this header;
 * to prevent this behavior, the header X-Content-Type-Options can be set
 * to nosniff.
 *
 * In requests, (such as POST or PUT), the client tells the server what
 * type of data is actually sent.
 */
val CONTENT_TYPE: HeaderName = HeaderName.from(StandardHeader.ContentType)

/**
 * Contains stored HTTP cookies previously sent by the server with the
 * Set-Cookie header.
 *
 * The Cookie header might be omitted entirely, if the privacy setting of
 * the browser are set to block them, for example.
 */
val COOKIE: HeaderName = HeaderName.from(StandardHeader.Cookie)

/**
 * Indicates the client's tracking preference.
 *
 * This header lets users indicate whether they would prefer privacy rather
 * than personalized content.
 */
val DNT: HeaderName = HeaderName.from(StandardHeader.Dnt)

/**
 * Contains the date and time at which the message was originated.
 */
val DATE: HeaderName = HeaderName.from(StandardHeader.Date)

/**
 * Identifier for a specific version of a resource.
 *
 * This header allows caches to be more efficient, and saves bandwidth, as
 * a web server does not need to send a full response if the content has
 * not changed. On the other side, if the content has changed, etags are
 * useful to help prevent simultaneous updates of a resource from
 * overwriting each other ("mid-air collisions").
 *
 * If the resource at a given URL changes, a new Etag value must be
 * generated. Etags are therefore similar to fingerprints and might also be
 * used for tracking purposes by some servers. A comparison of them allows
 * to quickly determine whether two representations of a resource are the
 * same, but they might also be set to persist indefinitely by a tracking
 * server.
 */
val ETAG: HeaderName = HeaderName.from(StandardHeader.Etag)

/**
 * Indicates expectations that need to be fulfilled by the server in order
 * to properly handle the request.
 *
 * The only expectation defined in the specification is Expect:
 * 100-continue, to which the server shall respond with:
 *
 * * 100 if the information contained in the header is sufficient to cause
 * an immediate success,
 *
 * * 417 (Expectation Failed) if it cannot meet the expectation; or any
 * other 4xx status otherwise.
 *
 * For example, the server may reject a request if its Content-Length is
 * too large.
 *
 * No common browsers send the Expect header, but some other clients such
 * as cURL do so by default.
 */
val EXPECT: HeaderName = HeaderName.from(StandardHeader.Expect)

/**
 * Contains the date/time after which the response is considered stale.
 *
 * Invalid dates, like the value 0, represent a date in the past and mean
 * that the resource is already expired.
 *
 * If there is a Cache-Control header with the "max-age" or "s-max-age"
 * directive in the response, the Expires header is ignored.
 */
val EXPIRES: HeaderName = HeaderName.from(StandardHeader.Expires)

/**
 * Contains information from the client-facing side of proxy servers that
 * is altered or lost when a proxy is involved in the path of the request.
 *
 * The alternative and de-facto standard versions of this header are the
 * X-Forwarded-For, X-Forwarded-Host and X-Forwarded-Proto headers.
 *
 * This header is used for debugging, statistics, and generating
 * location-dependent content and by design it exposes privacy sensitive
 * information, such as the IP address of the client. Therefore the user's
 * privacy must be kept in mind when deploying this header.
 */
val FORWARDED: HeaderName = HeaderName.from(StandardHeader.Forwarded)

/**
 * Contains an Internet email address for a human user who controls the
 * requesting user agent.
 *
 * If you are running a robotic user agent (e.g. a crawler), the From
 * header should be sent, so you can be contacted if problems occur on
 * servers, such as if the robot is sending excessive, unwanted, or invalid
 * requests.
 */
val FROM: HeaderName = HeaderName.from(StandardHeader.From)

/**
 * Specifies the domain name of the server and (optionally) the TCP port
 * number on which the server is listening.
 *
 * If no port is given, the default port for the service requested (e.g.,
 * "80" for an HTTP URL) is implied.
 *
 * A Host header field must be sent in all HTTP/1.1 request messages. A 400
 * (Bad Request) status code will be sent to any HTTP/1.1 request message
 * that lacks a Host header field or contains more than one.
 */
val HOST: HeaderName = HeaderName.from(StandardHeader.Host)

/**
 * Makes a request conditional based on the E-Tag.
 *
 * For GET and HEAD methods, the server will send back the requested
 * resource only if it matches one of the listed ETags. For PUT and other
 * non-safe methods, it will only upload the resource in this case.
 *
 * The comparison with the stored ETag uses the strong comparison
 * algorithm, meaning two files are considered identical byte to byte only.
 * This is weakened when the  W/ prefix is used in front of the ETag.
 *
 * There are two common use cases:
 *
 * * For GET and HEAD methods, used in combination with an Range header, it
 * can guarantee that the new ranges requested comes from the same resource
 * than the previous one. If it doesn't match, then a 416 (Range Not
 * Satisfiable) response is returned.
 *
 * * For other methods, and in particular for PUT, If-Match can be used to
 * prevent the lost update problem. It can check if the modification of a
 * resource that the user wants to upload will not override another change
 * that has been done since the original resource was fetched. If the
 * request cannot be fulfilled, the 412 (Precondition Failed) response is
 * returned.
 */
val IF_MATCH: HeaderName = HeaderName.from(StandardHeader.IfMatch)

/**
 * Makes a request conditional based on the modification date.
 *
 * The If-Modified-Since request HTTP header makes the request conditional:
 * the server will send back the requested resource, with a 200 status,
 * only if it has been last modified after the given date. If the request
 * has not been modified since, the response will be a 304 without any
 * body; the Last-Modified header will contain the date of last
 * modification. Unlike If-Unmodified-Since, If-Modified-Since can only be
 * used with a GET or HEAD.
 *
 * When used in combination with If-None-Match, it is ignored, unless the
 * server doesn't support If-None-Match.
 *
 * The most common use case is to update a cached entity that has no
 * associated ETag.
 */
val IF_MODIFIED_SINCE: HeaderName = HeaderName.from(StandardHeader.IfModifiedSince)

/**
 * Makes a request conditional based on the E-Tag.
 *
 * The If-None-Match HTTP request header makes the request conditional. For
 * GET and HEAD methods, the server will send back the requested resource,
 * with a 200 status, only if it doesn't have an ETag matching the given
 * ones. For other methods, the request will be processed only if the
 * eventually existing resource's ETag doesn't match any of the values
 * listed.
 *
 * When the condition fails for GET and HEAD methods, then the server must
 * return HTTP status code 304 (Not Modified). For methods that apply
 * server-side changes, the status code 412 (Precondition Failed) is used.
 * Note that the server generating a 304 response MUST generate any of the
 * following header fields that would have been sent in a 200 (OK) response
 * to the same request: Cache-Control, Content-Location, Date, ETag,
 * Expires, and Vary.
 *
 * The comparison with the stored ETag uses the weak comparison algorithm,
 * meaning two files are considered identical not only if they are
 * identical byte to byte, but if the content is equivalent. For example,
 * two pages that would differ only by the date of generation in the footer
 * would be considered as identical.
 *
 * When used in combination with If-Modified-Since, it has precedence (if
 * the server supports it).
 *
 * There are two common use cases:
 *
 * * For `GET` and `HEAD` methods, to update a cached entity that has an associated ETag.
 * * For other methods, and in particular for `PUT`, `If-None-Match` used with
 * the `*` value can be used to save a file not known to exist,
 * guaranteeing that another upload didn't happen before, losing the data
 * of the previous put; this problems is the variation of the lost update
 * problem.
 */
val IF_NONE_MATCH: HeaderName = HeaderName.from(StandardHeader.IfNoneMatch)

/**
 * Makes a request conditional based on range.
 *
 * The If-Range HTTP request header makes a range request conditional: if
 * the condition is fulfilled, the range request will be issued and the
 * server sends back a 206 Partial Content answer with the appropriate
 * body. If the condition is not fulfilled, the full resource is sent back,
 * with a 200 OK status.
 *
 * This header can be used either with a Last-Modified validator, or with
 * an ETag, but not with both.
 *
 * The most common use case is to resume a download, to guarantee that the
 * stored resource has not been modified since the last fragment has been
 * received.
 */
val IF_RANGE: HeaderName = HeaderName.from(StandardHeader.IfRange)

/**
 * Makes the request conditional based on the last modification date.
 *
 * The If-Unmodified-Since request HTTP header makes the request
 * conditional: the server will send back the requested resource, or accept
 * it in the case of a POST or another non-safe method, only if it has not
 * been last modified after the given date. If the request has been
 * modified after the given date, the response will be a 412 (Precondition
 * Failed) error.
 *
 * There are two common use cases:
 *
 * * In conjunction non-safe methods, like POST, it can be used to
 * implement an optimistic concurrency control, like done by some wikis:
 * editions are rejected if the stored document has been modified since the
 * original has been retrieved.
 *
 * * In conjunction with a range request with a If-Range header, it can be
 * used to ensure that the new fragment requested comes from an unmodified
 * document.
 */
val IF_UNMODIFIED_SINCE: HeaderName = HeaderName.from(StandardHeader.IfUnmodifiedSince)

/**
 * The Last-Modified header contains the date and time when the origin believes
 * the resource was last modified.
 *
 * The value is a valid Date/Time string defined in [RFC9910](https://datatracker.ietf.org/doc/html/rfc9110#section-5.6.7)
 */
val LAST_MODIFIED: HeaderName = HeaderName.from(StandardHeader.LastModified)

/**
 * Allows the server to point an interested client to another resource
 * containing metadata about the requested resource.
 */
val LINK: HeaderName = HeaderName.from(StandardHeader.Link)

/**
 * Indicates the URL to redirect a page to.
 *
 * The Location response header indicates the URL to redirect a page to. It
 * only provides a meaning when served with a 3xx status response.
 *
 * The HTTP method used to make the new request to fetch the page pointed
 * to by Location depends of the original method and of the kind of
 * redirection:
 *
 * * If 303 (See Also) responses always lead to the use of a GET method,
 * 307 (Temporary Redirect) and 308 (Permanent Redirect) don't change the
 * method used in the original request;
 *
 * * 301 (Permanent Redirect) and 302 (Found) doesn't change the method
 * most of the time, though older user-agents may (so you basically don't
 * know).
 *
 * All responses with one of these status codes send a Location header.
 *
 * Beside redirect response, messages with 201 (Created) status also
 * include the Location header. It indicates the URL to the newly created
 * resource.
 *
 * Location and Content-Location are different: Location indicates the
 * target of a redirection (or the URL of a newly created resource), while
 * Content-Location indicates the direct URL to use to access the resource
 * when content negotiation happened, without the need of further content
 * negotiation. Location is a header associated with the response, while
 * Content-Location is associated with the entity returned.
 */
val LOCATION: HeaderName = HeaderName.from(StandardHeader.Location)

/**
 * Indicates the max number of intermediaries the request should be sent
 * through.
 */
val MAX_FORWARDS: HeaderName = HeaderName.from(StandardHeader.MaxForwards)

/**
 * Indicates where a fetch originates from.
 *
 * It doesn't include any path information, but only the server name. It is
 * sent with CORS requests, as well as with POST requests. It is similar to
 * the Referer header, but, unlike this header, it doesn't disclose the
 * whole path.
 */
val ORIGIN: HeaderName = HeaderName.from(StandardHeader.Origin)

/**
 * HTTP/1.0 header usually used for backwards compatibility.
 *
 * The Pragma HTTP/1.0 general header is an implementation-specific header
 * that may have various effects along the request-response chain. It is
 * used for backwards compatibility with HTTP/1.0 caches where the
 * Cache-Control HTTP/1.1 header is not yet present.
 */
val PRAGMA: HeaderName = HeaderName.from(StandardHeader.Pragma)

/**
 * Defines the authentication method that should be used to gain access to
 * a proxy.
 *
 * Unlike `www-authenticate`, the `proxy-authenticate` header field applies
 * only to the next outbound client on the response chain. This is because
 * only the client that chose a given proxy is likely to have the
 * credentials necessary for authentication. However, when multiple proxies
 * are used within the same administrative domain, such as office and
 * regional caching proxies within a large corporate network, it is common
 * for credentials to be generated by the user agent and passed through the
 * hierarchy until consumed. Hence, in such a configuration, it will appear
 * as if Proxy-Authenticate is being forwarded because each proxy will send
 * the same challenge set.
 *
 * The `proxy-authenticate` header is sent along with a `407 Proxy
 * Authentication Required`.
 */
val PROXY_AUTHENTICATE: HeaderName = HeaderName.from(StandardHeader.ProxyAuthenticate)

/**
 * Contains the credentials to authenticate a user agent to a proxy server.
 *
 * This header is usually included after the server has responded with a
 * 407 Proxy Authentication Required status and the Proxy-Authenticate
 * header.
 */
val PROXY_AUTHORIZATION: HeaderName = HeaderName.from(StandardHeader.ProxyAuthorization)

/**
 * Associates a specific cryptographic public key with a certain server.
 *
 * This decreases the risk of MITM attacks with forged certificates. If one
 * or several keys are pinned and none of them are used by the server, the
 * browser will not accept the response as legitimate, and will not display
 * it.
 */
val PUBLIC_KEY_PINS: HeaderName = HeaderName.from(StandardHeader.PublicKeyPins)

/**
 * Sends reports of pinning violation to the report-uri specified in the
 * header.
 *
 * Unlike `Public-Key-Pins`, this header still allows browsers to connect
 * to the server if the pinning is violated.
 */
val PUBLIC_KEY_PINS_REPORT_ONLY: HeaderName = HeaderName.from(StandardHeader.PublicKeyPinsReportOnly)

/**
 * Indicates the part of a document that the server should return.
 *
 * Several parts can be requested with one Range header at once, and the
 * server may send back these ranges in a multipart document. If the server
 * sends back ranges, it uses the 206 Partial Content for the response. If
 * the ranges are invalid, the server returns the 416 Range Not Satisfiable
 * error. The server can also ignore the Range header and return the whole
 * document with a 200 status code.
 */
val RANGE: HeaderName = HeaderName.from(StandardHeader.Range)

/**
 * Contains the address of the previous web page from which a link to the
 * currently requested page was followed.
 *
 * The Referer header allows servers to identify where people are visiting
 * them from and may use that data for analytics, logging, or optimized
 * caching, for example.
 */
val REFERER: HeaderName = HeaderName.from(StandardHeader.Referer)

/**
 * Governs which referrer information should be included with requests
 * made.
 */
val REFERRER_POLICY: HeaderName = HeaderName.from(StandardHeader.ReferrerPolicy)

/**
 * Informs the web browser that the current page or frame should be
 * refreshed.
 */
val REFRESH: HeaderName = HeaderName.from(StandardHeader.Refresh)

/**
 * The Retry-After response HTTP header indicates how long the user agent
 * should wait before making a follow-up request. There are two main cases
 * this header is used:
 *
 * * When sent with a 503 (Service Unavailable) response, it indicates how
 * long the service is expected to be unavailable.
 *
 * * When sent with a redirect response, such as 301 (Moved Permanently),
 * it indicates the minimum time that the user agent is asked to wait
 * before issuing the redirected request.
 */
val RETRY_AFTER: HeaderName = HeaderName.from(StandardHeader.RetryAfter)

/**
 * The |Sec-WebSocket-Accept| header field is used in the WebSocket
 * opening handshake. It is sent from the server to the client to
 * confirm that the server is willing to initiate the WebSocket
 * connection.
 */
val SEC_WEBSOCKET_ACCEPT: HeaderName = HeaderName.from(StandardHeader.SecWebSocketAccept)

/**
 * The |Sec-WebSocket-Extensions| header field is used in the WebSocket
 * opening handshake. It is initially sent from the client to the
 * server, and then subsequently sent from the server to the client, to
 * agree on a set of protocol-level extensions to use for the duration
 * of the connection.
 */
val SEC_WEBSOCKET_EXTENSIONS: HeaderName = HeaderName.from(StandardHeader.SecWebSocketExtensions)

/**
 * The |Sec-WebSocket-Key| header field is used in the WebSocket opening
 * handshake. It is sent from the client to the server to provide part
 * of the information used by the server to prove that it received a
 * valid WebSocket opening handshake. This helps ensure that the server
 * does not accept connections from non-WebSocket clients (e.g., HTTP
 * clients) that are being abused to send data to unsuspecting WebSocket
 * servers.
 */
val SEC_WEBSOCKET_KEY: HeaderName = HeaderName.from(StandardHeader.SecWebSocketKey)

/**
 * The |Sec-WebSocket-Protocol| header field is used in the WebSocket
 * opening handshake. It is sent from the client to the server and back
 * from the server to the client to confirm the subprotocol of the
 * connection.  This enables scripts to both select a subprotocol and be
 * sure that the server agreed to serve that subprotocol.
 */
val SEC_WEBSOCKET_PROTOCOL: HeaderName = HeaderName.from(StandardHeader.SecWebSocketProtocol)

/**
 * The |Sec-WebSocket-Version| header field is used in the WebSocket
 * opening handshake.  It is sent from the client to the server to
 * indicate the protocol version of the connection.  This enables
 * servers to correctly interpret the opening handshake and subsequent
 * data being sent from the data, and close the connection if the server
 * cannot interpret that data in a safe manner.
 */
val SEC_WEBSOCKET_VERSION: HeaderName = HeaderName.from(StandardHeader.SecWebSocketVersion)

/**
 * Contains information about the software used by the origin server to
 * handle the request.
 *
 * Overly long and detailed Server values should be avoided as they
 * potentially reveal internal implementation details that might make it
 * (slightly) easier for attackers to find and exploit known security
 * holes.
 */
val SERVER: HeaderName = HeaderName.from(StandardHeader.Server)

/**
 * Used to send cookies from the server to the user agent.
 */
val SET_COOKIE: HeaderName = HeaderName.from(StandardHeader.SetCookie)

/**
 * Tells the client to communicate with HTTPS instead of using HTTP.
 */
val STRICT_TRANSPORT_SECURITY: HeaderName = HeaderName.from(StandardHeader.StrictTransportSecurity)

/**
 * Informs the server of transfer encodings willing to be accepted as part
 * of the response.
 *
 * See also the Transfer-Encoding response header for more details on
 * transfer encodings. Note that chunked is always acceptable for HTTP/1.1
 * recipients and you that don't have to specify "chunked" using the TE
 * header. However, it is useful for setting if the client is accepting
 * trailer fields in a chunked transfer coding using the "trailers" value.
 */
val TE: HeaderName = HeaderName.from(StandardHeader.Te)

/**
 * Allows the sender to include additional fields at the end of chunked
 * messages.
 */
val TRAILER: HeaderName = HeaderName.from(StandardHeader.Trailer)

/**
 * Specifies the form of encoding used to safely transfer the entity to the
 * client.
 *
 * `transfer-encoding` is a hop-by-hop header, that is applying to a
 * message between two nodes, not to a resource itself. Each segment of a
 * multi-node connection can use different `transfer-encoding` values. If
 * you want to compress data over the whole connection, use the end-to-end
 * header `content-encoding` header instead.
 *
 * When present on a response to a `HEAD` request that has no body, it
 * indicates the value that would have applied to the corresponding `GET`
 * message.
 */
val TRANSFER_ENCODING: HeaderName = HeaderName.from(StandardHeader.TransferEncoding)

/**
 * Contains a string that allows identifying the requesting client's
 * software.
 */
val USER_AGENT: HeaderName = HeaderName.from(StandardHeader.UserAgent)

/**
 * Used as part of the exchange to upgrade the protocol.
 */
val UPGRADE: HeaderName = HeaderName.from(StandardHeader.Upgrade)

/**
 * Sends a signal to the server expressing the client’s preference for an
 * encrypted and authenticated response.
 */
val UPGRADE_INSECURE_REQUESTS: HeaderName = HeaderName.from(StandardHeader.UpgradeInsecureRequests)

/**
 * Determines how to match future requests with cached responses.
 *
 * The `vary` HTTP response header determines how to match future request
 * headers to decide whether a cached response can be used rather than
 * requesting a fresh one from the origin server. It is used by the server
 * to indicate which headers it used when selecting a representation of a
 * resource in a content negotiation algorithm.
 *
 * The `vary` header should be set on a 304 Not Modified response exactly
 * like it would have been set on an equivalent 200 OK response.
 */
val VARY: HeaderName = HeaderName.from(StandardHeader.Vary)

/**
 * Added by proxies to track routing.
 *
 * The `via` general header is added by proxies, both forward and reverse
 * proxies, and can appear in the request headers and the response headers.
 * It is used for tracking message forwards, avoiding request loops, and
 * identifying the protocol capabilities of senders along the
 * request/response chain.
 */
val VIA: HeaderName = HeaderName.from(StandardHeader.Via)

/**
 * General HTTP header contains information about possible problems with
 * the status of the message.
 *
 * More than one `warning` header may appear in a response. Warning header
 * fields can in general be applied to any message, however some warn-codes
 * are specific to caches and can only be applied to response messages.
 */
val WARNING: HeaderName = HeaderName.from(StandardHeader.Warning)

/**
 * Defines the authentication method that should be used to gain access to
 * a resource.
 */
val WWW_AUTHENTICATE: HeaderName = HeaderName.from(StandardHeader.WwwAuthenticate)

/**
 * Marker used by the server to indicate that the MIME types advertised in
 * the `content-type` headers should not be changed and be followed.
 *
 * This allows to opt-out of MIME type sniffing, or, in other words, it is
 * a way to say that the webmasters knew what they were doing.
 *
 * This header was introduced by Microsoft in IE 8 as a way for webmasters
 * to block content sniffing that was happening and could transform
 * non-executable MIME types into executable MIME types. Since then, other
 * browsers have introduced it, even if their MIME sniffing algorithms were
 * less aggressive.
 *
 * Site security testers usually expect this header to be set.
 */
val X_CONTENT_TYPE_OPTIONS: HeaderName = HeaderName.from(StandardHeader.XContentTypeOptions)

/**
 * Controls DNS prefetching.
 *
 * The `x-dns-prefetch-control` HTTP response header controls DNS
 * prefetching, a feature by which browsers proactively perform domain name
 * resolution on both links that the user may choose to follow as well as
 * URLs for items referenced by the document, including images, CSS,
 * JavaScript, and so forth.
 *
 * This prefetching is performed in the background, so that the DNS is
 * likely to have been resolved by the time the referenced items are
 * needed. This reduces latency when the user clicks a link.
 */
val X_DNS_PREFETCH_CONTROL: HeaderName = HeaderName.from(StandardHeader.XDnsPrefetchControl)

/**
 * Indicates whether or not a browser should be allowed to render a page in
 * a frame.
 *
 * Sites can use this to avoid clickjacking attacks, by ensuring that their
 * content is not embedded into other sites.
 *
 * The added security is only provided if the user accessing the document
 * is using a browser supporting `x-frame-options`.
 */
val X_FRAME_OPTIONS: HeaderName = HeaderName.from(StandardHeader.XFrameOptions)

/**
 * Stop pages from loading when an XSS attack is detected.
 *
 * The HTTP X-XSS-Protection response header is a feature of Internet
 * Explorer, Chrome and Safari that stops pages from loading when they
 * detect reflected cross-site scripting (XSS) attacks. Although these
 * protections are largely unnecessary in modern browsers when sites
 * implement a strong Content-Security-Policy that disables the use of
 * inline JavaScript, they can still provide protections
 * for users of older web browsers that don't yet support CSP.
 */
val X_XSS_PROTECTION: HeaderName = HeaderName.from(StandardHeader.XXssProtection)

private val headerChars =
    byteArrayOf(
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        33,
        0,
        35,
        36,
        37,
        38,
        39,
        0,
        0,
        42,
        43,
        0,
        45,
        46,
        0,
        48,
        49,
        50,
        51,
        52,
        53,
        54,
        55,
        56,
        57,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        97,
        98,
        99,
        100,
        101,
        102,
        103,
        104,
        105,
        106,
        107,
        108,
        109,
        110,
        111,
        112,
        113,
        114,
        115,
        116,
        117,
        118,
        119,
        120,
        121,
        122,
        0,
        0,
        0,
        94,
        95,
        96,
        97,
        98,
        99,
        100,
        101,
        102,
        103,
        104,
        105,
        106,
        107,
        108,
        109,
        110,
        111,
        112,
        113,
        114,
        115,
        116,
        117,
        118,
        119,
        120,
        121,
        122,
        0,
        124,
        0,
        126,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
    )

private val headerCharsH2 =
    byteArrayOf(
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        33,
        34,
        35,
        36,
        37,
        38,
        39,
        0,
        0,
        42,
        43,
        0,
        45,
        46,
        0,
        48,
        49,
        50,
        51,
        52,
        53,
        54,
        55,
        56,
        57,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        94,
        95,
        96,
        97,
        98,
        99,
        100,
        101,
        102,
        103,
        104,
        105,
        106,
        107,
        108,
        109,
        110,
        111,
        112,
        113,
        114,
        115,
        116,
        117,
        118,
        119,
        120,
        121,
        122,
        0,
        124,
        0,
        126,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
    )

private fun parseHdr(data: ByteArray, table: ByteArray): HdrName? =
    when (data.size) {
        0 -> null
        in 1..scratchBufferSize -> {
            val name = ByteArray(data.size) { table[data[it].toInt() and 255] }
            val standard = StandardHeader.fromBytes(name)
            if (standard != null) {
                HdrName.from(standard)
            } else if (name.any { it == 0.toByte() }) {
                null
            } else {
                HdrName.custom(name, true)
            }
        }
        in (scratchBufferSize + 1)..maxHeaderNameLength -> HdrName.custom(data, false)
        else -> null
    }

private fun eqIgnoreAsciiCase(lower: ByteArray, other: ByteArray): Boolean =
    lower.size == other.size && lower.indices.all { lower[it] == headerChars[other[it].toInt() and 255] }

private const val scratchBufferSize = 64
