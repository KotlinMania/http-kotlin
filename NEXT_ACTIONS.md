# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 3/21 (14.3%)
- **Function parity:** 34/614 matched (target 64) — 5.5%
- **Class/type parity:** 9/92 matched (target 21) — 9.8%
- **Combined symbol parity:** 43/706 matched (target 85) — 6.1%
- **Average inline-code cosine:** 0.42 (function body across 3 matched files)
- **Average documentation cosine:** 0.97 (doc text across 3 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 3 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. method

- **Target:** `http.Method`
- **Similarity:** 0.53
- **Dependents:** 3
- **Priority Score:** 3022604.8
- **Functions:** 19/19 matched (target 34)
- **Missing functions:** _none_
- **Types:** 5/7 matched (target 17)
- **Missing types:** `Error`, `Err`
- **Tests:** 5/5 matched

### 2. version

- **Target:** `http.Version`
- **Similarity:** 0.39
- **Dependents:** 3
- **Priority Score:** 3010406.0
- **Functions:** 1/2 matched (target 5)
- **Missing functions:** `fmt`
- **Types:** 2/2 matched
- **Missing types:** _none_

### 3. status

- **Target:** `http.Status`
- **Similarity:** 0.35
- **Dependents:** 1
- **Priority Score:** 1052106.5
- **Functions:** 14/17 matched (target 25)
- **Missing functions:** `fmt`, `eq`, `from_str`
- **Types:** 2/4 matched (target 2)
- **Missing types:** `Err`, `Error`

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

