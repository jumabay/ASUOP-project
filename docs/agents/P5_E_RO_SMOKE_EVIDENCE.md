# P5-E RO smoke — evidence

Дата: **2026-11-24**.  
APPROVALS: P5-E + Production scoped RO override **APPROVED** (до `2026-11-25 18:00 +05:00`).  
Prep: [ASUOP_P5_E_TEST_RO_SMOKE_PREP.md](ASUOP_P5_E_TEST_RO_SMOKE_PREP.md) §7.  
Runner: `tools/p5e-ro-smoke` (SELECT-only). Пароль из открытого терминала (pid 1508); **не** логировался.

## 1. Identity — PASS

| Field | Value |
| --- | --- |
| session_user | `DBTC` (logon `Dbtc`) |
| current_schema | `DBTC` |
| db_name | `RAC` |
| service_name | `RAC.WORLD` |
| server_host | `tc-asuvp-03` |
| STOP-ID | none |

## 2. Metadata

| Object | column_count | Unlocked for smoke |
| --- | ---: | --- |
| `DBTC.WAGON` | 75 | `ID`, `CODE` (also seen: `LOCATIONSTATION_ID`, `CURRENTSTATE_ID`, `OPERATION_ID`, `REPORTDATE`, `LOCATIONDATE` — **not** mapped to DTO without FK unlock) |
| `DBTC.STATEREPORTRECORDS` | 29 | `WAGON_ID`, `REPORT_ID`, `GETTING_DATE` (also seen: `LOCATION_STATION_ID`, `LOCATION_DATE`, `OPERATION_ID`, `TRAIN_CODE` — station/op codes **SKIPPED_UNKNOWN_MAPPING** until join unlock) |

No invented passport joins.

## 3. Business

### A2 (CONTROL `54787361`)

| Field | Result |
| --- | --- |
| resolve | **PASS** `wagon_id=119465` |
| `wagonCode` | **MAPPED** `WAGON.CODE` |
| `model` | `SKIPPED_UNKNOWN_MAPPING` |
| `wagonKind` | `SKIPPED_UNKNOWN_MAPPING` |
| `ownerName` | `SKIPPED_UNKNOWN_MAPPING` |
| `homeStationCode` | `SKIPPED_UNKNOWN_MAPPING` |
| `homeStationName` | `SKIPPED_UNKNOWN_MAPPING` |
| `inventoryPark` | `SKIPPED_UNKNOWN_MAPPING` |
| `currentState` | `OUT_OF_SCOPE` |

### A1 (day `2026-11-24`, half-open `GETTING_DATE`)

| Check | Result |
| --- | --- |
| rows | **13** (limit 501, timeout 30s) |
| first / last | `2026-11-24 00:00:00` → `2026-11-24 11:00:00` |
| samples | report_id `588495`@00:00, `588498`@01:00, `588503`@02:00 |
| `reportId` | **MAPPED** `REPORT_ID` |
| `wagonCode` | **MAPPED** |
| `dislocationTime` | **MAPPED** `GETTING_DATE` |
| `stationCode` | `SKIPPED_UNKNOWN_MAPPING` |
| `operationCode` | `SKIPPED_UNKNOWN_MAPPING` |
| overflow | no |

**Verdict:** `SMOKE_COMPLETE` (exit 0). Success on `CODE`/`GETTING_DATE` **≠** full A2/A1 parity.

## Forbidden (not used)

EXECUTE, EXPLAIN/PLAN_TABLE, DDL/DML, Flyway, registry/watched JDBC, currentState mapping.
