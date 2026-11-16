# 28. Каталог бизнес-объектов DBTC (P3A, offline)

Статус: **P3A** — только материалы репозитория. Live словарь RAC не читался.  
Источники: [02_ORACLE_TABLES.md](../analysis/02_ORACLE_TABLES.md), [02_ORACLE_RELATIONSHIPS.md](../analysis/02_ORACLE_RELATIONSHIPS.md), [02_ORACLE_PERFORMANCE.md](../analysis/02_ORACLE_PERFORMANCE.md), [02-oracle-schema.md](../analysis/02-oracle-schema.md), [03_ORACLE_PACKAGES.md](../analysis/03_ORACLE_PACKAGES.md), [01_ASUVP_IMPORT_PIPELINE.md](../analysis/01_ASUVP_IMPORT_PIPELINE.md), [25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md](25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md).

Объёмы = `NUM_ROWS`/`BLOCKS` (приблизительно), не `COUNT(*)`.

## 1. Контур

| Факт | Значение | Confidence |
| --- | --- | --- |
| Схема | `DBTC` | CONFIRMED |
| БД/сервис (источник словаря) | `RAC` / `RAC.WORLD` на `tc-asuvp-03` | CONFIRMED (docs) |
| Партиции | нет (`ALL_PART_TABLES` пуст) | CONFIRMED |
| PACKAGE BODY в `ALL_SOURCE` | не видны | CONFIRMED |
| ~251 TABLE, ~29 пакетов, 96 VIEW | | CONFIRMED order |

## 2. Каталог по доменам

### 2.1 Реестр и текущее состояние

| Object | Role | ~Volume | PK | Key columns (known) | Confidence |
| --- | --- | --- | --- | --- | --- |
| `WAGON` | master + current location/repair | ~50k | `ID` | `LOCATIONSTATION_ID`, `CURRENTSTATE_ID`, `OPERATION_ID`, `REPORTDATE`, `LOCATIONDATE`, `CURRENTREPAIR_ID` | CONFIRMED |
| `WAGONTYPES` / `MODELS` / `STATES` / `STATUSES` / `OWNERS` / `INFO` | refs | small | ID triggers | — | CONFIRMED names |
| `GIVING` | current givings | ~9k | UNKNOWN text | — | CONFIRMED table |
| `WAGONAPPROACH` | current approach | ~27k | UNKNOWN | — | CONFIRMED |
| `CURRENTPARTS` | installed parts now | ~168k | UNKNOWN | — | CONFIRMED |

### 2.2 Срезы / история дислокации

| Object | Role | ~Volume | PK | Notes | Confidence |
| --- | --- | --- | --- | --- | --- |
| `IMPORTFILES` | file journal | ~313k | `ID` | `FILENAME`, `IMPORTDATE`, `TYPE`; business UK `(FILENAME,TYPE)` via client | CONFIRMED |
| `STATEREPORTS` | slice header | ~412k | `ID` | `GETTING_DATE`, `OPERATING_DATE`, `LAST`, `IMPORTFILES_ID`, `WAGONCOUNT` | CONFIRMED |
| `STATEREPORTRECORDS` | history tape | ~1.215e9 | **none** | `WAGON_ID`, `REPORT_ID`, `GETTING_DATE`, `LOCATION_DATE`, stations, ops, cargo… | CONFIRMED |
| `STATEREPORTRECORDS_TMP` | work buffer | ~492k | — | SKIP migrate | CONFIRMED |
| `STATEREPORTSWAGON` | derived | ~8.2M | none | job broken | CONFIRMED |
| `WAGONSTATEREPORTS(+RECORDS)` | document slices | ~2.3k / ~37.7M | `ID` | watched publish path | CONFIRMED |

### 2.3 Ремонты / пробег

| Object | Role | ~Volume | Notes | Confidence |
| --- | --- | --- | --- | --- |
| `REPAIRS` | repair cards | ~411k | `beginingdate`/`endingdate` via triggers; open predicate UNKNOWN | CONFIRMED |
| `REPAIRTYPES` | 0 ДР…7 ТР1… | 9 | types CONFIRMED in pkg docs | CONFIRMED |
| `REPAIRPLANTS`, budgets, docs | supporting | UNKNOWN sizes | column lists incomplete | PARTIAL |
| `WAGONMILEAGE` | mileage history | ~33.6M | date column name UNKNOWN | CONFIRMED table |

### 2.4 Склад / детали

| Object | Role | ~Volume | Confidence |
| --- | --- | --- | --- |
| `WAREHOUSES` / `WAREHOUSETYPES` | refs | 41 / ? | CONFIRMED |
| `WAREHOUSEDETAILS` | stock balances | UNKNOWN grain | CONFIRMED name |
| `VIRTUALWAREHOUSEDETAILS` | virtual queue | ~659k | CONFIRMED |
| `WAGONPARTS`, `PARTTYPES` | part masters | UNKNOWN | CONFIRMED names |
| `*DETAILS` documents | procurement/arrival/transfer/writeoff/repair/wheel | UNKNOWN | CONFIRMED family |

### 2.5 Справочники

| Object | Role | ~Volume | Confidence |
| --- | --- | --- | --- |
| `STATIONS` | hub (83 FK in) | ~37k | CONFIRMED |
| `CUSTOMERS` | partners | ~580 | CONFIRMED |
| `OPERATIONS`, `CARGOTYPES`, `CARGOKTZ`, `OWNERS` | refs | small–med | CONFIRMED |
| `STATIONSDISTANCE` (+ query ~61.6M) | distances | heavy | CONFIRMED name |

### 2.6 Архив (остаётся в DBTC при 12м cut)

`WAGONAPPROACHARCHIVE` (~886M), `GIVINGARCHIVE` (~44M), `WAGONDOWNTIME` (~184M), older tape & mileage — **ARCHIVE_DBTC**.

### 2.7 Пакеты (writers/readers)

| Package | Role | Confidence |
| --- | --- | --- |
| `ImportPkg` | write tape | CONFIRMED name; body UNKNOWN |
| `StateReportPackage` | read/write reports | CONFIRMED |
| `WagonPackage` | wagon / lists / Active | CONFIRMED; Active body UNKNOWN (G31) |
| `RepairsPkg`, `PartsPackage`, `WarehousePkg` | repairs/parts/wh | CONFIRMED names; Register bodies UNKNOWN |

### 2.8 Watched list

Отдельной таблицы **нет** (CONFIRMED absence in docs). Кандидаты: `GetActiveWagonList`, `wagons.txt`, document reports — см. G31.

## 3. Недостающие сведения → P3B

Полные `ALL_TAB_COLUMNS` / constraints / index expressions для: `STATEREPORTRECORDS`, `REPAIRS`, `WAREHOUSEDETAILS`, `WAGONPARTS`, `WAGONMILEAGE`, document headers.  
Natural key tape; open-repair SQL; `GETTING_DATE` vs `LOCATION_DATE` semantics.  
Скрипты: [sql/dba/01_dictionary_probes.sql](sql/dba/01_dictionary_probes.sql).
