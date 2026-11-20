# 36. P3B — словарь DBTC (live RAC), прогон 2

Статус: **DICTIONARY SUBSTANTIALLY COMPLETE** (таблицы ядра). Пакеты/G31 signature — ещё **OPEN**.  
Прогон: `DBTC_READONLY` → `RAC` / `RAC.WORLD` / `tc-asuvp-03`  
Инструмент: `tools/dbtc-dictionary-inspector`  
Манифест: `sql/dba/p3b_results/run_manifest_20261116_054752.csv`  
Предыдущий частичный: `…_053431` (только WAGON/REPAIRS/STATIONS).  
DDL/DML/EXECUTE пакетов/массовое чтение ленты/P4 — **не выполнялись**.

## 1. G0

| Check | Result |
| --- | --- |
| Identity | `DBTC_READONLY`, schema `DBTC_READONLY` |
| Target | `DB_NAME=RAC`, `SERVICE_NAME=RAC.WORLD`, `SERVER_HOST=tc-asuvp-03` |
| Privs | `CREATE SESSION` only; roles empty; dangerous **нет** |
| Object READ | `READ=368` (+ `INHERIT PRIVILEGES` NOTE) |
| Core visibility | `IMPORTFILES`, `STATEREPORTS`, `STATEREPORTRECORDS` — **все 3** |
| Probe triad | `ALL_TABLES=3` (WAGON+tape+import), `ALL_OBJECTS_WAGON=1` |

Пароль: в Process/User/Machine и `.env` **не найден**; использован session env терминала (`PASS_LEN=10`). Рекомендуется перенос в защищённый store + ротация (ранее светился в истории терминала).

## 2. G1–G5 execution

| Group | Rows (key) | Status |
| --- | --- | --- |
| G1 columns | 258 | OK — 15 probed tables all present |
| G1 constraints | 91 | OK |
| G2 indexes / ind_cols | 59 / 80 | OK |
| G2 partitions | 0 | OK — нет партиций |
| G2 stats | 13 | OK |
| G3 packages/args/deps | 0/0/0 | EMPTY — нет видимости пакетов |
| G4 repairs+WH | 35+3+66+22 | OK |
| G5 watch objects | 3 tables/views | OK names; args/source 0 |

`OBJECT_NAME` fix for `ALL_ARGUMENTS` applied — запросов без ORA-00904; результат пуст из‑за грантов на dictionary пакетов.

## 3. Подтверждённые структуры

### 3.1 Kits / slices

**IMPORTFILES** (6): `ID` PK, `FILENAME`, `IMPORTDATE`, `TYPE`, `USER_ID`, `FILESIZE`.  
Индекс `(TYPE,FILENAME)` **NONUNIQUE** — бизнес-UK остаётся client-side (как P3A).

**STATEREPORTS** (7): `ID` (**constraint name** `UK_REPORT_ID`, type P), `GETTING_DATE`/`OPERATING_DATE` TIMESTAMP(2), `USERID`, `WAGONCOUNT`, `LAST`, `IMPORTFILES_ID`.  
FK на `IMPORTFILES` **в словаре нет** (только `FK_USERID_USERS`) — связь kit soft/app-level **UNKNOWN enforce**.

### 3.2 Tape `STATEREPORTRECORDS` (29 cols) — NK1 / DT1

| Fact | Evidence |
| --- | --- |
| **No PK** | нет constraint P | **NK1 CONFIRMED: no PK** |
| NOT NULL | `WAGON_ID`, `REPORT_ID`, `CURRENT_STATE_ID` |
| Dates | `GETTING_DATE`, `LOCATION_DATE`, `DISPATCH_DATE`, `DEPARTURE_DATE` |
| FK | wagon, report→`UK_REPORT_ID`, stations×4, operation, state, customers, cargo, waybill |
| Indexes | `IDX_REC_GETTING_DATE(GETTING_DATE)`; `(WAGON_ID,GETTING_DATE)`; `LOCATION_DATE` in INFO/DATA; `(WAGON_ID,REPORT_ID,…)` LINKS **nonunique** |
| Stats | **~1.215e9** rows, ~23.4M blocks |

**DT1:** обе даты CONFIRMED. Предпочтительная ось фильтра/prune для 12м: **`GETTING_DATE`** (отдельный + составной индекс). Смысл «час файла» vs факт локации на строках — **UNKNOWN** без scoped sample (не делался).

**NK1 для RAILDB:** кандидат `(REPORT_ID, WAGON_ID)` логичен по FK/индексу, но **UNIQUE в Oracle нет** → UK в RAILDB остаётся **PROVISIONAL** до B1/дубликатов.

`STATEREPORTRECORDS_TMP` — те же 29 колонок; migrate **SKIP**.

### 3.3 Watched-related objects (G5 / G31 partial)

| Object | Type | Role clue |
| --- | --- | --- |
| `WAGONSTATEREPORTS` | TABLE | document slice header (`SYSDOCUMENT_ID`, `ENABLED`, …) |
| `WAGONSTATEREPORTSRECORDS` | TABLE | ~38M lines; mirror tape fields + `REPORTID`/`ID`/`ENABLED` |
| `WAGONSTATEREPORTSVIEW` | VIEW | name only |

`GETACTIVEWAGONLIST` / `WAGONPACKAGE` — **не видны**. Membership predicate **BLOCKED**.

### 3.4 Repairs — RP1

`REPAIRS` 23 cols; `BEGININGDATE`/`ENDINGDATE`; PK `ID`; FK wagon/type/stations.  
`REPAIRTYPES`(ID,NAME,GROUP_NAME); `REPAIRPLANTS`; `PLAN_REPAIRS_T`.  
Open SQL: гипотеза `ENDINGDATE IS NULL` и/или `WAGON.CURRENTREPAIR_ID` — **predicate OPEN** (нет тела пакета).

### 3.5 Warehouse / parts — WH1

| Table | Key finding |
| --- | --- |
| `WAREHOUSES` | PK `ID`; type + repair plant |
| `WAREHOUSEDETAILS` | UK `(WAREHOUSE_ID, OBJECT_ID, OBJECT_STATE)`; cols + `OBJECT_TYPE`, `OBJECT_COUNT`; ~8983 rows |
| `CURRENTPARTS` | `(WAGONID, WAGONPARTID)` FKs; **no PK** |
| `WAGONPARTS` | PK `ID`; UK `(PARTNUMBER, TYPE)` |
| Docs | `ARRIVAL*`/`PROCUREMENT`/`TRANSFER*`/`WRITINGOFF*` — `DELIVERYDATE` NOT NULL |

Grain остатка: баланс по складу+объекту+состоянию — **CONFIRMED UK**. Семантика `OBJECT_TYPE` / Register — **UNKNOWN**.

### 3.6 Mileage

`WAGONMILEAGE`: `WAGONID`+`CHANGEDATE` NOT NULL; ~33.6M rows. PK в G1 probe **не попал** (таблица вне constraint list G1.2) — key **PARTIAL**.

## 4. Гипотезы после прогона 2

| ID | Status | Note |
| --- | --- | --- |
| **NK1** | PARTIAL | No PK CONFIRMED; RAILDB UK provisional `(kit/report, wagon)` |
| **DT1** | PARTIAL | Columns+indexes CONFIRMED; prefer `GETTING_DATE` for 12м; business axis OPEN |
| **RP1** | PARTIAL | Date names CONFIRMED; open predicate OPEN |
| **WH1** | PARTIAL | UK grain CONFIRMED; Register/OBJECT_TYPE OPEN |
| **G31** | OPEN | Doc-report tables CONFIRMED exist; Active list procedure not visible |

## 5. Risks

- `STATEREPORTS.IMPORTFILES_ID` без FK — orphan kits возможны.  
- Лента ~1.2B без UK — миграция только батчами по `GETTING_DATE` + идемпотентность на стороне RAILDB.  
- Пакеты скрыты — writers/G31 не закрыть словарём.  
- Пароль только в session env терминала; защищённый local config отсутствует.

## 6. Next (не P4)

1. DBA: SELECT/READ на `ALL_OBJECTS`/`ALL_ARGUMENTS`/`ALL_SOURCE` для пакетов DBTC (без EXECUTE).  
2. Отдельный approve: scoped sample / B1 для NK1 duplicates и DT1 semantics.  
3. Не EXECUTE `GetActiveWagonList` без approve.
