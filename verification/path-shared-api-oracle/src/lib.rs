#[cfg(test)]
mod tests {
    use bytes::{Buf, Bytes};
    use http::{uri::PathAndQuery, Uri};
    fn check(input: &str, repr: &str, path: &str, query: Option<&str>, display: &str) {
        let values = [
            PathAndQuery::from_maybe_shared(Bytes::copy_from_slice(input.as_bytes())),
            PathAndQuery::from_maybe_shared(input.as_bytes().to_vec()),
            PathAndQuery::from_maybe_shared(input.to_owned()),
        ];
        for value in values {
            let value = value.unwrap();
            assert_eq!(value.as_str(), repr);
            assert_eq!(value.path(), path);
            assert_eq!(value.query(), query);
            assert_eq!(value.to_string(), display);
        }
    }
    #[test]
    #[allow(non_snake_case)]
    fn textForms() {
        check("/a?b", "/a?b", "/a", Some("b"), "/a?b");
        check("", "/", "/", None, "/");
        check("?q", "?q", "/", Some("q"), "/?q");
        check("/?", "/?", "/", Some(""), "/?");
        check("*", "*", "*", None, "*");
        check("abc", "abc", "abc", None, "/abc");
    }
    #[test]
    #[allow(non_snake_case)]
    fn utf8AndFragments() {
        check("/é?q=🍕", "/é?q=🍕", "/é", Some("q=🍕"), "/é?q=🍕");
        check("/a?b#ignored", "/a?b", "/a", Some("b"), "/a?b");
        check("/a#ignored\0", "/a", "/a", None, "/a");
    }
    #[test]
    fn invalid_ascii() {
        for value in [
            PathAndQuery::from_maybe_shared(Bytes::copy_from_slice("/a b".as_bytes())),
            PathAndQuery::from_maybe_shared("/a b".as_bytes().to_vec()),
            PathAndQuery::from_maybe_shared("/a b".to_owned()),
        ] {
            assert_eq!(value.unwrap_err().to_string(), "invalid uri character");
        }
        for value in [
            PathAndQuery::from_maybe_shared(Bytes::copy_from_slice("/a?b\n".as_bytes())),
            PathAndQuery::from_maybe_shared("/a?b\n".as_bytes().to_vec()),
            PathAndQuery::from_maybe_shared("/a?b\n".to_owned()),
        ] {
            assert_eq!(value.unwrap_err().to_string(), "invalid uri character");
        }
        for value in [
            PathAndQuery::from_maybe_shared(Bytes::copy_from_slice("\0".as_bytes())),
            PathAndQuery::from_maybe_shared("\0".as_bytes().to_vec()),
            PathAndQuery::from_maybe_shared("\0".to_owned()),
        ] {
            assert_eq!(value.unwrap_err().to_string(), "invalid uri character");
        }
        for value in [
            PathAndQuery::from_maybe_shared(Bytes::copy_from_slice("\n".as_bytes())),
            PathAndQuery::from_maybe_shared("\n".as_bytes().to_vec()),
            PathAndQuery::from_maybe_shared("\n".to_owned()),
        ] {
            assert_eq!(value.unwrap_err().to_string(), "invalid uri character");
        }
        for value in [
            PathAndQuery::from_maybe_shared(Bytes::copy_from_slice("/x?y\t".as_bytes())),
            PathAndQuery::from_maybe_shared("/x?y\t".as_bytes().to_vec()),
            PathAndQuery::from_maybe_shared("/x?y\t".to_owned()),
        ] {
            assert_eq!(value.unwrap_err().to_string(), "invalid uri character");
        }
    }
    #[test]
    fn invalid_utf8() {
        let raw = vec![47, 195, 40];
        for result in [
            PathAndQuery::from_maybe_shared(Bytes::copy_from_slice(&raw)),
            PathAndQuery::from_maybe_shared(raw),
        ] {
            assert_eq!(result.unwrap_err().to_string(), "invalid uri character");
        }
        let raw = vec![47, 63, 192, 128];
        for result in [
            PathAndQuery::from_maybe_shared(Bytes::copy_from_slice(&raw)),
            PathAndQuery::from_maybe_shared(raw),
        ] {
            assert_eq!(result.unwrap_err().to_string(), "invalid uri character");
        }
        let raw = vec![47, 237, 160, 128];
        for result in [
            PathAndQuery::from_maybe_shared(Bytes::copy_from_slice(&raw)),
            PathAndQuery::from_maybe_shared(raw),
        ] {
            assert_eq!(result.unwrap_err().to_string(), "invalid uri character");
        }
    }
    #[test]
    fn shared_handle_independence() {
        let mut original = Bytes::from_static(b"/a?b#fragment");
        let value = PathAndQuery::from_maybe_shared(original.clone()).unwrap();
        original.advance(2);
        assert_eq!(original.as_ref(), b"?b#fragment");
        assert_eq!(value.as_str(), "/a?b");
        assert_eq!(value.path(), "/a");
        assert_eq!(value.query(), Some("b"));
    }
    #[test]
    fn borrowed_copy_independence() {
        let mut original = b"/a?b".to_vec();
        let value = PathAndQuery::try_from(original.as_slice()).unwrap();
        original[1] = b'z';
        assert_eq!(value.as_str(), "/a?b");
    }
    #[test]
    fn uri_builder_consumer() {
        let path = PathAndQuery::from_maybe_shared(String::from("/é?q=🍕#fragment")).unwrap();
        let uri = Uri::builder()
            .scheme("http")
            .authority("example.org:+80")
            .path_and_query(path)
            .build()
            .unwrap();
        assert_eq!(uri.to_string(), "http://example.org:+80/é?q=🍕");
        assert_eq!(uri.port_u16(), Some(80));
    }
}
