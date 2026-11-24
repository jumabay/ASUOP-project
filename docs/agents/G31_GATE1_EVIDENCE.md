# P5-G31 Gate 1 — inventory park JDBC + card drill-down

Дата: **2026-11-24**.  
APPROVALS: `P5-G31 JDBC Active list («Инвентарный парк») + card drill-down` **APPROVED WITH CONSTRAINTS** до `2026-11-25 18:00 +05:00`.  
Contour: `tc-asuvp-03:1521/RAC.WORLD`, `DBTC`/`DBTC`, password `env:ASUOP_ORACLE_PASSWORD` (не логировался).  
Profile: **`dbtc-ro`**. Default `synthetic` unchanged.  
Паритет старому «На слежении» **не** заявляется.

## Membership

| Item | Value |
| --- | --- |
| Predicate | `DELETED=0 AND IN_INVENTORY_PARK=1 AND WAGONTYPE_ID<>67` |
| `membershipSource` | `DBTC_ACTIVE_INVENTORY_PARK` |
| API | `GET /api/v1/wagons/watched` |
| List tables | `WAGON` + soft `OWNERS`/`WAGONTYPES`/`WAGONMODELS` |
| Page | default 50, max 100; sort `CODE,ID`; search CODE literal contains (bind) |

## Live SQL probes (tools/p5e-ro-smoke)

| Probe | Result |
| --- | --- |
| `G31InventoryParkLiveProbe` | total **34332**; page 50 |
| `G31InventoryParkCardPickProbe` | CONTROL `54787361` in park; picks `28024669`/`28025377`/`28053023` with day lines |
| `G31InventoryParkScenarioProbe` | search CONTROL hits=1; 3 cards + day `2026-11-24` lines=11 (SQL cap path) |

## Live API (`dbtc-ro` `:18085`)

| Call | Result |
| --- | --- |
| `GET /api/v1/platform/runtime` | `DBTC_RO`, `watchedConnected=true` |
| `GET /api/v1/wagons/watched?page=0&size=50` | `membershipSource=DBTC_ACTIVE_INVENTORY_PARK`, **total=34332**, rows=50 |
| `GET .../watched?search=54787361` | total=1, code=`54787361` |
| Card+day for `28024669` / `28025377` / `28053023` | `inventoryPark=true`, `currentState.stationCode=72000`, `operationCode=03`, day lines **16** each (`2026-11-24`) |

## Offline tests

- `:platform-oracle` `DbtcInventoryParkWatchedSqlTest`, `DbtcLikeEscapeTest`
- `:modules:fleet` `WatchedWagonQueryTest`
- `:platform-app` `WagonControllerMockMvcTest`, `SyntheticWagonApiIntegrationTest`
- Frontend Vitest (watched membership / App / listPaging / deepLink) — see PR checks

## Fixes during Gate 1

1. Text-block `WHERE` + clause without space → `WHEREw.` (ORA-00933) — fixed.
2. Oracle `''` → NULL into `WatchedWagonItem` → NPE `model` — `nullToEmpty` in adapter.

## Forbidden / out of scope

EXECUTE package; writes/import/membership change; registry JDBC; T2; claiming old-screen «На слежении» parity; auto-merge.
