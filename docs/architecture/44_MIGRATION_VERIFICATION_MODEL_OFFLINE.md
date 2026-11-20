# 44. Модель offline-верификации готовности миграции DBTC → RAILDB

Статус: **DRAFT / docs-only** (2026-11-20).  
Расширяет: [25](25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md), [29](29_RAILDB_MIGRATION_12M_PLAN.md), [40](40_P4_0_RAILDB_STORAGE_CONTRACTS.md), [42](42_ADR0009_DECISION_READINESS.md).  
Журнал: [APPROVALS_TEMPLATE.md](APPROVALS_TEMPLATE.md).

**Запрещено этим документом:** Oracle connect; DDL/DML; Create RAILDB; Flyway apply; DBTC read for migration; RAILDB write; снятие `@Disabled` с JDBC parity; live FTP/RWL; секреты в Git.

**Назначение:** доказать правила отбора и сверки (**12 месяцев истории** + **FULL** активных объектов независимо от даты создания) на синтетических фикстурах **до** разрешения реального migrator.

---

## 0. Scope vs P4-0

| P4-0 (DONE, PR #6) | Этот документ (P4-1 offline) |
| --- | --- |
| Контракт таблиц read-path R1–R3 | Алгоритмы готовности миграции M1–M8 как спецификация |
| Mapping DTO ↔ RAIL_* | Матрица ожидаемых counts / orphans / chronology на фикстурах |
| Draft DDL V0/V0b | Verification harness design (DO NOT EXECUTE on RAC) |

---

## 1. Миграционный контракт

| Параметр | Правило |
| --- | --- |
| `cutoff` | `cutover_ts − 12 calendar months` |
| История ≥ cutoff | → RAILDB (`12M`) |
| История < cutoff | → ARCHIVE_DBTC, кроме BOOTSTRAP |
| Актуальные сущности | **FULL** независимо от даты создания |
| Открытые операции | **OPEN** (RP1 / `CURRENTREPAIR_ID`) независимо от давности |
| Ось ленты до DT1 | `GETTING_DATE` / `dislocation_ts` |
| Дубликаты ленты до NK1 | retain all; tech `event_id`; `provenance_key` |
| Watched (M8) | **BLOCKED** до G31; не заполнять из synthetic 60 |

---

## 2. Трёхуровневая готовность

| Уровень | Среда | Сейчас |
| --- | --- | --- |
| **L0** Spec + synthetic fixture selector/reconcile | docs + future JUnit | **Разрешён** (docs; CODE offline — отдельная строка) |
| **L1** Bounded dictionary probes | APPROVED RO + DBA tape plan | **DENIED** |
| **L2** Dry-run migrator | Create + Flyway + DBTC read + RAILDB write | **DENIED** |

---

## 3. L0 фикстуры (synthetic only)

Рукотворные JSON с кодами `W000001…`, без prod/TEST выгрузок:

- `refs`, `wagons` (старые create dates), `current`, `repairs`, `stock`
- `kits` + `tape` (archive / bootstrap / 12M / intentional duplicates / getting≠location)
- `expected_selection.json`, `expected_reconcile.json`

Масштаб: десятки–сотни строк (edge cases > объём). Не использовать R3 watched.json как источник M8.

---

## 4. Reconciliation checks (спецификация)

| Группа | Примеры инвариантов |
| --- | --- |
| Counts | REF/WAGON/CURRENT/OPEN/STOCK; tape-by-day; watched SKIP |
| Duplicates | retain; unique `event_id`; stable `provenance_key`; no DISTINCT |
| Chronology | window on getting; bootstrap = max ≤ cutoff; no future in primary load |
| FK/orphan | wagon/station/event/kit soft-null; hard FK = 0 after load (L2) |
| Idempotency | kit UK `(filename,type)` → 1 header on replay |

---

## 5. OPEN gaps blocking migrate apply

| ID | Status | Hold |
| --- | --- | --- |
| NK1 | BLOCKED | retain all; no UK on (wagon, getting) |
| DT1 | BLOCKED | filter on GETTING |
| G31 | OPEN | watched empty |
| KIT_FK 12м | UNKNOWN | nullable kit_id |
| R1-Q-TZ | OPEN | half-open day documented |

M1–M5 selection rules можно зеленеть на L0; M6 physical idempotency и M8 — нет.

---

## 6. Next Database tasks

### Offline-allowed

1. Commit this doc (docs-only).
2. Golden fixture pack + SelectionEngine unit tests (needs **CODE offline APPROVED**).
3. Reconcile report schema draft (DO NOT EXECUTE).
4. DBA handoff text for tape plan NK1/DT1 + G31 SPEC (no execute).

### Needs new APPROVALS

Create RAILDB → Flyway V0/V0b → TEST JDBC → DBA probes NK1/DT1 → G31 pick → migrator DBTC read + RAILDB write → L2 dry-run → cutover.

**Database verdict:** offline verification model **READY TO DOCUMENT**; migrate apply **BLOCKED**.
