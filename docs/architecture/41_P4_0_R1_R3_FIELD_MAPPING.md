# 41. P4-0 — mapping REST / DTO / domain → RAIL_* (R1–R3)

Статус: **docs-only**. Расширяет read-path поверх миграционной матрицы [31_P3A_FIELD_MAPPING_MATRIX.md](31_P3A_FIELD_MAPPING_MATRIX.md).  
**Не** изобретает закрытую семантику для **NK1**, **DT1**, **G31**, **R1-Q-TZ**.  
ADR-0009 = **PROPOSED**. JDBC / DDL apply = **DENIED**.

Источник полей приложения: `WagonCard`, `StateReportLine`, `WagonRegistryItem`, `WatchedWagonItem` (modules fleet/dislocation). REST DTO зеркалят эти записи ([23_R1_A1_A2_IMPLEMENTATION_PLAN.md](23_R1_A1_A2_IMPLEMENTATION_PLAN.md)).

Целевые колонки — из draft [sql/raildb/V0__draft_confirmed_core.sql](sql/raildb/V0__draft_confirmed_core.sql).

Условные обозначения confidence:

| Метка | Смысл |
| --- | --- |
| **INTENDED** | Явное соответствие draft DDL ↔ текущий DTO |
| **PROVISIONAL** | Разумный join/alias; нужна проверка на живых данных после JDBC |
| **UNKNOWN / OPEN** | Нет доказательства; не закрывать |

---

## 1. R1 — карточка вагона (`WagonCard`)

`GET /api/v1/wagons/{wagonCode}`

| Domain / DTO | Intended RAIL_* | Notes | Confidence |
| --- | --- | --- | --- |
| `wagonCode` | `rail_wagon.wagon_code` | UK lookup | **INTENDED** |
| `model` | `rail_wagon.model_code` | Имя поля DTO ≠ имя колонки | **INTENDED** |
| `wagonKind` | `rail_wagon.wagon_kind` | | **INTENDED** |
| `ownerName` | `rail_wagon.owner_name` | | **INTENDED** |
| `homeStationCode` | `rail_station.station_code` via `home_station_id` | LEFT JOIN; empty string policy = app | **INTENDED** |
| `homeStationName` | `rail_station.station_name` | NULL name → UNKNOWN display policy | **PROVISIONAL** |
| `inventoryPark` | `rail_wagon.inventory_park` (`Y`/`N` → boolean) | | **INTENDED** |

### Текущее состояние (не в `WagonCard` сейчас)

Synthetic card = passport only. Целевой join для будущего блока / ops:

| Concept | Intended RAIL_* | Notes | Confidence |
| --- | --- | --- | --- |
| current station | `rail_wagon_current.station_code` | Не путать с day lines | **INTENDED** (schema) |
| current operation | `rail_wagon_current.operation_code` | | **INTENDED** |
| current dislocation time | `rail_wagon_current.dislocation_ts` | TZ policy **R1-Q-TZ OPEN** | **OPEN** |
| train index | `rail_wagon_current.train_index` | | **INTENDED** |

SQL stub: [sql/raildb/read/wagon_card_by_code.sql](sql/raildb/read/wagon_card_by_code.sql).

---

## 2. R1 — строки суток (`StateReportLine`)

`GET /api/v1/wagons/{wagonCode}/state-reports?date=YYYY-MM-DD`  
Сутки: half-open `[D, D+1)`; лимит 500 → иначе 422.

| Domain / DTO | Intended RAIL_* | Notes | Confidence |
| --- | --- | --- | --- |
| `reportId` | `rail_disl_event.legacy_report_id` | Альтернатива: derive from `kit_id` — **PROVISIONAL**; NULL orphan → **UNKNOWN** display | **PROVISIONAL** |
| `wagonCode` | `rail_disl_event.wagon_code` (denorm) или join `rail_wagon` | Prefer denorm for tape rows | **INTENDED** |
| `snapshotTime` | `rail_disl_event.dislocation_ts` / getting | A1 T1: ≡ **GETTING_DATE** / getting_ts; DTO naive `LocalDateTime` | **INTENDED** (T1); R1-Q-TZ **OPEN** |
| `dislocationTime` | same as `snapshotTime` | **Deprecated alias** — must equal `snapshotTime`; never LOCATION | **INTENDED** (T1 compat) |
| `stationCode` | `rail_disl_event.station_code` | | **INTENDED** |
| `operationCode` | `rail_disl_event.operation_code` | | **INTENDED** |

Не маппить в текущий DTO (есть в таблице, нет в REST): `train_index`, `location_ts`, `cargo_*`, `provenance_key`, `source_ordinal`, …

| Filter | Rule | Confidence |
| --- | --- | --- |
| Wagon | resolve `wagon_id` by `wagon_code` UK | **INTENDED** |
| Day axis | `dislocation_ts >= :day_from AND dislocation_ts < :day_to` | **INTENDED** pending DT1 |
| `location_ts` | **не** использовать как ось суток | **DT1 BLOCKED** |
| Duplicates | return all matching rows | **NK1 OPEN** — no UK collapse |
| Order | `dislocation_ts ASC, event_id ASC` (provisional stable) | **PROVISIONAL** |

SQL stub: [sql/raildb/read/state_report_day.sql](sql/raildb/read/state_report_day.sql).

Связь с миграцией: `STATEREPORTRECORDS` → `RAIL_DISL_EVENT` в [31](31_P3A_FIELD_MAPPING_MATRIX.md) (**PARTIAL**, NK provisional).

---

## 3. R2 — реестр (`WagonRegistryItem`)

`GET /api/v1/wagons?page&size&sort&search`  
`size` ∈ 1…200; default page 0 / size 50.

| Domain / DTO | Intended RAIL_* | Notes | Confidence |
| --- | --- | --- | --- |
| `wagonCode` (item) | `rail_wagon.wagon_code` | Единственное поле строки реестра | **INTENDED** |
| search | `UPPER(wagon_code) LIKE …` or prefix | Exact predicate TBD with JDBC; synthetic = contains | **PROVISIONAL** |
| sort `code,asc|desc` | `ORDER BY wagon_code` | | **INTENDED** |
| paging | `OFFSET :off ROWS FETCH NEXT :lim ROWS ONLY` | | **INTENDED** |
| `totalElements` | `COUNT(*)` over same filter | Separate count query | **INTENDED** |

SQL stub: [sql/raildb/read/wagon_registry_page.sql](sql/raildb/read/wagon_registry_page.sql).

Миграция: `WAGON` FULL → `RAIL_WAGON` ([31](31_P3A_FIELD_MAPPING_MATRIX.md) CONFIRMED P3B).

---

## 4. R3 — слежение (`WatchedWagonItem`)

`GET /api/v1/wagons/watched?...`  
Поля паспорта = как карточка; membership отдельна.

| Domain / DTO | Intended RAIL_* | Notes | Confidence |
| --- | --- | --- | --- |
| membership | `rail_wagon_watched.wagon_id` | Snapshot; **G31 OPEN** — источник не утверждён | **UNKNOWN / OPEN** |
| `source_code` / `captured_at_utc` | колонки watched | Нет в REST DTO | schema only |
| `wagonCode` … `inventoryPark` | те же, что R1 card via join `rail_wagon` (+ station) | Synthetic: `WatchedWagonItem.fromCard` | **INTENDED** (passport) |
| Parity vs `GetActiveWagonList` | — | **не подтверждено** (R4 / G31) | **OPEN** |
| ≠ synthetic 60 | — | Fixture ≠ migration source | documented |

SQL stub: [sql/raildb/read/watched_wagon_page.sql](sql/raildb/read/watched_wagon_page.sql).

Миграция M8: [25](25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md) / [29](29_RAILDB_MIGRATION_12M_PLAN.md) — **G31 blocked**.

---

## 5. OPEN checklist (явный)

| ID | Не закрывать в mapping |
| --- | --- |
| **NK1** | Нет natural UK → не дедуплицировать day lines |
| **DT1** | Не переключать ось суток на `location_ts` |
| **G31** | Не утверждать membership = Active / wagons.txt / document tables |
| **R1-Q-TZ** | Не фиксировать Asia/Almaty или UTC как бизнес-факт в REST |

---

## 6. Ссылки

- Contracts overview: [40_P4_0_RAILDB_STORAGE_CONTRACTS.md](40_P4_0_RAILDB_STORAGE_CONTRACTS.md)
- ER: [27_RAILDB_ER_MODEL.md](27_RAILDB_ER_MODEL.md)
- Semantics: [37](37_P3C0_SEMANTICS_RESEARCH.md), [39](39_P3C1_SEMANTICS_FINDINGS.md)
- Watched research: [../analysis/11-nuff-active-wagon-list.md](../analysis/11-nuff-active-wagon-list.md)
