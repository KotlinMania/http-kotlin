// Build cached unchanged bytes 1.11.1 with rustc --crate-name bytes --crate-type rlib --edition 2021 --cfg 'feature="std"' <bytes-source>/src/lib.rs -o build/rust-kotlin-byte-str/libbytes.rlib
// rustc --test --edition 2021 --extern bytes=build/rust-kotlin-byte-str/libbytes.rlib verification/byte-str-oracle.rs -o build/rust-kotlin-byte-str/byte-str-oracle
// build/rust-kotlin-byte-str/byte-str-oracle
#[path = "../tmp/http/src/byte_str.rs"]
mod byte_str;
use byte_str::ByteStr;
use bytes::Bytes;
use std::cmp::Ordering;

#[test]
fn supplementary_scalars_sort_after_the_bmp_private_use_range() {
    assert_eq!(ByteStr::from("\u{10000}".to_string()).cmp(&ByteStr::from("\u{e000}".to_string())), Ordering::Greater);
    assert_eq!(ByteStr::from("\u{10ffff}".to_string()).cmp(&ByteStr::from("\u{ffff}".to_string())), Ordering::Greater);
}
#[test]
fn sorting_uses_utf8_byte_order() {
    let mut values: Vec<_> = ["\u{10000}", "a", "\u{e000}", "😀", "é", ""].into_iter().map(|s| ByteStr::from(s.to_string())).collect();
    values.sort();
    let strings: Vec<&str> = values.iter().map(|x| &**x).collect();
    assert_eq!(strings, ["", "a", "é", "\u{e000}", "\u{10000}", "😀"]);
}
#[test]
fn utf8_checked_and_static_constructors_preserve_the_same_order() {
    let high = ByteStr::from_utf8(Bytes::copy_from_slice("\u{10000}".as_bytes())).unwrap();
    let low = ByteStr::from_static("\u{e000}");
    assert_eq!(high.cmp(&low), Ordering::Greater);
    assert_eq!(low.cmp(&high), Ordering::Less);
}
#[test]
fn prefixes_ascii_and_equal_values_keep_their_order() {
    for (left, right) in [("", "a"), ("x", "xy"), ("z", "é"), ("😀", "😀a")] {
        assert_eq!(ByteStr::from(left.to_string()).cmp(&ByteStr::from(right.to_string())), Ordering::Less);
    }
    assert_eq!(ByteStr::from_static("é").cmp(&ByteStr::from("é".to_string())), Ordering::Equal);
}
