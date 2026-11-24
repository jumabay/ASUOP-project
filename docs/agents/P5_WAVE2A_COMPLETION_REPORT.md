# P5 Wave 2A — completion report

Дата: **2026-11-24**.  
Итоговый `main`: **`3f1739c87ed4de6faac2b9acd92046a9392a44ac`**  
(`Merge pull request #29 from jumabay/feature/p5-g-multi-filters`)

Wave 2A = **P5-C** (registry passport + card `currentState`) + **P5-G** (multi-field `search`/`owner`/`kind` AND filters).  
Scope: **synthetic/offline only**. E/F/H, Oracle JDBC, live FTP/RWL, Create RAILDB — **не** запускались.

## Merged PRs

| PR | Slice | Merge commit on main |
| --- | --- | --- |
| [#27](https://github.com/jumabay/ASUOP-project/pull/27) | APPROVALS C/G + [54_P5_G_FILTER_CONTRACT.md](../architecture/54_P5_G_FILTER_CONTRACT.md) | `76f6ecf` |
| [#28](https://github.com/jumabay/ASUOP-project/pull/28) | P5-C registry enrichment + optional `currentState` (+ e2e scale-smoke cell-count fix `f274005`) | `7ce05eb` |
| [#29](https://github.com/jumabay/ASUOP-project/pull/29) | P5-G multi-field filters (stacked on C, then rebased to `main`) | `3f1739c` |

Merge style: GitHub **merge commits** (не squash). Auto-merge не использовался.

## Delivered contracts (synthetic)

| Slice | Contract / behavior |
| --- | --- |
| **C** | Registry additive: `model`, `wagonKind`, `ownerName`, `homeStationCode`, `homeStationName`, `inventoryPark`. Card optional `currentState` (`stationCode`, `operationCode`, `dislocationTime`, `trainIndex`) from **separate** `*.current.json` — **not** derived from day lines. Unknown → UI «—». |
| **G** | Per [54](../architecture/54_P5_G_FILTER_CONTRACT.md): `search` = contains on `wagonCode`; `owner` = contains on `ownerName`; `kind` = equals on `wagonKind`; combination **AND**; same params on watched; sort/paging unchanged; FE deep-link `owner`/`kind` with list URL. **No** Oracle LIKE parity claim. |

## Post-merge verification @ `3f1739c` (local, 2026-11-24)

| Suite | Result |
| --- | --- |
| Gradle `test` (JDK 21 via `scripts/gradle-jdk21.ps1`) | **BUILD SUCCESSFUL** |
| Vitest | **44/44** (8 files) |
| `npm run build` | **OK** |
| Playwright full (`p5a`/`p5b`/`p5d`/`scale-smoke`/`synthetic-smoke`) | **19/19** (API `:18091`) |

### Spot-checks (REST, synthetic)

| Check | Result |
| --- | --- |
| Registry passport `SYN-00001` | `model=12-132`, `wagonKind=КРЫТЫЙ`, `ownerName=SYN-OWNER`, home `6900`/`SYN-STATION`, `inventoryPark=true` |
| Filters `search` / `owner` / `kind` / AND / empty | `search=SYN-00001` → 1; `owner=SYN-OWNER` → 250; `kind=КРЫТЫЙ` → 250; AND → 1; `owner=NO-SUCH-OWNER` → 0 |
| Pagination | page0 first `SYN-00001`, page1 first `SYN-00051`, `totalPages=5` @ size 50 |
| URL / back-forward | Covered by Playwright `p5a-shell` (F5 URL restore + browser back) + Vitest `deepLink` (incl. `owner`/`kind`) |
| `currentState` vs day last line | current `7100`/`05`/`2024-01-16T06:00:00`/`TRN-DIFF` ≠ day-2024-01-15 last `6902`/`01`/`…23:59:59.5` |
| Empty day `2024-01-16` | 0 lines; card `currentState` unchanged |

## CI on main

| Run | SHA | Status |
| --- | --- | --- |
| Merge #29 | `3f1739c` | **success** — https://github.com/jumabay/ASUOP-project/actions/runs/45327029721 (`unit-tests`, `frontend-unit`, `playwright-smoke`) |
| Merge #28 | `7ce05eb` | **success** |

## Open evidence (unchanged — not closed by Wave 2A)

| Item | Status |
| --- | --- |
| **G31** | **OPEN** — watched ≠ proven `GetActiveWagonList` |
| **NK1** | **BLOCKED** — no new DBA tape evidence |
| **DT1** | **BLOCKED** — no new date-axis evidence |
| **R1-Q-TZ** | OPEN (day axis disclosure) |
| Create RAILDB / JDBC / Flyway / live FTP·RWL / writes | **DENIED** |
| **P5-E** JDBC TEST | **DENIED UNTIL APPROVED** |
| **P5-F** prefs / multi-card | **DENIED UNTIL APPROVED** |
| **P5-H** writes / bulk / concurrency | **DENIED UNTIL APPROVED** |

## Residual risks

- Synthetic green ≠ legacy Oracle parity; G filters must not be described as Oracle LIKE-equivalent.
- `currentState` fixture independence is proven only for synthetic `SYN-00001`; JDBC mapping remains out of scope.
- Watched FIXTURE banner / G31 OPEN remain product honesty constraints (Wave 1 / P5-D).
- Scale-smoke asserts 7 passport `gridcell`s (P5-C); keep in sync with `REGISTRY_COLUMN_FIELDS`.

## Next (require new APPROVALS)

1. Product pick for next P5 slice (**E** / **F** / other) — each needs its own APPROVED row before CODE.  
2. Parallel track: DBA evidence for NK1/DT1/G31 — docs/SQL only until separate execute/connect gates.  
3. Do **not** Accept ADR-0009 or Create RAILDB without dedicated journal rows.
