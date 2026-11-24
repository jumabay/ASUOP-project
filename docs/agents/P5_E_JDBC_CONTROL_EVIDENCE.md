# P5-E JDBC CONTROL evidence (allow-list delta + A1/A2)

Дата: **2026-11-24**.  
Окно: до `2026-11-25 18:00 +05:00`.  
APPROVALS: `P5-E allow-list delta: ref SELECT + CONTROL JDBC/UI`.  
Target: `tc-asuvp-03:1521/RAC.WORLD`, user `Dbtc` / schema `DBTC`.  
Profile: **`dbtc-ro`** (opt-in). Default remains **`synthetic`**.

---

## 1. Display columns (models / kinds / owners) — CONFIRMED

Metadata + PK lookup for CONTROL FKs (`P5eRefLookupProbe`).

| Object | Display column for A2 | CONTROL sample |
| --- | --- | --- |
| `DBTC.WAGONMODELS` | **`NAME`** (also has `CODE`) | id=`2061` → NAME=`12-9796` |
| `DBTC.WAGONTYPES` | **`NAME`** (also `CODE`,`SHORTNAME`) | id=`2` → NAME=`Полувагон` |
| `DBTC.OWNERS` | **`NAME`** (also `SHORTNAME`) | id=`679` → NAME=`TОО ТрансКом` |

---

## 2. Business codes (leading zeros preserved)

| Source FK | Lookup | Business code column | Value |
| --- | --- | --- | --- |
| `WAGON.HOMESTATION_ID=6543` | `STATIONS` | `CODE` | `68390` |
| `SRR.LOCATION_STATION_ID=6621` | `STATIONS` | `CODE` | `69260` |
| `SRR.OPERATION_ID=142` | `OPERATIONS` | **`IDCODE`** (not numeric ID; prefer over `CODE` for zeros) | `81` |
| `SRR.OPERATION_ID=4` | `OPERATIONS` | **`IDCODE`** | **`04`** |

Adapter maps `operationCode = NVL(IDCODE, CODE)`.

---

## 3. `IN_INVENTORY_PARK` (CONTROL only)

| Field | Value | Note |
| --- | --- | --- |
| Raw | `1` | single CONTROL sample |
| API `inventoryPark` | `true` | `raw != 0` → boolean |
| Semantics | **PROVISIONAL** | **not** proof for all wagons / all non-zero meanings |

---

## 4. Oracle → API → UI (CONTROL `54787361` / day `2026-11-24`)

### Oracle (ref probe)

- Identity PASS (`DBTC` / `RAC.WORLD`).
- PK lookups only; no full dictionary scan; timeout 30s.
- Day distinct location stations: `{6621}`; operations: `{142, 4}`.

### API (`dbtc-ro`, port 18080)

`GET /api/v1/wagons/54787361`:

```json
{"wagonCode":"54787361","model":"12-9796","wagonKind":"Полувагон","ownerName":"TОО ТрансКом","homeStationCode":"68390","homeStationName":"Железорудная","inventoryPark":true,"currentState":null}
```

`GET /api/v1/wagons/54787361/state-reports?date=2026-11-24`:

- **14** lines (FETCH limit 501; no overflow).
- Day axis: `GETTING_DATE` → API field `dislocationTime` (**PROVISIONAL** slice time).
- Sample: `588495` / `2026-11-24T00:00:00` / station `69260` / op `81`; later lines use op `04`.
- `currentState` not mapped (out of approval).

### UI (Vite → proxy `API_PROXY_TARGET=http://127.0.0.1:18080`)

- Card: model `12-9796`, kind `Полувагон`, owner `TОО ТрансКом`, home `68390 — Железорудная`, inventory «Да».
- Day grid column header: **«Время среза»** (not «Время дислокации»).
- Rows show `69260` / `81` / `04` matching API.
- Watched/registry lists remain empty stubs under `dbtc-ro` (not approved JDBC).
- Badge text `R3 · synthetic` in shell is **hardcoded UI chrome**, not the Spring profile.

---

## 5. Time semantics (unchanged contract)

| Item | Decision |
| --- | --- |
| Day filter | `GETTING_DATE` half-open |
| UI label | «Время среза» |
| Fact dislocation time | **not** claimed; `LOCATION_DATE` / DT1 still OPEN |
| API field rename / dual timestamps | **out of scope** — separate decision |

---

## 6. Forbidden (not used)

EXECUTE, DDL, DML, EXPLAIN, Flyway, registry/watched JDBC, `currentState`, import, full ref-table scans.

---

## 7. Offline tests

- `:platform-oracle:test --tests platform.oracle.jdbc.DbtcMappingFactoriesTest` — PASS
- `:platform-app:test --tests platform.app.NoOracleDataSourceTest` — PASS (synthetic still no DataSource)
