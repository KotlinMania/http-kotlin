# Complete URI shared-input API coverage

Actual upstream `tmp/http/src/uri/mod.rs:280-290` exposes Uri::from_maybe_shared<T>. Bytes inputs enter the existing shared-buffer parser; other AsRef<[u8]> inputs use the existing byte-copy conversion. Kotlin omitted this public API. Three additive overloads now use canonical Bytes, ByteArray and String, delegating to existing complete-URI parsing without new validation, error, normalization or type-owner machinery.

The Bytes overload clones the canonical handle at Kotlin's non-consuming boundary. Rust consumes its input, whereas Kotlin callers retain a usable argument; independent cursors are necessary to preserve the represented buffer view. The byte-array/text overloads delegate to existing tryFrom. All original parser bodies and component/port implementations remain byte-preserved, including the 65,534-byte limit checked before fragment truncation. No URI rule is borrowed by assumption from Authority or PathAndQuery; actual complete-URI behavior is tested independently.

Eight groups execute unchanged upstream Uri::from_maybe_shared for Bytes, Vec<u8> and String: URI/origin/asterisk/authority forms; UTF-8 and fragments; invalid complete-URI syntax; invalid UTF-8; byte-length boundaries; shared-handle independence; copied-input independence; and parts round trip. Boundary cases distinguish UTF-8 bytes from UTF-16 units and reject overlong fragment-bearing input even when its retained path would be short. Shared/parts tests preserve example.org:+80 and numeric port 80. Socket parsing is unrelated and unchanged.

The Rust copied-input mutation control uses borrowed TryFrom<&[u8]>, the same copy path used by non-Bytes generic input. Kotlin mutable ByteArray isolation is an explicit binding test, not a claim that a Rust Vec can be mutated after move. Memory addresses, allocation counts and zero-copy equivalence are not asserted.

The Kotlin baseline failed compilation because fromMaybeShared was unresolved. No runtime red suite ran. Exact compile log and new test source/hash are retained, and all test inputs/assertions remain unchanged through final validation. Existing tests are untouched.

One bounded translation of the original mod.rs exited 0, emitting fromMaybeShared at UriReview.kt:768. Exact source/recorded-binary/output hashes and full invocation are retained. No source was extracted or renamed and no guard bypassed. Emitted scaffolding was inspected rather than compiled or blindly adopted. The binary is not attested as a current generator build; no generator edit, rebuild investigation or owner contact occurred, and the earlier recorded-binary-only caveat remains.

No existing public descriptor changes: only three overloads are added. A bounded source search for exact calls and canonical Uri/wildcard imports is retained in caller-review.txt. It is not a typechecked workspace resolver or broad downstream compatibility proof. No external owner files were edited or downstream builds launched.

The HTTP baseline 0b6d92f was clean, with 16 direct reverse-dependent repositories. Prior Authority 0b6d92f, PathAndQuery bc503c0, Port 6e4d010 and Serde 9ae88dd checkpoints remain separate and preserved. No pins, configurations or remote resources were changed.

Limits: arbitrary custom Rust AsRef<[u8]> input types are not represented by these three concrete overloads. Rust move/lifetime/static-bound/Drop/Send/Sync enforcement, malformed Kotlin UTF-16 behavior, allocation/address sharing, other platforms and downstream source/binary compatibility remain unverified.

Commands:

- `cargo test --offline --manifest-path verification/uri-shared-api-oracle/Cargo.toml --target-dir build/rust-kotlin-uri-shared-api/rust-target`
- `./gradlew jvmTest --tests '*UriSharedApiParityTest*' --console=plain` (compile-only API absence)
- `./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain` (final source)
