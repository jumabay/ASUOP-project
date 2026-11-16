# 34. P3B — Точная последовательность для Oracle SQL Developer

**Cursor к Oracle не подключается.** Этот документ — ручной прогон человеком/DBA.  
**Выполнение SELECT на производственной RAC** — только после отдельного разрешения владельца БД/DBA.  
Подготовка (этот пакет) разрешена после P3B-0 (`6bd65b4`).

Аудит: [32_…](32_P3B0_DICTIONARY_PROBE_AUDIT.md). Offline CSV: [35_…](35_P3B_CSV_OFFLINE_ANALYSIS.md).  
Монолитный скрипт (справочно): [sql/dba/01_dictionary_probes.sql](sql/dba/01_dictionary_probes.sql).

## 0. До Connect

| Шаг | Действие |
| --- | --- |
| 0.1 | Учётка **read-only** (`RAIL_DICT_RO` / `RAILWAY_READONLY`). Не `RAIL_APP` admin. |
| 0.2 | Нет DBA / ANY / DDL на DBTC. При нехватке прав — §4 ниже (адресные GRANT). |
| 0.3 | Пароль только в SQL Developer connection store. |
| 0.4 | Папка результатов: `docs/architecture/sql/dba/p3b_results/` (локально; CSV можно gitignore). |
| 0.5 | Иметь письменное подтверждение среды: TEST/copy **или** scoped prod. |

## 1. Группы SQL (выполнять строго по порядку)

| Group | File | Blocks | CSV names |
| --- | --- | --- | --- |
| **G0** | [p3b/G0_session_and_visibility.sql](sql/dba/p3b/G0_session_and_visibility.sql) | Identity + visibility | `p3b_G0_identity_*.csv`, `p3b_G0_visibility_*.csv` |
| **G1** | [p3b/G1_table_structure_keys.sql](sql/dba/p3b/G1_table_structure_keys.sql) | Columns + constraints | `p3b_G1_columns_*.csv`, `p3b_G1_constraints_*.csv` |
| **G2** | [p3b/G2_indexes_partitions_stats.sql](sql/dba/p3b/G2_indexes_partitions_stats.sql) | Indexes / parts / stats | `p3b_G2_*.csv` |
| **G3** | [p3b/G3_packages_dependencies.sql](sql/dba/p3b/G3_packages_dependencies.sql) | Packages / args / deps | `p3b_G3_*.csv` |
| **G4** | [p3b/G4_repairs_warehouse_meta.sql](sql/dba/p3b/G4_repairs_warehouse_meta.sql) | Repairs / WH meta | `p3b_G4_*.csv` |
| **G5** | [p3b/G5_watched_g31_clues.sql](sql/dba/p3b/G5_watched_g31_clues.sql) | Watched clues | `p3b_G5_*.csv` |

Каждый statement в файле — отдельно: выделить → **Run Statement** (`Ctrl+Enter`) → Export CSV UTF-8.  
**Не** Run Script (F5) на весь файл без необходимости.

## 2. Пошагово в SQL Developer

1. File → Open → нужный `G0_….sql`.  
2. Connect read-only.  
3. Run **G0.1**. Сверить и записать:

| Поле | Ожидание (заполнить до прогона) | Факт |
| --- | --- | --- |
| SESSION_USER / USER | read-only login | |
| CURRENT_SCHEMA | обычно = user (не обязан DBTC) | |
| DB_NAME | _______ | |
| CON_NAME | _______ (CDB/PDB; может быть пусто на non-CDB) | |
| SERVICE_NAME | _______ | |
| SERVER_HOST | _______ | |

4. При несовпадении среды → **STOP**, Disconnect.  
5. Run **G0.2**. Если `visible_rows = 0` для WAGON/STATEREPORTRECORDS → **STOP** (§4). Не трактовать как «таблиц нет».  
6. Только при visibility > 0: открыть G1 → … → G5 по таблице §1.  
7. После каждой группы — Export CSV в `p3b_results/` (имена как в §1).  
8. Не выполнять COUNT(*) / SELECT по ленте / EXECUTE пакетов в этом P3B dictionary pass.

## 3. Экспорт CSV

1. Query Result → правый клик / иконка Export.  
2. Format: **CSV**, Encoding: **UTF-8**, Header: On, Left/Right encl: none или `"`.  
3. Не включать connection password в путь/имя файла.  
4. Чеклист файлов — [35_…](35_P3B_CSV_OFFLINE_ANALYSIS.md) §1.

## 4. Если прав недостаточно (адресные запросы DBA)

Остановиться. Запросить **только** необходимое, например:

```text
-- примеры формулировок для DBA (не выполнять из приложения)
GRANT CREATE SESSION TO rail_dict_ro;
-- чтобы ALL_* видел объекты DBTC (вариант политики заказчика):
GRANT SELECT ON dbtc.wagon TO rail_dict_ro;
GRANT SELECT ON dbtc.statereportrecords TO rail_dict_ro;
-- … точечно по списку G1 tables
-- ИЛИ роль с чтением каталога по политике — без DBA / SELECT ANY TABLE / ANY
```

**Не запрашивать:** `DBA`, `SELECT ANY TABLE`, `EXECUTE ANY`, `ALTER ANY`, admin `RAIL_APP`.

## 5. Пустой ALL_* 

| Ситуация | Вывод |
| --- | --- |
| G0.2 = 0 | Нет видимости метаданных/объектов → grant gap |
| G0.2 > 0, одна таблица пустая в G1 | Возможно нет объекта **или** нет grant на неё → проверить `ALL_OBJECTS` для имени |
| G3 deps пусто | Тела скрыты / нет dep в словаре → UNKNOWN, не «пакет не использует таблицы» |
| G5 SOURCE 0 | Spec не виден / нет текста → не отрицает процедуру из ALL_ARGUMENTS |

## 6. Что закрывают CSV (open items)

| ID | Нужные CSV | Закрытие на dictionary pass |
| --- | --- | --- |
| **NK1** | G1 constraints + G2 ind on STATEREPORTRECORDS | Подтвердить отсутствие PK; кандидаты UK из индексов — ещё PROVISIONAL |
| **DT1** | G1 columns + G2 ind columns | Наличие GETTING_DATE / LOCATION_DATE; ось фильтра по индексу |
| **RP1** | G4 repair columns + G1 WAGON CURRENTREPAIR* | Имена дат/флагов; predicate всё ещё может быть OPEN |
| **WH1** | G4 wh columns/constraints | Grain кандидаты; без proof Register |
| **G31** | G3 args + G5 | Процедура/signature; **список кодов** — отдельный approve |

## 7. После сбора CSV

1. Скопировать CSV (без секретов) аналитику.  
2. Offline разбор по [35_…](35_P3B_CSV_OFFLINE_ANALYSIS.md) — **без** нового connect.  
3. Обновление ER/DDL — отдельный deliverable после анализа.  
4. Не создавать RAILDB / не Flyway / не P4.
