# ASUOP — план этапа P4-1: scale readiness + migration verification (offline)

Дата: **2026-11-20**. База: `main` (после PR #4…#7).  
Аудиты: `/database`, `/backend`, `/frontend`, `/qa` + lead.  
**Docs-only.** Не заменяет [APPROVALS_TEMPLATE.md](../architecture/APPROVALS_TEMPLATE.md).

---

## 1. Состояние и ADR-0009

| Проверка | Факт |
| --- | --- |
| PR #4 P0-5/P1-4 smoke | **MERGED** |
| P4-0 Storage Contracts | **DONE** (PR #6) |
| ADR-0009 Decision Readiness | PR #7 |
| ADR-0009 в журнале | **ACCEPTED (architecture direction only)** — см. строку Accept; **не** Create/JDBC/migrate/Flyway |
| Create RAILDB / Flyway / DBTC read / RAILDB write / JDBC TEST | **DENIED** |
| NK1 / DT1 / G31 | OPEN / BLOCKED |

Файл ADR-0009 в репозитории синхронизируется с журналом: direction Accept ≠ execution.

Не дублировать: работающий synthetic R1–R3 UI/API; P4-0 contracts; P0 CI.

---

## 2. Deliverables этого docs-пакета

| Doc | Содержание |
| --- | --- |
| [44](../architecture/44_MIGRATION_VERIFICATION_MODEL_OFFLINE.md) | L0 verification: 12M + FULL bootstrap без Oracle |
| [45](../architecture/45_API_SCALE_READINESS_18K.md) | API готовность к ~18k; synthetic 250/60 gap |
| [46](../architecture/46_UI_SCALE_UX_PROPOSAL_18K.md) | UX proposal: debounce, deep-link, card/day split, future repairs |
| [47](../architecture/47_SCALE_MIGRATION_TEST_STRATEGY.md) | Test strategy T0–T4 + migration risk detectors |

---

## 3. Задачи по агентам

### Database

| ID | Задача | Класс |
| --- | --- | --- |
| D1 | Fixture pack `fixtures/migration/v1` + SelectionEngine tests | Offline CODE — **needs APPROVED** |
| D2 | Reconcile report schema draft | Offline docs/CODE |
| D3 | DBA handoff NK1/DT1/G31 (text only) | Offline docs |
| D4 | Create RAILDB / Flyway / migrator execute | **DENIED** until APPROVALS |

### Backend

| ID | Задача | Класс |
| --- | --- | --- |
| B1 | Synthetic catalog ≥18k + paging tests | Offline CODE — **needs APPROVED** |
| B2 | Soft in-memory perf smoke / shared catalog bean | Offline CODE |
| B3 | JDBC adapters / enable parity | **DENIED** |
| B4 | REST contract break (keyset, new sorts) | **needs API APPROVED** |

### Frontend

| ID | Задача | Класс |
| --- | --- | --- |
| F1 | Debounce search; Enter/Найти; showTotal; deep-link | Offline CODE under R1–R3 — **needs UX CODE APPROVED** |
| F2 | Split card vs day fetch; disabled stubs → repairs/parts | Offline CODE |
| F3 | Real watched-on-card / rich registry DTOs | **needs API APPROVED** |
| F4 | Repairs/warehouse screens | **DENIED** until product + API |

### QA

| ID | Задача | Класс |
| --- | --- | --- |
| Q1 | Keep T0 CI green | Allowed |
| Q2 | `@Tag("scale")` / `testScale` + Playwright `@scale` | After Scale CODE APPROVED |
| Q3 | L0 migrator unit detectors | With D1 |
| Q4 | JDBC parity / live migrate reconcile | **BLOCKED** |

---

## 4. Рекомендуемая последовательность

```text
Merge this docs-only PR
  → Owner adds APPROVALS rows:
       (A) Scale offline fixtures T1–T4 + tagged tests
       (B) FE UX scale pack (R1–R3 contracts unchanged)
       (C) Migration L0 fixture harness CODE
  → feature/ branches for A/B/C (no Oracle)
  → DBA fills checklist 43 → separate APPROVED Create RAILDB
  → Flyway → TEST JDBC → probes NK1/DT1 → G31 → migrator
```

---

## 5. Stop line

| Allowed now | Forbidden |
| --- | --- |
| Docs 44–47; planning | Create RAILDB |
| (after new APPROVED) synthetic 18k + UX polish + L0 harness | Flyway apply; DBTC migrate read; RAILDB write |
| | Production/TEST Oracle without row; live FTP/RWL |
| | Claiming contractual p95 without measured log |

**Lead verdict:** implementation of scale/migration code **not** covered by existing APPROVED rows beyond docs. This PR is **docs-only**; stop for human merge + new APPROVALS before coding.
