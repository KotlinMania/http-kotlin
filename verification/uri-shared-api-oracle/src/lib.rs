#[cfg(test)]
mod tests {
    use bytes::{Buf, Bytes};
    use http::Uri;
    fn check(
        input: &str,
        display: &str,
        path: &str,
        query: Option<&str>,
        host: Option<&str>,
        port: Option<u16>,
    ) {
        for value in [
            Uri::from_maybe_shared(Bytes::copy_from_slice(input.as_bytes())),
            Uri::from_maybe_shared(input.as_bytes().to_vec()),
            Uri::from_maybe_shared(input.to_owned()),
        ] {
            let value = value.unwrap();
            assert_eq!(value.to_string(), display);
            assert_eq!(value.path(), path);
            assert_eq!(value.query(), query);
            assert_eq!(value.host(), host);
            assert_eq!(value.port_u16(), port);
        }
    }
    #[test]
    #[allow(non_snake_case)]
    fn uriForms() {
        check("/", "/", "/", None, None, None);
        check("*", "*", "*", None, None, None);
        check(
            "example.org:80",
            "example.org:80",
            "",
            None,
            Some("example.org"),
            Some(80),
        );
        check("/a?b", "/a?b", "/a", Some("b"), None, None);
        check(
            "http://example.org:+80/a?b",
            "http://example.org:+80/a?b",
            "/a",
            Some("b"),
            Some("example.org"),
            Some(80),
        );
    }
    #[test]
    #[allow(non_snake_case)]
    fn utf8AndFragments() {
        check("/é?q=🍕", "/é?q=🍕", "/é", Some("q=🍕"), None, None);
        check(
            "http://example.org/é?q=🍕#ignored",
            "http://example.org/é?q=🍕",
            "/é",
            Some("q=🍕"),
            Some("example.org"),
            None,
        );
    }
    #[test]
    fn invalid_syntax() {
        for result in [
            Uri::from_maybe_shared(Bytes::copy_from_slice("".as_bytes())),
            Uri::from_maybe_shared("".as_bytes().to_vec()),
            Uri::from_maybe_shared("".to_owned()),
        ] {
            assert!(result.is_err());
        }
        for result in [
            Uri::from_maybe_shared(Bytes::copy_from_slice("http:///x".as_bytes())),
            Uri::from_maybe_shared("http:///x".as_bytes().to_vec()),
            Uri::from_maybe_shared("http:///x".to_owned()),
        ] {
            assert!(result.is_err());
        }
        for result in [
            Uri::from_maybe_shared(Bytes::copy_from_slice("http://exa mple.org/x".as_bytes())),
            Uri::from_maybe_shared("http://exa mple.org/x".as_bytes().to_vec()),
            Uri::from_maybe_shared("http://exa mple.org/x".to_owned()),
        ] {
            assert!(result.is_err());
        }
        for result in [
            Uri::from_maybe_shared(Bytes::copy_from_slice("/a b".as_bytes())),
            Uri::from_maybe_shared("/a b".as_bytes().to_vec()),
            Uri::from_maybe_shared("/a b".to_owned()),
        ] {
            assert!(result.is_err());
        }
        for result in [
            Uri::from_maybe_shared(Bytes::copy_from_slice("http://[::1/x".as_bytes())),
            Uri::from_maybe_shared("http://[::1/x".as_bytes().to_vec()),
            Uri::from_maybe_shared("http://[::1/x".to_owned()),
        ] {
            assert!(result.is_err());
        }
    }
    #[test]
    fn invalid_utf8() {
        let raw = vec![47, 195, 40];
        for result in [
            Uri::from_maybe_shared(Bytes::copy_from_slice(&raw)),
            Uri::from_maybe_shared(raw),
        ] {
            assert_eq!(result.unwrap_err().to_string(), "invalid uri character");
        }
        let raw = vec![47, 63, 255];
        for result in [
            Uri::from_maybe_shared(Bytes::copy_from_slice(&raw)),
            Uri::from_maybe_shared(raw),
        ] {
            assert_eq!(result.unwrap_err().to_string(), "invalid uri character");
        }
    }
    #[test]
    fn byte_length_boundary() {
        for valid in [
            format!("/{}", "a".repeat(65533)),
            format!("/{}a", "é".repeat(32766)),
        ] {
            let value = Uri::from_maybe_shared(valid.clone()).unwrap();
            assert_eq!(value.path(), valid);
        }
        for invalid in [
            format!("/{}", "a".repeat(65534)),
            format!("/{}", "é".repeat(32767)),
            format!("/#{}", "a".repeat(65533)),
        ] {
            for result in [
                Uri::from_maybe_shared(Bytes::copy_from_slice(invalid.as_bytes())),
                Uri::from_maybe_shared(invalid.as_bytes().to_vec()),
                Uri::from_maybe_shared(invalid.clone()),
            ] {
                assert_eq!(result.unwrap_err().to_string(), "uri too long");
            }
        }
    }
    #[test]
    fn shared_handle_independence() {
        let mut original = Bytes::from_static(b"http://example.org:+80/a?b");
        let value = Uri::from_maybe_shared(original.clone()).unwrap();
        original.advance(7);
        assert_eq!(original.as_ref(), b"example.org:+80/a?b");
        assert_eq!(value.to_string(), "http://example.org:+80/a?b");
        assert_eq!(value.host(), Some("example.org"));
        assert_eq!(value.port_u16(), Some(80));
    }
    #[test]
    fn borrowed_copy_independence() {
        let mut original = b"http://example.org/a".to_vec();
        let value = Uri::try_from(original.as_slice()).unwrap();
        original[0] = b'x';
        assert_eq!(value.to_string(), "http://example.org/a");
    }
    #[test]
    fn parts_round_trip() {
        let value = Uri::from_maybe_shared(String::from("http://example.org:+80/a?b")).unwrap();
        let parts = value.into_parts();
        let rebuilt = Uri::from_parts(parts).unwrap();
        assert_eq!(rebuilt.to_string(), "http://example.org:+80/a?b");
        assert_eq!(rebuilt.port_u16(), Some(80));
    }
}
