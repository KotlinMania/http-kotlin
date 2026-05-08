# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 5/21 (23.8%)
- **Function parity:** 63/611 matched (target 112) — 10.3%
- **Class/type parity:** 19/92 matched (target 32) — 20.7%
- **Combined symbol parity:** 82/703 matched (target 144) — 11.7%
- **Average inline-code cosine:** 0.32 (function body across 5 matched files)
- **Average documentation cosine:** 0.94 (doc text across 5 matched files)
- **Cheat-zeroed Files:** 2
- **Critical Issues:** 4 files with <0.60 function similarity

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

- **Target:** `http.Method [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 3
- **Priority Score:** 3002610.0
- **Functions:** 19/19 matched (target 35)
- **Missing functions:** _none_
- **Types:** 7/7 matched (target 19)
- **Missing types:** _none_
- **Tests:** 5/5 matched

### 3. version

- **Target:** `http.Version`
- **Similarity:** 0.61
- **Dependents:** 3
- **Priority Score:** 3000404.0
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

### 5. status

- **Target:** `http.Status [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 1
- **Priority Score:** 1012110.0
- **Functions:** 16/17 matched (target 29)
- **Missing functions:** `new`
- **Types:** 4/4 matched
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Next Commands

```bash
# Initialize task queue for systematic porting
cd tools/ast_distance
./ast_distance --init-tasks ../../tmp/http/src rust ../../src/commonMain/kotlin/io/github/kotlinmania/http kotlin tasks.json ../../AGENTS.md

# Get next high-priority task
./ast_distance --assign tasks.json <agent-id>
```
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

