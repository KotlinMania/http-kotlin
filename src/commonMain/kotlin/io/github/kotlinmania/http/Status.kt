// port-lint: source status.rs
package io.github.kotlinmania.http

/**
 * HTTP status codes.
 *
 * This module contains HTTP-status code related structs and errors. The main
 * type in this module is `StatusCode` which is not intended to be used through
 * this module but rather the `StatusCode` type.
 *
 * # Examples
 *
 * ```
 * check(StatusCode.fromU16(200).getOrThrow() == StatusCode.OK)
 * check(StatusCode.NOT_FOUND.asU16() == 404)
 * check(StatusCode.OK.isSuccess())
 * ```
 */

/**
 * An HTTP status code (`status-code` in RFC 9110 et al.).
 *
 * Constants are provided for known status codes, including those in the IANA
 * [HTTP Status Code Registry](
 * https://www.iana.org/assignments/http-status-codes/http-status-codes.xhtml).
 *
 * Status code values in the range 100-999 (inclusive) are supported by this
 * type. Values in the range 100-599 are semantically classified by the most
 * significant digit. See `StatusCode.isSuccess()`, etc. Values above 599
 * are unclassified but allowed for legacy compatibility, though their use is
 * discouraged. Applications may interpret such values as protocol errors.
 *
 * # Examples
 *
 * ```
 * val ok = StatusCode.fromU16(200).getOrThrow()
 * check(ok == StatusCode.OK)
 * check(StatusCode.NOT_FOUND.asU16() == 404)
 * check(StatusCode.OK.isSuccess())
 * ```
 */
class StatusCode private constructor(
    private val value: Int,
) : Comparable<StatusCode> {
    companion object {
        /** Converts an integer to a status code. */
        fun fromU16(src: Int): Result<StatusCode> =
            if (src in 100..999) {
                Result.success(StatusCode(src))
            } else {
                Result.failure(InvalidStatusCode.new())
            }

        /** Converts a byte array to a status code. */
        fun fromBytes(src: ByteArray): Result<StatusCode> {
            if (src.size != 3) {
                return Result.failure(InvalidStatusCode.new())
            }

            val a = decimalValue(src[0])
            val b = decimalValue(src[1])
            val c = decimalValue(src[2])

            if (a !in 1..9 || b !in 0..9 || c !in 0..9) {
                return Result.failure(InvalidStatusCode.new())
            }

            val status = (a * 100) + (b * 10) + c
            return Result.success(StatusCode(status))
        }

        fun from(status: StatusCode): StatusCode = status

        fun fromStr(src: String): Result<StatusCode> = parse(src)

        fun parse(src: String): Result<StatusCode> {
            return fromBytes(src.encodeToByteArray())
        }

        fun tryFrom(src: String): Result<StatusCode> = fromStr(src)

        fun tryFrom(src: Int): Result<StatusCode> {
            return fromU16(src)
        }

        /** 100 Continue
         * [[RFC9110, Section 15.2.1](https://datatracker.ietf.org/doc/html/rfc9110#section-15.2.1)]
         */
        val CONTINUE: StatusCode = StatusCode(100)

        /** 101 Switching Protocols
         * [[RFC9110, Section 15.2.2](https://datatracker.ietf.org/doc/html/rfc9110#section-15.2.2)]
         */
        val SWITCHING_PROTOCOLS: StatusCode = StatusCode(101)

        /** 102 Processing
         * [[RFC2518, Section 10.1](https://datatracker.ietf.org/doc/html/rfc2518#section-10.1)]
         */
        val PROCESSING: StatusCode = StatusCode(102)

        /** 103 Early Hints
         * [[RFC8297, Section 2](https://datatracker.ietf.org/doc/html/rfc8297#section-2)]
         */
        val EARLY_HINTS: StatusCode = StatusCode(103)

        /** 200 OK
         * [[RFC9110, Section 15.3.1](https://datatracker.ietf.org/doc/html/rfc9110#section-15.3.1)]
         */
        val OK: StatusCode = StatusCode(200)

        /** 201 Created
         * [[RFC9110, Section 15.3.2](https://datatracker.ietf.org/doc/html/rfc9110#section-15.3.2)]
         */
        val CREATED: StatusCode = StatusCode(201)

        /** 202 Accepted
         * [[RFC9110, Section 15.3.3](https://datatracker.ietf.org/doc/html/rfc9110#section-15.3.3)]
         */
        val ACCEPTED: StatusCode = StatusCode(202)

        /** 203 Non-Authoritative Information
         * [[RFC9110, Section 15.3.4](https://datatracker.ietf.org/doc/html/rfc9110#section-15.3.4)]
         */
        val NON_AUTHORITATIVE_INFORMATION: StatusCode = StatusCode(203)

        /** 204 No Content
         * [[RFC9110, Section 15.3.5](https://datatracker.ietf.org/doc/html/rfc9110#section-15.3.5)]
         */
        val NO_CONTENT: StatusCode = StatusCode(204)

        /** 205 Reset Content
         * [[RFC9110, Section 15.3.6](https://datatracker.ietf.org/doc/html/rfc9110#section-15.3.6)]
         */
        val RESET_CONTENT: StatusCode = StatusCode(205)

        /** 206 Partial Content
         * [[RFC9110, Section 15.3.7](https://datatracker.ietf.org/doc/html/rfc9110#section-15.3.7)]
         */
        val PARTIAL_CONTENT: StatusCode = StatusCode(206)

        /** 207 Multi-Status
         * [[RFC4918, Section 11.1](https://datatracker.ietf.org/doc/html/rfc4918#section-11.1)]
         */
        val MULTI_STATUS: StatusCode = StatusCode(207)

        /** 208 Already Reported
         * [[RFC5842, Section 7.1](https://datatracker.ietf.org/doc/html/rfc5842#section-7.1)]
         */
        val ALREADY_REPORTED: StatusCode = StatusCode(208)

        /** 226 IM Used
         * [[RFC3229, Section 10.4.1](https://datatracker.ietf.org/doc/html/rfc3229#section-10.4.1)]
         */
        val IM_USED: StatusCode = StatusCode(226)

        /** 300 Multiple Choices
         * [[RFC9110, Section 15.4.1](https://datatracker.ietf.org/doc/html/rfc9110#section-15.4.1)]
         */
        val MULTIPLE_CHOICES: StatusCode = StatusCode(300)

        /** 301 Moved Permanently
         * [[RFC9110, Section 15.4.2](https://datatracker.ietf.org/doc/html/rfc9110#section-15.4.2)]
         */
        val MOVED_PERMANENTLY: StatusCode = StatusCode(301)

        /** 302 Found
         * [[RFC9110, Section 15.4.3](https://datatracker.ietf.org/doc/html/rfc9110#section-15.4.3)]
         */
        val FOUND: StatusCode = StatusCode(302)

        /** 303 See Other
         * [[RFC9110, Section 15.4.4](https://datatracker.ietf.org/doc/html/rfc9110#section-15.4.4)]
         */
        val SEE_OTHER: StatusCode = StatusCode(303)

        /** 304 Not Modified
         * [[RFC9110, Section 15.4.5](https://datatracker.ietf.org/doc/html/rfc9110#section-15.4.5)]
         */
        val NOT_MODIFIED: StatusCode = StatusCode(304)

        /** 305 Use Proxy
         * [[RFC9110, Section 15.4.6](https://datatracker.ietf.org/doc/html/rfc9110#section-15.4.6)]
         */
        val USE_PROXY: StatusCode = StatusCode(305)

        /** 307 Temporary Redirect
         * [[RFC9110, Section 15.4.7](https://datatracker.ietf.org/doc/html/rfc9110#section-15.4.7)]
         */
        val TEMPORARY_REDIRECT: StatusCode = StatusCode(307)

        /** 308 Permanent Redirect
         * [[RFC9110, Section 15.4.8](https://datatracker.ietf.org/doc/html/rfc9110#section-15.4.8)]
         */
        val PERMANENT_REDIRECT: StatusCode = StatusCode(308)

        /** 400 Bad Request
         * [[RFC9110, Section 15.5.1](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.1)]
         */
        val BAD_REQUEST: StatusCode = StatusCode(400)

        /** 401 Unauthorized
         * [[RFC9110, Section 15.5.2](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.2)]
         */
        val UNAUTHORIZED: StatusCode = StatusCode(401)

        /** 402 Payment Required
         * [[RFC9110, Section 15.5.3](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.3)]
         */
        val PAYMENT_REQUIRED: StatusCode = StatusCode(402)

        /** 403 Forbidden
         * [[RFC9110, Section 15.5.4](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.4)]
         */
        val FORBIDDEN: StatusCode = StatusCode(403)

        /** 404 Not Found
         * [[RFC9110, Section 15.5.5](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.5)]
         */
        val NOT_FOUND: StatusCode = StatusCode(404)

        /** 405 Method Not Allowed
         * [[RFC9110, Section 15.5.6](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.6)]
         */
        val METHOD_NOT_ALLOWED: StatusCode = StatusCode(405)

        /** 406 Not Acceptable
         * [[RFC9110, Section 15.5.7](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.7)]
         */
        val NOT_ACCEPTABLE: StatusCode = StatusCode(406)

        /** 407 Proxy Authentication Required
         * [[RFC9110, Section 15.5.8](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.8)]
         */
        val PROXY_AUTHENTICATION_REQUIRED: StatusCode = StatusCode(407)

        /** 408 Request Timeout
         * [[RFC9110, Section 15.5.9](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.9)]
         */
        val REQUEST_TIMEOUT: StatusCode = StatusCode(408)

        /** 409 Conflict
         * [[RFC9110, Section 15.5.10](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.10)]
         */
        val CONFLICT: StatusCode = StatusCode(409)

        /** 410 Gone
         * [[RFC9110, Section 15.5.11](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.11)]
         */
        val GONE: StatusCode = StatusCode(410)

        /** 411 Length Required
         * [[RFC9110, Section 15.5.12](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.12)]
         */
        val LENGTH_REQUIRED: StatusCode = StatusCode(411)

        /** 412 Precondition Failed
         * [[RFC9110, Section 15.5.13](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.13)]
         */
        val PRECONDITION_FAILED: StatusCode = StatusCode(412)

        /** 413 Payload Too Large
         * [[RFC9110, Section 15.5.14](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.14)]
         */
        val PAYLOAD_TOO_LARGE: StatusCode = StatusCode(413)

        /** 414 URI Too Long
         * [[RFC9110, Section 15.5.15](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.15)]
         */
        val URI_TOO_LONG: StatusCode = StatusCode(414)

        /** 415 Unsupported Media Type
         * [[RFC9110, Section 15.5.16](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.16)]
         */
        val UNSUPPORTED_MEDIA_TYPE: StatusCode = StatusCode(415)

        /** 416 Range Not Satisfiable
         * [[RFC9110, Section 15.5.17](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.17)]
         */
        val RANGE_NOT_SATISFIABLE: StatusCode = StatusCode(416)

        /** 417 Expectation Failed
         * [[RFC9110, Section 15.5.18](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.18)]
         */
        val EXPECTATION_FAILED: StatusCode = StatusCode(417)

        /** 418 I'm a teapot
         * [curiously not registered by IANA but [RFC2324, Section 2.3.2](https://datatracker.ietf.org/doc/html/rfc2324#section-2.3.2)]
         */
        val IM_A_TEAPOT: StatusCode = StatusCode(418)

        /** 421 Misdirected Request
         * [[RFC9110, Section 15.5.20](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.20)]
         */
        val MISDIRECTED_REQUEST: StatusCode = StatusCode(421)

        /** 422 Unprocessable Entity
         * [[RFC9110, Section 15.5.21](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.21)]
         */
        val UNPROCESSABLE_ENTITY: StatusCode = StatusCode(422)

        /** 423 Locked
         * [[RFC4918, Section 11.3](https://datatracker.ietf.org/doc/html/rfc4918#section-11.3)]
         */
        val LOCKED: StatusCode = StatusCode(423)

        /** 424 Failed Dependency
         * [[RFC4918, Section 11.4](https://tools.ietf.org/html/rfc4918#section-11.4)]
         */
        val FAILED_DEPENDENCY: StatusCode = StatusCode(424)

        /** 425 Too early
         * [[RFC8470, Section 5.2](https://httpwg.org/specs/rfc8470.html#status)]
         */
        val TOO_EARLY: StatusCode = StatusCode(425)

        /** 426 Upgrade Required
         * [[RFC9110, Section 15.5.22](https://datatracker.ietf.org/doc/html/rfc9110#section-15.5.22)]
         */
        val UPGRADE_REQUIRED: StatusCode = StatusCode(426)

        /** 428 Precondition Required
         * [[RFC6585, Section 3](https://datatracker.ietf.org/doc/html/rfc6585#section-3)]
         */
        val PRECONDITION_REQUIRED: StatusCode = StatusCode(428)

        /** 429 Too Many Requests
         * [[RFC6585, Section 4](https://datatracker.ietf.org/doc/html/rfc6585#section-4)]
         */
        val TOO_MANY_REQUESTS: StatusCode = StatusCode(429)

        /** 431 Request Header Fields Too Large
         * [[RFC6585, Section 5](https://datatracker.ietf.org/doc/html/rfc6585#section-5)]
         */
        val REQUEST_HEADER_FIELDS_TOO_LARGE: StatusCode = StatusCode(431)

        /** 451 Unavailable For Legal Reasons
         * [[RFC7725, Section 3](https://tools.ietf.org/html/rfc7725#section-3)]
         */
        val UNAVAILABLE_FOR_LEGAL_REASONS: StatusCode = StatusCode(451)

        /** 500 Internal Server Error
         * [[RFC9110, Section 15.6.1](https://datatracker.ietf.org/doc/html/rfc9110#section-15.6.1)]
         */
        val INTERNAL_SERVER_ERROR: StatusCode = StatusCode(500)

        /** 501 Not Implemented
         * [[RFC9110, Section 15.6.2](https://datatracker.ietf.org/doc/html/rfc9110#section-15.6.2)]
         */
        val NOT_IMPLEMENTED: StatusCode = StatusCode(501)

        /** 502 Bad Gateway
         * [[RFC9110, Section 15.6.3](https://datatracker.ietf.org/doc/html/rfc9110#section-15.6.3)]
         */
        val BAD_GATEWAY: StatusCode = StatusCode(502)

        /** 503 Service Unavailable
         * [[RFC9110, Section 15.6.4](https://datatracker.ietf.org/doc/html/rfc9110#section-15.6.4)]
         */
        val SERVICE_UNAVAILABLE: StatusCode = StatusCode(503)

        /** 504 Gateway Timeout
         * [[RFC9110, Section 15.6.5](https://datatracker.ietf.org/doc/html/rfc9110#section-15.6.5)]
         */
        val GATEWAY_TIMEOUT: StatusCode = StatusCode(504)

        /** 505 HTTP Version Not Supported
         * [[RFC9110, Section 15.6.6](https://datatracker.ietf.org/doc/html/rfc9110#section-15.6.6)]
         */
        val HTTP_VERSION_NOT_SUPPORTED: StatusCode = StatusCode(505)

        /** 506 Variant Also Negotiates
         * [[RFC2295, Section 8.1](https://datatracker.ietf.org/doc/html/rfc2295#section-8.1)]
         */
        val VARIANT_ALSO_NEGOTIATES: StatusCode = StatusCode(506)

        /** 507 Insufficient Storage
         * [[RFC4918, Section 11.5](https://datatracker.ietf.org/doc/html/rfc4918#section-11.5)]
         */
        val INSUFFICIENT_STORAGE: StatusCode = StatusCode(507)

        /** 508 Loop Detected
         * [[RFC5842, Section 7.2](https://datatracker.ietf.org/doc/html/rfc5842#section-7.2)]
         */
        val LOOP_DETECTED: StatusCode = StatusCode(508)

        /** 510 Not Extended
         * [[RFC2774, Section 7](https://datatracker.ietf.org/doc/html/rfc2774#section-7)]
         */
        val NOT_EXTENDED: StatusCode = StatusCode(510)

        /** 511 Network Authentication Required
         * [[RFC6585, Section 6](https://datatracker.ietf.org/doc/html/rfc6585#section-6)]
         */
        val NETWORK_AUTHENTICATION_REQUIRED: StatusCode = StatusCode(511)

        fun default(): StatusCode {
            return OK
        }

        private fun decimalValue(byte: Byte): Int = (byte.toInt() and 0xff) - '0'.code
    }

    /**
     * Returns the integer corresponding to this `StatusCode`.
     *
     * # Note
     *
     * This is the same as the `From<StatusCode>` implementation, but
     * included as an inherent method because that implementation doesn't
     * appear in rustdocs, as well as a way to force the type instead of
     * relying on inference.
     *
     * # Example
     *
     * ```
     * val status = StatusCode.OK
     * check(status.asU16() == 200)
     * ```
     */
    fun asU16(): Int {
        return value
    }

    /**
     * Returns a string representation of the `StatusCode`.
     *
     * The return value only includes a numerical representation of the
     * status code. The canonical reason is not included.
     *
     * # Example
     *
     * ```
     * val status = StatusCode.OK
     * check(status.asStr() == "200")
     * ```
     */
    fun asStr(): String {
        val offset = (value - 100) * 3

        // Invariant: self has checked range [100, 999] and CODE_DIGITS is
        // ASCII-only, of length 900 * 3 = 2700 bytes
        return CODE_DIGITS.substring(offset, offset + 3)
    }

    /**
     * Get the standardised `reason-phrase` for this status code.
     *
     * This is mostly here for servers writing responses, but could potentially have application
     * at other times.
     *
     * The reason phrase is defined as being exclusively for human readers. You should avoid
     * deriving any meaning from it at all costs.
     *
     * Bear in mind also that in HTTP/2.0 and HTTP/3.0 the reason phrase is abolished from
     * transmission, and so this canonical reason phrase really is the only reason phrase you'll
     * find.
     *
     * # Example
     *
     * ```
     * val status = StatusCode.OK
     * check(status.canonicalReason() == "OK")
     * ```
     */
    fun canonicalReason(): String? {
        return canonicalReason(value)
    }

    /** Check if status is within 100-199. */
    fun isInformational(): Boolean {
        return value in 100 until 200
    }

    /** Check if status is within 200-299. */
    fun isSuccess(): Boolean {
        return value in 200 until 300
    }

    /** Check if status is within 300-399. */
    fun isRedirection(): Boolean {
        return value in 300 until 400
    }

    /** Check if status is within 400-499. */
    fun isClientError(): Boolean {
        return value in 400 until 500
    }

    /** Check if status is within 500-599. */
    fun isServerError(): Boolean {
        return value in 500 until 600
    }

    fun eq(other: Int): Boolean {
        return value == other
    }

    /**
     * Debug-formats the status code as the bare numeric code, matching upstream
     * `impl fmt::Debug for StatusCode` in `tmp/http/src/status.rs:205-209` which
     * delegates to `fmt::Debug::fmt(&self.0, f)`.
     */
    fun debugString(): String {
        return value.toString()
    }

    /**
     * Display-formats the status code, *including* the canonical reason, matching
     * upstream `impl fmt::Display for StatusCode` in `tmp/http/src/status.rs:219-228`.
     * Equivalent to [toString].
     */
    fun fmt(): String = toString()

    fun fmt(formatter: StringBuilder): StringBuilder {
        formatter.append(toString())
        return formatter
    }

    /**
     * Formats the status code, *including* the canonical reason.
     *
     * # Example
     *
     * ```
     * check(StatusCode.OK.toString() == "200 OK")
     * ```
     */
    override fun toString(): String = "$value ${canonicalReason() ?: "<unknown status code>"}"

    override fun compareTo(other: StatusCode): Int = value.compareTo(other.value)

    override fun equals(other: Any?): Boolean =
        when (other) {
            is StatusCode -> value == other.value
            else -> false
        }

    override fun hashCode(): Int = value
}

/**
 * A possible error value when converting a `StatusCode` from an integer or string.
 *
 * This error indicates that the supplied input was not a valid number, was less
 * than 100, or was greater than 999.
 */
class InvalidStatusCode private constructor() : IllegalArgumentException("invalid status code") {
    companion object {
        internal fun new(): InvalidStatusCode = InvalidStatusCode()
    }

    /**
     * Debug-formats the error, matching upstream
     * `impl fmt::Debug for InvalidStatusCode` in `tmp/http/src/status.rs:533-539`
     * which prints `f.debug_struct("InvalidStatusCode").finish()`.
     */
    fun debugString(): String = "InvalidStatusCode"

    /**
     * Display-formats the error, matching upstream
     * `impl fmt::Display for InvalidStatusCode` in `tmp/http/src/status.rs:541-545`
     * which writes the literal string `invalid status code`.
     */
    override fun toString(): String = "invalid status code"

    fun fmt(): String = toString()

    fun fmt(formatter: StringBuilder): StringBuilder {
        formatter.append("invalid status code")
        return formatter
    }
}

private fun canonicalReason(num: Int): String? =
    when (num) {
        100 -> "Continue"
        101 -> "Switching Protocols"
        102 -> "Processing"
        103 -> "Early Hints"
        200 -> "OK"
        201 -> "Created"
        202 -> "Accepted"
        203 -> "Non Authoritative Information"
        204 -> "No Content"
        205 -> "Reset Content"
        206 -> "Partial Content"
        207 -> "Multi-Status"
        208 -> "Already Reported"
        226 -> "IM Used"
        300 -> "Multiple Choices"
        301 -> "Moved Permanently"
        302 -> "Found"
        303 -> "See Other"
        304 -> "Not Modified"
        305 -> "Use Proxy"
        307 -> "Temporary Redirect"
        308 -> "Permanent Redirect"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        402 -> "Payment Required"
        403 -> "Forbidden"
        404 -> "Not Found"
        405 -> "Method Not Allowed"
        406 -> "Not Acceptable"
        407 -> "Proxy Authentication Required"
        408 -> "Request Timeout"
        409 -> "Conflict"
        410 -> "Gone"
        411 -> "Length Required"
        412 -> "Precondition Failed"
        413 -> "Payload Too Large"
        414 -> "URI Too Long"
        415 -> "Unsupported Media Type"
        416 -> "Range Not Satisfiable"
        417 -> "Expectation Failed"
        418 -> "I'm a teapot"
        421 -> "Misdirected Request"
        422 -> "Unprocessable Entity"
        423 -> "Locked"
        424 -> "Failed Dependency"
        425 -> "Too Early"
        426 -> "Upgrade Required"
        428 -> "Precondition Required"
        429 -> "Too Many Requests"
        431 -> "Request Header Fields Too Large"
        451 -> "Unavailable For Legal Reasons"
        500 -> "Internal Server Error"
        501 -> "Not Implemented"
        502 -> "Bad Gateway"
        503 -> "Service Unavailable"
        504 -> "Gateway Timeout"
        505 -> "HTTP Version Not Supported"
        506 -> "Variant Also Negotiates"
        507 -> "Insufficient Storage"
        508 -> "Loop Detected"
        510 -> "Not Extended"
        511 -> "Network Authentication Required"
        else -> null
    }

// A string of packed 3-ASCII-digit status code values for the supported range
// of [100, 999] (900 codes, 2700 bytes).
private const val CODE_DIGITS: String =
    "100101102103104105106107108109110111112113114115116117118119" +
        "120121122123124125126127128129130131132133134135136137138139" +
        "140141142143144145146147148149150151152153154155156157158159" +
        "160161162163164165166167168169170171172173174175176177178179" +
        "180181182183184185186187188189190191192193194195196197198199" +
        "200201202203204205206207208209210211212213214215216217218219" +
        "220221222223224225226227228229230231232233234235236237238239" +
        "240241242243244245246247248249250251252253254255256257258259" +
        "260261262263264265266267268269270271272273274275276277278279" +
        "280281282283284285286287288289290291292293294295296297298299" +
        "300301302303304305306307308309310311312313314315316317318319" +
        "320321322323324325326327328329330331332333334335336337338339" +
        "340341342343344345346347348349350351352353354355356357358359" +
        "360361362363364365366367368369370371372373374375376377378379" +
        "380381382383384385386387388389390391392393394395396397398399" +
        "400401402403404405406407408409410411412413414415416417418419" +
        "420421422423424425426427428429430431432433434435436437438439" +
        "440441442443444445446447448449450451452453454455456457458459" +
        "460461462463464465466467468469470471472473474475476477478479" +
        "480481482483484485486487488489490491492493494495496497498499" +
        "500501502503504505506507508509510511512513514515516517518519" +
        "520521522523524525526527528529530531532533534535536537538539" +
        "540541542543544545546547548549550551552553554555556557558559" +
        "560561562563564565566567568569570571572573574575576577578579" +
        "580581582583584585586587588589590591592593594595596597598599" +
        "600601602603604605606607608609610611612613614615616617618619" +
        "620621622623624625626627628629630631632633634635636637638639" +
        "640641642643644645646647648649650651652653654655656657658659" +
        "660661662663664665666667668669670671672673674675676677678679" +
        "680681682683684685686687688689690691692693694695696697698699" +
        "700701702703704705706707708709710711712713714715716717718719" +
        "720721722723724725726727728729730731732733734735736737738739" +
        "740741742743744745746747748749750751752753754755756757758759" +
        "760761762763764765766767768769770771772773774775776777778779" +
        "780781782783784785786787788789790791792793794795796797798799" +
        "800801802803804805806807808809810811812813814815816817818819" +
        "820821822823824825826827828829830831832833834835836837838839" +
        "840841842843844845846847848849850851852853854855856857858859" +
        "860861862863864865866867868869870871872873874875876877878879" +
        "880881882883884885886887888889890891892893894895896897898899" +
        "900901902903904905906907908909910911912913914915916917918919" +
        "920921922923924925926927928929930931932933934935936937938939" +
        "940941942943944945946947948949950951952953954955956957958959" +
        "960961962963964965966967968969970971972973974975976977978979" +
        "980981982983984985986987988989990991992993994995996997998999"
