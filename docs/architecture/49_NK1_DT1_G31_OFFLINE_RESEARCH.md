# 49. P4-2 offline research — NK1 / DT1 / G31

Дата: **2026-11-20**. Gate: **P4-2 APPROVED WITH CONSTRAINTS** ([APPROVALS_TEMPLATE.md](APPROVALS_TEMPLATE.md)).  
Ветка: `docs/p4-2-dba-readiness-research`.  
Связано: [25](25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md), [27](27_RAILDB_ER_MODEL.md)–[29](29_RAILDB_MIGRATION_12M_PLAN.md), [32](32_P3B0_DICTIONARY_PROBE_AUDIT.md)–[39](39_P3C1_SEMANTICS_FINDINGS.md), [42](42_ADR0009_DECISION_READINESS.md), [44](44_MIGRATION_VERIFICATION_MODEL_OFFLINE.md), [38](38_P3C0_DBA_PACKAGE_SOURCE_REQUEST.md), analysis [08_GAPS](../analysis/08_GAPS_AND_UNCERTAINTIES.md) / [11](../analysis/11-nuff-active-wagon-list.md), L0 [`OpenGaps`](../../platform-oracle/src/main/java/platform/oracle/migration/l0/OpenGaps.java), fixtures [`migration/v1/README`](../../fixtures/migration/v1/README.md).

**Этот документ — docs-only.** Не исполняет SQL. Не закрывает gaps. Не меняет OPEN→CLOSED.

### Hard constraint (restate)

> **Watched / M8 stays empty until G31.**  
> Do not populate `RAIL_WAGON_WATCHED` from R3 synthetic `watched.json` (60 codes), from `wagons.txt`, or from any guessed predicate. M8 = **BLOCKED** / `items=[]` / tag `SKIP` while G31 remains OPEN ([44](44_MIGRATION_VERIFICATION_MODEL_OFFLINE.md) §1, [25](25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md) §2, L0 `SelectionEngine.WATCHED_REASON`).

---

## 0. Summary matrix

| ID | Topic | Offline status (P4-2) | Hold policy until evidence | Blocks |
| --- | --- | --- | --- | --- |
| **NK1** | Natural key / tape duplicates on `STATEREPORTRECORDS` | **BLOCKED** (OPEN for closure) | Retain **all** source rows; tech `event_id` + `provenance_key`; **no** UK on `(wagon, getting)` | Final tape UK / DISTINCT collapse / M6 physical idempotency claim |
| **DT1** | `GETTING_DATE` vs `LOCATION_DATE` axis | **BLOCKED** (OPEN for closure) | 12м filter + day axis = **`GETTING_DATE`** / `getting_ts` / `dislocation_ts` store-both | Changing migration filter to LOCATION; UI «факт» as sole axis |
| **G31** | Watched membership source (`GetActiveWagonList` predicate) | **OPEN** | **Watched empty**; M8 SKIP; no legacy parity claim on R3 | M8 populate; R3≡legacy; watched-on-card truth |

**Explicit:** statuses remain **OPEN/BLOCKED**. This research does **not** close them. Closure requires DBA evidence under a **separate** APPROVED scope (not P4-2).

---

## 1. NK1 — natural key / duplicates on dislocation tape

### 1.1 What is already CONFIRMED (cite)

| Fact | Cite |
| --- | --- |
| `STATEREPORTRECORDS` has **no PK** (constraint P absent) | [36](36_P3B_PARTIAL_DICTIONARY_FINDINGS.md) §3.2; [28](28_DBTC_OBJECT_CATALOG.md) § tape |
| NOT NULL columns include `WAGON_ID`, `REPORT_ID`, `CURRENT_STATE_ID` | [36](36_P3B_PARTIAL_DICTIONARY_FINDINGS.md) §3.2; [37](37_P3C0_SEMANTICS_RESEARCH.md) §1 |
| Indexes `(WAGON_ID, GETTING_DATE)` and `(WAGON_ID, REPORT_ID, …)` are **NONUNIQUE** — cannot treat as UK without proof | [36](36_P3B_PARTIAL_DICTIONARY_FINDINGS.md) §3.2; [37](37_P3C0_SEMANTICS_RESEARCH.md) §1 |
| `IDX_REC_GETTING_DATE(GETTING_DATE)` exists; tape ~**1.215e9** rows | [36](36_P3B_PARTIAL_DICTIONARY_FINDINGS.md) §3.2 |
| Migration hold: retain duplicates; tech PK / `event_id` + `provenance_key`; no DISTINCT collapse | [25](25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md) §7.2; [29](29_RAILDB_MIGRATION_12M_PLAN.md) §2; [44](44_MIGRATION_VERIFICATION_MODEL_OFFLINE.md) §1/§5 |
| P3C-1: NK1 data probes **not executed** — no DBA tape access plan; EXPLAIN PLAN gate not run | [39](39_P3C1_SEMANTICS_FINDINGS.md) §3; APPROVALS P3C-1 PARTIAL |
| L0 encodes retain-all; `OpenGaps.NK1` stays in `stillOpen()` | [`OpenGaps.java`](../../platform-oracle/src/main/java/platform/oracle/migration/l0/OpenGaps.java); [`SelectionEngine`](../../platform-oracle/src/main/java/platform/oracle/migration/l0/SelectionEngine.java); [fixtures README](../../fixtures/migration/v1/README.md) |

### 1.2 What evidence is MISSING

- Global (or multi-hour) duplicate frequency for candidates `(REPORT_ID, WAGON_ID)` and `(WAGON_ID, GETTING_DATE)`.
- Whether collisions are identical content vs distinct ops/stations (content-hash diversity).
- Written **DBA access plan**: 1-hour INDEX RANGE on `IDX_REC_GETTING_DATE` only; no FULL SCAN; PLAN_TABLE / EXPLAIN ownership.
- PASS of `00_explain_plan_gates` before any tape SELECT ([38](38_P3C0_DBA_PACKAGE_SOURCE_REQUEST.md), [37](37_P3C0_SEMANTICS_RESEARCH.md) § load table).
- Decision whether RAILDB may ever add UNIQUE on a business candidate (today: **forbidden** until evidence).

### 1.3 SQL / exports DBA must run later (list — do not execute)

Prerequisite: separate APPROVED scope + written tape access plan. Drafts: [sql/dba/p4-2/](sql/dba/p4-2/).

| # | Artifact | Purpose | Export (sanitized) |
| --- | --- | --- | --- |
| N0 | `00_explain_plan_gates.sql` | Prove INDEX RANGE on hour / wagon+getting; **no** FTS | `p4_2_NK1_plan_*.txt` |
| N1 | NK1.1 `(REPORT_ID, WAGON_ID)` dups in **one** hour | Collision evidence | `p4_2_NK1_report_wagon_dups_*.csv` (≤200 groups) |
| N2 | NK1.2 `(WAGON_ID, GETTING_DATE)` dups in same hour | Disprove UK assumption | `p4_2_NK1_wagon_getting_dups_*.csv` |
| N3 | NK1.3 content sample for one colliding pair | Same vs different payload | `p4_2_NK1_dup_content_*.csv` (ids only) |
| N4 | NK1.4 hour volume `COUNT(*)` inside indexed hour | Batch sizing | `p4_2_NK1_hour_volume_*.csv` |

**Forbidden without new approval:** full-table `COUNT`/`GROUP BY` on tape; Production; unscoped multi-day scans.

### 1.4 Migration risks if still open

- Silent UNIQUE / DISTINCT → **lost history** (hourly full snapshots must be preserved — project rule).
- Under-sized batches if hour volume UNKNOWN.
- False idempotency if `provenance_key` not stable while NK1 unknown ([42](42_ADR0009_DECISION_READINESS.md) D7).

### 1.5 Decisions needing owner APPROVAL

1. Approve **DBA tape access plan** + EXPLAIN-only gate (scoped TEST/RO).
2. After N1–N3: keep **retain-all** forever vs allow UK only if zero collisions across approved sample set.
3. Confirm staging UNIQUE on `provenance_key` design (D7) — separate from natural-key claim.

### 1.6 Status

**BLOCKED / OPEN — not closed by P4-2.**

---

## 2. DT1 — GETTING_DATE vs LOCATION_DATE

### 2.1 What is already CONFIRMED (cite)

| Fact | Cite |
| --- | --- |
| Both `GETTING_DATE` and `LOCATION_DATE` exist on tape; indexes on both axes | [36](36_P3B_PARTIAL_DICTIONARY_FINDINGS.md) §3.2; [37](37_P3C0_SEMANTICS_RESEARCH.md) §2 |
| Approved 12м migration filter axis = **`GETTING_DATE`** (index + plan docs) — do not change without evidence | [25](25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md) tape table; [29](29_RAILDB_MIGRATION_12M_PLAN.md) §2; [44](44_MIGRATION_VERIFICATION_MODEL_OFFLINE.md) §1 |
| `STATEREPORTS.GETTING_DATE` TIMESTAMP(2) = kit/header hour | [36](36_P3B_PARTIAL_DICTIONARY_FINDINGS.md) §3.1; [37](37_P3C0_SEMANTICS_RESEARCH.md) §2 |
| P3C-1: DT1 probes **not executed** (same tape gate as NK1) | [39](39_P3C1_SEMANTICS_FINDINGS.md) §3 |
| L0 / fixtures: axis = `getting_ts`; may have `getting_ts ≠ location_ts` without changing filter | [fixtures README](../../fixtures/migration/v1/README.md); [`DayBoundaryPolicy`](../../platform-oracle/src/main/java/platform/oracle/migration/l0/DayBoundaryPolicy.java) |
| Store-both design: RAILDB keeps getting-equivalent for partition/watermark; UI «факт» may use location **after** confirmation | [37](37_P3C0_SEMANTICS_RESEARCH.md) §2 |

### 2.2 What evidence is MISSING

- Bounded sample distributions: EQUAL / WITHIN_1H / GT_24H / NULL_SIDE between getting and location ([37](37_P3C0_SEMANTICS_RESEARCH.md) §2 checks).
- Proof that file field `[3] dislocationDate` maps to `LOCATION_DATE` (today **INFERRED** from C# only — [37](37_P3C0_SEMANTICS_RESEARCH.md)).
- Alignment rate: tape `GETTING_DATE` hour vs parent `STATEREPORTS.GETTING_DATE`.
- Business pick: which timestamp operators mean by «сутки» / day history (related **R1-Q-TZ** remains OPEN — out of NK1/DT1/G31 close, but coupled).

### 2.3 SQL / exports DBA must run later (list — do not execute)

Same tape plan prerequisite as NK1. Drafts under [sql/dba/p4-2/](sql/dba/p4-2/).

| # | Artifact | Purpose | Export |
| --- | --- | --- | --- |
| D0 | Share N0 EXPLAIN PASS | Same hour binds as NK1 | plan text |
| D1 | DT1.1 pairwise sample ≤5000 / 1 hour | Delta / relation labels | `p4_2_DT1_sample_pairs_*.csv` |
| D2 | DT1.2 relation buckets | Aggregate EQUAL/WITHIN_*/GT_24H/NULL | `p4_2_DT1_relation_buckets_*.csv` |
| D3 | DT1.3 report vs tape getting (start from `STATEREPORTS` filter) | Kit-hour alignment | `p4_2_DT1_report_vs_tape_*.csv` |
| D4 | DT1.4 null rates in sample | Completeness | `p4_2_DT1_null_rates_*.csv` |

### 2.4 Migration risks if still open

- Filtering 12м on LOCATION would drop/include wrong hours vs import kit timeline.
- Day UI vs store mismatch if operators expect location but filter uses getting (or reverse) without disclosure.
- Bootstrap «last event ≤ cutoff» wrong if axis flipped ([29](29_RAILDB_MIGRATION_12M_PLAN.md) M4).

### 2.5 Decisions needing owner APPROVAL

1. Keep GETTING as **sole** migrate/filter axis until DT1 sample reviewed (default hold).
2. After samples: confirm store-both + UI display field.
3. Couple or defer **R1-Q-TZ** day-boundary (half-open) policy — separate row if needed.

### 2.6 Status

**BLOCKED / OPEN — not closed by P4-2.** Hold axis = GETTING.

---

## 3. G31 — watched / Active membership

### 3.1 What is already CONFIRMED (cite)

| Fact | Cite |
| --- | --- |
| Remoting `IGivingDepartureCall.GetActiveWagons` → loader → `WagonPackage.GetActiveWagonList` (OUT cursor only) | [11](../analysis/11-nuff-active-wagon-list.md) §2; [08_GAPS](../analysis/08_GAPS_AND_UNCERTAINTIES.md) G31 |
| НУВ explorer list = `WagonPackage.GetWagonList` («Все вагоны») — **not** Active | [11](../analysis/11-nuff-active-wagon-list.md) §3 |
| UI call-site for Active in WagonApp/WagonLib/PlannerLib/WagonPlanner **not found** | [11](../analysis/11-nuff-active-wagon-list.md) §4; R4 DONE research |
| `wagons.txt` from `PublishRegistryStateReports*` ≠ Active | [11](../analysis/11-nuff-active-wagon-list.md) §5; [08_CROSS](../analysis/08_CROSS_SYSTEM_MATRIX.md) «Список на слежение» |
| No dedicated «watched» table in DBTC dictionary; `WAGONSTATEREPORTS*` exist (document slice clue only) | [28](28_DBTC_OBJECT_CATALOG.md) §2.8; [36](36_P3B_PARTIAL_DICTIONARY_FINDINGS.md) §3.3 |
| P3B/P3C: package / `ALL_ARGUMENTS` / `ALL_SOURCE` for Active = **0 rows** for RO account | [36](36_P3B_PARTIAL_DICTIONARY_FINDINGS.md) G3/G5; [39](39_P3C1_SEMANTICS_FINDINGS.md) § G31 |
| R3 synthetic watched (60) **≠** migration source; M8 blocked until pick | [25](25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md) §2; [44](44_MIGRATION_VERIFICATION_MODEL_OFFLINE.md); APPROVALS R3 / P4-2 |
| L0 watched = BLOCKED empty; reason `G31_source_unapproved` | [`SelectionEngine`](../../platform-oracle/src/main/java/platform/oracle/migration/l0/SelectionEngine.java) |

### 3.2 What evidence is MISSING

- `WAGONPACKAGE` SPEC (and BODY if policy allows): existence of `GetActiveWagonList`, signature, predicate (table/view/flag).
- Cursor column names for Active result.
- Any non-НУВ caller (other host / script) — still UNKNOWN.
- Business pick: Active cursor vs `wagons.txt` / publish list vs explicit cutover snapshot file.
- Link (if any) between `WAGONSTATEREPORTS.ENABLED` and Active — **not** proven ([39](39_P3C1_SEMANTICS_FINDINGS.md)).

### 3.3 SQL / exports DBA must run later (list — do not execute)

Per [38](38_P3C0_DBA_PACKAGE_SOURCE_REQUEST.md). Drafts: [sql/dba/p4-2/G31_package_source_discovery.sql](sql/dba/p4-2/G31_package_source_discovery.sql).

| # | Artifact | Purpose | Export |
| --- | --- | --- | --- |
| G1 | `ALL_OBJECTS` packages visibility | Grant gap vs hide | `p4_2_G31_packages_*.csv` |
| G2 | `ALL_ARGUMENTS` GetActive* / GetWagonList | Signature | `p4_2_G31_args_*.csv` |
| G3 | `ALL_SOURCE` **SPEC only** snippets | Text without EXECUTE | `p4_2_G31_spec_*.csv` or secured file-share export |
| G4 | Optional DBA file export of SPEC/BODY | If catalog SELECT denied | gitignored `_oracle_tmp/` copy |
| G5 | **Not** in this gate: `BEGIN GetActiveWagonList` / cursor dump | Needs **separate** approve | — |

Also useful (already partially done P3B): column skim `WAGONSTATEREPORTS*` — clue only, not membership proof.

### 3.4 Migration risks if still open

- Filling M8 from synthetic 60 or from `wagons.txt` → **wrong operational membership**.
- Claiming R3 REST ≡ legacy Active → false parity ([45](45_API_SCALE_READINESS_18K.md)).
- Blocking only watched is correct; do **not** block wagon FULL registry migrate on G31 ([25](25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md) §2.3).

### 3.5 Decisions needing owner APPROVAL

1. DBA grant / export for package SPEC (doc 38) — no EXECUTE.
2. **Business pick** of M8 source after SPEC (or if SPEC insufficient: cutover snapshot process).
3. Separate approve for Active **cursor materialization** (if required for cutover snapshot).
4. Restate product rule: watched empty until pick (this doc § Hard constraint).

### 3.6 Status

**OPEN — not closed by P4-2.** Watched remains empty.

---

## 4. Cross-cutting: what P4-2 does / does not unlock

| Allowed by P4-2 offline | Still DENIED |
| --- | --- |
| This research + non-executable SQL drafts | Create RAILDB / Flyway / JDBC / parity enable |
| DBA handoff text referencing these lists | DBTC tape SELECT / EXPLAIN without new APPROVED |
| Keep L0 retain-all / GETTING axis / empty watched | Claiming NK1/DT1/G31 CLOSED |
| Empty TEST DB create *planning* (see [50](50_P4_2_GO_NOGO_CREATE_RAILDB_TEST.md) if present) | Migrate apply; M8 fill |

NK1/DT1/G31 are **not** hard blockers for creating an *empty* TEST database, but **are** blockers for migrate apply and watched fill ([50](50_P4_2_GO_NOGO_CREATE_RAILDB_TEST.md) intent; [42](42_ADR0009_DECISION_READINESS.md) B6/F1).

---

## 5. Separate APPROVALS needed next (names only)

| Next scope | Needed to |
| --- | --- |
| DBA tape access plan + EXPLAIN/bounded SELECT (NK1/DT1) | Close or keep BLOCKED with facts |
| G31 package SPEC export / grants (no EXECUTE) | Predicate / signature evidence |
| Business pick Active vs wagons.txt vs other | Unlock M8 design |
| Create RAILDB **TEST** (empty) | Instance only — not migrate |
| Flyway V0/V0b apply on TEST | Schema |
| DBTC read + RAILDB write migrator | L2 |
| Active cursor dump (optional) | Cutover snapshot |

---

## 6. Sources mined (read-only)

| Source | Use |
| --- | --- |
| 25, 27–29, 32–39, 42, 44 | Migration/semantics/readiness |
| 38 | Package + tape plan request |
| analysis 08_GAPS, 08_CROSS, 11 | G31 / Active / wagons.txt |
| `fixtures/migration/v1/README.md` | L0 hold policies |
| `OpenGaps` / `SelectionEngine` / `DayBoundaryPolicy` / `TapeIdentity` comments | Encoded OPEN gaps |
| Existing drafts `sql/dba/p3c/NK1_*`, `DT1_*`, `G31_*`, `00_explain_*` | Basis for p4-2 copies |
| **Not edited:** `43_*`, `48_*` (Database agent) |

---

## 7. Verdict

| ID | Status after this research |
| --- | --- |
| NK1 | **BLOCKED** — dictionary CONFIRMED no PK/nonunique; frequency UNKNOWN; retain-all holds |
| DT1 | **BLOCKED** — columns/indexes CONFIRMED; sample UNKNOWN; GETTING axis holds |
| G31 | **OPEN** — Remoting/API CONFIRMED; predicate UNKNOWN; **watched empty** |

**Do not mark CLOSED without DBA evidence and owner review.**
