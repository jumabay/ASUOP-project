# 48. DBA Handoff Package — empty RAILDB TEST (P4-2)

Дата: **2026-11-20**.  
Gate: P4-2 **APPROVED WITH CONSTRAINTS** @ `main` / branch base `62fb1ff` — [APPROVALS_TEMPLATE.md](APPROVALS_TEMPLATE.md).  
Статус: **documentation handoff only**. **DO NOT EXECUTE** Create, DDL, Flyway, account provisioning, connect, or migrate.

Actionable checklist: [43_ADR0009_DBA_CREATE_CHECKLIST.md](43_ADR0009_DBA_CREATE_CHECKLIST.md).  
Sizing narrative: [24_RAILDB_DBA_INSTRUCTION.md](24_RAILDB_DBA_INSTRUCTION.md).  
Decision board: [42_ADR0009_DECISION_READINESS.md](42_ADR0009_DECISION_READINESS.md).  
Direction ADR: [ADR-0009](decisions/ADR-0009-raildb-primary-store.md).

---

## 1. Purpose

Give DBA a single package to prepare an **empty** Oracle store **`RAILDB`** (TEST first) for the new modular monolith, with schema **`RAIL_APP`**, without changing legacy **`DBTC`** / service **`RAC.WORLD`**.

| In scope (docs) | Out of scope (forbidden now) |
| --- | --- |
| Topology A vs B, capacity **estimates**, charset/license questions | Create instance/PDB |
| Account **design** (`RAIL_APP` / `RAIL_MIGRATE`) | Real passwords, wallets, Git secrets |
| Empty-TEST create **plan** + verification **plan** | Flyway apply, JDBC, data migrate |
| RMAN expectations (policy draft) | Production cutover / live intake |

---

## 2. Topology A vs B + identifiers (examples; exact names TBD)

### 2.1 Choice

| Option | Description | When |
| --- | --- | --- |
| **A** (preferred in doc 42) | Dedicated Oracle instance; `ORACLE_SID` / db name e.g. `RAILDB`; own or shared listener | Isolation from RAC/`DBTC` |
| **B** | PDB named e.g. `RAILDB` inside a CDB on the same host | If DBA mandates PDB consolidation |

**Decision required:** A or B — **UNKNOWN / TBD — DBA must confirm** (record in APPROVALS before Create).

### 2.2 Identifiers (examples only — not confirmed)

| Identifier | Example A | Example B | Actual |
| --- | --- | --- | --- |
| Host (candidate) | `TC-ASUVP-03` / `tc-asuvp-03` | same | **UNKNOWN / TBD — DBA must confirm** |
| Port | `1521` (typical) | `1521` | **TBD** |
| SID / db_name | `RAILDB` | CDB name **TBD** | **TBD** |
| PDB | n/a | `RAILDB` | **TBD** |
| Service name | `RAILDB` or `RAILDB.WORLD` | PDB service (`V$SERVICES`) | **TBD** |
| JDBC (redacted shape) | `jdbc:oracle:thin:@//tc-asuvp-03:1521/RAILDB.WORLD` | `jdbc:oracle:thin:@//tc-asuvp-03:1521/<PDB_SERVICE>` | **TBD** after create |

**Hard constraint:** do **not** rename or repurpose existing service `RAC.WORLD`. RAC schema `RAIL_APP` (5 GiB quota) is **not** the target prod store.

---

## 3. Charset: AL32UTF8 preference vs DBTC CL8MSWIN1251

| Preference | Rationale | Risk |
| --- | --- | --- |
| **`AL32UTF8`** (preferred for new `RAILDB`) | Modern Unicode; fits Java/`orai18n` path | Conversion when reading legacy `CL8MSWIN1251` bytes from `DBTC` during migration |
| **`CL8MSWIN1251`** | Byte-compat with current DBTC charset narrative | Limits Unicode; must be an **explicit** owner+DBA decision |

**Decision required** before Create — **UNKNOWN / TBD — DBA must confirm**.  
Document NLS_LANG / Java `orai18n` expectations with the chosen set (ADR-0003 driver line remains until version decision).

---

## 4. Oracle Partitioning license → V0 vs V0b

| Path | Physical model | License need | Draft (do not apply) |
| --- | --- | --- | --- |
| **V0** | `rail_disl_event` RANGE partition + LOCAL indexes | Oracle **Partitioning** option | `sql/raildb/V0__draft_confirmed_core.sql` |
| **V0b** | Heap + global btree `(wagon_id, dislocation_ts)` | No Partitioning option | `sql/raildb/V0b__draft_nonpartitioned_disl_fallback.sql` |

Logical columns/semantics are intended identical (docs 27/40/42).  
**Decision required:** licensed? → V0 : V0b — **UNKNOWN / TBD — DBA must confirm**.  
Flyway apply remains **DENIED** until a separate APPROVALS row after empty Create.

---

## 5. DB size, memory, disks, FRA (**estimates** from doc 24 / 30)

All figures below are **estimates / discussion minima**, not measured host inventory.

| Resource | Estimate | Comment |
| --- | --- | --- |
| Full hourly tape rows / year | ~**157.7M** | 18k × 24 × 365 |
| Raw tape 12m | ~**21–25 GiB** | ~140 B `AVG_ROW_LEN` order |
| Tape + indexes 12m | ~**70–100 GiB** | plan ×2 indexes; conservative **100 GiB** |
| Wagons/current/refs/ops (order) | **&lt; 10–30 GiB** combined reserve | repairs/stock **UNKNOWN** without live counts |
| Tablespace Y1 + headroom | ≥ **300 GiB** usable (prefer **500**) | reject 5 GiB RAC quota |
| FRA / backup area | orient ≥ **500 GiB** separate | ≥ 1× full + archive retention |
| Quarantine files (OS disk) | ≥ **100 GiB** | not tablespace |
| RAM instance | hypothesis **16–32 GiB** | 20–30 UI + bulk migration — **DBA confirm** |
| RMAN full at start | orient ≥ **150 GiB** | retention multiplies |
| Growth Y2+ if full hours kept | +~70–100 GiB / year | long-term **0.4–0.6 TiB** discussion (doc 30) |

**UNKNOWN / TBD — DBA must confirm:** free space on `TC-ASUVP-03`, actual IOPS, exact FRA mount, ASM vs FS layout.

---

## 6. RMAN backup / restore expectations

Policy draft for DBA agreement (not implemented by this package):

1. **`RAILDB` in ARCHIVELOG** mode from first create (TEST and later Production).
2. **RMAN:** daily incremental + weekly full (or customer standard equivalent).
3. Retention: **UNKNOWN / TBD — DBA must confirm** (orient 7–14 days from doc 24 narrative).
4. **Restore drill** on a copy **before** any Production cutover (ADR-0009 / 24).
5. Legacy **`DBTC`/RAC RMAN continues** under current ownership — this project does not cancel it.
6. After cutover, rollback is **not** “point datasource at DBTC only”; see ADR-0009 rollback window (quarantine replay). RMAN of `RAILDB` remains forensics.

---

## 7. TEST vs Production separation

| Rule | Detail |
| --- | --- |
| Create **TEST first** | Empty `RAILDB` (or TEST PDB/service) before any Production twin |
| Separate identifiers | Distinct SID/PDB/service and credentials from Production |
| App default | Profile `synthetic` until APPROVED TEST JDBC |
| Production access | **DENIED** until separate APPROVALS; never use prod for agent probes |
| Data | No production dumps in Git; no live migrate under P4-2 |

**Decision required:** naming convention TEST vs PROD services — **UNKNOWN / TBD — DBA must confirm**.

---

## 8. Accounts — least privilege (design; no real accounts)

| Proposed account | Role | Intended rights | Not allowed |
| --- | --- | --- | --- |
| **`RAIL_APP`** | Application runtime on `RAILDB` | `CREATE SESSION`; DML on own tables | `DBA`; arbitrary DDL in prod runtime |
| **`RAIL_MIGRATE`** (name TBD) | Migration tool | SELECT needed `DBTC` objects / read role; INSERT/UPDATE targets in `RAILDB.RAIL_APP` | Broad DBA; DDL on `DBTC` |
| Existing `RAILWAY_READONLY` | Legacy RAC smoke | Unrelated | Must not be treated as `RAILDB` owner |

- Passwords: secret store / environment only.  
- **P4-2 does not create accounts** and must not document real passwords.  
- Grants for migrator SELECT on `DBTC` need a **separate** APPROVALS row (`DBTC read for migration`).

---

## 9. Plan to create **empty** RAILDB TEST + verification (docs only)

### 9.1 Prerequisites (Go criteria for a future Create)

- [ ] Topology A/B + exact service name recorded  
- [ ] Charset decided  
- [ ] Partitioning license → V0/V0b known (Flyway later)  
- [ ] Capacity/FRA/quarantine disk agreed vs estimates §5  
- [ ] RMAN policy agreed  
- [ ] TEST≠Production design agreed  
- [ ] APPROVALS row **Create empty RAILDB TEST** = APPROVED  
- [ ] Accept ADR-0009 reconciled with APPROVALS (journal is execution authority)

### 9.2 Create sequence (DO NOT EXECUTE in P4-2)

1. DBA creates empty instance (A) or PDB (B) per §2.  
2. Set charset/NLS per §3.  
3. Create empty schema/user `RAIL_APP` (least privilege); store password in secret store.  
4. Size tablespaces / FRA per agreed minima.  
5. Enable ARCHIVELOG; configure RMAN baseline.  
6. Confirm `DBTC` / `RAC.WORLD` untouched.  
7. Return to engineering: host/port/service, A/B, charset, empty-schema confirmation — **no passwords in chat/Git**.

### 9.3 Verification steps (future DBA — not run here)

| # | Verification | Pass criteria |
| --- | --- | --- |
| V1 | Service resolves | Listener shows agreed service name |
| V2 | Charset | `NLS_CHARACTERSET` matches decision |
| V3 | Schema | `RAIL_APP` exists; no unauthorized business load |
| V4 | Isolation | Connection string is TEST-labelled; not Production |
| V5 | Legacy intact | Spot dictionary check: `DBTC` unchanged |
| V6 | Recoverability | ARCHIVELOG on; RMAN config present (full restore drill before cutover) |

Optional later (separate gates): Flyway V0/V0b → TEST JDBC RO → bounded parity — **not** part of empty create.

### 9.4 No-Go (stop)

- Create without APPROVALS Create row  
- Using RAC `RAIL_APP` 5 GiB as target store  
- Any DDL/DML on `DBTC`  
- Handing Production URL to app/CI  
- Applying Flyway or loading 12m history under “empty create”  
- Treating UNKNOWN license/charset/topology as CONFIRMED

---

## 10. Next APPROVALS rows (expected after this handoff)

| Order | Row (concept) | Depends on |
| --- | --- | --- |
| 1 | Accept ADR-0009 (if still DENIED in journal) | Owner |
| 2 | Create empty RAILDB **TEST** | Checklist 43 §1–6 closed |
| 3 | Flyway apply V0 **or** V0b | Partitioning answer + Create done |
| 4 | TEST JDBC RO (+ later write) | Empty schema + DDL |
| 5 | DBTC read for migration | Migrator design |
| 6 | RAILDB write (migrator/app) | Prior rows |
| 7 | Cutover + rollback drill evidence | RMAN + quarantine |

Create **Production** twin is a **separate** approval after TEST soak — not implied by TEST create.

---

## 11. Residual risks (for DBA / owner)

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Accept ADR-0009 header vs APPROVALS mismatch | Premature Create | Journal wins; reconcile before execute |
| Undersized disk/FRA vs bulk load | Outage / failed RMAN | Use conservative 300/500 GiB estimates; measure host |
| Charset mismatch with DBTC | Migration corruption / mojibake | Explicit decision + conversion tests on TEST |
| Partitioning assumed without license | Illegal DDL / compliance | Force V0b if unlicensed |
| Shared TEST/PROD credentials or service | Accidental prod write | Separate services + least privilege |
| Skipping restore drill | Unrecoverable cutover | Block cutover until drill evidence |
| NK1 / DT1 / G31 still open | Wrong UK / day axis / watched | Offline research separate; do not invent closures for Create |
| Quarantine disk omitted | Rollback window fails | ≥100 GiB OS path in Create plan |

---

## 12. Document links

| Doc | Role |
| --- | --- |
| [43_ADR0009_DBA_CREATE_CHECKLIST.md](43_ADR0009_DBA_CREATE_CHECKLIST.md) | Actionable Owner/DBA/Status checklist |
| [24_RAILDB_DBA_INSTRUCTION.md](24_RAILDB_DBA_INSTRUCTION.md) | Full DBA instruction + sizing math |
| [42_ADR0009_DECISION_READINESS.md](42_ADR0009_DECISION_READINESS.md) | D1–D8 / B1–B7 board |
| [30_RAILDB_SIZING_AND_PERF.md](30_RAILDB_SIZING_AND_PERF.md) | Preliminary perf/sizing |
| [40_P4_0_RAILDB_STORAGE_CONTRACTS.md](40_P4_0_RAILDB_STORAGE_CONTRACTS.md) | V0/V0b + open decisions |
| [APPROVALS_TEMPLATE.md](APPROVALS_TEMPLATE.md) | Execution authority |
| [ADR-0009](decisions/ADR-0009-raildb-primary-store.md) | Store direction + rollback concept |

---

## 13. Stop line

P4-2 delivers this handoff + checklist **as documentation**.  
**No** Oracle connect, **no** Create RAILDB, **no** DDL/Flyway, **no** real accounts, **no** migrate.  
Next human gate: close TBD list in §14 of checklist 43 / questions above → Approve **Create empty RAILDB TEST**.
