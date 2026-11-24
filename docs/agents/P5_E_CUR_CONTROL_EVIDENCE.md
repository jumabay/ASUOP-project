# P5-E-CUR — live CONTROL evidence (`dbtc-ro` only)

Дата: **2026-11-24**.  
APPROVALS: `P5-E-CUR JDBC currentState CONTROL` **APPROVED WITH CONSTRAINTS** до `2026-11-25 18:00 +05:00`.  
Target: `tc-asuvp-03:1521/RAC.WORLD`, `DBTC`/`DBTC`, password `env:ASUOP_ORACLE_PASSWORD` (не логировался).  
Profile: **`dbtc-ro` only**. Default `synthetic` unchanged.

**This file is live Oracle evidence only.** Offline synthetic suites are in [P5_E_CUR_OFFLINE_EVIDENCE.md](P5_E_CUR_OFFLINE_EVIDENCE.md) and **do not** prove the Oracle/`dbtc-ro` path.

PR HEAD (docs+probe): **`f52a5789d22b72dc09b800c218607bce1b4b6b33`**.  
CODE commit: **`966b40a025a786c496cf594709d8b5f7030e50a0`**.

## Mapping under E-CUR

| API | Source | Rule |
| --- | --- | --- |
| `stationCode` | soft `STATIONS.CODE` via `WAGON.LOCATIONSTATION_ID` | orphan → null |
| `operationCode` | `IDCODE` then `CODE` (string only) | empty → null |
| `dislocationTime` | **only** `WAGON.LOCATIONDATE` | null → null |
| `trainIndex` | — | **always null** (TRAIN discovery ≠ mapping) |
| whole block | — | all absent → `currentState=null`; JDBC errors propagate |

UI dbtc-ro: **«Дата положения по данным источника»** (+ tooltip; semantics PROVISIONAL).

## Live CONTROL `54787361` — VERIFIED once (no re-run required)

Channels: `P5eEcurCurrentProbe` SQL + API `dbtc-ro` **`:18084`** + UI Vite **`:5174`** → that API.

| Field | Actual |
| --- | --- |
| `stationCode` | `69260` |
| `operationCode` | `03` (IDCODE; leading zero) |
| `dislocationTime` | `2026-11-24T11:51` (`LOCATIONDATE`) |
| `trainIndex` | `null` / UI «—» |
| TRAIN metadata | `WAGON.TRAINCODE` VARCHAR2 — **not mapped** |
| Runtime | `DBTC_RO` / `DBTC read-only (time-boxed)` |
| Passport | model `12-9796`, Полувагон, TОО ТрансКом, home `68390`, inventory `true` |

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

DT1 / R1-Q-TZ / G31 / NK1 **OPEN**. Time semantics **PROVISIONAL**. T2 / registry / watched / import / writes out of scope.
