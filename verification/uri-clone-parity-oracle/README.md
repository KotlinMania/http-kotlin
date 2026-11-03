# URI derived-Clone consumer coverage

Rust derives Clone on Uri (uri/mod.rs:95), Authority (authority.rs:25), PathAndQuery (path.rs:19), Scheme and Scheme2 (scheme.rs:12,17), Protocol (scheme.rs:24), and private ByteStr (byte_str.rs:5). The concrete fields establish the algorithm: clone each field; ByteStr clones canonical Bytes; Scheme copies None/Protocol variants or clones its boxed ByteStr. Kotlin omitted these callable clone APIs.

The repair adds four public clone methods plus one internal ByteStr helper. Uri clones its three existing component fields; Authority clones ByteStr; PathAndQuery clones ByteStr and copies the query offset; Scheme preserves its None/Standard/Other state and clones only the ByteStr payload; ByteStr clones the existing canonical Bytes handle. No parser, formatter, normalization, validation, shared-input constructor, public descriptor replacement, dependency pin or type owner changes. No new generic clone framework or type redesign is introduced.

Seven Rust/Kotlin groups cover standard/custom schemes; authorities with userinfo/IPv6/URI plus ports; empty/relative/UTF-8/query/fragment paths; default, authority-form and ordinary Uri values; shared-input cursor movement; parts round trip; and canonical buffer-handle independence. Actual upstream derived Clone executes for all four public Rust types. Rust checks that clones survive explicit drop(original) in value groups; Kotlin checks values while references remain live and does not claim equivalent GC/destruction timing. Hash assertions compare original/clone within each runtime, not Rust numeric hashes against Kotlin hashCode. Kotlin fresh-wrapper identity assertions are binding controls, not a Rust address-equivalence claim.

The seventh group's scope is explicit: Rust tests actual bytes::Bytes::clone. Kotlin tests internal ByteStr.clone using a retained canonical input handle and cursor movement. Rust ByteStr is crate-private and is not directly invoked externally; its source-derived field clone supplies the relationship, while the four public actual-derived-Clone groups exercise it transitively. This is not a substituted Rust ByteStr implementation. Internal Kotlin mutation remains valid ASCII and is test instrumentation, not a new public mutation API.

Default and authority-form URI cases are essential: their hidden empty scheme/path states must be cloned directly. Reconstructing components through asStr/fromStr could throw or normalize empty data into a slash, changing equality/path/hash. Path empty-data equality and full URI getters are asserted to preserve those states.

The Kotlin baseline failed compilation at missing clone methods on Scheme, Authority, PathAndQuery, Uri and ByteStr. No runtime baseline ran. Exact compile log and test source/hash are retained; existing tests and all new assertions are unchanged.

Prior successful URI/Authority/PathAndQuery translator records were reused only after verifying current original-source and emitted-output hashes. They establish field/type layout; the actual source derive annotations and Rust compiler execution determine Clone behavior. No new translator process, generator edit, build investigation or owner contact occurred, and no generator fix or current-build attestation is claimed.

Public APIs are additive. The bounded canonical type/wildcard/alias import and clone-line review is in caller-review.json. New local clone callers form one owning-checkout dependency chain ending at canonical Bytes.clone. No external callers were edited or built, and text review is not downstream binary compatibility proof.

Baseline HTTP 6a41956 was clean. Prior component/shared-input/port and Serde 9ae88dd checkpoints remain separate; concurrent owners are untouched. No remote/configuration actions occurred.

Limits: Rust moves, borrows, Drop, Send/Sync, allocation counts and address sharing are not equivalently represented or newly verified. Other platforms and downstream compatibility remain unverified. Existing generic-input and Kotlin UTF-16 limits are unchanged.

Commands:

- `cargo test --offline --manifest-path verification/uri-clone-parity-oracle/Cargo.toml --target-dir build/rust-kotlin-uri-clone-parity/rust-target`
- `./gradlew jvmTest --tests '*UriCloneParityTest*' --console=plain` (compile-only missing API)
- `./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain` (final source)
