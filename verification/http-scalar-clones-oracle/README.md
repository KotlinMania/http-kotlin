# StatusCode and Version scalar Clone APIs

Rust derives Clone/Copy on StatusCode(NonZeroU16), status.rs:44, and Version(Http), version.rs:25. Kotlin omitted callable clone methods. The repair adds exactly two direct field copies: StatusCode(value) and Version(http). It does not call constructors that parse/validate input, change formatting/classification/reasons/ordering, add type owners or invent a generic clone framework. All previous runtime bodies and dependency pins remain byte-preserved.

Six groups execute actual upstream derived Clone: every accepted status 100..999 (900 values); classification/reason boundaries through nonstandard 600 and 999; representative status ordering; all five public version constants; every version ordering pair (25 pairs); and repeated clones/default Version. Kotlin additionally checks fresh wrappers as a binding control, not Rust address equivalence. Rust implicit Copy assignment semantics, Clone trait bounds/default clone_from and Kotlin allocation/GC behavior are not modeled by these methods.

Properties compare originals and clones within their runtime. Hash assertions do not compare numeric Rust/Kotlin hash algorithms. Rust Version exposes Debug formatting; Kotlin's existing fmt/toString binding is compared to itself across cloning, not mislabeled as a new Rust Display implementation. Public versions are covered; the private non-exhaustive sentinel is not constructible through the tested public API and is not claimed covered.

Baseline Kotlin failed compilation at missing clone methods on StatusCode and Version. No runtime baseline ran or failed assertions. Exact compile log and raw test source are retained. Before production changes, identical intermediate values were named to avoid long call chains; the three expected values, clone counts, inputs and assertions are unchanged, with exact before/after recorded. Existing tests are untouched.

Two bounded translations of the original status.rs/version.rs exited 0. Exact invocation and source/recorded-binary/output hashes are retained. Emitted field/type structure was inspected, not compiled or used as proof of current generator behavior. The derive annotations and real Rust compiler execution determine the scalar clone behavior. No generator edits, rebuild investigation or owner contact occurred; earlier recorded-binary caveats remain.

The APIs are additive. Bounded canonical type/wildcard/alias imports and clone lines are recorded in caller-review.json. Neither method changes an existing descriptor or calls another public parser. No external owners were edited or downstream builds run, so compatibility outside this owning checkout is unverified.

HTTP baseline a4418aa was clean. Method/URI clone, shared-input and URI plus-port work remain preserved and separate, as does Serde 9ae88dd. No pre-existing dirty work is included, and no remote/configuration changes occurred.

Limits: general Clone/Copy traits, Rust moves/borrows, address/allocation/GC equivalence, other targets and downstream source/binary compatibility remain unverified. This step binds only the represented derived value-clone operation.

Commands:

- `cargo test --offline --manifest-path verification/http-scalar-clones-oracle/Cargo.toml --target-dir build/rust-kotlin-http-scalar-clones/rust-target`
- `./gradlew jvmTest --tests '*ScalarCloneParityTest*' --console=plain` (compile-only API absence)
- `./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain` (final source)
