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
| `LOCATION_STATION_ID` | `DBTC.STATIONS.ID` (`FK_SRR_LOCATION_STATIONS`) | `STATIONS.CODE` (NAME for display) | `stationCode` — **CONFIRMED** via join; ID ≠ code |
| `OPERATION_ID` | `DBTC.OPERATIONS.ID` (`FK_OPERATION_REPORTRECORDS`) | **`OPERATIONS.IDCODE`** (fallback `CODE`; preserves leading zeros) | `operationCode` — **CONFIRMED** via join; ID ≠ code |
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
| `model` | `WAGON_MODEL_ID` | → `WAGONMODELS.ID` | `WAGONMODELS` | **`NAME`** | FK + display **CONFIRMED** (CONTROL) |
| `wagonKind` | `WAGONTYPE_ID` | → `WAGONTYPES.ID` | `WAGONTYPES` | **`NAME`** | FK + display **CONFIRMED** (CONTROL) |
| `ownerName` | `OWNER_ID` | → **`OWNERS.ID`** (не `WAGONOWNERS`) | `OWNERS` | **`NAME`** | FK + display **CONFIRMED** (CONTROL) |
| `homeStationCode` | `HOMESTATION_ID` | → `STATIONS.ID` | `STATIONS` | `CODE` | FK + SELECT **CONFIRMED** |
| `homeStationName` | `HOMESTATION_ID` | → `STATIONS.ID` | `STATIONS` | `NAME` | same |
| `inventoryPark` | `IN_INVENTORY_PARK` | none | — | NUMBER | column **CONFIRMED**; CONTROL raw=`1`→`true`; **PROVISIONAL** (не глобальное доказательство) |

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

## 4. Дополнение к разрешению — **APPROVED** (владелец, 2026-11-24)

Зафиксировано в `docs/architecture/APPROVALS_TEMPLATE.md`  
(`P5-E allow-list delta: ref SELECT + CONTROL JDBC/UI`).

Разрешено: SELECT + metadata на `STATIONS`/`OPERATIONS`/`WAGONMODELS`/`WAGONTYPES`/`OWNERS`  
по PK из CONTROL; A1/A2 JDBC profile `dbtc-ro`; UI label «Время среза».  
Запрещено: EXECUTE/DDL/DML/EXPLAIN; registry/watched JDBC; currentState; import; full scans.

---

## 5. Реализация / проверка API/UI

| Item | Status |
| --- | --- |
| Offline mappers + SQL drafts | `platform-oracle` `jdbc` + `DbtcMappingFactoriesTest` PASS |
| Live Oracle → API → UI for CONTROL | **PASS** — evidence [P5_E_JDBC_CONTROL_EVIDENCE.md](P5_E_JDBC_CONTROL_EVIDENCE.md) |
| Synthetic regression | default profile unchanged; `NoOracleDataSourceTest` PASS |

`dislocationTime` остаётся **PROVISIONAL** (`GETTING_DATE`); UI: «Время среза». Смена API-контракта времени — отдельное решение.

---

## 6. Итог (не полное закрытие A1/A2 / DT1)

- Доступ/чтение + ref lookups: **PASS**
- Подтверждено для CONTROL: display `NAME` model/kind/owner; station `CODE`; op `IDCODE` (`04`/`81`); home `68390`; inventory raw `1`→`true` (**sample only**)
- Не VERIFIED глобально: `dislocationTime` as fact; inventory semantics beyond CONTROL; registry/watched/currentState
- API/UI live CONTROL: **PASS** under time-boxed `dbtc-ro`
