# 28. Каталог бизнес-объектов DBTC (P3B dictionary)

Статус: **P3B dictionary** (прогон `20261116_054752`, `DBTC_READONLY` / `RAC.WORLD`).  
Ядро таблиц (wagon/tape/kits/repairs/warehouse/parts) — live CONFIRMED. Пакеты — не видны.  
P3B findings: [36_P3B_PARTIAL_DICTIONARY_FINDINGS.md](36_P3B_PARTIAL_DICTIONARY_FINDINGS.md).

Объёмы = `NUM_ROWS`/`BLOCKS` (приблизительно), не `COUNT(*)`.

## 1. Контур

| Факт | Значение | Confidence |
| --- | --- | --- |
| Схема | `DBTC` | CONFIRMED |
| БД/сервис | `RAC` / `RAC.WORLD` на `tc-asuvp-03` | **CONFIRMED P3B** |
| Партиции | нет | **CONFIRMED P3B** |
| PACKAGE / ALL_ARGUMENTS | не видны `DBTC_READONLY` | CONFIRMED this account |
| Object READ grants | 368 | **CONFIRMED P3B** |

## 2. Каталог по доменам

### 2.1 Реестр и текущее состояние

| Object | Role | ~Volume | PK | Key columns | Confidence |
| --- | --- | --- | --- | --- | --- |
| `WAGON` | master + current | **50146** | `ID` + UK `CODE` | 75 cols; location/repair FKs | **CONFIRMED P3B** |
| `CURRENTPARTS` | installed now | **169610** | none | `WAGONID`,`WAGONPARTID` | **CONFIRMED P3B** |

### 2.2 Срезы / история дислокации

| Object | Role | ~Volume | PK | Notes | Confidence |
| --- | --- | --- | --- | --- | --- |
| `IMPORTFILES` | file journal | **313199** | `ID` | idx (TYPE,FILENAME) nonunique | **CONFIRMED P3B** |
| `STATEREPORTS` | slice header | **411502** | `UK_REPORT_ID` | `IMPORTFILES_ID` no DB FK | **CONFIRMED P3B** |
| `STATEREPORTRECORDS` | history tape | **~1.215e9** | **none** | 29 cols; GETTING/LOCATION dates | **CONFIRMED P3B** |
| `STATEREPORTRECORDS_TMP` | work buffer | **491749** | — | SKIP migrate | **CONFIRMED P3B** |
| `WAGONSTATEREPORTS(+RECORDS)` | document slices | **2326** / **~38M** | header/records IDs | G31 clue | **CONFIRMED P3B** |

### 2.3 Ремонты / пробег

| Object | Role | ~Volume | Notes | Confidence |
| --- | --- | --- | --- | --- |
| `REPAIRS` | repair cards | **411295** | `BEGININGDATE`/`ENDINGDATE`; PK ID | **CONFIRMED P3B** |
| `REPAIRTYPES` | types | small | ID,NAME,GROUP_NAME | **CONFIRMED P3B** |
| `REPAIRPLANTS` | plants | | CODE,NAME,… | **CONFIRMED P3B** |
| `WAGONMILEAGE` | mileage hist | **~33.6M** | `WAGONID`+`CHANGEDATE` | **CONFIRMED P3B** |

### 2.4 Склад / детали

| Object | Role | ~Volume | Confidence |
| --- | --- | --- | --- |
| `WAREHOUSES` | refs | PK ID | **CONFIRMED P3B** |
| `WAREHOUSEDETAILS` | stock | **8983**; UK (WAREHOUSE_ID,OBJECT_ID,OBJECT_STATE) | **CONFIRMED P3B** |
| `VIRTUALWAREHOUSEDETAILS` | virtual | 6 cols | **CONFIRMED P3B** |
| `WAGONPARTS` / `PARTTYPES` | parts | UK (PARTNUMBER,TYPE) | **CONFIRMED P3B** |
| WH documents | arrival/procurement/transfer/writeoff | `DELIVERYDATE` | **CONFIRMED headers** |

### 2.5 Справочники

| Object | Role | ~Volume | Confidence |
| --- | --- | --- | --- |
| `STATIONS` | hub | **37257**; 13 cols | **CONFIRMED P3B** |
| `CUSTOMERS` / ops / cargo / owners | refs | via FKs from tape | names CONFIRMED |

### 2.6 Архив

`WAGONAPPROACHARCHIVE`, `GIVINGARCHIVE`, `WAGONDOWNTIME`, older tape — **ARCHIVE_DBTC** (>12м).

### 2.7 Пакеты

Имена известны из docs; **live ALL_OBJECTS/ARGUMENTS пусты** для этой учётки → body/signature UNKNOWN (G31).

### 2.8 Watched list

Отдельной Active-таблицы нет. Видны `WAGONSTATEREPORTS*`. `GetActiveWagonList` не подтверждён словарём.

## 3. Остаток

Пакеты для G31; B1 sample NK1/DT1; семантика WH `OBJECT_TYPE`/Register.  
Скрипты: [sql/dba/p3b/G*.sql](sql/dba/p3b/).
