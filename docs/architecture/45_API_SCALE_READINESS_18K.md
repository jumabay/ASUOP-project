# 45. API scale readiness (~18 000+ wagons) — synthetic vs JDBC

Дата: **2026-11-20**. Offline assessment.  
Связь: [16_TESTING_STRATEGY.md](16_TESTING_STRATEGY.md), [40](40_P4_0_RAILDB_STORAGE_CONTRACTS.md), [41](41_P4_0_R1_R3_FIELD_MAPPING.md).

**Не** открывает JDBC / Create RAILDB / смену REST-контрактов.

---

## 1. Executive verdict

| Layer | Contract ready for ~18k? | Load evidence? |
| --- | --- | --- |
| REST + validation R1–R3 | **YES** (page/size/sort/day caps) | N/A |
| Ports | **YES** (page-oriented) | N/A |
| Synthetic adapters | **PARTIAL** (semantics OK) | **NO** — fixtures 250/60 |
| JDBC + indexes | **NOT STARTED** | **BLOCKED** (APPROVALS) |

**Bottom line:** API *shape* is scale-safe. Synthetic runtime is a demo, not an 18k proof.

---

## 2. Contract facts (code)

| Endpoint | Limits | Notes |
| --- | --- | --- |
| R2 `GET /api/v1/wagons` | page≥0, size 1…200, sort `code,asc\|desc`, search contains | Page DTO: content + totals |
| R3 `GET /api/v1/wagons/watched` | same | Membership ≠ legacy (G31 OPEN) |
| R1 card | 404 `WAGON_NOT_FOUND` | |
| R1 day | date required; **500** lines → **422** | No silent truncate |

Errors: `INVALID_PAGE/SIZE/SORT/DATE`, `MISSING_DATE`, `RESULT_LIMIT_EXCEEDED`.

---

## 3. Synthetic today

| Fixture | Count |
| --- | --- |
| `catalog.json` | **250** |
| `watched.json` | **60** |
| Rich card / day files | essentially **1** wagon |

Algorithm: in-memory filter → sort → `subList`. Fine for CI; **not** Oracle proof. No 18k perf suite. List path must **not** scan dislocation tape ([16](16_TESTING_STRATEGY.md)).

---

## 4. Gaps before JDBC

| ID | Gap | Needs |
| --- | --- | --- |
| S-SEARCH | contains vs LIKE/prefix | Spec + index plan |
| S-OFFSET | deep OFFSET cost | Monitor; keyset = API change |
| IDX-* | wagon_code / (wagon_id, dislocation_ts) | V0/V0b + DBA |
| G31 | watched source | Business pick |
| TIMEOUT | statement timeout | APPROVED TEST config |

---

## 5. Recommendations

### Offline (no JDBC APPROVALS; needs CODE APPROVED for fixtures)

1. Publish this doc.
2. Expand synthetic catalog to **≥18 000** codes + paging/sort tests.
3. Optional: shared `SyntheticCatalog` bean; day fixture with 501 lines → 422; soft CI timing budget for in-memory page0.

### Needs APPROVALS

ojdbc DataSource; enable `JdbcParityIT`; Create/Flyway/indexes; DBTC read / RAILDB write; breaking REST changes; G31 closure.

**Backend recommendation:** docs `45` now; synthetic-18k mini-slice after CODE APPROVED; JDBC dark until TEST store APPROVED.
