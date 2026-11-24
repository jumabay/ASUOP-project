# DT1 T1 CODE — snapshotTime additive

Дата: **2026-11-24**. APPROVALS: `DT1 time contract CODE (snapshotTime additive) / T1`.

Merged on `main`: **`5fb1878`** (PR #35). Итоговый отчёт: [DT1_T1_COMPLETION_REPORT.md](DT1_T1_COMPLETION_REPORT.md).

## Delivered

- Domain/API: `snapshotTime`; `dislocationTime` deprecated alias (= snapshot).
- `dbtc-ro`: source `GETTING_DATE`; synthetic: fixture line clock → `snapshotTime`.
- UI «Время среза» → `sliceTimeDisplay` (prefer snapshot, fallback dislocation; no Date parse).
- No `locationTime`; DT1 / R1-Q-TZ remain OPEN.
- Docs: [55](../architecture/55_DT1_TIME_CONTRACT.md) T2 clarified (SPEC ≠ column mapping).

## Out of scope

currentState, registry/watched, import, live Oracle, T2, auto-merge.
