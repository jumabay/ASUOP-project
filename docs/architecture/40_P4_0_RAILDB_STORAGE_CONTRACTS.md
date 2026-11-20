# 40. P4-0 — контракты хранения RAILDB (read path)

Статус: **docs-only / DRAFT**. APPROVALS: **P4-0 Storage Contracts (offline)** = APPROVED / DONE (PR #6).  
**ADR-0009 = ACCEPTED (architecture direction only, 2026-11-20)** — этот пакет по-прежнему **не** Create RAILDB, **не** Flyway apply, **не** JDBC connect. Open: NK1/DT1/G31/R1-Q-TZ.

Цель: зафиксировать логический контракт таблиц `RAILDB.RAIL_APP` для чтения **реестра / карточки / текущего состояния / суточной истории / слежения**, согласованный с REST R1–R3, без исполнения DDL/DML.

Связанные артефакты:

| Артефакт | Назначение |
| --- | --- |
| [ADR-0009](decisions/ADR-0009-raildb-primary-store.md) | Целевой store (PROPOSED) |
| [27_RAILDB_ER_MODEL.md](27_RAILDB_ER_MODEL.md) | ER / ключи |
| [31_P3A_FIELD_MAPPING_MATRIX.md](31_P3A_FIELD_MAPPING_MATRIX.md) | DBTC → RAIL_* (миграция) |
| [41_P4_0_R1_R3_FIELD_MAPPING.md](41_P4_0_R1_R3_FIELD_MAPPING.md) | REST/DTO → RAIL_* (read path) |
| [sql/raildb/V0__draft_confirmed_core.sql](sql/raildb/V0__draft_confirmed_core.sql) | Draft DDL (partitioned disl) |
| [sql/raildb/V0b__draft_nonpartitioned_disl_fallback.sql](sql/raildb/V0b__draft_nonpartitioned_disl_fallback.sql) | Fallback без Partitioning |
| [sql/raildb/read/](sql/raildb/read/) | SELECT stubs (bind + page) |

---

## 1. Scope read-path (P4-0)

| Use-case | REST | Port | Таблицы (логическая) |
| --- | --- | --- | --- |
| Карточка | `GET /api/v1/wagons/{code}` | `WagonCardPort` | `rail_wagon` + `rail_station` (home) |
| Текущее состояние | (поля на карточке / будущий блок) | — / card join | `rail_wagon_current` |
| Сутки истории | `GET /api/v1/wagons/{code}/state-reports?date=` | `StateReportDayPort` | `rail_disl_event` (+ resolve `wagon_id`) |
| Реестр | `GET /api/v1/wagons?page&size&sort&search` | `WagonRegistryPort` | `rail_wagon` |
| Слежение | `GET /api/v1/wagons/watched?...` | `WatchedWagonPort` | `rail_wagon_watched` ⨝ `rail_wagon` (+ station) |

Вне P4-0 read-path: repairs/warehouse UI, intake/writer, migrator M1–M8 apply, live RWL/FTP.

REST-контракты R1–R3 **не меняются** этим пакетом.

---

## 2. Таблицы и ключи (контракт)

Орфография в DDL draft — lowercase identifiers; логические имена в docs часто `RAIL_*`.

### 2.1. `rail_station`

| | |
| --- | --- |
| PK | `station_id` |
| UK | `station_code` |
| Роль read | Home station name/code для карточки и watched |

### 2.2. `rail_wagon`

| | |
| --- | --- |
| PK | `wagon_id` |
| UK | `wagon_code` (lookup R1/R2/R3) |
| FK | `home_station_id` → `rail_station` |
| Роль read | Реестр (page), паспорт карточки / watched |

Колонки паспорта (draft): `wagon_code`, `model_code`, `wagon_kind`, `owner_name`, `inventory_park`, `home_station_id`.

### 2.3. `rail_wagon_current`

| | |
| --- | --- |
| PK | `wagon_id` (= FK → `rail_wagon`) |
| Роль read | Последнее известное положение (не полная лента) |
| Поля draft | `station_id` / `station_code`, `operation_code`, `dislocation_ts`, `train_index`, `updated_at_utc` |

Не смешивать с суточной выборкой из `rail_disl_event`.

### 2.4. `rail_disl_event` (day history)

| | |
| --- | --- |
| PK | `(event_id, dislocation_ts)` в partitioned draft; `event_id` alone в non-partitioned fallback |
| Natural UK | **нет** (NK1 **BLOCKED** / OPEN) — дубликаты источника **сохраняются** |
| Index read | `(wagon_id, dislocation_ts)` — обязателен в обоих вариантах DDL |
| Ось времени | `dislocation_ts` ≡ **getting_ts / GETTING_DATE** до закрытия **DT1** |
| Attr | `location_ts` (LOCATION_DATE) — хранить; **не** ось фильтра суток до DT1 |
| Лимит API | ≤ **500** строк на сутки; иначе 422 `RESULT_LIMIT_EXCEEDED` (без тихой обрезки) |
| Сутки | half-open `[D 00:00, D+1 00:00)`; TZ = **R1-Q-TZ OPEN** |

### 2.5. `rail_wagon_watched`

| | |
| --- | --- |
| PK | `wagon_id` |
| FK | → `rail_wagon` |
| Поля | `source_code`, `captured_at_utc` |
| Membership | **G31 OPEN** — таблица пуста до утверждённого snapshot / SPEC |
| ≠ | synthetic fixture 60 кодов; ≠ доказанный `GetActiveWagonList` |

### 2.6. Supporting (не R1–R3 UI, но в V0)

`rail_intake_event`, `rail_import_kit`, `rail_hour_expectation`, repair/stock/audit — см. ER `27` / V0. Не требуются для текущего REST R1–R3.

---

## 3. Constraints / integrity (read-side expectations)

| Правило | Статус |
| --- | --- |
| Lookup вагона только по `wagon_code` UK | Draft + legacy UK CONFIRMED (P3B) |
| Day filter: `wagon_id` + `dislocation_ts` range; retain duplicates | Policy P3C-0; NK1 freq UNKNOWN |
| `kit_id` nullable; provenance/hash for replay | Draft |
| Watched join только по membership rows | Empty until G31 |
| Page size registry/watched | API `1…200` (`WagonRegistryCriteria.MAX_SIZE`) |
| Day row cap | 500 (REST) |

---

## 4. Отношение к REST R1–R3

| API | Источник данных (целевой) | Примечание |
| --- | --- | --- |
| R1 card | `rail_wagon` ⟕ `rail_station` | Current block → `rail_wagon_current` (опционально поверх паспорта) |
| R1 day | `rail_disl_event` | `reportId` ← `legacy_report_id` / kit link — см. mapping `41` |
| R2 registry | `rail_wagon` | search/sort по `wagon_code` |
| R3 watched | `rail_wagon_watched` ⨝ passport | Membership UNKNOWN до G31 |

Детальный field map: [41_P4_0_R1_R3_FIELD_MAPPING.md](41_P4_0_R1_R3_FIELD_MAPPING.md).  
Миграционный DBTC→RAIL map: [31_P3A_FIELD_MAPPING_MATRIX.md](31_P3A_FIELD_MAPPING_MATRIX.md) — не дублировать и не «закрывать» OPEN.

---

## 5. Explicit OPEN items (не закрывать догадкой)

| ID | Вопрос | Влияние на P4-0 | Статус |
| --- | --- | --- | --- |
| **NK1** | Natural key / частота дублей ленты `STATEREPORTRECORDS` | Нет UK на `rail_disl_event`; retain all | **BLOCKED** (нет DBA tape plan) |
| **DT1** | GETTING_DATE vs LOCATION_DATE как бизнес-ось UI | Фильтр суток = `dislocation_ts` (getting) | **BLOCKED**; axis unchanged |
| **G31** | Источник membership «слежение» / Active predicate | `rail_wagon_watched` пуст; R3≠legacy | **OPEN** |
| **R1-Q-TZ** | TZ суток / wall-clock vs UTC store | Bind day bounds; partition key TIMESTAMP без TZ | **OPEN** |

Evidence: [37](37_P3C0_SEMANTICS_RESEARCH.md), [39](39_P3C1_SEMANTICS_FINDINGS.md), [23](23_R1_A1_A2_IMPLEMENTATION_PLAN.md) §R1-Q-TZ.

---

## 6. ADR-0009 PROPOSED — checklist нерешённых решений

До **Accept ADR-0009** и отдельных строк APPROVALS следующие пункты **OPEN** (P4-0 их не исполняет):

| # | Решение | Нужно до / для |
| --- | --- | --- |
| 1 | Accept ADR-0009 (supersede sole-DBTC store в 05/06/15) | Любой create/Flyway/write |
| 2 | Форма инстанса A (SID) vs B (PDB) + service name | Create Oracle RAILDB |
| 3 | Charset / NLS / sizing / RMAN / FRA (см. `24`) | Create |
| 4 | Partitioning option licensed? | Выбор V0 vs V0b при Flyway |
| 5 | Create empty `RAILDB` + schema `RAIL_APP` | DDL apply |
| 6 | Flyway apply business DDL (V0 или V0b) | Schema materialization |
| 7 | APPROVED TEST URL + RO credentials (не Production) | JDBC read adapters |
| 8 | DBTC read for migration + RAILDB write | Migrator / cutover writer |
| 9 | Закрытие NK1/DT1 (DBA tape plan) | Уверенность UK/оси ленты |
| 10 | G31 business pick (Active vs wagons.txt vs other) | M8 watched + R3 parity claim |
| 11 | R1-Q-TZ policy (store UTC wall vs local) | Day bounds + partition semantics |
| 12 | Cutover + rollback drill (ADR-0009 §) | Production switch |

**Запрещено этим пакетом:** Accept, Create, Flyway apply, JDBC connect, DML, снятие `@Disabled` с parity.

---

## 7. Partitioning license + non-partitioned fallback

Черновик [V0__draft_confirmed_core.sql](sql/raildb/V0__draft_confirmed_core.sql) использует `PARTITION BY RANGE (dislocation_ts)` + `LOCAL` indexes на `rail_disl_event`.

| Факт | Следствие |
| --- | --- |
| Oracle **Partitioning** — отдельная опция EE | Без лицензии DDL упадёт или нарушит compliance |
| DBTC tape historically non-partitioned (P3B dictionary) | Partitioning — целевая оптимизация, не legacy parity |
| Fallback | [V0b__draft_nonpartitioned_disl_fallback.sql](sql/raildb/V0b__draft_nonpartitioned_disl_fallback.sql): heap table + btree `(wagon_id, dislocation_ts)` + archive/delete policy вручную |

Подробности лицензии: [32_P3B0_DICTIONARY_PROBE_AUDIT.md](32_P3B0_DICTIONARY_PROBE_AUDIT.md) §4.  
Выбор V0 vs V0b — решение DBA при Accept / Flyway gate, **не** в P4-0.

---

## 8. SQL read stubs

Каталог [sql/raildb/read/](sql/raildb/read/):

- `wagon_card_by_code.sql`
- `wagon_registry_page.sql`
- `state_report_day.sql`
- `watched_wagon_page.sql`

Все: **DO NOT EXECUTE**; named binds; `FETCH` / page limits; ось суток = getting_ts до DT1.

---

## 9. Stop / next gates

P4-0 DATABASE/docs slice: контракты + drafts committed.  
Далее (отдельные APPROVALS): backend fail-fast stubs (если ещё не в той же ветке) → QA → PR; затем S2 Accept/Create — только человеком.
