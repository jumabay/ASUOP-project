# 50. P4-2 — Go/No-Go для Create RAILDB TEST

Дата: **2026-11-20**. Статус: **docs-only**.  
Журнал: [APPROVALS_TEMPLATE.md](APPROVALS_TEMPLATE.md) — строка **P4-2** = APPROVED WITH CONSTRAINTS (docs/research).  
Связано: [43](43_ADR0009_DBA_CREATE_CHECKLIST.md), [48](48_DBA_HANDOFF_PACKAGE.md), [49](49_NK1_DT1_G31_OFFLINE_RESEARCH.md), [24](24_RAILDB_DBA_INSTRUCTION.md), [42](42_ADR0009_DECISION_READINESS.md).

**Этот документ не разрешает Create RAILDB.** Создание возможно только после **отдельной** строки APPROVALS `Create Oracle RAILDB` (TEST scope) и заполненного checklist 43.

---

## 1. Предварительный порядок (TEST only)

```text
P4-2 docs (this pack) MERGED
  → DBA fills checklist 43 (UNKNOWN → confirmed values; no invent)
  → Owner reviews Go criteria below
  → Separate APPROVED: Create Oracle RAILDB (TEST only)
  → DBA creates empty RAILDB TEST + empty RAIL_APP (no business DML)
  → Separate APPROVED: RAILDB Flyway apply (TEST)   [later]
  → Separate APPROVED: TEST JDBC / enable adapters   [later]
  → Separate APPROVED: DBTC read for migration       [later]
  → NK1/DT1 probes + G31 package export              [later, own scopes]
```

Production Create / Production JDBC / cutover — **out of scope** until additional rows.

---

## 2. Go criteria (все обязательны)

| # | Criterion | Evidence |
| --- | --- | --- |
| G1 | Topology A or B chosen; SID/PDB/service names recorded | Checklist 43 signed by DBA |
| G2 | Host capacity (CPU/RAM/disk/IOPS) confirmed for TEST sizing | DBA note (not guessed) |
| G3 | Charset decision recorded (`AL32UTF8` preferred vs DBTC NLS compat) | Checklist 43 |
| G4 | Partitioning license YES/NO → V0 or V0b selected | License evidence path |
| G5 | Tablespace + FRA + RMAN policy for TEST agreed | Checklist 43 |
| G6 | TEST vs Production isolation documented (no shared writer with RAC `DBTC` for app) | Handoff 48 |
| G7 | Account model least privilege designed (no passwords in Git) | Checklist 43 |
| G8 | Empty-create verification plan agreed (DUAL/service/NLS/empty schema checks) | Handoff 48 |
| G9 | Journal row **Create Oracle RAILDB** = APPROVED with **TEST-only** constraint | APPROVALS |
| G10 | Rollback of empty TEST instance documented (drop PDB/SID per DBA policy) | Checklist 43 |

---

## 3. No-Go (любой пункт = стоп)

| # | Condition |
| --- | --- |
| N1 | Any UNKNOWN treated as CONFIRMED |
| N2 | Attempt to create on Production RAC/`DBTC` or reuse 5 GiB RAC `RAIL_APP` as target store |
| N3 | No separate APPROVED Create row |
| N4 | Plan includes Flyway apply / JDBC / migrate in the same Create step without own APPROVED rows |
| N5 | Watched/M8 population promised before G31 source pick |
| N6 | Secrets/passwords in repository or chat |
| N7 | NK1/DT1/G31 claimed CLOSED without DBA evidence |

---

## 4. Отдельные APPROVALS для следующих шагов

| Next decision | Status today | Opens |
| --- | --- | --- |
| Create Oracle RAILDB (**TEST**) | **DENIED** until new row | Empty instance/PDB + empty `RAIL_APP` |
| Create Oracle RAILDB (Production) | **DENIED** | Prod topology |
| RAILDB Flyway apply / business DDL | **DENIED** | V0/V0b materialize |
| R1 JDBC / TEST DataSource / enable parity | **DENIED** | App read path |
| DBTC read for migration | **DENIED** | Migrator SELECT |
| RAILDB write (migrator/app) | **DENIED** | Load |
| DBA tape plan execute (NK1/DT1 EXPLAIN/bounded SELECT) | needs own scoped APPROVED | Close/keep BLOCKED with facts |
| G31 package SPEC export / membership pick | needs own scoped APPROVED | M8 / watched truth |
| Accept ADR-0009 (execution path) | **DENIED** (direction docs ≠ Create) | Policy sync 05/06/15 |
| Live FTP/RWL | **DENIED** | Intake |

---

## 5. Manager recommendation

**P4-2 docs pack** prepares a safe path to **empty TEST RAILDB**.  
**Do not** start Create until G1–G10 are green and a dedicated APPROVED row exists.  
NK1/DT1/G31 remain **OPEN/BLOCKED** for migration semantics; they are **not** hard blockers for creating an *empty* TEST database, but **are** blockers for migrate apply and watched fill.
