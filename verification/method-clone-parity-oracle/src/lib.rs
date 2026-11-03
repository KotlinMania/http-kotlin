#[cfg(test)]
mod tests {
    use http::Method;
    use std::hash::{Hash, Hasher};
    fn hash(x: &Method) -> u64 {
        let mut h = std::collections::hash_map::DefaultHasher::new();
        x.hash(&mut h);
        h.finish()
    }
    fn check(text: &str) {
        let original = Method::from_bytes(text.as_bytes()).unwrap();
        let clone = original.clone();
        assert_eq!(clone, original);
        assert_eq!(hash(&clone), hash(&original));
        assert_eq!(clone.is_safe(), original.is_safe());
        assert_eq!(clone.is_idempotent(), original.is_idempotent());
        drop(original);
        assert_eq!(clone.as_str(), text);
        assert_eq!(clone.to_string(), text);
    }
    #[test]
    fn standard_methods() {
        for text in [
            "GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "CONNECT", "PATCH", "TRACE",
        ] {
            check(text);
        }
    }
    #[test]
    fn inline_extensions() {
        for text in ["x", "GE", "get", "CUSTOM", "123456789012345"] {
            check(text);
        }
    }
    #[test]
    fn allocated_extensions() {
        for len in [16, 17, 64, 256] {
            check(&"X".repeat(len));
        }
    }
    #[test]
    fn case_sensitive() {
        for (upper, lower) in [("GET", "get"), ("POST", "post"), ("CUSTOM", "custom")] {
            let a: Method = upper.parse().unwrap();
            let b: Method = lower.parse().unwrap();
            assert_ne!(a.clone(), b.clone());
            assert_eq!(a.clone().as_str(), upper);
            assert_eq!(b.clone().as_str(), lower);
        }
    }
    #[test]
    fn byte_domain() {
        for byte in 0u8..=255 {
            let allowed = byte.is_ascii_alphanumeric() || b"!#$%&'*+-.^_`|~".contains(&byte);
            let result = Method::from_bytes(&[byte]);
            assert_eq!(result.is_ok(), allowed, "byte={byte}");
            if let Ok(original) = result {
                let clone = original.clone();
                assert_eq!(clone, original);
                assert_eq!(clone.as_str().as_bytes(), &[byte]);
            }
        }
    }
    #[test]
    fn input_copy_independence() {
        for text in ["GET", "custom", "XXXXXXXXXXXXXXXX"] {
            let mut input = text.as_bytes().to_vec();
            let original = Method::from_bytes(&input).unwrap();
            let clone = original.clone();
            input[0] = b'Z';
            assert_eq!(original.as_str(), text);
            drop(original);
            assert_eq!(clone.as_str(), text);
        }
    }
    #[test]
    fn clone_of_clone() {
        for len in [1, 15, 16, 32] {
            let text = "X".repeat(len);
            let original = Method::from_bytes(text.as_bytes()).unwrap();
            let clone = original.clone().clone();
            assert_eq!(clone, original);
            assert_eq!(clone.as_str(), text);
            assert_eq!(hash(&clone), hash(&original));
        }
    }
}
