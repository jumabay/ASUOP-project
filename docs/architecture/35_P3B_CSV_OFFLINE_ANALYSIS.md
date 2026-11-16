# 35. P3B — Offline-анализ CSV (без повторного подключения к RAC)

После ручного экспорта из SQL Developer анализ делается **только по файлам**. Cursor/агент не подключается к Oracle для этого шага.

Рекомендуемая папка (локально): `docs/architecture/sql/dba/p3b_results/`  
(см. `.gitignore` рядом — CSV по умолчанию не коммитить, если содержат чувствительный inventory).

## 1. Чеклист файлов

| Файл | Обязателен |
| --- | --- |
| `p3b_G0_identity_*.csv` | да |
| `p3b_G0_visibility_*.csv` | да |
| `p3b_G1_columns_*.csv` | да |
| `p3b_G1_constraints_*.csv` | да |
| `p3b_G2_indexes_*.csv` | да |
| `p3b_G2_ind_columns_*.csv` | да |
| `p3b_G2_partitions_*.csv` | да |
| `p3b_G2_stats_*.csv` | да |
| `p3b_G3_packages_*.csv` | да |
| `p3b_G3_arguments_*.csv` | да |
| `p3b_G3_dependencies_*.csv` | желательно |
| `p3b_G4_repair_columns_*.csv` | да |
| `p3b_G4_repair_triggers_*.csv` | желательно |
| `p3b_G4_wh_columns_*.csv` | да |
| `p3b_G4_wh_constraints_*.csv` | да |
| `p3b_G5_*.csv` | да для G31 signature |

## 2. Процедура анализа (Excel / LibreOffice / Python pandas — локально)

1. Открыть G0 identity — зафиксировать среду в notes.  
2. G0 visibility — подтвердить `visible_rows > 0`.  
3. G1 columns — фильтр `TABLE_NAME=STATEREPORTRECORDS` → список колонок дат (**DT1**).  
4. G1 constraints — фильтр tape: есть ли `CONSTRAINT_TYPE=P` (**NK1**).  
5. G2 ind_columns — индексы с `GETTING_DATE` / `LOCATION_DATE` / `WAGON_ID` (**DT1/NK1**).  
6. G4 — точные имена `BEGIN*`/`END*` на REPAIRS; колонки `WAREHOUSEDETAILS` (**RP1/WH1**).  
7. G3/G5 — `GETACTIVEWAGONLIST` (**G31** signature only).  
8. Заполнить матрицу §3 ниже → вставить в следующий P3B analysis doc (отдельный approve на запись выводов в architecture).

## 3. Матрица закрытия OPEN items

| ID | Признак «словарь закрыл» | Остаётся OPEN |
| --- | --- | --- |
| **NK1** | Нет PK на STATEREPORTRECORDS в G1; перечислены уникальные/неуникальные индексы | Бизнес-UK для RAILDB без B1 |
| **DT1** | Обе колонки дат видны; индекс показывает предпочтительную ось | Смысл «час файла» на выборке строк (нужен scoped sample) |
| **RP1** | Имена дат + CURRENTREPAIR на WAGON | SQL predicate OPEN без тела пакета |
| **WH1** | Колонки + PK/UK WAREHOUSEDETAILS | Семантика Register / остатки |
| **G31** | Процедура в ALL_ARGUMENTS | Список вагонов / предикат Active |

## 4. Шаблон notes (копировать в `p3b_results/NOTES.md` локально)

```text
Environment: db=… service=… con=… host=… user=…
Visibility WAGON/TAPE: OK | FAIL
NK1: PK on STATEREPORTRECORDS = YES/NO; candidate indexes = …
DT1: GETTING_DATE present=… LOCATION_DATE present=… preferred index=…
RP1: date columns=… CURRENTREPAIR column=…
WH1: WAREHOUSEDETAILS columns summary=…
G31: GETACTIVEWAGONLIST visible=… args=… membership still BLOCKED
Partitioning on DBTC tables=…
Stats STATEREPORTRECORDS num_rows=… last_analyzed=…
```

## 5. Запреты offline-фазы

- Не подключаться к RAC «чтобы перепроверить».  
- Не EXECUTE GetActiveWagonList без нового approve.  
- Не коммитить пароли / connection XML SQL Developer.
