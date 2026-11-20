# 29. План миграции DBTC → RAILDB (12 месяцев) — P3C-1

Не выполнять. Cutoff = `cutover_ts − 12 months`.  
Связано: [25](25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md), [27](27_RAILDB_ER_MODEL.md), [31](31_P3A_FIELD_MAPPING_MATRIX.md), [36](36_P3B_PARTIAL_DICTIONARY_FINDINGS.md), [37](37_P3C0_SEMANTICS_RESEARCH.md), [39](39_P3C1_SEMANTICS_FINDINGS.md), [ADR-0009](decisions/ADR-0009-raildb-primary-store.md).

## 1. Фазы

| Phase | Content | Gate |
| --- | --- | --- |
| M0 | Inventory + dictionary | **DONE** P3B-2 |
| M0b | Semantics probes P3C-1 | **PARTIAL DONE** — RP1/WH1/KIT_FK; NK1/DT1/G31 open |
| M1 | REF_FULL | stations, types, warehouses, parts |
| M2 | `WAGON` FULL + current | UK CODE |
| M3 | CURRENT repairs + stock FULL + parts | **RP1/WH1 rules CONFIRMED** |
| M4 | BOOTSTRAP last disl ≤ cutoff | by **getting_ts** (DT1 still open) |
| M5 | 12M kits | IMPORTFILES+STATEREPORTS; orphan flags (7d sample clean) |
| M6 | 12M tape batches | GETTING_DATE; retain duplicates; tech PK (**NK1 freq BLOCKED**) |
| M7 | 12M WH docs | DELIVERYDATE |
| M8 | Watched snapshot | **G31 blocked** |
| M9–M11 | delta / cutover / archive | APPROVALS |

## 2. Стратегия ленты

1. Filter `STATEREPORTRECORDS.GETTING_DATE >= cutoff` — **не менять** без DT1 evidence (DT1 BLOCKED).
2. Batches by day/hour; never full-table scan/COUNT; DBA access plan required before any tape probe.
3. Each source row → `event_id` + `provenance_key` + `content_hash`.
4. **Do not delete** duplicate source rows.
5. `kit_id` from soft `IMPORTFILES_ID`; keep legacy ids (7d: 701 OK / 0 orphan).
6. Bootstrap: max `getting_ts` ≤ cutoff per wagon.

## 3. Ремонты / склад (CONFIRMED P3C-1)

| Set | Rule |
| --- | --- |
| Open | all `WAGON.CURRENTREPAIR_ID` → `is_open='Y'` (ignore begin age; ignore ENDINGDATE alone) |
| Closed 12м | `ENDINGDATE` in window (~25k order) |
| Stock | FULL `WAREHOUSEDETAILS`; `OBJECT_ID`→`WAGONPARTS`; type observed `0` |
| WH docs | 12м headers/lines by `DELIVERYDATE` |

## 4. OPEN / UNKNOWN / BLOCKED

| ID | Status after P3C-1 |
| --- | --- |
| G31 | OPEN — package text not visible; need doc 38 |
| NK1 | **BLOCKED** — no DBA tape access plan |
| DT1 | **BLOCKED** — same; GETTING axis held |
| RP1 | **CLOSED** for migration rule |
| WH1 | **CLOSED** for stock grain (BUYINGTYPES names still UNKNOWN) |
| KIT_FK | **PARTIAL CLOSED** — 7d clean; 12м orphan rate UNKNOWN |
| DUP1 | OPEN — needs NK1 hour probes |

## 5. Stop

P3C-1 finished. **P4 / RAILDB create / Flyway — не начинать** без нового approve.
