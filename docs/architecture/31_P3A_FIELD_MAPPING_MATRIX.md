# 31. Матрица переноса таблиц/полей DBTC → RAILDB (P3B update)

Расширяет [25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md](25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md).  
Live словарь: [36_P3B_PARTIAL_DICTIONARY_FINDINGS.md](36_P3B_PARTIAL_DICTIONARY_FINDINGS.md) (прогон `20261116_054752`).

| DBTC | Filter | RAILDB | Field notes | Confidence |
| --- | --- | --- | --- | --- |
| `STATIONS` | REF_FULL | `RAIL_STATION` | 13 cols; PK ID; CODE/NAME/STAGE | **CONFIRMED P3B** |
| `WAGON` | FULL | `RAIL_WAGON` + `RAIL_WAGON_CURRENT` | 75 cols; UK CODE; location/repair FKs | **CONFIRMED P3B** |
| watched | snapshot | `RAIL_WAGON_WATCHED` | possible clue: `WAGONSTATEREPORTS*`; Active proc not visible | **G31 OPEN** |
| `IMPORTFILES` | 12M `IMPORTDATE` | `RAIL_IMPORT_KIT` (+ intake) | PK ID; idx (TYPE,FILENAME) nonunique | **CONFIRMED P3B** |
| `STATEREPORTS` | 12M | kit header attrs | PK `UK_REPORT_ID`; `GETTING_DATE` TIMESTAMP(2); `IMPORTFILES_ID` **no DB FK** | **CONFIRMED P3B** |
| `STATEREPORTRECORDS` | 12M `GETTING_DATE` | `RAIL_DISL_EVENT` | 29 cols; **no PK**; FKs wagon/report/stations… | **PARTIAL** (NK provisional) |
| bootstrap ≤ cutoff | BOOTSTRAP | seed events/current | last row by GETTING_DATE ≤ cutoff | CONFIRMED need |
| `REPAIRS` | 12M∪OPEN | `RAIL_REPAIR` | BEGININGDATE/ENDINGDATE; open predicate OPEN | **PARTIAL** |
| `REPAIRTYPES` | REF | `RAIL_REPAIR_TYPE` | ID,NAME,GROUP_NAME | **CONFIRMED P3B** |
| `CURRENTPARTS` | FULL | `RAIL_PART_INSTALL` | WAGONID+WAGONPARTID; no PK | **CONFIRMED P3B** |
| `WAGONPARTS` | REF/FULL | `RAIL_PART` | PK ID; UK (PARTNUMBER,TYPE) | **CONFIRMED P3B** |
| `WAREHOUSES` | REF | `RAIL_WAREHOUSE` | PK ID | **CONFIRMED P3B** |
| `WAREHOUSEDETAILS` | FULL | `RAIL_STOCK_BALANCE` | UK (WAREHOUSE_ID,OBJECT_ID,OBJECT_STATE) | **PARTIAL** (type semantics OPEN) |
| WH docs | 12M `DELIVERYDATE` | `RAIL_WH_DOCUMENT*` | arrival/procurement/transfer/writeoff | **CONFIRMED headers** |
| `WAGONMILEAGE` | policy | optional hist | WAGONID+CHANGEDATE | PARTIAL |
| — | NEW | `RAIL_INTAKE_*`, audit, errors, watermarks | | NEW |

KZ↔RWL canonical: [27_RAILDB_ER_MODEL.md](27_RAILDB_ER_MODEL.md) §3, [13-rwl-kz-field-matrix.md](../analysis/13-rwl-kz-field-matrix.md).
