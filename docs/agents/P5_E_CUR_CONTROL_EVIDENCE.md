# P5-E-CUR — CONTROL evidence (`currentState` from WAGON)

Дата: **2026-11-24**.  
APPROVALS: `P5-E-CUR JDBC currentState CONTROL` **APPROVED WITH CONSTRAINTS** до `2026-11-25 18:00 +05:00`.  
Target: `tc-asuvp-03:1521/RAC.WORLD`, `DBTC`/`DBTC`, password `env:ASUOP_ORACLE_PASSWORD`.  
Default profile remains **`synthetic`**. Opt-in: **`dbtc-ro`**.

## Mapping (CODE)

| API | Source | Rule |
| --- | --- | --- |
| `stationCode` | soft `STATIONS.CODE` via `WAGON.LOCATIONSTATION_ID` | orphan → null |
| `operationCode` | `IDCODE` then `CODE` (`DbtcOperationCode`, string only) | empty → null |
| `dislocationTime` | **only** `WAGON.LOCATIONDATE` | null → null; no GETTING/SYSDATE/REPORTDATE |
| `trainIndex` | — | **always null** (TRAIN metadata ≠ mapping) |
| whole block | — | all absent → `currentState: null`; JDBC errors propagate |

UI dbtc-ro label: **«Дата положения по данным источника»** (+ tooltip G3/G4 / DT1 OPEN).

## Offline tests

Filled after local Gradle/Vitest/Playwright run on feature HEAD.

## Live CONTROL `54787361`

Filled after one Oracle→API→UI run under approval window.

| Field | Actual |
| --- | --- |
| HEAD | |
| `stationCode` | |
| `operationCode` | |
| `dislocationTime` | |
| `trainIndex` | |
| TRAIN metadata columns | |
| CI | |

## Open (unchanged)

DT1 / R1-Q-TZ / G31 / NK1 **OPEN**. T2 / registry / watched / import / writes **out of scope**.
