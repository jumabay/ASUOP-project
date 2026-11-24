# G31 — источник «На слежении» / Active: gate после BODY

Дата: **2026-11-24** (обновлено после Gate 0 export).  
База: `main` @ `e3af8c9` (после P5-E-CUR).  
Evidence: [G31_GATE0_EVIDENCE.md](G31_GATE0_EVIDENCE.md). Raw BODY/SPEC: gitignored `_oracle_tmp/g31_wagonpackage/`.  
Тип: **research + gate** (без CODE JDBC watched). **Не** расширяет P5-E / E-CUR CONTROL.

Evidence base: [11-nuff-active-wagon-list.md](../analysis/11-nuff-active-wagon-list.md), [08_GAPS G31](../analysis/08_GAPS_AND_UNCERTAINTIES.md), [49](../architecture/49_NK1_DT1_G31_OFFLINE_RESEARCH.md), [38](../architecture/38_P3C0_DBA_PACKAGE_SOURCE_REQUEST.md), P5-D honesty.

---

## 1. Источник (после Gate 0)

| Item | Result | Grade |
| --- | --- | --- |
| Remoting | `IGivingDepartureCall.GetActiveWagons` → `GivingDepartureLoader.GetActiveWagonList` | **CONFIRMED** |
| Oracle | `WagonPackage.GetActiveWagonList(Result OUT SYS_REFCURSOR)` — клиентских filter-аргументов **нет** | **CONFIRMED** |
| Membership SQL | `WAGON.DELETED=0` AND `IN_INVENTORY_PARK=1` AND `WAGONTYPE_ID <> 67` (`TYPE_CONTAINER`) | **CONFIRMED** |
| User / client / group | **нет** в SQL BODY | **CONFIRMED absent** |
| Cursor cols | `ID, CODE, WAGONOWNER, WAGONTYPE, TAR_WEIGHT, CAPICITY` | **CONFIRMED** |
| vs `GetWagonList` | тот же каркас **без** `IN_INVENTORY_PARK=1`; другие колонки | **CONFIRMED** |
| UI НУВ «На слежении» → Active | call-site не найден | **UNKNOWN** |

### Что это **не** является

| Кандидат | Почему |
| --- | --- |
| Все `WAGON` / полный парк | Нет фильтра inventory / deleted / container |
| `GetWagonList` | «Обозреватель» — без `IN_INVENTORY_PARK=1` |
| Synthetic FIXTURE 60 | Demo honesty (P5-D) ≠ legacy Active |
| E-CUR / CONTROL card SELECT | Один вагон; не membership |

**Product note:** Oracle Active = **инвентарный парк** (не personal watch-list). Привязка UI «На слежении» ↔ Active — решение владельца при Gate 1.

---

## 2. Gate 0 — discovery

| Решение | Статус | Детали |
| --- | --- | --- |
| G31 WAGONPACKAGE SPEC/BODY export | **APPROVED WITH CONSTRAINTS** (time-boxed) → **DONE** | SPEC 124 + BODY 600 lines via `ALL_SOURCE`; not wrapped. Contour `tc-asuvp-03` / `DBTC` до `2026-11-25 18:00 +05`. No EXECUTE. Evidence: [G31_GATE0_EVIDENCE.md](G31_GATE0_EVIDENCE.md) |

---

## 3. Конкретная строка Gate 1 (для утверждения — пока DENIED)

| Решение | Статус | Детали |
| --- | --- | --- |
| P5-G31 JDBC watched list + card drill-down | **DENIED UNTIL APPROVED** | Владелец (черновик после Gate 0, 2026-11-24): **отдельная** строка — **не** расширение E-CUR/P5-E. Contour предложить при approve: `tc-asuvp-03:1521/RAC.WORLD`, user `DBTC`, `env:ASUOP_ORACLE_PASSWORD`, time-box указать. **Разрешено после APPROVED:** (1) SELECT-only membership = predicate Gate 0 на `DBTC.WAGON` + soft `DBTC.OWNERS`, `DBTC.WAGONTYPES`, `DBTC.WAGONMODELS`; (2) server **count** / **page** / **search** (CODE contains) / **sort** (`CODE` asc\|desc); max page size 50–100; timeout 30s; (3) drill-down `GET /api/v1/wagons/{code}` + day history (паспорт + currentState E-CUR + A1 T1) — smoke N≥3 codes from Active page; (4) `membershipSource` honesty ≠ FIXTURE; read error ≠ empty list; synthetic FIXTURE retained on default profile. **Запрещено:** EXECUTE `GetActiveWagonList`; full-park в браузер; client-only pagination of entire result; writes/import/membership change; T2; silent allow-list expand beyond listed objects; auto-merge; claiming per-user watch without evidence. Draft SQL: ниже. |

### Draft SQL (do not run until Gate 1 APPROVED)

**Count**

```sql
SELECT COUNT(*) AS total_elements
  FROM dbtc.wagon w
 WHERE w.deleted = 0
   AND w.in_inventory_park = 1
   AND w.wagontype_id != 67
   AND (:search IS NULL OR w.code LIKE '%' || :search || '%');
```

**Page**

```sql
SELECT w.id,
       w.code,
       o.name  AS owner_name,
       wt.name AS wagon_kind,
       w.tar_weight,
       wm.capicity AS model_capacity
  FROM dbtc.wagon w,
       dbtc.owners o,
       dbtc.wagontypes wt,
       dbtc.wagonmodels wm
 WHERE w.deleted = 0
   AND w.owner_id = o.id(+)
   AND wt.id(+) = w.wagontype_id
   AND w.wagon_model_id = wm.id(+)
   AND w.in_inventory_park = 1
   AND w.wagontype_id != 67
   AND (:search IS NULL OR w.code LIKE '%' || :search || '%')
 ORDER BY w.code ASC   -- or DESC via app
 OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY;
```

**Card smoke (existing):** reuse E-CUR / A1 paths for codes from Active page — **не** re-open unbounded tape; objects already gated separately.

---

## 4. Out of scope until later gates

Writes, import, change inventory membership, T2, closing DT1/R1-Q-TZ, EXECUTE Active cursor dump for bulk export.

---

## 5. Статус сейчас

| Item | |
| --- | --- |
| Предикат Active | **CONFIRMED** (inventory park, not container) |
| UI «На слежении» = Active | **UNKNOWN** |
| Gate 0 | **DONE** |
| Gate 1 JDBC CODE | **BLOCKED** — ждёт APPROVED |
| Следующий шаг владельца | Approve Gate 1 (или отклонить / alternate source) |
