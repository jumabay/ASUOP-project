# P3C diagnostic SQL

Статус: P3C-1 **partial executed** (`run/` subset, stamp `20261116_062146`).  
Findings: [../../39_P3C1_SEMANTICS_FINDINGS.md](../../39_P3C1_SEMANTICS_FINDINGS.md).  
CSV: `../p3c_results/` (gitignored).

## Правила

1. Только `SELECT`. **EXPLAIN PLAN** — только с отдельным DBA approve (пишет в PLAN_TABLE).
2. Перед любым запросом к `STATEREPORTRECORDS` — одобренный DBA access plan (NK1/DT1 иначе BLOCKED).
3. Bind/literals для дат/лимитов. `ROWNUM` / `FETCH FIRST` обязательны.
4. Запрещены: `COUNT(*)` по всей ленте, `GROUP BY` по всей ленте, `SELECT *` с ленты, `EXECUTE` пакетов, DDL/DML.

## Файлы

| File | Topic | Expected load |
| --- | --- | --- |
| [00_explain_plan_gates.sql](00_explain_plan_gates.sql) | plan-only gates | metadata, seconds |
| [NK1_natural_key_probes.sql](NK1_natural_key_probes.sql) | natural key / dups | indexed range ≤ 1 hour |
| [DT1_date_semantics_probes.sql](DT1_date_semantics_probes.sql) | GETTING vs LOCATION | indexed sample ≤ 5k rows |
| [G31_watched_dictionary.sql](G31_watched_dictionary.sql) | package visibility | dictionary only |
| [RP1_open_repair_probes.sql](RP1_open_repair_probes.sql) | open repairs | WAGON+REPAIRS by PK/FK |
| [WH1_warehouse_semantics_probes.sql](WH1_warehouse_semantics_probes.sql) | OBJECT_* semantics | ≤10k rows table |
| [KIT_FK_import_link_probes.sql](KIT_FK_import_link_probes.sql) | report↔file orphans | STATEREPORTS ~411k filtered |

DBA package text request (no EXECUTE): [../../38_P3C0_DBA_PACKAGE_SOURCE_REQUEST.md](../../38_P3C0_DBA_PACKAGE_SOURCE_REQUEST.md).
