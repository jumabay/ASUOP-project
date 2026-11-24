# G31 Gate 0 evidence — `WAGONPACKAGE.GetActiveWagonList`

Дата: **2026-11-24**.  
APPROVALS: `G31 WAGONPACKAGE SPEC/BODY export` **APPROVED WITH CONSTRAINTS** до `2026-11-25 18:00 +05:00`.  
Contour: `tc-asuvp-03:1521/RAC.WORLD`, user `DBTC`.  
Method: `ALL_SOURCE` / `ALL_OBJECTS` / `ALL_ARGUMENTS` **только** `DBTC.WAGONPACKAGE`. **No EXECUTE.**

Raw export (gitignored): `docs/analysis/_oracle_tmp/g31_wagonpackage/`  
(`spec_*.sql` 124 lines, `body_*.sql` 600 lines, arguments CSV). **Not wrapped.**

---

## 1. Visibility

| Object | Status |
| --- | --- |
| `PACKAGE` WAGONPACKAGE | **VALID**, SPEC exported |
| `PACKAGE BODY` WAGONPACKAGE | **VALID**, BODY exported (readable text) |
| `ALL_ARGUMENTS` `GetActiveWagonList` | OUT `RESULT` REF CURSOR only |
| `ALL_ARGUMENTS` `GetWagonList` | OUT `RESULT` REF CURSOR only |

---

## 2. Exact SQL `GetActiveWagonList` (from BODY)

Package constant: `TYPE_CONTAINER CONSTANT NUMBER := 67`.

```sql
SELECT W.ID,
       W.CODE,
       O.NAME       AS WAGONOWNER,
       WT.NAME      AS WAGONTYPE,
       W.TAR_WEIGHT,
       WM.CAPICITY          -- Oracle spelling in WAGONMODELS
  FROM WAGON W,
       OWNERS O,
       WAGONTYPES WT,
       WAGONMODELS WM
 WHERE W.DELETED = 0
   AND W.OWNER_ID = O.ID(+)
   AND WT.ID(+) = W.WAGONTYPE_ID
   AND W.IN_INVENTORY_PARK = 1
   AND W.WAGON_MODEL_ID = WM.ID(+)
   AND W.WAGONTYPE_ID != 67          -- TYPE_CONTAINER
 ORDER BY W.CODE;
```

### Predicate / context

| Factor | Finding | Grade |
| --- | --- | --- |
| Active definition | `DELETED = 0` **and** `IN_INVENTORY_PARK = 1` **and** not container type | **CONFIRMED** |
| User / session / client / group | **нет** в SQL; нет bind от Remoting кроме OUT cursor | **CONFIRMED absent** |
| Soft joins | `OWNERS`, `WAGONTYPES`, `WAGONMODELS` outer `(+)` | **CONFIRMED** |
| vs `GetWagonList` | тот же каркас **без** фильтра `IN_INVENTORY_PARK = 1`; колонки другие (COLOR, IN_INVENTORY_PARK) | **CONFIRMED** |

**Product note:** Oracle «Active» = **инвентарный парк** (не personal watch-list). UI call-site «На слежении» в НУВ по-прежнему не найден; mapping product label ↔ Active — решение владельца при Gate 1.

---

## 3. Состав выходного курсора

| # | Column | Source |
| --- | ---: | --- |
| 1 | `ID` | `WAGON.ID` |
| 2 | `CODE` | `WAGON.CODE` |
| 3 | `WAGONOWNER` | `OWNERS.NAME` |
| 4 | `WAGONTYPE` | `WAGONTYPES.NAME` |
| 5 | `TAR_WEIGHT` | `WAGON.TAR_WEIGHT` |
| 6 | `CAPICITY` | `WAGONMODELS.CAPICITY` |

Нет station/operation/current/time в курсоре Active — карточка по-прежнему через существующие A2 / E-CUR / A1.

---

## 4. Objects for Gate 1 SELECT allow-list (draft)

| Object | Role |
| --- | --- |
| `DBTC.WAGON` | membership + passport keys |
| `DBTC.OWNERS` | soft join owner name / search |
| `DBTC.WAGONTYPES` | soft join kind name / filter |
| `DBTC.WAGONMODELS` | soft join capacity (list optional) |
| Existing card path | `WAGON`+refs / day tape — **уже** под E-CUR / P5-E (не расширять молча; перечислить в Gate 1) |

**Forbidden without separate approve:** EXECUTE `GetActiveWagonList`; unbounded dump without page limit; treating all `WAGON` without inventory predicate.

---

## 5. Gate 1 status

**Still DENIED** — see filled draft in [G31_WATCHED_SOURCE_GATE.md](G31_WATCHED_SOURCE_GATE.md) § Gate 1 (updated) and APPROVALS row.  
**No JDBC watched CODE** in this Gate 0 deliverable.
