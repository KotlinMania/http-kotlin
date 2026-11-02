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

#[cfg(test)]
mod header_value_tests {
    use http::header::{HeaderValue, HeaderName, InvalidHeaderValue, ToStrError};
    use std::cmp::Ordering;
    use std::hash::{Hash, Hasher};
    use std::collections::hash_map::DefaultHasher;

    #[test]
    fn all_byte_values_have_the_source_validation_and_text_contract() {
        let mut accepted = 0;
        let mut text = 0;
        for byte in 0u8..=255 {
            let result: Result<HeaderValue, InvalidHeaderValue> = HeaderValue::from_bytes(&[byte]);
            let valid = byte == 9 || (32..=126).contains(&byte) || byte >= 128;
            assert_eq!(result.is_ok(), valid, "byte {byte}");
            if let Ok(value) = result {
                accepted += 1;
                assert_eq!(value.as_bytes(), &[byte]);
                let converted: Result<&str, ToStrError> = value.to_str();
                assert_eq!(converted.is_ok(), byte == 9 || (32..=126).contains(&byte));
                if converted.is_ok() { text += 1; }
            }
        }
        assert_eq!((accepted, text), (224, 96));
    }
    #[test]
    fn empty_and_ascii_values_have_text_and_length() {
        for input in ["", "hello", " \t~"] {
            let value = HeaderValue::from_static(input);
            assert_eq!(value.to_str().unwrap(), input);
            assert_eq!(value.len(), input.len());
            assert_eq!(value.is_empty(), input.is_empty());
            assert!(!value.is_sensitive());
        }
    }
    #[test]
    fn unicode_string_input_is_opaque_but_not_text() {
        let value = HeaderValue::from_str("é").unwrap();
        assert_eq!(value.as_bytes(), "é".as_bytes());
        assert!(value.to_str().is_err());
        assert_eq!(format!("{value:?}"), "\"\\xc3\\xa9\"");
    }
    #[test]
    fn static_constructor_rejects_nonvisible_characters() {
        for input in ["\n", "\r", "\0", "\u{7f}", "é"] {
            assert!(std::panic::catch_unwind(|| HeaderValue::from_static(input)).is_err());
        }
    }
    #[test]
    fn header_name_conversion_preserves_normalized_bytes() {
        let name = HeaderName::from_bytes(b"X-Custom").unwrap();
        assert_eq!(HeaderValue::from_name(name).as_bytes(), b"x-custom");
        assert_eq!(HeaderValue::from(HeaderName::from_static("content-type")).as_bytes(), b"content-type");
    }
    #[test]
    fn cloning_preserves_value_and_independent_sensitivity() {
        let mut value = HeaderValue::from_static("secret");
        value.set_sensitive(true);
        let mut copy = value.clone();
        assert!(copy.is_sensitive());
        copy.set_sensitive(false);
        assert!(value.is_sensitive());
        assert!(!copy.is_sensitive());
        assert_eq!(value, copy);
        assert_eq!(value.cmp(&copy), Ordering::Equal);
        let hash = |v: &HeaderValue| { let mut h = DefaultHasher::new(); v.hash(&mut h); h.finish() };
        assert_eq!(hash(&value), hash(&copy));
        assert_eq!(format!("{value:?}"), "Sensitive");
        assert_eq!(copy.to_str().unwrap(), "secret");
    }
    #[test]
    fn borrowed_input_is_copied() {
        let mut input = b"hello".to_vec();
        let value = HeaderValue::from_bytes(&input).unwrap();
        input[0] = 0;
        assert_eq!(value.as_bytes(), b"hello");
    }
    #[test]
    fn shared_and_owned_checked_constructors_match() {
        assert_eq!(HeaderValue::from_maybe_shared(String::from("hello")).unwrap(), "hello");
        assert_eq!(HeaderValue::from_maybe_shared(b"hello".to_vec()).unwrap(), "hello");
        assert_eq!(HeaderValue::try_from("hello").unwrap(), "hello");
        assert_eq!(HeaderValue::try_from(b"hello".as_slice()).unwrap(), "hello");
    }
    #[test]
    fn debug_escaping_is_exact() {
        for (input, expected) in [
            ("hello", "\"hello\""),
            ("hello \"world\"", "\"hello \\\"world\\\"\""),
            ("\u{7fff}hello", "\"\\xe7\\xbf\\xbfhello\""),
            ("\t\\", concat!("\"", "\t\\", "\"")),
        ] {
            assert_eq!(format!("{:?}", HeaderValue::from_str(input).unwrap()), expected);
        }
        assert_eq!(format!("{:?}", HeaderValue::from_bytes(&[128, 255]).unwrap()), "\"\\x80\\xff\"");
    }
    #[test]
    fn equality_and_order_are_case_sensitive_unsigned_bytes() {
        let value = HeaderValue::from_bytes(&[255]).unwrap();
        assert_eq!(value.partial_cmp(&[128][..]), Some(Ordering::Greater));
        assert_eq!(value.partial_cmp("é"), Some(Ordering::Greater));
        assert_eq!(value, &[255][..]);
        let lower = HeaderValue::from_static("a");
        assert_ne!(lower, "A");
        assert_eq!(lower.partial_cmp("A"), Some(Ordering::Greater));
    }
    #[test]
    fn error_messages_and_debug_are_source_values() {
        let invalid = HeaderValue::from_bytes(b"\n").unwrap_err();
        assert_eq!(invalid.to_string(), "failed to parse header value");
        assert_eq!(format!("{invalid:?}"), "InvalidHeaderValue");
        let nontext = HeaderValue::from_bytes(&[255]).unwrap();
        let err = nontext.to_str().unwrap_err();
        assert_eq!(err.to_string(), "failed to convert header to a str");
        assert_eq!(format!("{err:?}"), "ToStrError { _priv: () }");
    }
}

#[cfg(test)]
mod shared_header_value_tests {
    use bytes::{Bytes, Buf};
    use http::header::HeaderValue;

    #[test]
    fn shared_bytes_keep_an_independent_owned_cursor() {
        let mut retained = Bytes::from_static(b"hello");
        let value = HeaderValue::from_maybe_shared(retained.clone()).unwrap();
        retained.advance(1);
        assert_eq!(retained.as_ref(), b"ello");
        assert_eq!(value.as_bytes(), b"hello");
    }
}
