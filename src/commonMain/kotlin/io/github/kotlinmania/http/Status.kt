// port-lint: source src/status.rs
package io.github.kotlinmania.http

public class InvalidStatusCode private constructor() : IllegalArgumentException("invalid status code") {
    internal companion object {
        fun new(): InvalidStatusCode = InvalidStatusCode()
    }
}

public class StatusCode private constructor(
    private val code: Int,
) : Comparable<StatusCode> {
    public companion object {
        public fun fromU16(src: Int): Result<StatusCode> {
            if (src in 100..999) {
                return Result.success(StatusCode(src))
            }
            return Result.failure(InvalidStatusCode.new())
        }

        public fun fromBytes(src: ByteArray): Result<StatusCode> {
            if (src.size != 3) {
                return Result.failure(InvalidStatusCode.new())
            }

            val a = src[0].toInt() - '0'.code
            val b = src[1].toInt() - '0'.code
            val c = src[2].toInt() - '0'.code

            if (a == 0 || a > 9 || b > 9 || c > 9) {
                return Result.failure(InvalidStatusCode.new())
            }

            val status = (a * 100) + (b * 10) + c
            return Result.success(StatusCode(status))
        }

        public fun fromString(src: String): Result<StatusCode> =
            fromBytes(src.encodeToByteArray())

        public val CONTINUE: StatusCode = StatusCode(100)
        public val SWITCHING_PROTOCOLS: StatusCode = StatusCode(101)
        public val PROCESSING: StatusCode = StatusCode(102)
        public val EARLY_HINTS: StatusCode = StatusCode(103)
        public val OK: StatusCode = StatusCode(200)
        public val CREATED: StatusCode = StatusCode(201)
        public val ACCEPTED: StatusCode = StatusCode(202)
        public val NON_AUTHORITATIVE_INFORMATION: StatusCode = StatusCode(203)
        public val NO_CONTENT: StatusCode = StatusCode(204)
        public val RESET_CONTENT: StatusCode = StatusCode(205)
        public val PARTIAL_CONTENT: StatusCode = StatusCode(206)
        public val MULTI_STATUS: StatusCode = StatusCode(207)
        public val ALREADY_REPORTED: StatusCode = StatusCode(208)
        public val IM_USED: StatusCode = StatusCode(226)
        public val MULTIPLE_CHOICES: StatusCode = StatusCode(300)
        public val MOVED_PERMANENTLY: StatusCode = StatusCode(301)
        public val FOUND: StatusCode = StatusCode(302)
        public val SEE_OTHER: StatusCode = StatusCode(303)
        public val NOT_MODIFIED: StatusCode = StatusCode(304)
        public val USE_PROXY: StatusCode = StatusCode(305)
        public val TEMPORARY_REDIRECT: StatusCode = StatusCode(307)
        public val PERMANENT_REDIRECT: StatusCode = StatusCode(308)
        public val BAD_REQUEST: StatusCode = StatusCode(400)
        public val UNAUTHORIZED: StatusCode = StatusCode(401)
        public val PAYMENT_REQUIRED: StatusCode = StatusCode(402)
        public val FORBIDDEN: StatusCode = StatusCode(403)
        public val NOT_FOUND: StatusCode = StatusCode(404)
        public val METHOD_NOT_ALLOWED: StatusCode = StatusCode(405)
        public val NOT_ACCEPTABLE: StatusCode = StatusCode(406)
        public val PROXY_AUTHENTICATION_REQUIRED: StatusCode = StatusCode(407)
        public val REQUEST_TIMEOUT: StatusCode = StatusCode(408)
        public val CONFLICT: StatusCode = StatusCode(409)
        public val GONE: StatusCode = StatusCode(410)
        public val LENGTH_REQUIRED: StatusCode = StatusCode(411)
        public val PRECONDITION_FAILED: StatusCode = StatusCode(412)
        public val PAYLOAD_TOO_LARGE: StatusCode = StatusCode(413)
        public val URI_TOO_LONG: StatusCode = StatusCode(414)
        public val UNSUPPORTED_MEDIA_TYPE: StatusCode = StatusCode(415)
        public val RANGE_NOT_SATISFIABLE: StatusCode = StatusCode(416)
        public val EXPECTATION_FAILED: StatusCode = StatusCode(417)
        public val IM_A_TEAPOT: StatusCode = StatusCode(418)
        public val MISDIRECTED_REQUEST: StatusCode = StatusCode(421)
        public val UNPROCESSABLE_ENTITY: StatusCode = StatusCode(422)
        public val LOCKED: StatusCode = StatusCode(423)
        public val FAILED_DEPENDENCY: StatusCode = StatusCode(424)
        public val TOO_EARLY: StatusCode = StatusCode(425)
        public val UPGRADE_REQUIRED: StatusCode = StatusCode(426)
        public val PRECONDITION_REQUIRED: StatusCode = StatusCode(428)
        public val TOO_MANY_REQUESTS: StatusCode = StatusCode(429)
        public val REQUEST_HEADER_FIELDS_TOO_LARGE: StatusCode = StatusCode(431)
        public val UNAVAILABLE_FOR_LEGAL_REASONS: StatusCode = StatusCode(451)
        public val INTERNAL_SERVER_ERROR: StatusCode = StatusCode(500)
        public val NOT_IMPLEMENTED: StatusCode = StatusCode(501)
        public val BAD_GATEWAY: StatusCode = StatusCode(502)
        public val SERVICE_UNAVAILABLE: StatusCode = StatusCode(503)
        public val GATEWAY_TIMEOUT: StatusCode = StatusCode(504)
        public val HTTP_VERSION_NOT_SUPPORTED: StatusCode = StatusCode(505)
        public val VARIANT_ALSO_NEGOTIATES: StatusCode = StatusCode(506)
        public val INSUFFICIENT_STORAGE: StatusCode = StatusCode(507)
        public val LOOP_DETECTED: StatusCode = StatusCode(508)
        public val NOT_EXTENDED: StatusCode = StatusCode(510)
        public val NETWORK_AUTHENTICATION_REQUIRED: StatusCode = StatusCode(511)
    }

    public fun asU16(): Int = code

    public fun asStr(): String {
        val offset = (code - 100) * 3
        return CODE_DIGITS.substring(offset, offset + 3)
    }

    public fun canonicalReason(): String? = canonicalReason(code)

    public fun isInformational(): Boolean = code in 100..<200

    public fun isSuccess(): Boolean = code in 200..<300

    public fun isRedirection(): Boolean = code in 300..<400

    public fun isClientError(): Boolean = code in 400..<500

    public fun isServerError(): Boolean = code in 500..<600

    override fun compareTo(other: StatusCode): Int = code.compareTo(other.code)

    override fun equals(other: Any?): Boolean =
        other is StatusCode && code == other.code

    override fun hashCode(): Int = code

    override fun toString(): String =
        "$code ${canonicalReason() ?: "<unknown status code>"}"
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
