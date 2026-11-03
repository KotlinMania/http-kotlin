# PathAndQuery shared-input API coverage

Upstream `tmp/http/src/uri/path.rs:163-173` exposes from_maybe_shared<T>: Bytes inputs enter the existing shared-buffer parser, while other AsRef<[u8]> inputs use the existing byte-copy conversion. Kotlin had the parser but omitted this public capability. Three overloads now bind the source's input families to canonical Bytes, ByteArray and String, without introducing another parser or type owner.

The Bytes overload clones the existing canonical handle before retaining it. Rust consumes its input handle; Kotlin callers can still access their argument, so an independent cursor handle is necessary. This preserves the represented buffer-view behavior while not claiming Rust move, borrow, Drop or allocation/address equivalence. The byte-array/text overloads delegate directly to existing tryFrom conversions. Existing parser, validation, fragment removal, UTF-8 handling, errors, other constructors and dependency pins are byte-preserved.

The actual unchanged upstream HTTP crate is tested through PathAndQuery::from_maybe_shared using Bytes, Vec<u8> and String. Seven groups cover text/empty/query-relative forms; UTF-8 and fragments; invalid ASCII and UTF-8; shared-handle independence; copied-input independence; and URI builder use with example.org:+80. The Rust copied-input mutation control invokes borrowed TryFrom<&[u8]>, the same copy path used by the non-Bytes generic branch; it does not pretend to mutate a Rust Vec after moving it. Kotlin independently checks mutable ByteArray isolation. Builder coverage preserves the previous Rust-valid URI-port plus behavior; socket parsing is unrelated and unchanged.

Baseline Kotlin failed compilation because fromMaybeShared was unresolved. No runtime baseline suite ran or failed assertions; the compile log and exact test source are retained. One chain-layout correction before production, then one additional line-break correction after the first runtime pass, are recorded. Inputs, expectations and assertions are unchanged. The first full runtime receipts and formatting failure are preserved. Final checks ran against the corrected final test source.

A fresh bounded original-file translation exited 0 and emitted fromMaybeShared at PathReview.kt:336. Its generic downcast scaffolding was inspected, not copied or compiled. Invocation and source/recorded-binary/output hashes are retained. The binary is not attested as the latest generator build; there was no generator edit, rebuild investigation or owner contact. Existing generator caveats remain unchanged.

This is an additive API: existing public signatures were not replaced. A bounded workspace source search found only the new test calls to the canonical HTTP PathAndQuery.fromMaybeShared, including canonical import/alias searches; ramahttp's independently owned PathAndQuery is unrelated. No external caller edits or downstream builds were performed, so source/binary compatibility outside this checkout is not established.

HTTP has 16 direct reverse dependents. The worktree was clean at 6e4d010; no pre-existing dirty work is included. Log-kotlin's concurrent dirty conversion work was inspected read-only and left untouched. Prior Serde 9ae88dd and its historical generator evidence remain separate.

Limits: no Rust lifetime/static-bound or Send/Sync enforcement, allocation/pointer-sharing equivalence, malformed Kotlin UTF-16 policy, other-platform or downstream compatibility claim. Invalid UTF-8 cases use real byte inputs representable in Rust. Bytes refcount/GC destruction behavior is outside this bounded API repair.

Commands:

- `cargo test --offline --manifest-path verification/path-shared-api-oracle/Cargo.toml --target-dir build/rust-kotlin-path-shared-api/rust-target`
- `./gradlew jvmTest --tests '*PathSharedApiParityTest*' --console=plain` (API-absence compile check)
- `./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain` (final source)
