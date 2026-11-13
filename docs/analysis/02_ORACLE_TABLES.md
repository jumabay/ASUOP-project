# Каталог таблиц Oracle (`DBTC`)

Источник: живая сессия `ASUVP_SERVICE` → `tc-asuvp-03:1521/RAC.WORLD`, `ALTER SESSION SET CURRENT_SCHEMA=DBTC`.  
Только словарь/статистика (`ALL_*`, `PRODUCT_COMPONENT_VERSION`). `COUNT(*)` не выполнялся. Объёмы — `ALL_TABLES.NUM_ROWS` / `BLOCKS` (приблизительно).

Связанные документы: [02_ORACLE_RELATIONSHIPS.md](02_ORACLE_RELATIONSHIPS.md), [02_ORACLE_PERFORMANCE.md](02_ORACLE_PERFORMANCE.md), пакетный обзор [02-oracle-schema.md](02-oracle-schema.md).

## 1. Версия и сессия

| Поле | Значение | Источник |
|---|---|---|
| Product | Oracle Database 12c Enterprise Edition 12.2.0.1.0, 64bit Production | `PRODUCT_COMPONENT_VERSION` |
| DB_NAME | `RAC` | `SYS_CONTEXT('USERENV','DB_NAME')` |
| SERVICE_NAME | `RAC.WORLD` | `SYS_CONTEXT` |
| SERVER_HOST | `tc-asuvp-03` | `SYS_CONTEXT` |
| USER / CURRENT_SCHEMA | `ASUVP_SERVICE` / `DBTC` | сессия |
| Доступные owners с таблицами | практически только `DBTC` (251 TABLE, 58 PACKAGE(+BODY), 96 VIEW, ~1133 объектов) | `ALL_OBJECTS` / `ALL_TABLES` |
| Партиции в `DBTC` | **нет** (`ALL_PART_TABLES` = 0) | словарь |
| Тела пакетов в `ALL_SOURCE` | **не видны** (есть только PACKAGE spec) | `ALL_SOURCE` |
| `ALL_DEPENDENCIES` PACKAGE→TABLE | почти пусто (тела недоступны) | словарь |

## 2. Схемы

Видимые owners с объектами под этой учёткой: `DBTC` (прикладная), `PUBLIC`, `ORACLE_OCM`. Рабочая схема системы — **`DBTC`**. MSSQL `terminal` (АСУОП) к Oracle не относится.

## 3. Матрица «компонент → таблицы»

| Компонент | Роль к БД | Ключевые таблицы / views | Пакеты (факт в `ALL_OBJECTS`) |
|---|---|---|---|
| АСУ-ВП импорт (ApplicationService «приложение») | **запись** срезов/файлов | `IMPORTFILES`, `STATEREPORTS`, `STATEREPORTRECORDS`, `STATEREPORTRECORDS_TMP`, `GIVINGDEPARTURELISTSIMPORT`, `GIVINGDEPARTURES`, … | `IMPORTPKG`, `IMPORTTYPESPKG` |
| НУВ (WagonApp / WagonPlanner) | **чтение** срезов и вагонов; правка справочника вагонов | `STATEREPORTS`/`STATEREPORTRECORDS`, `WAGON`, `GIVING*`, `WAGONAPPROACH*`, `WORKLISTS*` | `STATEREPORTPACKAGE`, `WAGONPACKAGE`, `GIVINGPKG`, `GIVINGDEPARTUREPACKAGE`, `APPROACHPKG`, `WORKLISTSPKG` |
| УВП | ремонт / пробег / детали | `REPAIRS`, `REPAIRTYPES`, `REPAIRPLANTS`, `WAGONMILEAGE`, `WAGONPARTS`, `CURRENTPARTS` | `REPAIRSPKG`, `REPAIRBUDGETPKG`, `PARTSPACKAGE` |
| НУД | склад / документы деталей | `WAREHOUSES`, `WAREHOUSEDETAILS`, `*DETAILS`, `DETAILSWORKINGSHEET*` | `WAREHOUSEPKG`, `PARTSPACKAGE` |
| Отчёты | чтение агрегатов | `WAGONDOWNTIME`, `STATEREPORTSWAGON`, `SYSTEMREPORTS`, `REPORTDATES` | `REPORTSPKG`, `DOWNTIMEPKG`, `REPAIRBUDGETPKG` |
| АСОУП (сервис/НУВ, не cncClient) | сообщения | `WAGONSMESSAGES` | `ASOUPPKG` |
| АСУОП cncClient | **не Oracle** | — | MSSQL `terminal` |

## 4. Текущее / полный срез / история (по схеме, не по догадке)

| Класс | Таблицы | Доказательство |
|---|---|---|
| **Текущее состояние вагона** | `WAGON` (поля `LOCATIONSTATION_ID`, `CURRENTSTATE_ID`, `OPERATION_ID`, `REPORTDATE`, `LOCATIONDATE`, …); `WAGONAPPROACH` (~27k); `GIVING` (~9k); `CURRENTPARTS` | колонки `ALL_TAB_COLUMNS`; объёмы малы относительно архивов |
| **Полные/пакетные срезы дислокации** | `IMPORTFILES` → `STATEREPORTS` → `STATEREPORTRECORDS`; буфер `STATEREPORTRECORDS_TMP` | `ImportPkg.SaveImportFile` / `SaveStateReport` / `SaveStateReportLine*`; `StateReportPackage` comment «срезами»; `%ROWTYPE` на `STATEREPORTRECORDS`; FK `FK_REPORT_RECORD` |
| **Производный «срез по вагону»** | `STATEREPORTSWAGON` (~8.2M) | имя + FK на `WAGON`/`STATIONS`; job `FILLSTATEREPROTSWAGONSTATIONS` (сейчас FAILED/disabled) |
| **Ручные/документные срезы** | `WAGONSTATEREPORTS` + `WAGONSTATEREPORTSRECORDS` | отдельные PK/триггеры ID; `StateReportPackage.SaveWagonStateReport*` |
| **Архив / события / лента** | `WAGONAPPROACHARCHIVE` (~886M), `GIVINGARCHIVE` (~44M), `WAGONDOWNTIME` (~184M), `WAGONMILEAGE` (~34M), `PARTSREMOVED`/`PARTSAFTER`, `SYS_DOCUMENTHISTORY` | суффиксы ARCHIVE/REMOVED/HISTORY + объёмы |

Разделение «полный срез vs только изменения» **внутри** `STATEREPORTRECORDS` по словарю не видно: одна лента строк на `REPORT_ID` без флага delta/full. Признак «последний срез» есть у заголовка: `STATEREPORTS.LAST`.

## 5. Ключевые таблицы (профиль)

Объёмы — приблизительные (`NUM_ROWS`, дата `LAST_ANALYZED`). PK/FK/индексы — `ALL_CONSTRAINTS` / `ALL_CONS_COLUMNS` / `ALL_INDEXES`.

### 5.1 Импорт и срезы (АСУ-ВП → НУВ)

**`IMPORTFILES`** — журнал принятых файлов.  
PK `ID`. Колонки: `FILENAME`, `IMPORTDATE`, `TYPE`, `USER_ID`, `FILESIZE`.  
Индекс `IDX_IMPORTFILES_FILENAME`. Триггер `TR_IMPORTFILES_ID`.  
~313k строк (стат. 2026-04-01).  
Связь: `ImportPkg.SaveImportFile` / `IsImportedFile`; `STATEREPORTS.IMPORTFILES_ID`.

**`STATEREPORTS`** — заголовок среза.  
PK `ID` (`UK_REPORT_ID`). Колонки: `GETTING_DATE`, `OPERATING_DATE`, `USERID`→`USERS`, `WAGONCOUNT`, `LAST`, `IMPORTFILES_ID`.  
Триггер `TR_STATE_REPORT_ID`. ~412k строк.  
Пакеты: `ImportPkg.SaveStateReport`, `StateReportPackage.GetStateReports` / `DeleteReport`.

**`STATEREPORTRECORDS`** — строки среза (главная time-series).  
**PK в словаре нет.** Колонки (фрагмент): `WAGON_ID`, `REPORT_ID`, станции (`LOCATION_*`, `DEST_*`, `DISPATCH_*`), `LOCATION_DATE`, `OPERATION_ID`, `TRAIN_CODE`, `GETTING_DATE`, sender/receiver/cargo, контейнеры, `GIVINGLIST_ID`, `DEPARTURELIST_ID`, `WAYBILL_ID`.  
FK (13 шт.), в т.ч. `FK_REPORT_RECORD` → `STATEREPORTS`, станции → `STATIONS`, `WAGONSTATES`, `CUSTOMERS`, `OPERATIONS`, `CARGOKTZ`, `WAYBILLS`.  
Индексы: `IDX_REC_WAGON_ID(WAGON_ID,GETTING_DATE)`, `IDX_REC_GETTING_DATE`, `IX_REPORT_RECORD(REPORT_ID)`, `IDX_STATEREPORTRECORDS_INFO`, `IDX_STATEREPORTRECORDS_LINKS`, …  
**~1.215e9 строк, ~23.4M блоков, partitioned=NO** (стат. 2026-10-18).  
Явная ссылка в spec: `StateReportPackage` … `return statereportrecords%rowtype`.

**`STATEREPORTRECORDS_TMP`** — рабочий буфер (~492k, свежий analyze 2026-11-12).

**`STATEREPORTSWAGON`** — текущая/агрегированная дислокация по вагону (~8.2M). PK нет. FK на `WAGON`, станции, `ROUTES`, `CUSTOMERS`, грузы. Индексов в выборке key-set не найдено.

**`WAGONSTATEREPORTS` / `WAGONSTATEREPORTSRECORDS`** — отдельный контур документных срезов (~2.3k / ~37.7M). PK `ID`. Триггеры `TR_WAGONSTATEREPORTS_*`.

### 5.2 Вагоны

**`WAGON`** — мастер (~50k). PK `ID`. Много колонок текущего положения и ремонтов (`LOCATIONSTATION_ID`, `CURRENTSTATE_ID`, `CURRENTREPAIR_ID`, пробеги, даты). FK out ≈15; FK in ≈27 (высокая связность). Триггер `TR_WAGON_ID`.  
Справочники: `WAGONTYPES`, `WAGONSTATES`, `WAGONSTATUSES`, `WAGONOWNERS`, `WAGONMODELS`, `WAGONINFO`.

### 5.3 Подачи / уборки / подход

| Таблица | ~NUM_ROWS | Назначение |
|---|---:|---|
| `GIVING` | 9 020 | текущие подачи |
| `GIVINGARCHIVE` | 43 926 482 | архив подач |
| `GIVINGLISTS` / `GIVINGLISTCARRIAGES` | 855k / 19.7M | списки подачи и вагоны |
| `DEPARTURELISTS` / `DEPARTURELISTCARRIAGES` | 962k / 19.8M | уборка |
| `GIVINGDEPARTURES` | 9.7M | импорт подачи/уборки (строки файла) |
| `GIVINGDEPARTURELISTSIMPORT` | 1.45M | staging импорта |
| `GIVINGDEPARTURESREGISTRY` | 1.07M | реестр |
| `WAGONAPPROACH` | 26 680 | текущий подход |
| `WAGONAPPROACHARCHIVE` | 886 287 013 | архив подхода |
| `APPROACHOPERATIONS` | 4 | справочник операций |

Views: `GIVING_ROUTES_VIEW`, `GIVINGLISTSVIEW` (в `ALL_VIEWS`).

### 5.4 Ремонты и пробег

`REPAIRS` (~411k), `REPAIRTYPES` (9), `REPAIRPLANTS`, `REPAIRDOCUMENTS`, `REPAIRDETAILS`, `REPAIRBUDGET`/`REPAIRBUDGETWAGONS`, `WAGONMILEAGE` (~33.6M). Триггеры `TR_REPAIR_ID`, `TR_REPAIRS_TIME`.

### 5.5 Детали и склады (НУД)

`WAREHOUSES` (41), `WAREHOUSETYPES`, `WAREHOUSEDETAILS`, `VIRTUALWAREHOUSEDETAILS` (~659k), `PARTTYPES`, `CURRENTPARTS` (~168k), `INSTALLEDPARTS`, `PARTSREMOVED`, `PARTSAFTER`, `WAGONPARTS`, документы `ARRIVALDETAILS` / `PROCUREMENTDETAILS` / `TRANSFERDETAILS` / `WRITINGOFFDETAILS` / `REPAIRDETAILS` / `WHEELPAIRREPAIR*`.

### 5.6 Предприятия / станции / справочники

`CUSTOMERS` (~580, FK in 51), `OWNERS`, `STATIONS` (~37k, **FK in 83** — самый связанный узел), `STATIONSDISTANCE`, `CUSTOMERSTATIONREGISTRY`, `OPERATIONS`, `CARGOTYPES` / `CARGOKTZ`.

### 5.7 Отчёты / простои / сообщения

`WAGONDOWNTIME` (~184M), `SYSTEMREPORTS`, `REPORTDATES`, `WAGONSMESSAGES`, `REPORT1`/`REPORT2` (имена в `ALL_TABLES`).

## 6. Триггеры на ключевых таблицах

Почти все `TR_*_ID` — BEFORE INSERT (генерация ID). Исключения: `TR_REPAIRS_TIME` (INSERT OR UPDATE), `TR_STATIONS_CHANGED` (AFTER INSERT/UPDATE/DELETE). Список — `ALL_TRIGGERS` для key-set (34 шт.).

## 7. Ограничения исследования

- Тела `PACKAGE BODY` не читаются → внутренние INSERT/UPDATE в `ImportPkg` подтверждены контрактом spec + именами таблиц/FK, а не текстом тела.
- `v$version` недоступен (`ORA-00942`); версия взята из `PRODUCT_COMPONENT_VERSION`.
- Scheduler jobs: 35 в `ALL_SCHEDULER_JOBS` owner `DBTC` (см. performance-док).
- Пароль/учётка в документы не выносились.
