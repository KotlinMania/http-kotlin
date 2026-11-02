# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 11/21 (52.4%)
- **Function parity:** 210/634 matched (target 292) — 33.1%
- **Class/type parity:** 26/92 matched (target 56) — 28.3%
- **Combined symbol parity:** 236/726 matched (target 348) — 32.5%
- **Average inline-code cosine:** 0.15 (function body across 10 matched files)
- **Average documentation cosine:** 0.81 (doc text across 10 matched files)
- **Cheat-zeroed Files:** 2
- **Critical Issues:** 11 files with <0.60 function similarity
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

- **Target:** `http.ByteStr`
- **Similarity:** 0.23
- **Dependents:** 5
- **Priority Score:** 5031007.5
- **Functions:** 6/8 matched (target 12)
- **Missing functions:** `from`, `from`
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Target`

### 2. method

- **Target:** `http.Method`
- **Similarity:** 0.16
- **Dependents:** 3
- **Priority Score:** 3063908.5
- **Functions:** 28/32 matched (target 36)
- **Missing functions:** `eq`, `eq`, `eq`, `eq`
- **Types:** 5/7 matched (target 17)
- **Missing types:** `Error`, `Err`
- **Tests:** 5/5 matched

### 3. version

- **Target:** `http.Version`
- **Similarity:** 0.21
- **Dependents:** 3
- **Priority Score:** 3000408.0
- **Functions:** 2/2 matched (target 6)
- **Missing functions:** _none_
- **Types:** 2/2 matched
- **Missing types:** _none_

### 4. header.name

- **Target:** `header.Name`
- **Similarity:** 0.13
- **Dependents:** 1
- **Priority Score:** 1247008.8
- **Functions:** 41/62 matched (target 65)
- **Missing functions:** `as_ref`, `fmt`, `fmt`, `try_from`, `try_from`, `try_from`, `from`, `from`, `eq`, `eq`, `eq`, `fmt`, `fmt`, `from`, `eq`, `hash`, `hash`, `uninit_u8_array`, `slice_assume_init`, `test_bounds`, `check_bounds`
- **Types:** 5/8 matched (target 10)
- **Missing types:** `Repr`, `Err`, `Error`
- **Tests:** 18/20 matched

### 5. uri.authority

- **Target:** `uri.Authority`
- **Similarity:** 0.15
- **Dependents:** 1
- **Priority Score:** 1245808.5
- **Functions:** 33/54 matched (target 38)
- **Missing functions:** `from_maybe_shared`, `eq`, `eq`, `eq`, `eq`, `eq`, `eq`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `hash`, `try_from`, `try_from`, `fmt`, `fmt`, `create_authority`, `compares_with_a_string`
- **Types:** 1/4 matched (target 2)
- **Missing types:** `AuthorityError`, `Error`, `Err`
- **Tests:** 17/18 matched

### 6. uri.scheme

- **Target:** `uri.Scheme`
- **Similarity:** 0.14
- **Dependents:** 1
- **Priority Score:** 1112608.6
- **Functions:** 12/21 matched (target 16)
- **Missing functions:** `fmt`, `fmt`, `eq`, `eq`, `hash`, `is_none`, `from`, `from`, `scheme`
- **Types:** 3/5 matched (target 11)
- **Missing types:** `Error`, `Err`
- **Tests:** 2/3 matched

### 7. uri.port

- **Target:** `uri.Port [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 1
- **Priority Score:** 1061510.0
- **Functions:** 8/14 matched (target 11)
- **Missing functions:** `fmt`, `fmt`, `from`, `eq`, `eq`, `partialeq_port_different_reprs`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Tests:** 3/4 matched

### 8. extensions

- **Target:** `http.Extensions`
- **Similarity:** 0.07
- **Dependents:** 1
- **Priority Score:** 1053009.2
- **Functions:** 21/24 matched (target 30)
- **Missing functions:** `write`, `write_u64`, `finish`
- **Types:** 4/6 matched (target 5)
- **Missing types:** `AnyMap`, `IdHasher`
- **Tests:** 1/1 matched

### 9. status

- **Target:** `http.Status [STUB]`
- **Similarity:** 0.00
- **Dependents:** 1
- **Priority Score:** 1052810.0
- **Functions:** 21/24 matched (target 30)
- **Missing functions:** `eq`, `from`, `try_from`
- **Types:** 2/4 matched (target 2)
- **Missing types:** `Err`, `Error`

### 10. uri.builder

- **Target:** `uri.Builder`
- **Similarity:** 0.22
- **Dependents:** 1
- **Priority Score:** 1021307.8
- **Functions:** 10/12 matched (target 13)
- **Missing functions:** `default`, `build_from_string_ref`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Tests:** 3/4 matched

### 11. uri.path

- **Target:** `uri.PathAndQuery`
- **Similarity:** 0.16
- **Dependents:** 0
- **Priority Score:** 255408.4
- **Functions:** 28/50 matched (target 35)
- **Missing functions:** `from_maybe_shared`, `try_from`, `try_from`, `try_from`, `fmt`, `fmt`, `hash`, `eq`, `eq`, `eq`, `eq`, `eq`, `eq`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `partial_cmp`, `compares_with_a_string`, `pq`
- **Types:** 1/4 matched (target 2)
- **Missing types:** `PathAndQueryError`, `Error`, `Err`
- **Tests:** 15/17 matched

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

