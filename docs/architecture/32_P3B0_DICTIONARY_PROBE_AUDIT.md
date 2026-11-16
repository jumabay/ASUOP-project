# 32. P3B-0 — Аудит SQL словаря DBTC

Дата: 2026-11-16. **SQL не выполнялся.** Скрипт: [sql/dba/01_dictionary_probes.sql](sql/dba/01_dictionary_probes.sql).  
Инструкция SQL Developer: [33_P3B0_SQLDEVELOPER_RUNBOOK.md](33_P3B0_SQLDEVELOPER_RUNBOOK.md).

## 1. Вердикт безопасности

| Критерий | Результат |
| --- | --- |
| Только SELECT к словарю / DUAL / SYS_CONTEXT | **PASS** (после P3B-0 правки) |
| Нет DDL/DML/MERGE | **PASS** |
| Нет анонимного PL/SQL / DBMS_* / EXECUTE | **PASS** |
| Нет ALTER SESSION | **PASS** (явно запрещён в runbook) |
| Нет COUNT(*) по бизнес-таблицам | **PASS** (удалены опасные комментарии-примеры) |
| Нет unbounded SELECT по `STATEREPORTRECORDS` | **PASS** (вынесены в BLOCKED) |
| Учётка | Рекомендуется **read-only**, не `RAIL_APP` как admin |

Исходный файл P3A содержал **закомментированные** `COUNT(*)` и sample по ленте — для ручного DBA это риск раскомментирования. В P3B-0 они заменены явным разделом BLOCKED.

## 2. Каталог блоков (цель / права / нагрузка / результат)

| Block | Цель | Права (типично) | Нагрузка | Ожидаемый результат |
| --- | --- | --- | --- | --- |
| A Session | Идентичность БД/сервиса/user | CREATE SESSION | ничтожная | 1 строка |
| B Columns | Структура core-таблиц | ALL_TAB_COLUMNS | низкая | десятки–сотни строк |
| C Constraints | PK/UK/FK | ALL_CONSTRAINTS(+COLS) | низкая | ключи; у tape PK может отсутствовать |
| D Indexes | Список индексов | ALL_INDEXES | низкая | в т.ч. partitioned=NO/YES |
| E Ind columns | Порядок колонок индексов | ALL_IND_COLUMNS | низкая–средняя | подтверждение `(WAGON_ID,GETTING_DATE)` |
| F Partitions | Партиции DBTC | ALL_PART_TABLES | ничтожная | ожидаемо 0 строк |
| G Stats | NUM_ROWS/BLOCKS | ALL_TABLES | ничтожная | без COUNT(*) |
| H Packages | Наличие пакетов | ALL_OBJECTS | низкая | PACKAGE ± BODY object rows |
| I Arguments | Сигнатуры процедур | ALL_ARGUMENTS | низкая–средняя | GetActiveWagonList и др. |
| J Dependencies | PACKAGE→objects | ALL_DEPENDENCIES | низкая | часто пусто (тела скрыты) |
| K Repairs meta | Колонки/триггеры ремонтов | ALL_TAB_COLUMNS, ALL_TRIGGERS | низкая | имена дат для OPEN |
| L Warehouse meta | Склад/детали | ALL_TAB_* | низкая | grain кандидаты |
| M Watched clues | G31 без данных membership | ALL_OBJECTS/ARGUMENTS/SOURCE(SPEC) | низкая | имена; membership **не** здесь |

## 3. BLOCKED (словарь недостаточен)

| ID | Вопрос | Почему BLOCKED | Безопасная альтернатива |
| --- | --- | --- | --- |
| B1 | Точный COUNT ленты | Запрещён полный COUNT(*) | NUM_ROWS (Block G); выборочный estimate на **копии** по approve |
| B2 | Семантика GETTING_DATE vs LOCATION_DATE | Нужны строки ленты | Отдельный scoped SELECT с `ROWNUM <= N` на копии + approve |
| B3 | Open-repair predicate | Тело Register скрыто | После Block K: согласовать SQL; сначала join `WAGON.CURRENTREPAIR_ID` без full scan |
| B4 | Состав Active/watched | Курсор пакета = бизнес-данные | `EXECUTE` GetActiveWagonList на копии + экспорт кодов; или файл `wagons.txt` offline |
| B5 | Дедуп ImportPkg | Тело пакета | B1-тесты на копии, не dictionary |
| B6 | PARTITION на RAILDB | Лицензия | См. §4 |

## 4. Черновой DDL `V0__draft_confirmed_core.sql` vs Oracle 12.2 / лицензии

| Конструкция | 12.2 совместимость | Лицензия / риск |
| --- | --- | --- |
| `GENERATED … AS IDENTITY` | Да (12.1+) | Standard |
| `TIMESTAMP WITH TIME ZONE` | Да | Standard |
| `PARTITION BY RANGE` + `LOCAL` indexes на `RAIL_DISL_EVENT` | Синтаксис 12.2 OK | **Oracle Partitioning** — отдельная опция EE; без лицензии DDL упадёт или нарушит compliance |
| Composite PK `(event_id, dislocation_ts)` | Да | Нужно для partition key в PK |
| `TIMESTAMP WITH TIME ZONE` как partition key | Ограничения/сложность | В draft partition key уже **TIMESTAMP без TZ** — предпочтительно |

**Рекомендация P3B-0:** до создания RAILDB DBA подтверждает наличие **Partitioning option**. Fallback без опции: heap table + index `(wagon_id, dislocation_ts)` + ручной range delete/archive (документировать в Accept ADR-0009).

## 5. Учётка выполнения

| Использовать | Не использовать |
| --- | --- |
| Read-only dictionary account (`RAILWAY_READONLY` или новая `RAIL_DICT_RO`) | `RAIL_APP` как «админ» / DDL user |
| Копия TEST, если доступна | Пром без scoped approve |
| OWNER filter `DBTC` в SQL | `ALTER SESSION SET CURRENT_SCHEMA` без нужды |

## 6. Следующий gate

- P3B-0 (этот аудит): docs + безопасный скрипт — **готово к review**.  
- Выполнение Block A–M в SQL Developer: только после строки APPROVALS **P3B** / «DBTC dictionary probes».  
- P3B результаты → обновить ER/DDL UK; P4 без этого не начинать.
