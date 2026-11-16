# 31. Матрица переноса таблиц/полей DBTC → RAILDB (P3A)

Расширяет [25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md](25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md). Поля **PROVISIONAL** без P3B.

| DBTC | Filter | RAILDB | Field notes | Confidence |
| --- | --- | --- | --- | --- |
| `STATIONS` | REF_FULL | `RAIL_STATION` | code/name; legacy_id | CONFIRMED names |
| `WAGON` | FULL | `RAIL_WAGON` + `RAIL_WAGON_CURRENT` | code + location cols | CONFIRMED partial cols |
| watched | snapshot | `RAIL_WAGON_WATCHED` | G31 | BLOCKED |
| `IMPORTFILES` | 12M `IMPORTDATE` | `RAIL_IMPORT_KIT` (+ intake) | `(FILENAME,TYPE)` | CONFIRMED |
| `STATEREPORTS` | 12M | kit header attrs | `GETTING_DATE`, `LAST` | CONFIRMED |
| `STATEREPORTRECORDS` | 12M `GETTING_DATE` | `RAIL_DISL_EVENT` | map KZ canonical; NK UNKNOWN | PARTIAL |
| bootstrap ≤ cutoff | BOOTSTRAP | seed events/current | | CONFIRMED need |
| `REPAIRS` | 12M∪OPEN | `RAIL_REPAIR` | open predicate UNKNOWN | PARTIAL |
| `REPAIRTYPES` | REF | `RAIL_REPAIR_TYPE` | | CONFIRMED |
| `CURRENTPARTS` | FULL | install table TBD | columns P3B | CONFIRMED table |
| `WAREHOUSEDETAILS` | FULL | `RAIL_STOCK_BALANCE` | grain UNKNOWN | PARTIAL |
| — | NEW | `RAIL_INTAKE_*`, audit, errors, watermarks | | NEW |

KZ↔RWL canonical: [27_RAILDB_ER_MODEL.md](27_RAILDB_ER_MODEL.md) §3, [13-rwl-kz-field-matrix.md](../analysis/13-rwl-kz-field-matrix.md).
