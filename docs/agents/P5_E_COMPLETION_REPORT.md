# P5-E — итоговый отчёт (CONTROL JDBC A1/A2)

Дата: **2026-11-24**.  
Итоговый `main`: **`ad1b8e722c3f4a3afc5ed9f34706958e210943c0`**  
(`Merge pull request #32 from jumabay/feature/p5-e-jdbc-a1-a2-control`)

Scope: time-boxed RO read на legacy `tc-asuvp-03` / `RAC.WORLD` / `Dbtc` для CONTROL  
`54787361` / day `2026-11-24`; opt-in profile **`dbtc-ro`**.  
Default runtime profile остаётся **`synthetic`**. Auto-merge не использовался.

Evidence: [P5_E_RO_SMOKE_EVIDENCE.md](P5_E_RO_SMOKE_EVIDENCE.md),  
[P5_E_MAPPING_FOLLOWUP.md](P5_E_MAPPING_FOLLOWUP.md),  
[P5_E_JDBC_CONTROL_EVIDENCE.md](P5_E_JDBC_CONTROL_EVIDENCE.md).  
Plan: [ASUOP_P5_E_JDBC_READ_PLAN.md](ASUOP_P5_E_JDBC_READ_PLAN.md).

---

## Merged PR

| PR | Содержание | Merge on main |
| --- | --- | --- |
| [#32](https://github.com/jumabay/ASUOP-project/pull/32) | docs + `dbtc-ro` A1/A2 JDBC, honesty runtime, «Время среза», list stubs | `ad1b8e7` |

---

## Verdict matrix

| Item | Status |
| --- | --- |
| CONTROL A1/A2 Oracle → API → UI | **VERIFIED** (live once under APPROVALS; no re-run for this close-out) |
| Default profile | **`synthetic`** (no Oracle DataSource) |
| `inventoryPark` general semantics | **OPEN** (only CONTROL raw `1 → true` confirmed; other raw → unknown/`null`) |
| DT1 / factual dislocation time | **OPEN** |
| API time contract (dual timestamps / rename) | **OPEN** (unchanged; UI label «Время среза» for day `GETTING_DATE` axis) |
| `currentState` via JDBC | **not implemented** this stage |
| registry / watched JDBC | **not implemented** this stage (stubs → «Источник не подключён») |

---

## Delivered (constraints)

- Profile `dbtc-ro`: A2 passport joins + A1 day lines on `GETTING_DATE`; password from env only.
- Safe honesty: `GET /api/v1/platform/runtime` (`displayLabel`, list connectivity) — no JDBC URL / credentials.
- `operationCode`: `IDCODE` preferred, string fallback to `CODE` (leading zeros).
- UI day column: **«Время среза»** (not claimed fact location time).

---

## Post-merge offline regression @ `ad1b8e7` (local, 2026-11-24)

| Suite | Result |
| --- | --- |
| Gradle `test --rerun-tasks` (JDK 21 via `scripts/gradle-jdk21.ps1`) | **BUILD SUCCESSFUL** |
| Vitest | **46/46** (9 files) |
| `npm run build` | **OK** |
| Playwright full (`p5a`/`p5b`/`p5d`/`scale-smoke`/`synthetic-smoke`) | **19/19** (synthetic API `:8080`) |

Live Oracle re-read: **not performed** (not required).

---

## CI on main

| Run | SHA | Status |
| --- | --- | --- |
| Merge #32 | `ad1b8e7` | **success** — https://github.com/jumabay/ASUOP-project/actions/runs/45354419364 |

---

## Open / out of stage

| Item | Note |
| --- | --- |
| DT1 | `LOCATION_DATE` vs `GETTING_DATE`; fact dislocation not claimed |
| inventoryPark | global 0/1/other semantics beyond CONTROL |
| G31 / registry-watched JDBC | lists remain disconnected under `dbtc-ro` |
| currentState | out of P5-E allow-list |
| APPROVALS | time-boxed RO through `2026-11-25 18:00 +05:00` — journal **not** expanded in this close-out |
