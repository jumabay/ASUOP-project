# 29. План миграции DBTC → RAILDB (12 месяцев) — P3A

Не выполнять. Cutoff = `cutover_ts − 12 months`. Связано: [25_…](25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md), [27_…](27_RAILDB_ER_MODEL.md), [ADR-0009](decisions/ADR-0009-raildb-primary-store.md).

## 1. Фазы

| Phase | Content | Gate |
| --- | --- | --- |
| M0 | Freeze inventory + P3B dictionary results | DBA SELECTs done |
| M1 | REF_FULL → `RAIL_*` refs | row counts |
| M2 | `WAGON` FULL + `WAGON_CURRENT` | sample parity |
| M3 | OPEN repairs + stock + parts | set compare |
| M4 | BOOTSTRAP last disl ≤ cutoff per wagon | coverage % |
| M5 | 12M `IMPORTFILES`/`STATEREPORTS` → kits | UK idempotent |
| M6 | 12M `STATEREPORTRECORDS` batches → `DISL_EVENT` | daily counts |
| M7 | 12M warehouse docs (when columns known) | |
| M8 | Watched snapshot | **blocked on G31** |
| M9 | Delta loop to cutover | watermarks |
| M10 | Cutover + disable DBTC dependency in app | APPROVALS |
| M11 | Archive mode DBTC (>12м stays) | ops sign-off |

## 2. Детали по требованиям

### Полный актуальный реестр
`WAGON` **FULL** → `RAIL_WAGON` + seed `RAIL_WAGON_CURRENT` from location columns (CONFIRMED names).

### 12м дислокации
Filter `STATEREPORTRECORDS.GETTING_DATE >= cutoff` (CONFIRMED index axis). Parents kits by `IMPORTDATE`/`GETTING_DATE`. Preserve kit type (0 KZ full semantics: all lines including unchanged — CONFIRMED C#).

### Состояние на начало периода
Для каждого `WAGON_ID`: max row with `GETTING_DATE <= cutoff` (or equivalent after P3B) → bootstrap event / current edge. Without this, as-of at cutoff is incomplete (**CONFIRMED risk**).

### Список слежения
Policy in matrix §2; membership **BLOCKED** until G31 source chosen. Registry migration not blocked.

### Ремонты / склад
- Closed repairs with dates in window **OR** OPEN (predicate UNKNOWN → P3B).  
- `CURRENTPARTS` / `WAREHOUSEDETAILS` FULL.  
- Document lines 12м when date column confirmed (`DELIVERYDATE` partial).

### Справочники
`STATIONS`, customers, types, operations, cargo, repair types — REF_FULL before FKs.

### Initial + delta
Watermarks in `RAIL_MIGRATION_WATERMARK`; idempotent keys; resume batches; until cutover **DBTC wins** conflicts.

### Integrity
Daily tape counts; checksum samples; orphan FK=0; kit replay; current field sample vs packages.

### Safe switch
Soak → stop new-app writes to nowhere → final delta → app `RailDbJdbc` only → keep FTP/API quarantine for rollback replay (ADR-0009). Do not mark DBTC archive until success criteria met.

## 3. OPEN research items

| ID | Topic | Status |
| --- | --- | --- |
| G31 | Watched source | BLOCKED |
| NK1 | Natural key `STATEREPORTRECORDS` | UNKNOWN (no PK) |
| DT1 | `GETTING_DATE` vs `LOCATION_DATE` | UNKNOWN business axis |
| RP1 | Open repair predicate | UNKNOWN body |
| WH1 | `WAREHOUSEDETAILS` grain + doc links | UNKNOWN |
| DUP1 | Repeat kits / immobile rows | CONFIRMED client keeps all lines; Oracle dedup UNKNOWN |

## 4. P3A vs P3B

| | P3A (this) | P3B (needs approve) |
| --- | --- | --- |
| Work | docs, draft DDL, DBA SQL scripts | run dictionary SELECTs; finalize columns/UK |
| Oracle | none | read-only probes by DBA/approved account |
