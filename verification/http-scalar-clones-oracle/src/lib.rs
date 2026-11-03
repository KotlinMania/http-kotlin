#[cfg(test)]
mod tests {
    use http::{StatusCode, Version};
    use std::hash::{Hash, Hasher};
    fn hash<T: Hash>(x: &T) -> u64 {
        let mut h = std::collections::hash_map::DefaultHasher::new();
        x.hash(&mut h);
        h.finish()
    }
    fn versions() -> [Version; 5] {
        [
            Version::HTTP_09,
            Version::HTTP_10,
            Version::HTTP_11,
            Version::HTTP_2,
            Version::HTTP_3,
        ]
    }
    #[test]
    fn all_statuses() {
        for code in 100u16..=999 {
            let original = StatusCode::from_u16(code).unwrap();
            let clone = original.clone();
            assert_eq!(clone, original);
            assert_eq!(clone.as_u16(), code);
            assert_eq!(clone.as_str(), original.as_str());
            assert_eq!(clone.to_string(), original.to_string());
            assert_eq!(hash(&clone), hash(&original));
        }
    }
    #[test]
    fn status_flags_and_reasons() {
        for code in [100, 199, 200, 299, 300, 399, 400, 499, 500, 599, 600, 999] {
            let original = StatusCode::from_u16(code).unwrap();
            let clone = original.clone();
            assert_eq!(clone.is_informational(), original.is_informational());
            assert_eq!(clone.is_success(), original.is_success());
            assert_eq!(clone.is_redirection(), original.is_redirection());
            assert_eq!(clone.is_client_error(), original.is_client_error());
            assert_eq!(clone.is_server_error(), original.is_server_error());
            assert_eq!(clone.canonical_reason(), original.canonical_reason());
        }
    }
    #[test]
    fn status_order() {
        for (a, b) in [(100, 999), (200, 404), (599, 600), (999, 999)] {
            let a = StatusCode::from_u16(a).unwrap();
            let b = StatusCode::from_u16(b).unwrap();
            assert_eq!(a.clone().cmp(&b.clone()), a.cmp(&b));
        }
    }
    #[test]
    fn version_values() {
        for original in versions() {
            let clone = original.clone();
            assert_eq!(clone, original);
            assert_eq!(format!("{clone:?}"), format!("{original:?}"));
            assert_eq!(hash(&clone), hash(&original));
        }
    }
    #[test]
    fn version_order() {
        let all = versions();
        for a in all {
            for b in all {
                assert_eq!(a.clone().cmp(&b.clone()), a.cmp(&b));
            }
        }
    }
    #[test]
    fn clone_chains_and_default() {
        assert_eq!(StatusCode::OK.clone().clone().as_u16(), 200);
        assert_eq!(
            StatusCode::from_u16(999).unwrap().clone().clone().as_u16(),
            999
        );
        assert_eq!(Version::default().clone().clone(), Version::HTTP_11);
    }
}
