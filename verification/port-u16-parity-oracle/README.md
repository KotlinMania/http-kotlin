# HTTP URI port unsigned parsing parity

Upstream `tmp/http/src/uri/port.rs:38-44` delegates to Rust `str::parse::<u16>()`, wraps the numeric value with the original representation, and maps any parse error to InvalidPort. Its constructor is crate-private. The oracle directly executes that exact standard parser across the full u16 domain and hostile/valid syntax, and separately executes unchanged upstream HTTP Authority::port/port_u16 through public APIs. It does not claim direct external invocation of the private Port constructor.

Nine matching groups cover bounds, optional leading plus, arbitrarily long leading zeros (through 1,000 zeros), negative forms including -0, 160 Unicode-digit combinations, invalid syntax, overflow, every 0..65535 value, and ten Authority consumer cases. Kotlin additionally checks the precise InvalidUri/ErrorKind.InvalidPort/message binding for direct failures, preserves input representation, and checks numeric display. Existing tests/assertions are unchanged; the entire new test source is unchanged from the pre-fix red run.

Actual Rust passed all nine groups before the repair. Kotlin's baseline compiled and ran nine groups, failing negative-zero, Unicode-digit and Authority-consumer groups; six controls passed. Each failed group stops at its first assertion, so later hostile cases are established by the passing final run rather than claimed as individually executed red. Original red XML/source/hash are retained in build/rust-kotlin-port-u16-parity.

One bounded original-file translation to PortReview.kt exited 0. Exact input/output/CLI hashes are recorded. Emitted code retains parse<UShort>() delegation (line 75); this is structural comparison, not compiled generated code. The recorded CLI binary is not attested as the current generator build. No generator edit, rebuild investigation or competing prompt occurred. The consumer adaptation adds only an ASCII/sign precondition before existing Kotlin signed parsing and u16 range validation; it does not implement a replacement numeric parser or introduce another type owner.

The guard rejects minus signs (including negative zero) and non-ASCII digits, permits one leading +, and lets the existing parser/range check reject empty strings, bare plus and overflow. The source's u16 grammar accepts plus and leading zeros; the prior Kotlin no-sign documentation is corrected accordingly. Original text, display, equality/hash, Int storage, public signatures and error mapping remain unchanged.

Source-level local impact: Authority.port delegates to Port.fromStr; Uri.port delegates through Authority. The oracle/Kotlin regressions exercise Authority directly, not a broad downstream build. HTTP has 16 direct reverse-dependent repositories, its worktree was clean at 5235ba5, and no pre-existing dirty work is included. All dependency pins and sibling owners remain untouched.

Limits: String ownership/borrowing and Rust's generic representation are not modeled; Kotlin Port remains non-generic with Int storage for u16. Formatting flags, other targets and downstream binary compatibility are not newly verified. Unicode invalid-input assertions are about actual Rust strings, not Kotlin-only malformed UTF-16. Generator output was inspected rather than compiled. Preserve prior Serde checkpoints including 9ae88dd and its recorded-binary-only generator caveat; this repair changes HTTP only.

Commands:

- `cargo test --offline --manifest-path verification/port-u16-parity-oracle/Cargo.toml --target-dir build/rust-kotlin-port-u16-parity/rust-target`
- `./gradlew jvmTest --tests '*PortU16ParityTest*' --console=plain` (baseline)
- `./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain` (final source)
