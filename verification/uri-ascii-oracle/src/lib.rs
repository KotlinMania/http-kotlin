// Run cargo test --manifest-path verification/uri-ascii-oracle/Cargo.toml --offline --locked
// The path dependency compiles unchanged upstream HTTP code.
#[cfg(test)]
mod tests {
    use http::uri::{Authority, Scheme};
    use http::{Uri, HeaderName};
    use std::cmp::Ordering;

    #[test]
    fn scheme_comparison_does_not_fold_unicode() {
        for (ascii, unicode) in [("k", "K"), ("s", "ſ"), ("i", "ı")] {
            let scheme: Scheme = ascii.parse().unwrap();
            assert!(!scheme.eq(unicode));
            assert!(scheme.eq(ascii.to_ascii_uppercase().as_str()));
        }
    }
    #[test]
    fn authority_comparison_does_not_fold_unicode() {
        for (ascii, unicode) in [("k.example", "K.example"), ("s.example", "ſ.example"), ("i.example", "ı.example")] {
            let authority: Authority = ascii.parse().unwrap();
            assert_ne!(authority, unicode);
            assert_eq!(authority, ascii.to_ascii_uppercase());
        }
    }
    #[test]
    fn uri_component_comparison_does_not_fold_unicode() {
        let uri: Uri = "k://k.example/Path?Q=1".parse().unwrap();
        assert_ne!(uri, "K://k.example/Path?Q=1");
        assert_ne!(uri, "k://K.example/Path?Q=1");
        assert_eq!(uri, "K://K.EXAMPLE/Path?Q=1");
    }
    #[test]
    fn authority_order_maps_to_lowercase_before_comparing() {
        for (left, right) in [("[::1]", "a.example"), ("_", "a"), ("z", "~")] {
            let left: Authority = left.parse().unwrap();
            let right_owned: Authority = right.parse().unwrap();
            assert_eq!(left.partial_cmp(&right_owned), Some(Ordering::Less));
            assert_eq!(left.partial_cmp(right), Some(Ordering::Less));
        }
    }
    #[test]
    fn authority_order_uses_unsigned_bytes_for_unicode_strings() {
        let authority: Authority = "k.example".parse().unwrap();
        assert_eq!(authority.partial_cmp("K.example"), Some(Ordering::Less));
        assert_eq!(authority.partial_cmp("K.EXAMPLE"), Some(Ordering::Equal));
    }
    #[test]
    fn path_and_query_case_and_fragment_semantics_stay_distinct() {
        let uri: Uri = "http://example/Path?Q=1".parse().unwrap();
        assert_ne!(uri, "HTTP://EXAMPLE/path?Q=1");
        assert_ne!(uri, "HTTP://EXAMPLE/Path?q=1");
        assert_eq!(uri, "HTTP://EXAMPLE/Path?Q=1#K");
    }
    #[test]
    fn unicode_scheme_and_authority_input_still_fails_parsing() {
        for text in ["K", "ſ", "ı"] {
            assert!(text.parse::<Scheme>().is_err());
            assert!(text.parse::<Authority>().is_err());
        }
    }
    #[test]
    fn header_names_keep_ascii_only_comparison() {
        let header = HeaderName::from_static("key");
        assert_eq!(header, "KEY");
        assert_ne!(header, "Key");
    }
}

#[cfg(test)]
mod path_tests {
    use http::uri::PathAndQuery;
    use http::Uri;
    use std::cmp::Ordering;

    #[test]
    fn utf8_path_and_query_use_byte_boundaries() {
        for (text, path, query) in [("/é?q=1", "/é", "q=1"), ("/😀?", "/😀", ""), ("/é😀?q=é😀", "/é😀", "q=é😀")] {
            let value: PathAndQuery = text.parse().unwrap();
            assert_eq!(value.path(), path);
            assert_eq!(value.query(), Some(query));
            assert_eq!(value.as_str(), text);
        }
        let value = PathAndQuery::from_static("/😀?");
        assert_eq!(value.path(), "/😀");
        assert_eq!(value.query(), Some(""));
    }
    #[test]
    fn fragments_and_uri_delegation_preserve_unicode() {
        let value: PathAndQuery = "/é?q=☃#ignored".parse().unwrap();
        assert_eq!(value.path(), "/é");
        assert_eq!(value.query(), Some("q=☃"));
        assert_eq!(value.as_str(), "/é?q=☃");
        let uri: Uri = "http://example/é😀?q=☃".parse().unwrap();
        assert_eq!(uri.path(), "/é😀");
        assert_eq!(uri.query(), Some("q=☃"));
    }
    #[test]
    fn ordering_uses_unsigned_utf8_bytes() {
        let high: PathAndQuery = "/\u{10000}".parse().unwrap();
        let low: PathAndQuery = "/\u{e000}".parse().unwrap();
        assert_eq!(high.partial_cmp(&low), Some(Ordering::Greater));
        assert_eq!(high.partial_cmp("/\u{e000}"), Some(Ordering::Greater));
        let query_high: PathAndQuery = "/?q=\u{10000}".parse().unwrap();
        let query_low: PathAndQuery = "/?q=\u{e000}".parse().unwrap();
        assert_eq!(query_high.partial_cmp(&query_low), Some(Ordering::Greater));
    }
    #[test]
    fn empty_and_case_sensitive_ordering_keep_source_behavior() {
        let empty = PathAndQuery::from_static("");
        let slash = PathAndQuery::from_static("/");
        assert_eq!(empty.path(), "/");
        assert_eq!(empty.query(), None);
        assert_eq!(empty.partial_cmp(&slash), Some(Ordering::Equal));
        assert_ne!(empty, slash);
        assert_eq!(PathAndQuery::from_static("/A").partial_cmp("/a"), Some(Ordering::Less));
        let unicode: PathAndQuery = "/😀".parse().unwrap();
        assert_eq!(unicode.path(), "/😀");
        assert_eq!(unicode.query(), None);
    }
    #[test]
    fn invalid_utf8_still_returns_invalid_uri() {
        for text in [&b"/\xff?x"[..], &b"/x?\xff"[..]] {
            assert!(PathAndQuery::try_from(text).is_err());
        }
    }
    #[test]
    fn query_offset_keeps_upstream_u16_narrowing() {
        for pos in [65534usize, 65535, 65536] {
            let text = format!("/{}?q", "a".repeat(pos - 1));
            let parsed: PathAndQuery = text.parse().unwrap();
            let constant = PathAndQuery::from_static(Box::leak(text.clone().into_boxed_str()));
            for value in [parsed, constant] {
                match pos {
                    65534 => { assert_eq!(value.path().len(), 65534); assert_eq!(value.query(), Some("q")); }
                    65535 => { assert_eq!(value.path().len(), 65537); assert_eq!(value.query(), None); }
                    65536 => { assert_eq!(value.path(), "/"); assert_eq!(value.query().unwrap(), &text[1..]); }
                    _ => unreachable!(),
                }
            }
        }
    }
}
