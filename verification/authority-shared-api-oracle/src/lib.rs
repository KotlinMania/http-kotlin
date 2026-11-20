#[cfg(test)]
mod tests {
    use bytes::{Buf, Bytes};
    use http::{uri::Authority, Uri};
    fn check(input: &str, host: &str, port: Option<u16>) {
        for value in [
            Authority::from_maybe_shared(Bytes::copy_from_slice(input.as_bytes())),
            Authority::from_maybe_shared(input.as_bytes().to_vec()),
            Authority::from_maybe_shared(input.to_owned()),
        ] {
            let value = value.unwrap();
            assert_eq!(value.as_str(), input);
            assert_eq!(value.host(), host);
            assert_eq!(value.port_u16(), port);
            assert_eq!(value.to_string(), input);
        }
    }
    #[test]
    #[allow(non_snake_case)]
    fn asciiAndPorts() {
        check("example.org", "example.org", None);
        check("example.org:80", "example.org", Some(80));
        check("EXAMPLE.org:+80", "EXAMPLE.org", Some(80));
        check("example.org:00080", "example.org", Some(80));
        check("example.org:-0", "example.org", None);
        check("example.org:65536", "example.org", None);
    }
    #[test]
    #[allow(non_snake_case)]
    fn userinfoAndIpv6() {
        check("user:pass@example.org:+80", "example.org", Some(80));
        check("a%2f:b%2f@example.com", "example.com", None);
        check("[::1]:443", "[::1]", Some(443));
    }
    #[test]
    fn invalid_syntax() {
        for value in [
            Authority::from_maybe_shared(Bytes::copy_from_slice("".as_bytes())),
            Authority::from_maybe_shared("".as_bytes().to_vec()),
            Authority::from_maybe_shared("".to_owned()),
        ] {
            assert_eq!(value.unwrap_err().to_string(), "empty string");
        }
        for value in [
            Authority::from_maybe_shared(Bytes::copy_from_slice("example.org/path".as_bytes())),
            Authority::from_maybe_shared("example.org/path".as_bytes().to_vec()),
            Authority::from_maybe_shared("example.org/path".to_owned()),
        ] {
            assert_eq!(value.unwrap_err().to_string(), "invalid uri character");
        }
        for value in [
            Authority::from_maybe_shared(Bytes::copy_from_slice("example.org?query".as_bytes())),
            Authority::from_maybe_shared("example.org?query".as_bytes().to_vec()),
            Authority::from_maybe_shared("example.org?query".to_owned()),
        ] {
            assert_eq!(value.unwrap_err().to_string(), "invalid uri character");
        }
        for value in [
            Authority::from_maybe_shared(Bytes::copy_from_slice("example.org#fragment".as_bytes())),
            Authority::from_maybe_shared("example.org#fragment".as_bytes().to_vec()),
            Authority::from_maybe_shared("example.org#fragment".to_owned()),
        ] {
            assert_eq!(value.unwrap_err().to_string(), "invalid uri character");
        }
        for value in [
            Authority::from_maybe_shared(Bytes::copy_from_slice("exa mple.org".as_bytes())),
            Authority::from_maybe_shared("exa mple.org".as_bytes().to_vec()),
            Authority::from_maybe_shared("exa mple.org".to_owned()),
        ] {
            assert_eq!(value.unwrap_err().to_string(), "invalid uri character");
        }
        for value in [
            Authority::from_maybe_shared(Bytes::copy_from_slice("é.example".as_bytes())),
            Authority::from_maybe_shared("é.example".as_bytes().to_vec()),
            Authority::from_maybe_shared("é.example".to_owned()),
        ] {
            assert_eq!(value.unwrap_err().to_string(), "invalid uri character");
        }
        for value in [
            Authority::from_maybe_shared(Bytes::copy_from_slice("[::1".as_bytes())),
            Authority::from_maybe_shared("[::1".as_bytes().to_vec()),
            Authority::from_maybe_shared("[::1".to_owned()),
        ] {
            assert_eq!(value.unwrap_err().to_string(), "invalid authority");
        }
        for value in [
            Authority::from_maybe_shared(Bytes::copy_from_slice("example%2f.com".as_bytes())),
            Authority::from_maybe_shared("example%2f.com".as_bytes().to_vec()),
            Authority::from_maybe_shared("example%2f.com".to_owned()),
        ] {
            assert_eq!(value.unwrap_err().to_string(), "invalid authority");
        }
    }
    #[test]
    fn invalid_utf8() {
        let raw = vec![195, 40];
        for result in [
            Authority::from_maybe_shared(Bytes::copy_from_slice(&raw)),
            Authority::from_maybe_shared(raw),
        ] {
            assert_eq!(result.unwrap_err().to_string(), "invalid uri character");
        }
        let raw = vec![255];
        for result in [
            Authority::from_maybe_shared(Bytes::copy_from_slice(&raw)),
            Authority::from_maybe_shared(raw),
        ] {
            assert_eq!(result.unwrap_err().to_string(), "invalid uri character");
        }
        let raw = vec![101, 120, 97, 109, 112, 108, 101, 0];
        for result in [
            Authority::from_maybe_shared(Bytes::copy_from_slice(&raw)),
            Authority::from_maybe_shared(raw),
        ] {
            assert_eq!(result.unwrap_err().to_string(), "invalid uri character");
        }
    }
    #[test]
    fn shared_handle_independence() {
        let mut original = Bytes::from_static(b"example.org:+80");
        let value = Authority::from_maybe_shared(original.clone()).unwrap();
        original.advance(2);
        assert_eq!(original.as_ref(), b"ample.org:+80");
        assert_eq!(value.as_str(), "example.org:+80");
        assert_eq!(value.host(), "example.org");
        assert_eq!(value.port_u16(), Some(80));
    }
    #[test]
    fn borrowed_copy_independence() {
        let mut original = b"example.org".to_vec();
        let value = Authority::try_from(original.as_slice()).unwrap();
        original[0] = b'z';
        assert_eq!(value.as_str(), "example.org");
    }
    #[test]
    fn builder_consumer() {
        let authority =
            Authority::from_maybe_shared(Bytes::from_static(b"example.org:+80")).unwrap();
        let uri = Uri::builder()
            .scheme("http")
            .authority(authority)
            .path_and_query("/x")
            .build()
            .unwrap();
        assert_eq!(uri.to_string(), "http://example.org:+80/x");
        assert_eq!(uri.port_u16(), Some(80));
    }
}
