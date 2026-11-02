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
