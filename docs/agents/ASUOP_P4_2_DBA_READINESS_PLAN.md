# ASUOP — план этапа P4-2: DBA readiness + NK1/DT1/G31 offline research

Дата: **2026-11-20**. База: `main` после PR #15 (APPROVALS P4-2).  
**Docs-only.** Не заменяет [APPROVALS_TEMPLATE.md](../architecture/APPROVALS_TEMPLATE.md).

---

## 1. Scope

| In | Out |
| --- | --- |
| Checklist [43](../architecture/43_ADR0009_DBA_CREATE_CHECKLIST.md) | Create RAILDB |
| Handoff [48](../architecture/48_DBA_HANDOFF_PACKAGE.md) | Oracle connect / JDBC |
| Research [49](../architecture/49_NK1_DT1_G31_OFFLINE_RESEARCH.md) | Flyway apply / migrate |
| Go/No-Go [50](../architecture/50_P4_2_GO_NOGO_CREATE_RAILDB_TEST.md) | Real accounts / live FTP·RWL |
| Non-executable SQL drafts under `sql/dba/p4-2/` | Claiming UNKNOWN as CONFIRMED |

---

## 2. Agent map

| Agent | Deliverable |
| --- | --- |
| Database | 43 + 48 |
| Backend (+ DB rigor) | 49 + optional `sql/dba/p4-2/*` |
| Lead | 50 + this plan + PR |
| QA | Docs review; Ask-mode note |

---

## 3. Stop line

Allowed after P4-2 merge: human DBA fills 43; owner adds **Create RAILDB TEST** APPROVED.  
Forbidden: Create/connect/DDL/migrate without new rows.
