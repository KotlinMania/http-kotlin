# HTTP Method derived-Clone coverage

Actual upstream method.rs derives Clone on Method (line 44), Inner (line 52), InlineExtension ([u8; 15], u8; line 311) and AllocatedExtension (Box<[u8]>; line 315). The concrete operation is field-wise cloning: copy unit variants, copy the complete inline array and length, or clone the boxed byte slice. Kotlin omitted the callable Method clone API.

One public Method.clone plus two private storage helpers now follow that operation directly. Standard immutable Inner objects are retained inside a fresh Method wrapper. Inline data.copyOf copies all fixed storage and preserves len; allocated data.copyOf copies the boxed-slice representation. Nothing reparses or normalizes the method. All previous parser/table/validation, case sensitivity, safe/idempotent classification, formatting, equality/hash and other APIs remain byte-preserved. There is no new generic Clone framework or duplicate type owner.

Seven groups execute actual upstream Method::clone: nine standard methods; short extensions including lowercase get and the 15-byte boundary; allocated lengths 16/17/64/256; case-sensitive distinctions; every byte 0..255 against the RFC token alphabet; original input mutation and clone lifetime; and repeated clones at lengths 1/15/16/32. Kotlin additionally asserts a fresh wrapper. Rust explicitly drops originals before inspecting clones in value/input groups; Kotlin does not force GC and does not claim equivalent destruction timing. Hash consistency is original-versus-clone within each runtime, not numeric Rust/Kotlin hash equivalence. Public observable tests do not independently prove allocation counts, physical storage identity or private array layout; source-derived copy operations provide that implementation evidence.

Baseline Kotlin failed compilation at the missing Method.clone receiver method. No runtime baseline ran or failed assertions. Exact compile log and new test source/hash are retained; existing tests and all new assertions remain unchanged through final checks.

One bounded original method.rs translation exited 0; exact invocation and input/recorded-binary/output hashes are retained. Output was inspected for representation/type relationships, not compiled or adopted as an unverified implementation. Actual source derive annotations and Rust compiler execution determine Clone behavior. No current-generator attestation, generator edit, build investigation or owner contact occurred.

The API is additive. A bounded canonical Method/wildcard/alias import and clone-line review is retained in caller-review.json. New calls stay in the same owning Method source file and private helpers. External owners were not edited or built. This is not downstream source/binary compatibility proof.

HTTP baseline 5bacb5a was clean. Previous URI shared-input/clone, URI plus-port and Serde checkpoints are preserved and separate. No dependency pins, configurations or remote resources changed.

Limits: Rust moves/borrows/Drop/Send/Sync, physical allocation/address behavior, generic Clone trait bounds and default clone_from, other platforms and downstream compatibility are not newly represented or verified. Existing Kotlin storage models are retained; no broad type redesign is claimed.

Commands:

- `cargo test --offline --manifest-path verification/method-clone-parity-oracle/Cargo.toml --target-dir build/rust-kotlin-method-clone-parity/rust-target`
- `./gradlew jvmTest --tests '*MethodCloneParityTest*' --console=plain` (compile-only API absence)
- `./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain` (final source)
