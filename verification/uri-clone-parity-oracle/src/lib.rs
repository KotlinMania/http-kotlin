#[cfg(test)]
mod tests {
    use bytes::{Buf, Bytes};
    use http::{
        uri::{Authority, PathAndQuery, Scheme},
        Uri,
    };
    use std::hash::{Hash, Hasher};
    fn hash<T: Hash>(x: &T) -> u64 {
        let mut h = std::collections::hash_map::DefaultHasher::new();
        x.hash(&mut h);
        h.finish()
    }
    #[test]
    fn schemes() {
        for text in ["http", "https", "custom+V1"] {
            let original: Scheme = text.parse().unwrap();
            let clone = original.clone();
            assert_eq!(clone, original);
            assert_eq!(hash(&clone), hash(&original));
            drop(original);
            assert_eq!(clone.as_str(), text);
        }
    }
    #[test]
    fn authorities() {
        for text in ["example.org:+80", "user:pass@example.org", "[::1]:443"] {
            let original: Authority = text.parse().unwrap();
            let clone = original.clone();
            assert_eq!(clone, original);
            assert_eq!(clone.host(), original.host());
            assert_eq!(clone.port_u16(), original.port_u16());
            assert_eq!(hash(&clone), hash(&original));
            drop(original);
            assert_eq!(clone.as_str(), text);
        }
    }
    #[test]
    fn paths() {
        for text in ["", "/", "?q", "/é?q=🍕#fragment"] {
            let original: PathAndQuery = text.parse().unwrap();
            let clone = original.clone();
            assert_eq!(clone, original);
            assert_eq!(clone.as_str(), original.as_str());
            assert_eq!(clone.path(), original.path());
            assert_eq!(clone.query(), original.query());
            assert_eq!(hash(&clone), hash(&original));
            let display = original.to_string();
            drop(original);
            assert_eq!(clone.to_string(), display);
        }
    }
    #[test]
    fn uri_forms() {
        for original in [
            Uri::default(),
            "example.org:80".parse().unwrap(),
            "*".parse().unwrap(),
            "/a?b".parse().unwrap(),
            "http://example.org:+80/é?q=🍕".parse().unwrap(),
        ] {
            let clone = original.clone();
            assert_eq!(clone, original);
            assert_eq!(clone.path(), original.path());
            assert_eq!(clone.query(), original.query());
            assert_eq!(clone.host(), original.host());
            assert_eq!(clone.port_u16(), original.port_u16());
            assert_eq!(hash(&clone), hash(&original));
            let display = original.to_string();
            drop(original);
            assert_eq!(clone.to_string(), display);
        }
    }
    #[test]
    fn shared_input_cursor() {
        let mut input = Bytes::from_static(b"http://example.org:+80/a?b");
        let original = Uri::from_maybe_shared(input.clone()).unwrap();
        let clone = original.clone();
        input.advance(7);
        assert_eq!(input.as_ref(), b"example.org:+80/a?b");
        assert_eq!(original.to_string(), "http://example.org:+80/a?b");
        assert_eq!(clone.to_string(), "http://example.org:+80/a?b");
    }
    #[test]
    fn parts_round_trip() {
        let original: Uri = "http://example.org:+80/a?b".parse().unwrap();
        let rebuilt = Uri::from_parts(original.clone().into_parts()).unwrap();
        assert_eq!(original, rebuilt);
        assert_eq!(rebuilt.port_u16(), Some(80));
    }
    #[test]
    fn canonical_buffer_clone_control() {
        let mut original = Bytes::from_static(b"abc");
        let clone = original.clone();
        original.advance(1);
        assert_eq!(original.as_ref(), b"bc");
        assert_eq!(clone.as_ref(), b"abc");
    }
}
