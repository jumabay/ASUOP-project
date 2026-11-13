# Производительность и объёмы Oracle (`DBTC`)

Источник: `ALL_TABLES` / `ALL_INDEXES` / `ALL_PART_TABLES` / `ALL_SCHEDULER_JOBS` на `tc-asuvp-03`, схема `DBTC`.  
Объёмы **приблизительные** (`NUM_ROWS`, `BLOCKS`, `LAST_ANALYZED`). **`COUNT(*)` не выполнялся.**

См. также [02_ORACLE_TABLES.md](02_ORACLE_TABLES.md), [02_ORACLE_RELATIONSHIPS.md](02_ORACLE_RELATIONSHIPS.md).

## 1. Крупнейшие таблицы (hotspot-кандидаты)

| Таблица | ~NUM_ROWS | ~BLOCKS | AVG_ROW_LEN | LAST_ANALYZED | Partitioned |
|---|---:|---:|---:|---|---|
| `STATEREPORTRECORDS` | 1 215 316 604 | 23 386 502 | 140 | 2026-10-18 | **NO** |
| `WAGONAPPROACHARCHIVE` | 886 287 013 | 8 631 543 | 64 | 2026-08-26 | **NO** |
| `WAGONDOWNTIME` | 184 393 064 | 1 278 165 | 43 | 2026-10-16 | **NO** |
| `STATIONSDISTANCEQUERY` | 61 589 349 | 116 687 | 8 | 2026-08-30 | NO |
| `GIVINGARCHIVE` | 43 926 482 | 726 863 | 113 | 2023-07-19 | NO |
| `WAGONSTATEREPORTSRECORDS` | 37 725 967 | 711 650 | 110 | 2026-11-08 | NO |
| `WAGONMILEAGE` | 33 642 136 | 253 465 | 49 | 2026-09-16 | NO |
| `GIVING_ROUTES_VIEW` | 32 096 505 | 1 173 227 | 241 | 2026-11-11 | NO (view/mview stats) |
| `DEPARTURELISTCARRIAGES` | 19 770 160 | 91 360 | 30 | 2026-11-11 | NO |
| `GIVINGLISTCARRIAGES` | 19 659 616 | 756 758 | 258 | 2026-11-11 | NO |
| `GIVINGDEPARTURES` | 9 689 154 | 187 748 | 137 | 2024-09-22 | NO |
| `STATEREPORTSWAGON` | 8 233 489 | 56 788 | 45 | 2026-11-06 | NO |

Во всей схеме `DBTC`: **`ALL_PART_TABLES` пуст** — секционирования нет даже у миллиардных таблиц.

Оценка порядка размера `STATEREPORTRECORDS`: ~23.4M блоков × 8 KiB ≈ **~180+ GiB** данных (грубо, без учёта сжатия/pctfree).

## 2. Индексы на критичных таблицах

### `STATEREPORTRECORDS` (11+ индексов, PK отсутствует)

| Index | Columns |
|---|---|
| `IDX_REC_WAGON_ID` | `WAGON_ID, GETTING_DATE` |
| `IDX_REC_GETTING_DATE` | `GETTING_DATE` |
| `IX_REPORT_RECORD` | `REPORT_ID` |
| `IDX_STATEREPORTRECORDS_INFO` | `WAGON_ID, LOCATION_DATE, DEST_*, LOCATION_*, DISPATCH_*, OPERATION_ID` |
| `IDX_STATEREPORTRECORDS_LINKS` | `WAGON_ID, REPORT_ID, CURRENT_STATE_ID, …` |
| `IDX_STATEREPORTRECORDS_DATA` | `TRAIN_CODE, LOCATION_DATE` |
| FK-supporting | `DEST_`, `DISPATCH_`, `LOCATION_`, `OPERATION_`, `CURRENT_STATE_ID` |

Риск: широкий набор nonunique-индексов на таблице ~1.2B строк → дорогое поддержание при каждом `SaveStateReportLine*` (импорт каждые ~45–60 с).

### `STATEREPORTS`

`UK_REPORT_ID(ID)`, `STATEREPORTS_IDX$$_008A0007(GETTING_DATE,ID)`, `IX_USERID_USERS`.

### `WAGONAPPROACHARCHIVE`

Много одиночных FK-индексов (`IX_WAGONAPPROACHARC_*` на wagon/stations/sender/receiver/…) + `IDX_WAGONAPPROACHARC` — при ~886M строк тоже тяжёлый DML/архивация.

### `STATEREPORTSWAGON`

В профиле key-set **индексов не найдено** при ~8.2M строк и 10 FK — риск full scan при join/отчётах.

### `WAGON`

~19 индексов на ~50k строк — нормально для OLTP-карточки; не объёмный hotspot.

## 3. Конфликты нагрузки writers vs readers

| Writer | Частота (из app-конфига / jobs) | Таблицы | Readers |
|---|---|---|---|
| FTP → `ImportPkg.SaveStateReportLine*` | опрос ~45–60 с | `IMPORTFILES`, `STATEREPORTS`, `STATEREPORTRECORDS` | НУВ `GetWagonStateReports`, отчёты, `Refresh*` |
| FTP → giving/departure lines | потоковые задачи сервиса | `GIVINGDEPARTURES*`, list carriages | НУВ планировщик |
| Scheduler `FILLGIVINGREPORTS` | daily 01:00, ENABLED | производные giving-отчёты | UI/отчёты |
| `UPDATEALLREPAIRDATES` | daily 01:00, ENABLED | даты ремонтов | УВП |
| `UPDATEWAGONMONTHDISTANCESDAILY` | в каталоге jobs | `WAGONMONTHDISTANCES` | НУВ |
| `AVAILABILITY_VIEW_JOB` | daily 12:00 | availability | НУВ |
| `FILLKPI` | daily 02:00 | KPI | отчёты |

Динамические jobs `ImportPkg.CreateJobUpdateGivingDeparture` / `CreateJobUpdateRegistryMain` / `StateReportPackage.CreateJobUpdateStateReports` могут плодить разовые задания — в `ALL_SCHEDULER_JOBS` видны связанные имена (`UPDATEREGISTRYMAIN`, `IMPORTPKGUPDATEGIVINGS`, часть DISABLED).

## 4. Узкие места (выводы с опорой на словарь)

1. **`STATEREPORTRECORDS` без партиций и без PK** при >1B строк — главный риск: рост, purge, range-запросы по дате, конкуренция с онлайн-импортом.  
2. **`WAGONAPPROACHARCHIVE`** (~886M, без партиций) — второй архивный колосс.  
3. **`WAGONDOWNTIME`** (~184M) на пути отчётов (`DownTimePkg`) — тяжёлые отчёты без партиционирования.  
4. **Дублирование слоёв дислокации**: лента `STATEREPORTRECORDS` + `STATEREPORTSWAGON` + поля на `WAGON` + `WAGONSTATEREPORTSRECORDS` → несколько путей чтения одного домена.  
5. **Устаревшая статистика** на части крупных объектов (`GIVINGARCHIVE` 2023-07, `GIVINGDEPARTURES` 2024-09) — риск плохих планов.  
6. **Хаб `STATIONS` (83 FK in)** — любое изменение станций (`TR_STATIONS_CHANGED`) затрагивает широкую модель.  
7. Отсутствие видимости package body мешает оценить, есть ли пакетный upsert/дедуп при импорте одинаковых часов.

## 5. Рекомендации (без DDL в этой сессии)

Только направления для согласования с DBA (здесь **не** выполнялось):

- Партиционирование `STATEREPORTRECORDS` / архивов по `GETTING_DATE` или `LOCATION_DATE`.  
- Политика retention + job очистки (есть DISABLED `DELETEROWSIMPOORTFILES` — проверить отдельно).  
- Актуализация статистики на таблицах с `LAST_ANALYZED` старше ~6–12 мес.  
- Проверить необходимость всех индексов на `STATEREPORTRECORDS` vs профиль реальных SQL НУВ.  
- Индексация `STATEREPORTSWAGON` под фактические фильтры UI.  
- Не запускать параллельно новый writer срезов, пока не прочитан body `ImportPkg`/`UpdateStateReport`.

## 6. Jobs `DBTC` (сводка)

В `ALL_SCHEDULER_JOBS` owner `DBTC`: **35** заданий. ENABLED примеры: `FILLGIVINGREPORTS`, `FILLKPI`, `UPDATEALLREPAIRDATES`, `AVAILABILITY_VIEW_JOB`, `UPDATEWAGONMONTHDISTANCESDAILY`. Многие historical/one-shot — DISABLED/FAILED (`FILLSTATEREPROTSWAGONSTATIONS`, `IMPORTPKGUPDATEGIVINGS`, …). Полный список снят в исследовательской сессии (не дублируется целиком здесь).
