# Authority shared-input API coverage

Actual upstream `tmp/http/src/uri/authority.rs:75-85` exposes from_maybe_shared<T>. Bytes inputs use the existing shared-buffer parser; other AsRef<[u8]> inputs use the existing byte-copy conversion. Kotlin omitted this API despite having both parser paths. Three additive overloads now use canonical Bytes, ByteArray and String, delegating to those existing paths without another parser, validation policy or type owner.

The Bytes overload clones the existing canonical handle because Rust consumes its argument whereas Kotlin's caller can still advance its original handle. The byte-array/text overloads delegate to existing tryFrom. Existing complete-authority validation, host extraction, port parsing, representation, errors, constructors, dependency pins and PathAndQuery APIs remain byte-preserved. This authority parser rejects path/query/fragment suffixes and non-ASCII bytes; no behavior is inferred from the separately completed PathAndQuery API.

Seven actual-Rust/Kotlin groups cover ASCII/port forms; userinfo and IPv6; invalid syntax (including empty input, suffix delimiters, whitespace, Unicode host, brackets and hostname percent encoding); invalid UTF-8/NUL bytes; independent shared-handle cursors; copied mutable input; and URI builder use. Rust invokes unchanged upstream Authority::from_maybe_shared for Bytes, Vec<u8> and String. The Rust mutation control uses borrowed TryFrom<&[u8]>, the same byte-copy path used by non-Bytes generic input; Kotlin ByteArray isolation is explicitly a mutable-input binding control, not an attempted mutation after Rust move.

Cases preserve Rust-valid URI plus prefixes, leading zeros, and absent numeric ports for negative zero or overflow. Existing socket parsing is unrelated and untouched. Builder coverage checks http://example.org:+80/x and numeric port 80.

The baseline Kotlin check failed compilation because fromMaybeShared was unresolved. No runtime baseline executed; no assertion failure or red runtime count is claimed. The complete new test source is unchanged from that compile check through final tests; no existing tests or assertions changed.

One bounded original-file translation exited 0, emitting fromMaybeShared at AuthorityReview.kt:261. Exact source/recorded-binary/output hashes are retained. Output scaffolding was inspected, not compiled or blindly adopted. No latest-generator attestation, generator edit, rebuild investigation or owner contact occurred. The earlier generator finding remains recorded-binary-only and current-build unverified.

The API is additive and replaces no existing public descriptor. A bounded workspace source search for the exact call and canonical Authority/wildcard imports is retained in caller-review.txt; external owners were not edited or built. This is source-level evidence, not full downstream compatibility proof.

HTTP has 16 direct reverse-dependent repositories. Worktree baseline bc503c0 was clean; no pre-existing dirty work is included. HTTP 6e4d010 and Serde 9ae88dd remain separate preserved checkpoints. No remote/configuration actions were taken.

Limits: no Rust move/lifetime/static-bound/Drop/Send/Sync enforcement, allocation/address-sharing guarantee, malformed Kotlin UTF-16 policy, other-platform or downstream binary compatibility claim. Independent cursors, copied mutable inputs, actual valid Rust text and raw bytes are the tested scope.

Commands:

- `cargo test --offline --manifest-path verification/authority-shared-api-oracle/Cargo.toml --target-dir build/rust-kotlin-authority-shared-api/rust-target`
- `./gradlew jvmTest --tests '*AuthoritySharedApiParityTest*' --console=plain` (compile-only API absence)
- `./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain` (final source)
