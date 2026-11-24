# P5-E follow-up after SMOKE_COMPLETE — mapping / semantics

Дата: **2026-11-24**.  
Окно APPROVED: до `2026-11-25 18:00 +05:00`.  
Probe: `tools/p5e-ro-smoke` → `P5eMappingProbe` (dictionary + CONTROL dates; **no** SELECT data from STATIONS/OPERATIONS/OWNERS/…).  
`currentState` — вне scope.

---

## 1. Доступ и чтение — PASS

| Check | Result |
| --- | --- |
| Identity | PASS (`DBTC` / `RAC.WORLD` / `tc-asuvp-03`) |
| Allow-list used | `DUAL`, `ALL_TAB_COLUMNS`, `ALL_CONSTRAINTS`, `DBTC.WAGON`, `DBTC.STATEREPORTRECORDS` |
| Ref **data** SELECT | **не** выполнялся |
| EXECUTE / DDL / DML / EXPLAIN | не использовались |

---

## 2. Подтверждённые поля / связи

### 2.1 A1 tape ↔ справочники (FK dictionary)

| Tape column | FK target | Business code column (dictionary) | API field |
| --- | --- | --- | --- |
| `LOCATION_STATION_ID` | `DBTC.STATIONS.ID` (`FK_SRR_LOCATION_STATIONS`) | `STATIONS.CODE` (NAME for display) | `stationCode` — **join needed**; ID ≠ code |
| `OPERATION_ID` | `DBTC.OPERATIONS.ID` (`FK_OPERATION_REPORTRECORDS`) | `OPERATIONS.CODE` | `operationCode` — **join needed**; ID ≠ code |
| `REPORT_ID` | `DBTC.STATEREPORTS.ID` | — | `reportId` **CONFIRMED** (numeric) |
| `WAGON_ID` | `DBTC.WAGON.ID` | via `WAGON.CODE` | `wagonCode` **CONFIRMED** |
| `GETTING_DATE` | — | — | day **filter axis** **CONFIRMED**; see §3 — **not** VERIFIED as fact time |
| `LOCATION_DATE` | — | — | **not** in A1 DTO today; candidate fact time (DT1 OPEN) |
| `TRAIN_CODE` | — | VARCHAR2 on tape | not in A1 DTO (P5-C current only) |

`DEST_STATION_ID` / `DISPATCH_STATION_ID` also → `STATIONS` — **not** chosen as UI `stationCode` without product rule (location FK is the dislocation station).

### 2.2 A2 passport — точный маппинг WAGON

| API field | WAGON column | FK (ALL_CONSTRAINTS) | Dictionary | Display column | Status |
| --- | --- | --- | --- | --- | --- |
| `wagonCode` | `CODE` | UK | — | itself | **CONFIRMED** |
| `model` | `WAGON_MODEL_ID` | → `WAGONMODELS.ID` | `WAGONMODELS` | **UNKNOWN** (need ALL_TAB_COLUMNS/SELECT on models) | FK **CONFIRMED**; display **BLOCKED** |
| `wagonKind` | `WAGONTYPE_ID` | → `WAGONTYPES.ID` | `WAGONTYPES` | **UNKNOWN** display col | FK **CONFIRMED**; display **BLOCKED** |
| `ownerName` | `OWNER_ID` | → **`OWNERS.ID`** (не `WAGONOWNERS`) | `OWNERS` | **UNKNOWN** (NAME?) | FK **CONFIRMED**; display **BLOCKED** |
| `homeStationCode` | `HOMESTATION_ID` | → `STATIONS.ID` | `STATIONS` | `CODE` | FK + target col **CONFIRMED**; need **SELECT** grant |
| `homeStationName` | `HOMESTATION_ID` | → `STATIONS.ID` | `STATIONS` | `NAME` | same |
| `inventoryPark` | `IN_INVENTORY_PARK` | none | — | NUMBER | column **CONFIRMED**; 0/1→boolean **PROVISIONAL** |

**Нет DB FK** на `WAGON.LOCATIONSTATION_ID` / `WAGON.OPERATION_ID` (soft refs) — для `currentState` (вне smoke).

---

## 3. Неизвестная семантика / DT1

CONTROL `54787361` / day `2026-11-24` (14 rows in probe; earlier smoke counted 13 — same window, retain-all):

| Metric | Value |
| --- | --- |
| `getting_date == location_date` | **0** |
| `getting_date != location_date` | **14** |
| Example | getting `2026-11-24 00:00`, location `2026-11-21 01:25` |

Вывод (legacy DT1 docs + CONTROL):

- `GETTING_DATE` ≈ ось суток / час комплекта среза (фильтр A1) — **использовать для day window**.
- `LOCATION_DATE` ≈ время фактической дислокации (file[3] hypothesis в [37](../architecture/37_P3C0_SEMANTICS_RESEARCH.md)) — **не** совпадает с getting на CONTROL.
- API `dislocationTime`: пока **PROVISIONAL** = `GETTING_DATE` for day listing only — **не VERIFIED** as «факт дислокации». DT1 остаётся OPEN.

Не выдавать `location_station_id=6621` / `operation_id=4|142` как `stationCode`/`operationCode`.

---

## 4. Дополнение к разрешению (не применено самостоятельно)

Нужен SELECT (RO) + при необходимости ALL_TAB_COLUMNS на:

| Object | Зачем |
| --- | --- |
| `DBTC.STATIONS` | `CODE`/`NAME` для home + A1 location |
| `DBTC.OPERATIONS` | `CODE` для A1 operation |
| `DBTC.WAGONMODELS` | display для `model` |
| `DBTC.WAGONTYPES` | display для `wagonKind` |
| `DBTC.OWNERS` | display для `ownerName` |

Черновик строки (владелец утверждает отдельно):

```text
| P5-E allow-list delta: ref SELECT for A1/A2 codes | APPROVED WITH CONSTRAINTS | Owner: extend P5-E RO window same target/user/deadline.
  ADD SELECT on DBTC.STATIONS, DBTC.OPERATIONS, DBTC.WAGONMODELS, DBTC.WAGONTYPES, DBTC.OWNERS
  (bind by PK id only; FETCH FIRST 1 per lookup; timeout 30s).
  Purpose: resolve CODE/NAME for mapped FKs only. No EXECUTE/DDL/DML/EXPLAIN/registry/watched/currentState.
  Same CONTROL optional. | <date> |
```

До утверждения JDBC join на эти таблицы **не** включать в live smoke.

---

## 5. Реализация / проверка API/UI

| Item | Status |
| --- | --- |
| Offline mappers + SQL drafts | `platform-oracle` `jdbc` package + unit tests |
| Live Oracle → API → UI for CONTROL | **не выполнялось** (ждёт ref SELECT delta + adapter activation) |
| Synthetic regression | unchanged default profile |

Код маппит только подтверждённое; station/operation codes остаются пустыми без join; `dislocationTime` from `GETTING_DATE` с явной пометкой provisional в docs/tests.

---

## 6. Итог (не полное закрытие A1/A2)

- Доступ/чтение: **PASS**
- Подтверждено: `wagonCode`, A1 `reportId`/`wagonCode`, FK graph station/op/home/model/type/owner, day axis `GETTING_DATE`
- Неизвестно / не VERIFIED: display columns models/types/owners; `dislocationTime` as fact; inventory 0/1 semantics; full passport/day parity
- API/UI live: **не проверено** в этом шаге
