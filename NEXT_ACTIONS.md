# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 12/21 (57.1%)
- **Function parity:** 172/634 matched (target 278) — 27.1%
- **Class/type parity:** 28/92 matched (target 50) — 30.4%
- **Combined symbol parity:** 200/726 matched (target 328) — 27.5%
- **Average inline-code cosine:** 0.10 (function body across 11 matched files)
- **Average documentation cosine:** 0.72 (doc text across 11 matched files)
- **Cheat-zeroed Files:** 3
- **Critical Issues:** 12 files with <0.60 function similarity
- **Needs Review:** 0 files with 0.60-0.84 function similarity
- **Excellent:** 0 files with >=0.85 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. byte_str

- **Target:** `ByteStr`
- **Similarity:** 0.23
- **Dependents:** 5
- **Priority Score:** 5031007.5
- **Functions:** 6/8 matched (target 14)
- **Missing functions:** `from`, `from`
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Target`

### 2. method

- **Target:** `Method`
- **Similarity:** 0.12
- **Dependents:** 3
- **Priority Score:** 3113908.8
- **Functions:** 23/32 matched (target 34)
- **Missing functions:** `eq`, `eq`, `eq`, `eq`, `test_method_eq`, `test_invalid_method`, `test_is_idempotent`, `test_extension_method`, `test_extension_method_chars`
- **Types:** 5/7 matched (target 16)
- **Missing types:** `Error`, `Err`
- **Tests:** 0/5 matched

### 3. version

- **Target:** `Version`
- **Similarity:** 0.21
- **Dependents:** 3
- **Priority Score:** 3000408.0
- **Functions:** 2/2 matched (target 7)
- **Missing functions:** _none_
- **Types:** 2/2 matched
- **Missing types:** _none_

### 4. header.name

- **Target:** `header.Name`
- **Similarity:** 0.06
- **Dependents:** 1
- **Priority Score:** 1437009.4
- **Functions:** 22/62 matched (target 40)
- **Missing functions:** `as_ref`, `fmt`, `fmt`, `try_from`, `try_from`, `try_from`, `from`, `from`, `eq`, `eq`, `eq`, `fmt`, `fmt`, `from`, `eq`, `hash`, `hash`, `eq_ignore_ascii_case`, `uninit_u8_array`, `slice_assume_init`, `test_bounds`, `check_bounds`, `test_parse_invalid_headers`, `test_invalid_name_lengths`, `test_static_invalid_name_lengths`, `test_from_hdr_name`, `test_eq_hdr_name`, `test_from_static_std`, `test_from_static_std_uppercase`, `test_from_static_std_symbol`, `test_from_static_custom_short`, `test_from_static_custom_short_uppercase`, `test_from_static_custom_short_symbol`, `test_from_static_custom_long`, `test_from_static_custom_long_uppercase`, `test_from_static_custom_long_symbol`, `test_from_static_custom_single_char`, `test_from_static_empty`, `test_all_tokens`, `test_from_lowercase`
- **Types:** 5/8 matched (target 9)
- **Missing types:** `Repr`, `Err`, `Error`
- **Tests:** 0/20 matched

### 5. uri.authority

- **Target:** `uri.Authority`
- **Similarity:** 0.07
- **Dependents:** 1
- **Priority Score:** 1405809.2
- **Functions:** 17/54 matched (target 25)
- **Missing functions:** `eq`, `eq`, `eq`, `eq`, `eq`, `eq`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `hash`, `try_from`, `try_from`, `fmt`, `fmt`, `create_authority`, `parse_empty_string_is_error`, `equal_to_self_of_same_authority`, `not_equal_to_self_of_different_authority`, `equates_with_a_str`, `from_static_equates_with_a_str`, `not_equal_with_a_str_of_a_different_authority`, `equates_with_a_string`, `equates_with_a_string_of_a_different_authority`, `compares_to_self`, `compares_with_a_str`, `compares_with_a_string`, `allows_percent_in_userinfo`, `rejects_percent_in_hostname`, `allows_percent_in_ipv6_address`, `reject_obviously_invalid_ipv6_address`, `rejects_percent_outside_ipv6_address`, `rejects_invalid_utf8`, `rejects_invalid_use_of_brackets`
- **Types:** 1/4 matched (target 1)
- **Missing types:** `AuthorityError`, `Error`, `Err`
- **Tests:** 0/18 matched

### 6. uri.scheme

- **Target:** `uri.Scheme`
- **Similarity:** 0.11
- **Dependents:** 1
- **Priority Score:** 1132608.9
- **Functions:** 10/21 matched (target 14)
- **Missing functions:** `fmt`, `fmt`, `eq`, `eq`, `hash`, `is_none`, `from`, `from`, `scheme_eq_to_str`, `invalid_scheme_is_error`, `scheme`
- **Types:** 3/5 matched (target 10)
- **Missing types:** `Error`, `Err`
- **Tests:** 0/3 matched

### 7. uri.port

- **Target:** `uri.Port [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 1
- **Priority Score:** 1091510.0
- **Functions:** 5/14 matched (target 8)
- **Missing functions:** `fmt`, `fmt`, `from`, `eq`, `eq`, `partialeq_port`, `partialeq_port_different_reprs`, `partialeq_u16`, `u16_from_port`
- **Types:** 1/1 matched
- **Missing types:** _none_
- **Tests:** 0/4 matched

### 8. extensions

- **Target:** `Extensions`
- **Similarity:** 0.06
- **Dependents:** 1
- **Priority Score:** 1073009.4
- **Functions:** 20/24 matched (target 29)
- **Missing functions:** `write`, `write_u64`, `finish`, `test_extensions`
- **Types:** 3/6 matched (target 3)
- **Missing types:** `AnyMap`, `IdHasher`, `MyType`
- **Tests:** 0/1 matched

### 9. status

- **Target:** `Status [STUB]`
- **Similarity:** 0.00
- **Dependents:** 1
- **Priority Score:** 1052810.0
- **Functions:** 21/24 matched (target 31)
- **Missing functions:** `eq`, `from`, `try_from`
- **Types:** 2/4 matched (target 2)
- **Missing types:** `Err`, `Error`

### 10. uri.builder

- **Target:** `uri.Builder`
- **Similarity:** 0.12
- **Dependents:** 1
- **Priority Score:** 1051308.9
- **Functions:** 7/12 matched (target 10)
- **Missing functions:** `default`, `build_from_str`, `build_from_string`, `build_from_string_ref`, `build_from_uri`
- **Types:** 1/1 matched
- **Missing types:** _none_
- **Tests:** 0/4 matched

### 11. uri.path

- **Target:** `uri.PathAndQuery [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 395410.0
- **Functions:** 14/50 matched (target 23)
- **Missing functions:** `try_from`, `try_from`, `try_from`, `fmt`, `fmt`, `hash`, `eq`, `eq`, `eq`, `eq`, `eq`, `eq`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `equal_to_self_of_same_path`, `not_equal_to_self_of_different_path`, `equates_with_a_str`, `not_equal_with_a_str_of_a_different_path`, `equates_with_a_string`, `not_equal_with_a_string_of_a_different_path`, `compares_to_self`, `compares_with_a_str`, `compares_with_a_string`, `ignores_valid_percent_encodings`, `ignores_invalid_percent_encodings`, `allow_utf8_in_path`, `allow_utf8_in_query`, `rejects_invalid_utf8_in_path`, `rejects_invalid_utf8_in_query`, `json_is_fine`, `pq`
- **Types:** 1/4 matched (target 1)
- **Missing types:** `PathAndQueryError`, `Error`, `Err`
- **Tests:** 0/17 matched

### 12. header.value

- **Target:** `header.Value`
- **Similarity:** 0.08
- **Dependents:** 0
- **Priority Score:** 326009.2
- **Functions:** 25/55 matched (target 43)
- **Missing functions:** `from_maybe_shared_unchecked`, `from_shared`, `try_from_generic`, `fmt`, `it_can_insert_header_name_as_header_value`, `from_str`, `try_from`, `try_from`, `it_converts_using_try_from`, `is_valid`, `fmt`, `fmt`, `fmt`, `hash`, `eq`, `eq`, `partial_cmp`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `test_try_from`, `test_debug`
- **Types:** 3/5 matched (target 3)
- **Missing types:** `Err`, `Error`
- **Tests:** 0/4 matched

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `header.mod` | `header.Mod` | 0 | `header/mod.rs` | `header/Mod.kt` |
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |
| `uri.mod` | `uri.Mod` | 0 | `uri/mod.rs` | `uri/Mod.kt` |

