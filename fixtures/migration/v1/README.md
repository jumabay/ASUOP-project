# Migration L0 fixture pack `v1`

Synthetic golden pack for **P4-1 (C)** SelectionEngine / reconcile harness (doc [44](../../../docs/architecture/44_MIGRATION_VERIFICATION_MODEL_OFFLINE.md)).

**Not** prod/TEST dumps. **Not** a Create-RAILDB / Flyway / JDBC / live-migrate input.

## Window

| Field | Value |
| --- | --- |
| `cutover_ts` | `2026-11-16T00:00:00` |
| `cutoff_ts` | `2025-11-16T00:00:00` (= cutover − 12 calendar months) |
| Tape / kit axis | `getting_ts` ≡ `GETTING_DATE` / `dislocation_ts` until **DT1** closes |

## Files

| File | Role |
| --- | --- |
| `manifest.json` | Pack id, window, file list, open gaps |
| `refs.json` | Stations, types, ops, repair types, warehouses, part types |
| `wagons.json` | Active wagons (`W000001…`), including old `create_ts` |
| `current.json` | Current location snapshot; `current_repair_id` for OPEN |
| `repairs.json` | Open (via CURRENT), closed 12M, closed archive |
| `stock.json` | Stock balances **FULL** |
| `kits.json` | Import kit headers (12M / archive / UK replay collision) |
| `tape.json` | Dislocation events (archive / bootstrap / 12M / dups / getting≠location) |
| `expected_selection.json` | Golden selection tags per entity |
| `expected_reconcile.json` | Golden counts, exclusions, dup/chronology notes |
| `schema/*.schema.json` | JSON Schema drafts for Backend assertions |
| `_generate.ps1` | Regenerator (offline); re-run then re-validate |
| `validate.ps1` | Structural + golden invariant checks (no Oracle) |

## Selection tags

| Tag | Meaning |
| --- | --- |
| `REF_FULL` | All refs in pack |
| `FULL` | Wagons, current, stock — ignore create age |
| `OPEN` | Repair only if pointed by `current.current_repair_id` (RP1) |
| `12M` | History with axis ts ≥ cutoff and &lt; cutover |
| `BOOTSTRAP` | Per wagon: single max `getting_ts` **strictly &lt; cutoff** among pre-window rows (seed edge) |
| `ARCHIVE_DBTC` | Pre-cutoff history not chosen as BOOTSTRAP (stays in DBTC) |
| `SKIP` | Out of primary load (e.g. future after cutover; watched; ending-null without CURRENT) |

## How Backend should consume

1. Load source entities from `refs` / `wagons` / `current` / `repairs` / `stock` / `kits` / `tape`.
2. Read `cutover_ts` / `cutoff_ts` / `axis` from `manifest.json` (or `expected_selection.json`).
3. Run `SelectionEngine` → produce a selection result shaped like `schema/selection_result.schema.json`.
4. Assert equality (or set-equality by id) against `expected_selection.json` tags:
   - wagons/current/stock → all `FULL`
   - repairs → `OPEN` / `12M` / `ARCHIVE_DBTC` / `SKIP` as listed
   - kits/tape → window tags; BOOTSTRAP event_ids must match
   - `watched` → `status=BLOCKED`, `items=[]`, tag `SKIP` (**G31**; do **not** read R3 `watched.json`)
5. Build reconcile report shaped like `schema/reconcile_report.schema.json` and compare counts / exclusions / dup `retain_all` groups to `expected_reconcile.json`.
6. Keep gaps **OPEN**: do not assert NK1 / DT1 / G31 / R1-Q-TZ closed.

## Edge cases covered

- Old wagon `create_ts` still **FULL**
- Tape: archive / bootstrap / on-cutoff **12M** / inside 12M
- Intentional duplicates (same wagon + getting) — retain all; distinct `event_id` + `provenance_key`
- `getting_ts` ≠ `location_ts` (axis still getting; **DT1** OPEN)
- Open repair with old begin via CURRENT pointer
- Closed repairs in-window vs pre-cutoff archive
- Ending-null repair **without** CURRENT → `SKIP` (not OPEN)
- Kit UK replay collision `(filename,type)` documented for M6 (not physically replayed)
- Orphan `kit_id` on tape (KIT_FK 12m UNKNOWN)
- Future getting after cutover → `SKIP` + exclusion
- Dense 48-hour 12M strip (volume without scale claim)
- Day-boundary events for **R1-Q-TZ** documentation (OPEN)
- Watched M8 **BLOCKED** / empty

## Open gaps (do not close in this pack)

| ID | Status in pack |
| --- | --- |
| NK1 | OPEN — retain-all policy encoded; no UK claimed |
| DT1 | BLOCKED/OPEN — axis = GETTING; location may differ |
| G31 | OPEN — watched empty / BLOCKED |
| R1-Q-TZ | OPEN — half-open day noted only |

## Denied

Create RAILDB, Flyway apply, DBTC read, RAILDB write, JDBC, live migrate, FTP/RWL, secrets.
