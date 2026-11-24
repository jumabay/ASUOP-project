# P5-E-CUR — CONTROL evidence (`currentState` from WAGON)

Дата: **2026-11-24**.  
APPROVALS: `P5-E-CUR JDBC currentState CONTROL` **APPROVED WITH CONSTRAINTS** до `2026-11-25 18:00 +05:00`.  
Target: `tc-asuvp-03:1521/RAC.WORLD`, `DBTC`/`DBTC`, password `env:ASUOP_ORACLE_PASSWORD` (не логировался).  
Default profile remains **`synthetic`**. Opt-in: **`dbtc-ro`**.

Feature HEAD: **`966b40a025a786c496cf594709d8b5f7030e50a0`**

## Mapping (CODE)

| API | Source | Rule |
| --- | --- | --- |
| `stationCode` | soft `STATIONS.CODE` via `WAGON.LOCATIONSTATION_ID` | orphan → null |
| `operationCode` | `IDCODE` then `CODE` (`DbtcOperationCode`, string only) | empty → null |
| `dislocationTime` | **only** `WAGON.LOCATIONDATE` | null → null; no GETTING/SYSDATE/REPORTDATE |
| `trainIndex` | — | **always null** (TRAIN metadata ≠ mapping) |
| whole block | — | all absent → `currentState: null`; JDBC errors propagate |

UI dbtc-ro label: **«Дата положения по данным источника»** (+ tooltip G3/G4 / DT1 OPEN).

## Offline tests (Independent QA + focused)

| Suite | Result |
| --- | --- |
| Gradle `test` (JDK 21) | **PASS** (~179 tests, 0 fail, 1 skipped `JdbcParityIT`) |
| Vitest | **51/51** |
| `npm run build` | **OK** |
| Playwright synthetic | **19/19** |

## Live CONTROL `54787361` — VERIFIED once

Probe SQL (`P5eEcurCurrentProbe`) + API `dbtc-ro` `:18084` + UI Vite `:5174` proxy.

| Field | Actual |
| --- | --- |
| HEAD | `966b40a` |
| `stationCode` | `69260` |
| `operationCode` | `03` (IDCODE; leading zero preserved) |
| `dislocationTime` | `2026-11-24T11:51` (`LOCATIONDATE` only) |
| `trainIndex` | `null` / UI «—» |
| TRAIN metadata | column **`TRAINCODE` VARCHAR2** present — **not mapped** |
| Runtime honesty | `DBTC_RO` / `DBTC read-only (time-boxed)` |
| UI time label | **Дата положения по данным источника** |
| Passport (unchanged) | model `12-9796`, kind Полувагон, owner TОО ТрансКом, home `68390`, inventory `true` |

Sample API JSON:

```json
{
  "wagonCode": "54787361",
  "currentState": {
    "stationCode": "69260",
    "operationCode": "03",
    "dislocationTime": "2026-11-24T11:51",
    "trainIndex": null
  }
}
```

## Open (unchanged)

DT1 / R1-Q-TZ / G31 / NK1 **OPEN**. T2 / registry / watched / import / writes **out of scope**.  
`TRAINCODE` discovery alone does **not** authorize mapping without a separate APPROVALS row.
