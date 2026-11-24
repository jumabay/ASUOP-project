# DT1 T1 — completion report (`snapshotTime` additive)

Дата: **2026-11-24**.  
Итоговый `main`: **`5fb187828d10bf8d1293800ed9c1c699de83979a`**  
(`Merge pull request #35 from jumabay/feature/dt1-t1-snapshot-time`)

Scope: **T1 only** per [55_DT1_TIME_CONTRACT.md](../architecture/55_DT1_TIME_CONTRACT.md).  
Live Oracle re-run **не** требовался и **не** выполнялся. T2 / `locationTime` / DT1-close / R1-Q-TZ-close — **вне** этого отчёта.

## Merged / superseded

| PR | Role | State |
| --- | --- | --- |
| [#35](https://github.com/jumabay/ASUOP-project/pull/35) | T1 CODE + docs cherry-pick | **MERGED** → `5fb1878` |
| [#34](https://github.com/jumabay/ASUOP-project/pull/34) | docs-only contract (superseded by #35) | **CLOSED** (superseded) |

Auto-merge не использовался.

## Delivered (T1)

| Item | Behavior |
| --- | --- |
| API A1 line | additive `snapshotTime` (slice / `GETTING_DATE` axis) |
| Alias | `dislocationTime` deprecated; **always equal** to `snapshotTime` |
| UI «Время среза» | `sliceTimeDisplay`: prefer `snapshotTime`, fallback `dislocationTime`; raw ISO, **no** `Date`/TZ shift |
| Absent | `locationTime`, confidence, «факт» column |
| Adapters | synthetic fixture clock → `snapshotTime`; `dbtc-ro` ← `GETTING_DATE` (existing allow-list, no expand) |

## Confirmation checklist (post-merge, offline)

| Check | Result | Evidence |
| --- | --- | --- |
| `snapshotTime` present | **OK** | Live synthetic `GET :8081/api/v1/wagons/SYN-00001/state-reports?date=2024-01-15` — 3 lines; MockMvc / integration tests |
| `dislocationTime` == `snapshotTime` | **OK** | Same REST sample; `SyntheticWagonApiIntegrationTest.dayLineSnapshotTimeEqualsDeprecatedDislocationTimeAlias` |
| UI without TZ shift | **OK** | `frontend/src/sliceTime.test.ts` — preserves naive/fractional ISO |
| Fallback old backend | **OK** | Vitest `falls back to dislocationTime when snapshotTime missing`; e2e path in `p5b-day.spec.ts` |
| `locationTime` absent | **OK** | Live JSON `has_locationTime_key=False`; MockMvc `doesNotExist()` |

## Post-merge verification @ `5fb1878`

### Local (Independent QA Test Runner, 2026-11-24)

| Suite | Result |
| --- | --- |
| Gradle `test` (JDK 21 via `scripts/gradle-jdk21.ps1`) | **PASS** — 142 tests, 0 fail, 1 skipped (`JdbcParityIT` `@Disabled`) |
| Vitest | **50/50** |
| `npm run build` | **OK** |
| Playwright full (`p5a`/`p5b`/`p5d`/`scale-smoke`/`synthetic-smoke`) | **19/19** (`API_PROXY_TARGET` → `:8081`) |

### CI on main

| Run | SHA | Status |
| --- | --- | --- |
| Merge #35 | `5fb1878` | **success** — https://github.com/jumabay/ASUOP-project/actions/runs/45359344946 (`unit-tests`, `frontend-unit`, `playwright-smoke`) |

## Open (unchanged — not closed by T1)

| Item | Status |
| --- | --- |
| **DT1** (full date-axis / fact semantics) | **OPEN** |
| **R1-Q-TZ** | **OPEN** |
| **T2** `locationTime` as fact | **DENIED** until BODY or file↔Oracle |
| Create RAILDB / new live Oracle / Flyway / FTP·RWL writes | **DENIED** (unchanged) |

## T2 readiness note (research only — not implemented)

**Verdict: cannot prove** `disl_kz` field[3] → `LOCATION_DATE` **without new evidence** (BODY and/or paired file↔Oracle). Live re-run not claimed as required if artifacts appear offline.

| Material | Status |
| --- | --- |
| Industrial `disl_kz*.txt` in ASUVP workspace | **Absent** (docs: G1 / import pipeline) |
| Synthetic parser fixture | Not industrial; not Oracle-paired |
| Known `REPORT_ID` samples (P5-E) | `588495` / `588498` / `588503` for wagon `54787361` day `2026-11-24` — **no** paired source filename/kit |
| SPEC / `ALL_ARGUMENTS` `SaveStateReportLineKz` | Present (signature only) — **≠** column mapping |
| BODY `SaveStateReportLineKz` | **Absent** |

### Missing evidence only (for T2 mapping proof)

1. **DT1-B2:** BODY (or export) of `ImportPkg.SaveStateReportLineKz` showing param → `LOCATION_DATE`.  
2. **DT1-E:** one real `disl_kz` + matching kit/`REPORT_ID` + that row’s `LOCATION_DATE` for field[3] compare.

(Business label «факт дислокации» remains a separate gate after mapping proof. APPROVALS not expanded in this report.)

## Residual risks

- T1 green ≠ legacy fact-time parity; DT1 stays OPEN.  
- Clients must keep treating `dislocationTime` as **slice**, not location fact.  
- Adjacent LMS `disl_kz` files (if any outside repo) are **not** ASUVP evidence until paired with kit/`LOCATION_DATE` export.

## Next

1. Do **not** start T2 CODE without new APPROVALS after DT1-B2 or DT1-E.  
2. Keep DT1 and R1-Q-TZ **OPEN**.  
3. No auto-merge; human merge only for follow-up docs/CODE PRs.
