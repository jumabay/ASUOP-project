# 54. P5-G — контракт multi-field фильтров (synthetic)

Статус: **CONTRACT** (зафиксирован до CODE P5-G). Дата: 2026-11-24.  
Gate: P5-G APPROVED WITH CONSTRAINTS (Wave 2A). Depends on P5-C additive registry passport fields.

**Не** Oracle LIKE parity. **Не** меняет sort/paging defaults.

---

## 1. Query parameters (`GET /api/v1/wagons`)

| Param | Type | Matching | Empty / omitted |
| --- | --- | --- | --- |
| `search` | string | **contains** on `wagonCode` (существующее поведение; trim; blank→null) | no code filter |
| `owner` | string | **contains**, case-sensitive, on registry `ownerName` (null ownerName → no match) | no owner filter |
| `kind` | string | **equals**, case-sensitive, on registry `wagonKind` (null kind → no match) | no kind filter |
| `page` | int | unchanged (≥0) | default 0 |
| `size` | int | unchanged (1…200) | default 50 |
| `sort` | `code,asc` \| `code,desc` | unchanged; sort **after** filters on `wagonCode` | default `code,asc` |

Same params apply to `GET /api/v1/wagons/watched` for `search`/`owner`/`kind` (passport fields on watched items).

Invalid `page`/`size`/`sort` → existing 400 codes. Unknown query keys ignored.

---

## 2. Combination rules (AND)

All provided filters are **AND**:
`codeContains AND ownerContains AND kindEquals`.

Order of evaluation (synthetic): filter stream → sort by code → page slice.

---

## 3. Compatibility

| Case | Result |
| --- | --- |
| Only `search` (as today) | identical to pre-G behavior |
| No filter params | full catalog / watched set, paged |
| Filters yield zero rows | empty `content`, `totalElements=0` (HTTP 200) |

---

## 4. Frontend

- Registry (and optionally watched): inputs for owner / kind + existing search.
- Deep-link URL: add `owner`, `kind` alongside existing list params (P5-A).
- Empty unknown cells still «—» (from P5-C); filters never invent values.

---

## 5. Out of scope

JDBC predicates, collation claims, prefix-only modes, Enterprise AG Grid filter model, P5-F prefs.
