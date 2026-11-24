# P5 Wave 1 — completion report

Дата: **2026-11-24**.  
Итоговый `main`: **`68564a56f0fcfed48e5c7b30a5e89949022c464b`**  
(`Merge pull request #24 from jumabay/feature/p5-d-watched-source`)

## Merged PRs

| PR | Slice | Merge commit on main |
| --- | --- | --- |
| [#21](https://github.com/jumabay/ASUOP-project/pull/21) | APPROVALS A/B/D | `3a0c7e0` |
| [#22](https://github.com/jumabay/ASUOP-project/pull/22) | P5-A shell / search / URL | `6614b8d` |
| [#23](https://github.com/jumabay/ASUOP-project/pull/23) | P5-B day UX / retry | `cb3ad17` |
| [#24](https://github.com/jumabay/ASUOP-project/pull/24) | P5-D watched FIXTURE honesty | `68564a5` |

Merge style: GitHub **merge commits** (не squash) для #22–#24.

## Delivered behavior (synthetic/offline)

| Slice | Verified |
| --- | --- |
| **A** | AppShell + global search → card only; list URL `search/page/size/sort`; back/forward |
| **B** | Day loading/empty/error + retry; `N / max 500`; list retry; no day API semantics change |
| **D** | `membershipSource=FIXTURE` (demo 60 ≠ Active); RAIL stub `EMPTY_PENDING_G31` empty; banner + card tag honesty |

## Post-merge verification @ `68564a5` (local, 2026-11-24)

| Suite | Result |
| --- | --- |
| Gradle smoke + watched (`SyntheticRuntimeSmokeTest`, `*Watched*`, `*WagonController*`, RailDb/NoOracle, fleet/oracle watched) | **PASS** (JDK 21 via `scripts/gradle-jdk21.ps1`) |
| Vitest | **37/37** |
| `npm run build` | **OK** |
| Playwright T0 `synthetic-smoke` + `p5a-shell` + `p5b-day` + `p5d-watched` | **18/18** (API `:18095`, process stopped after) |
| Live API spot-check | `membershipSource=FIXTURE`, card `SYN-00001`, day `2024-01-15` → 3 lines |

## CI on main

| Run | SHA | Status (at report time) |
| --- | --- | --- |
| Merge #24 | `68564a5` | **in_progress** → https://github.com/jumabay/ASUOP-project/actions/runs/45316829501 |
| Merge #23 | `cb3ad17` | **success** |
| Merge #22 | `6614b8d` | **success** |

## Open evidence (unchanged)

| Item | Status |
| --- | --- |
| **G31** | **OPEN** — watched ≠ proven `GetActiveWagonList` |
| **NK1** | **BLOCKED** — no new DBA tape evidence |
| **DT1** | **BLOCKED** — no new date-axis evidence |
| Create RAILDB / JDBC / Flyway / live FTP·RWL / writes | **DENIED** |
| P5-C / E / F / G / H | **DENIED UNTIL APPROVED** |

## Residual risks

- Synthetic green ≠ legacy Oracle parity.
- Watched FIXTURE banner must not be read as company Active membership.
- Vitest cold start remains slow (~1–2 min) on this machine; no flake in this post-merge run.
