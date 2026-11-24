# G31 — Active / «Инвентарный парк»

Дата: **2026-11-24**.  
База: Gate 0 BODY **DONE** ([G31_GATE0_EVIDENCE.md](G31_GATE0_EVIDENCE.md)).  
Продукт: раздел **«Инвентарный парк»** = предикат `GetActiveWagonList`.  
Паритет старому экрану «На слежении» **не заявляется**.

## Gate 0 — DONE

Predicate CONFIRMED: `DELETED=0 AND IN_INVENTORY_PARK=1 AND WAGONTYPE_ID<>67`.  
Cursor: `ID, CODE, WAGONOWNER, WAGONTYPE, TAR_WEIGHT, CAPICITY`.  
Session/user/client/group: **absent** in BODY.

## Gate 1 — APPROVED WITH CONSTRAINTS (time-boxed)

| Item | Value |
| --- | --- |
| Contour | `tc-asuvp-03:1521/RAC.WORLD`, `DBTC`, до `2026-11-25 18:00 +05` |
| API | existing `GET /api/v1/wagons/watched` under `dbtc-ro` |
| `membershipSource` | `DBTC_ACTIVE_INVENTORY_PARK` |
| List tables | `WAGON` + soft `OWNERS` / `WAGONTYPES` / `WAGONMODELS` |
| Page | default 50, max 100; sort `CODE, ID`; search CODE literal contains (bind) |
| Cards | existing A2 + E-CUR current + A1 day; smoke 3 codes; day `2026-11-24` ≤501 |
| Forbidden | EXECUTE; writes/import; registry JDBC; T2; old-screen parity claim; auto-merge |

Synthetic default unchanged (FIXTURE honesty for demo tab).
