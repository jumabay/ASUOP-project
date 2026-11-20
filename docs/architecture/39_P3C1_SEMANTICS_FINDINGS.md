# 39. P3C-1 — бизнес-семантика DBTC (прогон)

Статус: **DONE (partial)**. Oracle read-only SELECT под `DBTC_READONLY`.  
Прогон: `20261116_062146` → CSV в `sql/dba/p3c_results/` (**gitignored**).  
Инструмент: `tools/dbtc-dictionary-inspector` task `runP3c`.  
**P4 / RAILDB create / Flyway / live RWL — не начинались.**

## 1. G0

| Check | Result |
| --- | --- |
| Identity | `DBTC_READONLY` / schema `DBTC_READONLY` |
| Target | `DB_NAME=RAC`, `SERVICE_NAME=RAC.WORLD`, `SERVER_HOST=tc-asuvp-03` |
| Session privs | `CREATE SESSION` only |
| Roles | none |
| Dangerous | none |
| Needed objects | 8/8: WAGON, REPAIRS, STATEREPORTS, IMPORTFILES, WAREHOUSEDETAILS, WAREHOUSES, WAGONSTATEREPORTS, WAGONSTATEREPORTSRECORDS |

## 2. Закрытые / уточнённые вопросы

### RP1 — CONFIRMED (правило CURRENT)

| Probe | Result | ms |
| --- | --- | --- |
| Wagons with `CURRENTREPAIR_ID` | **1452** | 26 |
| CURRENT join → `ENDING_NULL` / `ENDING_SET` | **1428** / **24** | 23 |
| `ENDINGDATE IS NULL` and not CURRENT (begin ≥ 2025-10-12) | **0** | 119 |
| CURRENT with `BEGININGDATE` &lt; 2025-11-16 (sample) | **63** rows (старше 12м, часть с 2006) | 119 |
| Closed repairs in 12м by `ENDINGDATE` | **25094** | 16 |

**Правило миграции (CONFIRMED):**

1. `is_open='Y'` ⇔ строка `REPAIRS` указана в `WAGON.CURRENTREPAIR_ID` (не `ENDINGDATE IS NULL` alone).
2. Гипотеза «open = ENDINGDATE IS NULL» — **FALSE** как единственный критерий (24 CURRENT с заполненным ENDINGDATE).
3. Все CURRENT переносятся **независимо от возраста** `BEGININGDATE`.
4. Закрытые за 12м: `ENDINGDATE` в окне, **и** не являющиеся CURRENT (или переносятся как closed history + open pointer отдельно).

### WH1 — CONFIRMED (остатки = instance parts)

| Probe | Result | ms |
| --- | --- | --- |
| `OBJECT_TYPE` histogram | только **`0`**: 8880 rows, sum_count 13650 | 74 |
| `OBJECT_STATE` | 1→329; 2→8206; 21→28; NULL→317 | 19 |
| Join `BUYINGTYPES` | **ORA-00942** (нет SELECT grant) | 26 |
| `OBJECT_ID` ∈ `WAGONPARTS.ID` | **8877 / 8880** | 37 |
| `OBJECT_ID` ∈ `PARTTYPES.ID` | **0 / 8880** | 15 |
| UK violations | **0** | 79 |
| Warehouses + detail counts | **43** warehouses | 27 |

**Правило:** текущие остатки = FULL snapshot `WAREHOUSEDETAILS` на cutover; `OBJECT_ID` ≈ экземпляр детали (`WAGONPARTS`), не тип (`PARTTYPES`); `OBJECT_TYPE=0` — единственное наблюдаемое значение (домен не финализировать шире без новых типов).

### KIT_FK — CONFIRMED (окно 7 суток)

Окно шапок: `STATEREPORTS.GETTING_DATE` ∈ [2026-11-09, 2026-11-16).

| Probe | Result | ms |
| --- | --- | --- |
| Orphan sample (NULL / missing parent) | **0 rows** | 32 |
| Status histogram | **OK=701** | 13 |
| OK sample | 500 rows (capped) | 162 |
| Files without report (by IMPORTDATE same window) | **0** | 628 |

**Правило:** soft link `IMPORTFILES_ID` в этом окне целостен; модель `kit_id` nullable + флаги orphan **сохраняем** (физического FK нет; глобальная доля за 12м — UNKNOWN).

### G31 — всё ещё OPEN/BLOCKED на предикат

| Probe | Result | ms |
| --- | --- | --- |
| PACKAGE / BODY visibility | **0** | 81 |
| `ALL_ARGUMENTS` GetActive* | **0** | 89 |
| `ALL_SOURCE` SPEC snippets | **0** | 98 |
| Columns `WAGONSTATEREPORTS*` | **39** cols (есть `ENABLED`) | 104 |

Предикат Active / membership **не закрыт**. Остаётся [38_P3C0_DBA_PACKAGE_SOURCE_REQUEST.md](38_P3C0_DBA_PACKAGE_SOURCE_REQUEST.md). Document-slice таблицы видны; это **не** доказательство = Active list.

## 3. BLOCKED / не выполнялось

| ID | Reason |
| --- | --- |
| **NK1** | Нет одобренного DBA access plan на ленту; EXPLAIN PLAN запрещён (PLAN_TABLE) |
| **DT1** | Тот же gate; ось 12м = `GETTING_DATE` **без изменений** |
| `00_explain_plan_gates.sql` | Не запускался |

## 4. Длительности (сводка)

Все выполненные SELECT ≤ **628 ms**. Timeout tool = 30 s. Жёстких timeout/FTS-stop не было.  
Ошибка без abort: WH1.2 `BUYINGTYPES` ORA-00942.

## 5. Готовность к P4

**Нет.** Закрыты RP1/WH1/KIT_FK (ограниченно). NK1/DT1/G31 Active — открыты. P4 только после отдельного approve.

## 6. Stop

P3C-1 завершён. Не начинать P4 / DDL RAILDB / Flyway / EXECUTE пакетов.
