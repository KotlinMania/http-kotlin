# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 8/21 (38.1%)
- **Function parity:** 72/633 matched (target 132) — 11.4%
- **Class/type parity:** 19/99 matched (target 33) — 19.2%
- **Combined symbol parity:** 91/732 matched (target 165) — 12.4%
- **Average inline-code cosine:** 0.43 (function body across 5 matched files)
- **Average documentation cosine:** 0.85 (doc text across 5 matched files)
- **Cheat-zeroed Files:** 4
- **Critical Issues:** 7 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. byte_str

- **Target:** `http.ByteStr`
- **Similarity:** 0.59
- **Dependents:** 5
- **Priority Score:** 5010804.0
- **Functions:** 6/6 matched (target 12)
- **Missing functions:** _none_
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Target`

### 2. method

- **Target:** `http.Method`
- **Similarity:** 0.55
- **Dependents:** 3
- **Priority Score:** 3022604.5
- **Functions:** 19/19 matched (target 36)
- **Missing functions:** _none_
- **Types:** 5/7 matched (target 17)
- **Missing types:** `Error`, `Err`
- **Tests:** 5/5 matched

### 3. version

- **Target:** `http.Version`
- **Similarity:** 0.62
- **Dependents:** 3
- **Priority Score:** 3000403.8
- **Functions:** 2/2 matched (target 6)
- **Missing functions:** _none_
- **Types:** 2/2 matched
- **Missing types:** _none_

### 4. extensions

- **Target:** `http.Extensions`
- **Similarity:** 0.38
- **Dependents:** 1
- **Priority Score:** 1042906.2
- **Functions:** 20/23 matched (target 30)
- **Missing functions:** `write`, `write_u64`, `finish`
- **Types:** 5/6 matched
- **Missing types:** `IdHasher`
- **Tests:** 1/1 matched

### 5. uri.port

- **Target:** `uri.Port [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 1
- **Priority Score:** 1031210.0
- **Functions:** 8/11 matched
- **Missing functions:** `fmt`, `from`, `partialeq_port_different_reprs`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Tests:** 3/4 matched

### 6. status

- **Target:** `http.Status [STUB]`
- **Similarity:** 0.00
- **Dependents:** 1
- **Priority Score:** 1022110.0
- **Functions:** 17/17 matched (target 30)
- **Missing functions:** _none_
- **Types:** 2/4 matched (target 2)
- **Missing types:** `Err`, `Error`

### 7. uri.mod

- **Target:** `uri.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 293210.0
- **Functions:** 0/25 matched (target 7)
- **Missing functions:** `builder`, `from_parts`, `from_maybe_shared`, `from_shared`, `from_static`, `into_parts`, `path_and_query`, `path`, `scheme`, `scheme_str`, `authority`, `host`, `port`, `port_u16`, `query`, `has_path`, `try_from`, `from`, `parse_full`, `from_str`, `eq`, `default`, `fmt`, `s`, `hash`
- **Types:** 3/7 matched (target 3)
- **Missing types:** `Uri`, `Parts`, `Error`, `Err`

### 8. header.mod

- **Target:** `header.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

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
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |

