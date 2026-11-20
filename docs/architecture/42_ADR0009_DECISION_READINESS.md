# 42. ADR-0009 Decision Readiness Pack

Дата: **2026-11-20**. Gate: PR #6 (P4-0) merged; Decision Readiness PR #7 merged.  
Статус ADR-0009: **ACCEPTED — architecture direction only** (владелец, 2026-11-20). Create/Flyway/JDBC/migrate по-прежнему **DENIED**.  
Источники: [ADR-0009](decisions/ADR-0009-raildb-primary-store.md), [24](24_RAILDB_DBA_INSTRUCTION.md)–[31](31_P3A_FIELD_MAPPING_MATRIX.md), [39](39_P3C1_SEMANTICS_FINDINGS.md)–[41](41_P4_0_R1_R3_FIELD_MAPPING.md), [43](43_ADR0009_DBA_CREATE_CHECKLIST.md), V0/V0b, [APPROVALS_TEMPLATE.md](APPROVALS_TEMPLATE.md).  
Аудиты: Backend / Database / QA (чат ASUOP Manager, 2026-11-20).

**После Accept direction по-прежнему запрещено без новых строк:** Create RAILDB; JDBC; Flyway apply; миграция данных; Production/TEST connect; закрытие NK1/DT1/G31/R1-Q-TZ без evidence.

---

## 1. Executive verdict

| Вопрос | Ответ |
| --- | --- |
| Готов ли пакет для **человеческого** решения Accept / defer? | **Да** — Accept direction **зафиксирован** владельцем 2026-11-20 |
| Готовы ли Create / Flyway / JDBC / migrate? | **Нет** — **BLOCKED** (DENIED + DBA checklist [43](43_ADR0009_DBA_CREATE_CHECKLIST.md)) |
| Принятый вариант | Accept direction; topology **A** preferred; charset **AL32UTF8** preferred; DDL **V0∥V0b**; 12м+FULL plan; watched empty until G31; rollback ≤72h policy |

---

## 2. Classification board

### READY FOR APPROVAL (владелец продукта)

| # | Решение |
| --- | --- |
| R1 | **Accept ADR-0009**: `RAILDB` primary store; `RAIL_APP` внутри неё; `DBTC` = migrate + archive |
| R2 | Логический ER + P4-0 read contracts R1–R3 (docs 27/40/41; drafts only) |
| R3 | План миграции: 12м history + FULL/OPEN/REF + BOOTSTRAP (25/29); pre-cutoff → ARCHIVE_DBTC |
| R4 | Правила: retain tape duplicates; day axis = GETTING until DT1; open repairs = `CURRENTREPAIR_ID` (RP1) |
| R5 | Концепт rollback ≤~72h + quarantine replay (ADR-0009 §) — как **политика**, не как proven drill |
| R6 | Reconciliation checklist (counts / current / open repairs / stock / FK / kit UK) как **план** |

### REQUIRES DBA CONFIRMATION

| # | Решение |
| --- | --- |
| D1 | Topology **A** (SID) vs **B** (PDB) + exact service name |
| D2 | Host capacity `TC-ASUVP-03`; sizing ≥~300 GiB data + FRA; RMAN |
| D3 | Charset (prefer `AL32UTF8` vs DBTC `CL8MSWIN1251` compat) |
| D4 | **Partitioning licensed?** → materialize **V0** vs **V0b** |
| D5 | Account model: `RAIL_APP` runtime vs `RAIL_MIGRATE`; secrets out of Git |
| D6 | Tape access plan + optional EXPLAIN for **NK1/DT1**; package SPEC for **G31** (doc 38) |
| D7 | Tape batch idempotency: UNIQUE/staging on `provenance_key` vs current INDEX-only (уточнить до migrator) |
| D8 | Quarantine retention / rollback drill evidence before cutover |

### BLOCKED (отдельные APPROVALS + факты)

| # | Item |
| --- | --- |
| B1 | Create Oracle RAILDB |
| B2 | Flyway apply business DDL |
| B3 | JDBC connect / enable DataSource / ojdbc on app path |
| B4 | DBTC read for migration; RAILDB write |
| B5 | Production access; live FTP/RWL |
| B6 | **NK1** / **DT1** closure (no DBA tape plan) |
| B7 | Cutover execution without drill evidence |

### DEFERRED

| # | Item |
| --- | --- |
| F1 | **G31** watched source → M8 / R3 legacy parity claim |
| F2 | **R1-Q-TZ** store/filter policy |
| F3 | Live RWL as primary intake (needs B1) |
| F4 | Expedition / finance / 1C |
| F5 | Card fields from `rail_wagon_current` (not on `WagonCard` today) |
| F6 | Search predicate / `reportId` mapping (PROVISIONAL in 41) |

---

## 3. Recommended architecture (for owner)

1. **Store:** standalone Oracle **`RAILDB`** + schema **`RAIL_APP`**. Do **not** use RAC schema `RAIL_APP` (5 GiB) as prod store.  
2. **Topology:** prefer **A** (isolated SID/service); accept **B** (PDB) if DBA mandates — record service name in APPROVALS before Create.  
3. **DDL:** identical logical model; physical **V0** if Partitioning licensed, else **V0b** heap + btree `(wagon_id, dislocation_ts)`.  
4. **Java:** keep modular monolith, ports R1–R3, synthetic default until APPROVED TEST JDBC; fail-fast `raildb-test` stubs remain until real adapters.  
5. **Migration:** 12m on GETTING axis; FULL wagons/current/stock; OPEN repairs via CURRENT; REF_FULL; bootstrap ≤ cutoff; watched empty until G31 pick.  
6. **Legacy:** `DBTC` / `RAC.WORLD` unchanged by this project; archive after cutover.  
7. **Intake:** RWL not assumed FTP replacement.

---

## 4. Backend readiness (JDBC future — not implemented)

| Port | API | Verdict |
| --- | --- | --- |
| `WagonCardPort` | `findByCode` | **READY** for adapter |
| `WagonRegistryPort` | `findPage(criteria)` page 1…200 | **READY** |
| `StateReportDayPort` | `findLinesForDay` + query 500-cap | **READY** (filter contract) |
| `WatchedWagonPort` | same criteria | **DEFERRED** parity (G31) |

**Connect blockers (by design until APPROVED):** no ojdbc on `platform-oracle`/`platform-app`; `DataSourceAutoConfiguration` excluded; stubs throw `RailDbJdbcDenied`; no TEST URL; ADR Accept DENIED.

**After APPROVED TEST RAILDB (list only):** ojdbc10+orai18n + spring-jdbc; profile-scoped DataSource; replace stubs; bind/page SQL from `sql/raildb/read/`; bounded `JdbcParityIT` on TEST only.

---

## 5. Data model summary

### Core tables (R1–R3)

| Table | Keys | Notes |
| --- | --- | --- |
| `rail_station` | UK `station_code` | REF |
| `rail_wagon` | UK `wagon_code` | FULL migrate |
| `rail_wagon_current` | PK = wagon | Not day tape |
| `rail_disl_event` | Tech PK; **no natural UK** | Retain dups; getting axis |
| `rail_wagon_watched` | PK wagon | Empty until G31 |

### Dislocation

- Full hourly snapshots retained (not delta-only).  
- `provenance_key` + `content_hash`; kit nullable.  
- Day: half-open `[D,D+1)` on `dislocation_ts` until DT1 says otherwise.

### 12m + active objects

| Tag | Rule |
| --- | --- |
| REF_FULL | Stations/types/… for FK |
| FULL | Wagons, current, stock, parts installs |
| OPEN | Repairs via CURRENT pointer (RP1) |
| 12M | Tape/kits/closed repairs/WH docs by date |
| BOOTSTRAP | Last getting_ts ≤ cutoff per wagon |
| ARCHIVE | Pre-cutoff tape stays in DBTC |

Sizing order: ~70–100 GiB tape+index / 12m; plan ≥300 GiB + FRA (doc 24).

---

## 6. NK1 / DT1 / G31 / R1-Q-TZ

| ID | Status | Hold policy until closed |
| --- | --- | --- |
| **NK1** | **BLOCKED** | No UK; retain all rows; no DISTINCT in JDBC |
| **DT1** | **BLOCKED** | Filter/migrate on GETTING / `dislocation_ts` |
| **G31** | **OPEN** | Watched table empty; no R3=Active claim |
| **R1-Q-TZ** | **OPEN** | Document naive day bounds; decide UTC wall vs local later |

Do **not** invent closures.

---

## 7. V0 vs V0b

| | V0 | V0b |
| --- | --- | --- |
| Physical | RANGE partition + LOCAL indexes | Heap + global btree |
| License | Partitioning option | None |
| Recommendation | If licensed | Fallback |

Logical columns/semantics identical. Choice at **Flyway** gate after DBA license answer.

---

## 8. Migration safety (QA)

| Topic | Finding |
| --- | --- |
| Data loss &lt; cutoff | Intentional if DBTC retained — **READY** as policy |
| Premature DBTC drop | **BLOCKED** until retention sign-off |
| Idempotency kits/intake | UK present — sound |
| Idempotency tape | INDEX on `provenance_key` only — **REQUIRES DBA** clarify before migrator |
| Reconciliation | Plan OK; probes need DBA |
| Rollback | Concept OK; drill/RMAN/quarantine **REQUIRES DBA/ops** |

---

## 9. Owner decision questions

1. **Accept ADR-0009?** (supersede sole-DBTC target in 05/06/15)  
2. Topology **A or B** + service name?  
3. **Charset** preference?  
4. **Partitioning licensed?** → V0 or V0b  
5. Authorize **Create empty RAILDB** (capacity/RMAN from 24)?  
6. Authorize **TEST** URL + RO (later write) — separate from Production?  
7. Authorize **DBTC read** + **RAILDB write** for migrator?  
8. **G31** source: Active vs `wagons.txt` vs other?  
9. **R1-Q-TZ** policy?  
10. Accept **≤72h rollback** + quarantine replay as ops policy?

Suggested sequence after Accept: DBA D1–D5 → Create (B1) → Flyway V0/V0b (B2) → TEST RO JDBC card/registry/day (B3) → tape plan NK1/DT1 → migrator approvals → G31 → cutover drill.

---

## 10. Agent summaries

| Agent | Verdict |
| --- | --- |
| Backend | Ports READY (watched DEFERRED); connect BLOCKED until Accept+TEST+adapters |
| Database | Recommend Accept + A + V0/V0b conditional; open items not invented closed |
| QA | **PASS** for human decision; Create/migrate not authorized; tape UNIQUE gap noted |

---

## 11. Stop line

Decision Readiness выполнен; Accept **direction** зафиксирован в ADR-0009 + APPROVALS.  
**Следующий gate:** DBA заполняет [43](43_ADR0009_DBA_CREATE_CHECKLIST.md) → владелец APPROVED Create (отдельно).  
Никакой Oracle/DDL/JDBC/миграции из пакета Accept direction.
