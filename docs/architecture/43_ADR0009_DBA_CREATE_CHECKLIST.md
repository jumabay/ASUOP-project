# 43. ADR-0009 — DBA Create Checklist (RAILDB)

Дата пакета: **2026-11-20**.  
Gate: P4-2 **APPROVED WITH CONSTRAINTS** — [APPROVALS_TEMPLATE.md](APPROVALS_TEMPLATE.md) @ `62fb1ff`.  
Статус этого файла: **docs-only readiness**. **Не выполнять** Create / DDL / Flyway / connect / выдачу учёток.

Источники (без выдуманных CONFIRMED-фактов):

- [ADR-0009](decisions/ADR-0009-raildb-primary-store.md)
- [24_RAILDB_DBA_INSTRUCTION.md](24_RAILDB_DBA_INSTRUCTION.md)
- [42_ADR0009_DECISION_READINESS.md](42_ADR0009_DECISION_READINESS.md)
- [30_RAILDB_SIZING_AND_PERF.md](30_RAILDB_SIZING_AND_PERF.md)
- [40_P4_0_RAILDB_STORAGE_CONTRACTS.md](40_P4_0_RAILDB_STORAGE_CONTRACTS.md) §7 (V0 / V0b)
- Пакет handoff: [48_DBA_HANDOFF_PACKAGE.md](48_DBA_HANDOFF_PACKAGE.md)

**Правило статусов:** `OPEN` = не решено; `TBD` = ждёт DBA/владельца; `CONFIRMED` только после явной подписи DBA/владельца в этом чеклисте или новой строке APPROVALS. Любой параметр без доказательства помечать **UNKNOWN / TBD — DBA must confirm**.

---

## 0. Gate snapshot (журнал)

| Item | APPROVALS (as of P4-2) | Implication |
| --- | --- | --- |
| P4-2 DBA Readiness (docs) | **APPROVED WITH CONSTRAINTS** | Этот чеклист и handoff **разрешены** как документы |
| Accept ADR-0009 | **DENIED UNTIL APPROVED** | Create / Flyway / write **не** открыты журналом |
| Create Oracle RAILDB | **DENIED UNTIL APPROVED** | Пустая TEST БД — только после новой строки |
| RAILDB Flyway / business DDL | **DENIED UNTIL APPROVED** | V0/V0b черновики не apply |
| DBTC read for migration / RAILDB write | **DENIED UNTIL APPROVED** | Migrator не стартует |
| Production access / TEST JDBC | **DENIED UNTIL APPROVED** | Нет connect из проекта |

**UNKNOWN / TBD — DBA must confirm:** заголовок ADR-0009 на диске может расходиться с журналом APPROVALS (Accept). **Источник истины для исполнения — APPROVALS.** Владелец должен согласовать статус Accept до Create.

---

## 1. Pre-create decisions (Owner + DBA)

| # | Action / decision | Owner | DBA | Status |
| --- | --- | --- | --- | --- |
| 1.1 | Accept ADR-0009 direction (`RAILDB` primary; `DBTC` migrate+archive) recorded in APPROVALS | Owner | Inform | **OPEN** — journal DENIED |
| 1.2 | Choose topology **A** (dedicated SID/instance) **or** **B** (CDB/PDB) | Owner + DBA | Decide/confirm | **UNKNOWN / TBD — DBA must confirm** (prefer A per doc 42) |
| 1.3 | Exact `ORACLE_SID` / db_name / PDB name / **service name** (not examples) | — | DBA | **UNKNOWN / TBD — DBA must confirm** |
| 1.4 | Host candidate `TC-ASUVP-03`: CPU, RAM, free disk, IOPS, OS version | — | DBA | **UNKNOWN / TBD — DBA must confirm** |
| 1.5 | Oracle version for `RAILDB` (12.2 now vs 19c) vs client `ojdbc10:19.33.0.0` | Owner + DBA | Confirm | **UNKNOWN / TBD — DBA must confirm** |
| 1.6 | Charset: prefer `AL32UTF8` vs DBTC-compat `CL8MSWIN1251` (+ NLS / `orai18n`) | Owner + DBA | Confirm | **UNKNOWN / TBD — DBA must confirm** |
| 1.7 | Oracle **Partitioning** option licensed on target? → **V0** vs **V0b** | Owner (license) | Confirm | **UNKNOWN / TBD — DBA must confirm** |
| 1.8 | License coverage for additional instance/PDB | — | DBA | **UNKNOWN / TBD — DBA must confirm** |
| 1.9 | Capacity: tablespace ≥ ~300 GiB (prefer 500), FRA ≥ ~500 GiB, quarantine OS ≥ ~100 GiB — **estimates** from doc 24/30 | Owner | Size/confirm | **OPEN** (estimates only; not measured) |
| 1.10 | RAM instance hypothesis 16–32 GiB | — | DBA | **UNKNOWN / TBD — DBA must confirm** |
| 1.11 | RMAN policy: ARCHIVELOG; daily incremental + weekly full; retention days | Owner + DBA | Define | **UNKNOWN / TBD — DBA must confirm** |
| 1.12 | TEST vs Production: separate instance/PDB/service; no shared writer path with prod | Owner + DBA | Design | **OPEN** (policy required before Create) |
| 1.13 | Account model: empty schema `RAIL_APP` (runtime); migrator account name (`RAIL_MIGRATE` TBD); secrets out of Git | Owner | Create later | **OPEN** — **no passwords / no accounts in this package** |
| 1.14 | Do **not** use RAC schema `RAIL_APP` (5 GiB quota) as prod store; do **not** rename `RAC.WORLD` | Owner | Enforce | Policy from ADR-0009 / 24 — **CONFIRMED as project rule** (not host measurement) |
| 1.15 | New APPROVALS row: **Create empty RAILDB TEST** (not Production) | Owner | Execute only after | **DENIED** until approved |

---

## 2. Topology identifiers (examples only)

Fill exact values only after DBA measurement. Examples from ADR-0009 / doc 24 — **not** production names.

| Field | Topology A (example) | Topology B (example) | Actual | Status |
| --- | --- | --- | --- | --- |
| Form | Dedicated instance | PDB in CDB | | **TBD** |
| Host | `tc-asuvp-03` (candidate) | same candidate | | **UNKNOWN / TBD — DBA must confirm** |
| SID / db_name | `RAILDB` (≤8 if SID rule applies) | CDB name **TBD** | | **TBD** |
| PDB name | n/a | `RAILDB` | | **TBD** |
| Service name | e.g. `RAILDB` / `RAILDB.WORLD` | PDB service from `V$SERVICES` | | **TBD** |
| Listener | shared or dedicated; **must not** replace `RAC.WORLD` | same | | **TBD** |
| JDBC URL shape (no secrets) | `jdbc:oracle:thin:@//tc-asuvp-03:1521/RAILDB.WORLD` | `jdbc:oracle:thin:@//tc-asuvp-03:1521/<PDB_SERVICE>` | | **TBD** after create |

Existing legacy service `RAC.WORLD` / schema `DBTC` — **do not alter** for this project.

---

## 3. Capacity & backup (estimates — not measured)

| # | Check | Source estimate | Owner | DBA | Status |
| --- | --- | --- | --- | --- | --- |
| 3.1 | Tape 12m raw ~21–25 GiB; data+index ~70–100 GiB | docs 24/30 | Inform | Validate vs live stats later | **ESTIMATE ONLY** |
| 3.2 | Tablespace Y1 + headroom ≥ **300 GiB** usable (prefer **500**) | 24 §4 | Approve budget | Allocate | **TBD** |
| 3.3 | FRA / backup area ≥ **1× full + archives** (orient **≥ 500 GiB**) | 24 §4 | Approve | Allocate | **TBD** |
| 3.4 | Quarantine file disk (OS, not tablespace) ≥ **100 GiB** | 24 §4 | Approve | Provide path | **TBD** |
| 3.5 | Online redo / FRA peak during bulk load (tens–hundreds GiB) planned | 24 §4 | Inform | Plan | **UNKNOWN / TBD — DBA must confirm** |
| 3.6 | RMAN full size orient ≥ **150 GiB** at start; retention multiplies | 24 §4 | Policy | Implement | **ESTIMATE ONLY** |
| 3.7 | Reject 5 GiB RAC quota as sizing guide | ADR-0009 / 24 | — | — | Project rule |
| 3.8 | Restore drill on copy **before** cutover | ADR-0009 / 24 | Require | Execute later | **OPEN** (not in P4-2) |
| 3.9 | DBTC/RAC RMAN continues under current owner — not cancelled by RAILDB | 24 §6 | — | Confirm | **UNKNOWN / TBD — DBA must confirm** |

---

## 4. Charset & Partitioning → DDL variant

| # | Decision | Options | Owner | DBA | Status |
| --- | --- | --- | --- | --- | --- |
| 4.1 | Database character set | Prefer **`AL32UTF8`** (new); alt **`CL8MSWIN1251`** if DBTC byte-compat required | Decide | Set at create | **UNKNOWN / TBD — DBA must confirm** |
| 4.2 | NLS / client `orai18n` implications documented for Java | Inform | Confirm | **TBD** |
| 4.3 | Partitioning licensed? | Yes → draft **V0** (`PARTITION BY RANGE`); No → **V0b** heap + btree | License owner | Confirm | **UNKNOWN / TBD — DBA must confirm** |
| 4.4 | Flyway apply of V0/V0b | Only after Create + separate APPROVALS | Owner | Apply later | **DENIED** now |

Draft SQL paths (do **not** execute): `docs/architecture/sql/raildb/V0__draft_confirmed_core.sql`, `V0b__draft_nonpartitioned_disl_fallback.sql`.

---

## 5. Accounts (design only — no creation in P4-2)

| Account (proposed name) | Purpose | Target privileges (design) | Status |
| --- | --- | --- | --- |
| `RAIL_APP` | App runtime on `RAILDB` | `CREATE SESSION`; DML on own objects; **no** `DBA`; no prod-runtime DDL (DDL via Flyway/DBA) | **Design only** — not created |
| `RAIL_MIGRATE` (name **TBD**) | Migrator | SELECT on needed `DBTC` objects (or read role); INSERT/UPDATE on `RAILDB.RAIL_APP` migration targets | **UNKNOWN / TBD — DBA must confirm** name + grants |
| `RAILWAY_READONLY` | Legacy RAC smoke | **Not** owner of `RAILDB` | Do not confuse |

Passwords / wallets / connection strings with credentials: **secret store / env only** — never in Git, chat, or this checklist.

---

## 6. Pre-create sign-off checklist (copy of 24 §7, actionable)

| # | Item | Owner | DBA | Status |
| --- | --- | --- | --- | --- |
| 6.1 | Resources on `TC-ASUVP-03` confirmed | — | ☐ | **TBD** |
| 6.2 | License (instance/PDB + Partitioning if V0) confirmed | ☐ | ☐ | **TBD** |
| 6.3 | Topology A or B chosen; SID/PDB/service recorded | ☐ | ☐ | **TBD** |
| 6.4 | Charset fixed | ☐ | ☐ | **TBD** |
| 6.5 | Tablespace + FRA capacity agreed (≥ estimates §3) | ☐ | ☐ | **TBD** |
| 6.6 | RMAN policy agreed | ☐ | ☐ | **TBD** |
| 6.7 | APPROVALS: Create empty RAILDB **TEST** = APPROVED | ☐ | — | **DENIED** now |
| 6.8 | Cutover rollback concept read (ADR-0009) | ☐ | ☐ | Policy read — drill **OPEN** |
| 6.9 | Explicit: `DBTC` / `RAC.WORLD` unchanged | ☐ | ☐ | Project rule |

---

## 7. Plan: create **empty** RAILDB TEST (docs only — DO NOT EXECUTE)

Order is **preliminary**. Each step needs human/DBA action and, where marked, a new APPROVALS row.

1. Close §1 decisions (topology, charset, license/V0|V0b, capacity, TEST≠Prod).
2. Owner adds APPROVALS: **Create empty RAILDB TEST**.
3. DBA creates empty database/PDB per chosen topology (no business data load).
4. DBA creates empty schema/user `RAIL_APP` (least privilege); secrets via secret store.
5. DBA enables ARCHIVELOG + baseline RMAN (policy from §3).
6. DBA returns to engineering (no passwords in chat): JDBC URL shape, A/B, SID/PDB/service, charset/NLS, confirmation `DBTC` untouched.
7. **Separate** APPROVALS later: Flyway V0/V0b → TEST JDBC RO → migrator grants → etc.

Verification steps after create (still docs / future DBA work — **not** run here):

| Check | Expected | Status |
| --- | --- | --- |
| Instance/PDB open | Service reachable from DBA workstation | Future |
| Charset query | Matches decision 4.1 | Future |
| Schema `RAIL_APP` exists, empty of business tables (pre-Flyway) or only objects allowed by that gate | Future |
| `RAC.WORLD` / `DBTC` objects unchanged | Spot-check dictionary | Future |
| RMAN config present | ARCHIVELOG on | Future |
| No Production URL handed to app CI | TEST only | Future |

---

## 8. Explicit non-goals (do not do under P4-2)

- Connect to TEST or Production Oracle from Cursor/agents.
- Create `RAILDB`, apply DDL/Flyway, create real accounts, migrate data.
- JDBC enablement / remove `@Disabled` parity.
- DDL/DML on `DBTC`; rename or replace `RAC.WORLD`.
- Invent CONFIRMED host metrics, service names, license answers, or tape semantics (NK1/DT1/G31).

---

## 9. Sign-off block (fill when executing — leave blank now)

| Role | Name | Date | Notes |
| --- | --- | --- | --- |
| Owner | | | |
| DBA | | | |
| APPROVALS Create TEST row ID | | | |

---

## 10. Links

- [48_DBA_HANDOFF_PACKAGE.md](48_DBA_HANDOFF_PACKAGE.md)
- [24_RAILDB_DBA_INSTRUCTION.md](24_RAILDB_DBA_INSTRUCTION.md)
- [42_ADR0009_DECISION_READINESS.md](42_ADR0009_DECISION_READINESS.md)
- [APPROVALS_TEMPLATE.md](APPROVALS_TEMPLATE.md)
- ADR: [ADR-0009](decisions/ADR-0009-raildb-primary-store.md)
