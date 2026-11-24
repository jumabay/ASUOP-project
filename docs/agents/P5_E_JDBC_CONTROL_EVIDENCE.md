# P5-E JDBC CONTROL evidence (allow-list delta + A1/A2)

Дата: **2026-11-24** (обновлено к merge-ready).  
Окно: до `2026-11-25 18:00 +05:00`.  
APPROVALS: `P5-E allow-list delta: ref SELECT + CONTROL JDBC/UI`.  
Target: `tc-asuvp-03:1521/RAC.WORLD`, user `Dbtc` / schema `DBTC`.  
Profile: **`dbtc-ro`** (opt-in). Default remains **`synthetic`**.

---

## Verdict

| Scope | Status |
| --- | --- |
| CONTROL `54787361` / day `2026-11-24` Oracle→API→UI | **VERIFIED** (live once under approval; no required re-run) |
| Global `inventoryPark` semantics | **OPEN** (only `1 → true` confirmed) |
| DT1 / factual dislocation time | **OPEN** (`GETTING_DATE` = day/slice axis only) |

---

## 1. Display columns (models / kinds / owners) — CONFIRMED

| Object | Display column for A2 | CONTROL sample |
| --- | --- | --- |
| `DBTC.WAGONMODELS` | **`NAME`** | id=`2061` → `12-9796` |
| `DBTC.WAGONTYPES` | **`NAME`** | id=`2` → `Полувагон` |
| `DBTC.OWNERS` | **`NAME`** | id=`679` → `TОО ТрансКом` |

---

## 2. Business codes (leading zeros preserved)

| Source | Column | Value |
| --- | --- | --- |
| home station | `STATIONS.CODE` | `68390` |
| location | `STATIONS.CODE` | `69260` |
| operation 142 | `OPERATIONS.IDCODE` | `81` |
| operation 4 | `OPERATIONS.IDCODE` | **`04`** (string; IDCODE preferred over CODE) |

Resolver: `DbtcOperationCode.resolve(idcode, code)` via `ResultSet.getString` only — no numeric getters.

---

## 3. `IN_INVENTORY_PARK`

| Raw | Mapped | Note |
| --- | --- | --- |
| `1` | `true` | **CONFIRMED** on CONTROL |
| `null` | `null` (unknown UI `—`) | no invented false |
| `0` / other | `null` (unknown) | **no** `raw != 0` rule; general semantics **OPEN** |

---

## 4. Oracle → API → UI (CONTROL) — VERIFIED

### API (`dbtc-ro`)

- Card: model/kind/owner/home codes as above; `inventoryPark=true`; `currentState=null`.
- Day: **14** lines on `GETTING_DATE`; ops `81`/`04`; station `69260`.
- `dislocationTime` = getting axis (**PROVISIONAL** slice time).

### UI

- Header label from `GET /api/v1/platform/runtime` → `displayLabel` (e.g. `DBTC read-only (time-boxed)`). **No** hardcoded `R3 · synthetic`. Payload has **no** JDBC URL / user / password.
- Day grid header: **«Время среза»**.
- Registry/watched empty: **«Источник не подключён»** (`registryConnected`/`watchedConnected=false`, watched `SOURCE_NOT_CONNECTED`).

---

## 5. Time semantics (unchanged API contract)

| Item | Decision |
| --- | --- |
| Day filter | `GETTING_DATE` half-open |
| UI label | «Время среза» |
| Fact dislocation | **not** claimed; DT1 OPEN |
| API dual timestamps | out of scope |

---

## 6. Forbidden (not used)

EXECUTE, DDL, DML, EXPLAIN, Flyway, registry/watched JDBC, `currentState`, import, full ref scans.

---

## 7. Offline verification (merge-ready)

- Gradle: `:platform-oracle:test`, `:platform-app:test` (incl. `NoOracleDataSourceTest`, synthetic runtime honesty)
- Frontend: Vitest (incl. `platformRuntime.test.tsx`), `npm run build`
- Playwright: synthetic/offline e2e (mocked/new honesty via Vitest; e2e against synthetic API)
