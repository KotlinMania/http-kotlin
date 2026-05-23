# http-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Fhttp--kotlin-blue.svg)](https://github.com/KotlinMania/http-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/http-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/http-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/http-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/http-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`hyperium/http`](https://github.com/hyperium/http).

**Original Project:** This port is based on [`hyperium/http`](https://github.com/hyperium/http). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

The upstream README and license text are treated as upstream-authored source documents. This repository adds Kotlin-port wrapper sections, absolute-link edits, and port-specific notices while keeping upstream authorship attached to the original text.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `hyperium/http`

> The text below is reproduced and lightly edited from [`https://github.com/hyperium/http`](https://github.com/hyperium/http). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs so they continue to resolve from this repository.

## HTTP

A general purpose library of common HTTP types

[![CI](https://github.com/hyperium/http/workflows/CI/badge.svg)](https://github.com/hyperium/http/actions?query=workflow%3ACI)
[![Crates.io](https://img.shields.io/crates/v/http.svg)](https://crates.io/crates/http)
[![Documentation](https://docs.rs/http/badge.svg)][dox]

More information about this crate can be found in the [crate
documentation][dox].

[dox]: https://docs.rs/http

## Usage

To use `http`, first add this to your `Cargo.toml`:

```toml
[dependencies]
http = "1.0"
```

Next, add this to your crate:

```rust
use http::{Request, Response};

fn main() {
    // ...
}
```

## Examples

Create an HTTP request:

```rust
use http::Request;

fn main() {
    let request = Request::builder()
      .uri("https://www.rust-lang.org/")
      .header("User-Agent", "awesome/1.0")
      .body(())
      .unwrap();
}
```

Create an HTTP response:

```rust
use http::{Response, StatusCode};

fn main() {
    let response = Response::builder()
      .status(StatusCode::MOVED_PERMANENTLY)
      .header("Location", "https://www.rust-lang.org/install.html")
      .body(())
      .unwrap();
}
```

# Supported Rust Versions

This project follows the [hyper's MSRV _policy_][msrv], though it can be lower, and is currently set to `1.57`.

[msrv]: https://hyper.rs/contrib/msrv/

# License

Licensed under either of

- Apache License, Version 2.0 ([LICENSE-APACHE](https://github.com/hyperium/http/blob/HEAD/LICENSE-APACHE) or https://apache.org/licenses/LICENSE-2.0)
- MIT license ([LICENSE-MIT](https://github.com/hyperium/http/blob/HEAD/LICENSE-MIT) or https://opensource.org/licenses/MIT)

# Contribution

Unless you explicitly state otherwise, any contribution intentionally submitted
for inclusion in the work by you, as defined in the Apache-2.0 license, shall be
dual licensed as above, without any additional terms or conditions.

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:http-kotlin:0.1.1")
}
```

### Maintainer

Sydney Renee <sydney@solace.ofharmony.ai> (GitHub: [@sydneyrenee](https://github.com/sydneyrenee)) maintains this Kotlin port. Sydney Renee is the founder of The Solace Project.
### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same dual Apache-2.0 OR MIT license as the upstream [`hyperium/http`](https://github.com/hyperium/http). See [LICENSE-APACHE](LICENSE-APACHE), [LICENSE-MIT](LICENSE-MIT), and [NOTICE](NOTICE) for the full text and Kotlin port notice.

Original work copyrighted by the http authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.
Byline: Sydney Renee <sydney@solace.ofharmony.ai>, founder of The Solace Project.

### Acknowledgments

Thanks to the [`hyperium/http`](https://github.com/hyperium/http) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
