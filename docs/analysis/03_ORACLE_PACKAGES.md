# Каталог PL/SQL, представлений и заданий (`DBTC`)

Сессия только на чтение словаря: `ASUVP_SERVICE` → `tc-asuvp-03:1521/RAC.WORLD`, `CURRENT_SCHEMA=DBTC`.  
Запросы: `ALL_OBJECTS`, `ALL_PROCEDURES`, `ALL_ARGUMENTS`, `ALL_SOURCE`, `ALL_TRIGGERS`, `ALL_VIEWS`, `ALL_SCHEDULER_JOBS`, `ALL_DEPENDENCIES`.  
Процедуры и функции **не вызывались**. Секреты подключений не копировались.

Связанные документы: [03_BUSINESS_LOGIC.md](03_BUSINESS_LOGIC.md), [03_OBJECT_DEPENDENCIES.md](03_OBJECT_DEPENDENCIES.md), [02-oracle-schema.md](02-oracle-schema.md), [02_ORACLE_TABLES.md](02_ORACLE_TABLES.md).

## 1. Ограничения прав

| Что видно | Что не видно |
|---|---|
| 58 спецификаций `PACKAGE` в `ALL_SOURCE` (3192 строки) | **0** объектов `PACKAGE BODY`. Тела пакетов недоступны. Код тел не восстанавливался |
| 19 автономных `PROCEDURE` целиком (867 строк), включая DML | Автономных `FUNCTION` нет |
| 137 триггеров в `ALL_TRIGGERS` и короткие тела | В `ALL_OBJECTS` тип `TRIGGER` для `DBTC` = 0 (видимость учётки) |
| 96 `VIEW` (имена и `TEXT_LENGTH`) | Текст представлений в этот каталог не копировался |
| 34 строки `ALL_SCHEDULER_JOBS` | Объекты `WAGONUPDATEDISL`, `REFERENCESFILL`, `RUNSTATIONDUBLICATESDELETE` не найдены ни в `ALL_OBJECTS`, ни в `ALL_SYNONYMS` |
| `ALL_DEPENDENCIES`: 475 строк | `PACKAGE` → `TABLE` только 5 ссылок, потому что тела скрыты |

В спецификациях пакетов нет `EXCEPTION`, `COMMIT`, `ROLLBACK`, `PRAGMA AUTONOMOUS_TRANSACTION`. Транзакции и обработка ошибок **внутри пакетов не установлены**. Единственный явный `COMMIT` среди автономных процедур — `SP_UPDATEWAGONGRREGISTRATION`.

Клиентский контур (этап 1): каждый вызов `System.Data.OracleClient` идёт отдельной командой, своей транзакции на файл среза в C# нет. Это не свойство PL/SQL.

Пометка **вывод** ниже значит: таблицы выведены из имён, `%ROWTYPE`, DLL и FK этапа 2, а не из тела пакета.

## 2. Сводка

| Вид | Число | Статус |
|---|---:|---|
| PACKAGE (только spec) | 58 | все попавшие в выборку `VALID` у автономных процедур; пакеты в `ALL_OBJECTS` присутствуют |
| Членов пакетов (`ALL_PROCEDURES`, ненулевое имя) | см. разделы | параметры — `ALL_ARGUMENTS` |
| PROCEDURE (автономные) | 19 | `VALID`, исходник виден |
| FUNCTION (автономные) | 0 | — |
| TRIGGER | 137 | 135 `BEFORE INSERT` (почти все — присвоение id из sequence), 2 особых |
| VIEW | 96 | — |
| MATERIALIZED VIEW | 0 в `ALL_OBJECTS` | — |
| SCHEDULER JOB | 34 | **10** `ENABLED` |

Публичные переменные пакета `ImportPkg` (`prev_len`, `current_stage_id`, `prev_station_id`, …) живут в состоянии сессии. При пуле соединений это риск переноса контекста между файлами. Подтверждено спецификацией, не телом.

## 3. Дислокация и срезы

### 3.1 `DBTC.IMPORTPKG`

Назначение по заголовку spec: импорт (автор DENIS, 2006). Вызывается сервисом АСУ-ВП (`RemoteLib`), не клиентами НУВ/УВП напрямую.

| Член | Параметры (spec) | Возврат | Таблицы | Кто вызывает |
|---|---|---|---|---|
| `IsImportedFile` | `filename varchar2`, `type number` | `number` | вывод: чтение `IMPORTFILES` | АСУ-ВП, тип `0` = KZ; истина, если `1` |
| `SaveImportFile` | `filename`, `importdate date`, `type`, `filesize number` | `number` (id файла) | вывод: запись `IMPORTFILES` | все текстовые каналы среза |
| `SaveStateReport` | `importFilesId number` | `number` (id среза) | вывод: `STATEREPORTS` | после `SaveImportFile` |
| `SaveStateReportLineKz` | `stateReportId`, коды вагона/станций/операции, индекс поезда, даты строками, контрагенты, вес, контейнеры, … | нет | вывод: `STATEREPORTRECORDS` (+ справочники) | `disl_kz` и каналы, которые зовут KZ-процедуру |
| `SaveStateReportLineRu` | урезанный набор: вагон, дислокация, индекс, вес, операция, назначение, получатель, груз, тип, собственник | нет | то же | канал RU |
| `SaveStateReportLineContainer` | контейнер, документ, станции, даты, ОКПО, платформа | нет | то же | контейнеры |
| `UpdateStateReport` | `id number` | нет | вывод: закрытие/пересчёт среза; обновление `WAGON` **не доказано** | конец текстового файла |
| `SaveFileContent` | `importFilesId in number`, `cur_out out sys_refcursor` | курсор | вывод: `IMPORTFILESCONTENT_TMP` | XLS |
| `UpdateStateReportXml` | `stateReportId`, `importFileId` | нет | вывод: разбор XML/XLS внутри пакета | XLS после `SaveFileContent` |
| `SaveGivingReportLine` / `Amt` / `Tsl` / `Dr` | файл, тип подачи, дата, станции, груз, вагон, отправитель/получатель | нет | вывод: списки подачи/уборки | каналы giving |
| `SaveRegistryMainReportLine` / `SaveRegistryAdditionalReportLine` | суммы, статья, станция, месяц реестра | нет | вывод: реестры | реестр |
| `Save*ManufactoryReportLine*` | вагон, индекс, пробег, плановый ремонт, собственник | нет | вывод: цеховые строки giving/departure | DG, SSGPO, CAS и базовые |
| `UpdateGivingDeparture` / `CreateJobUpdateGivingDeparture` | `id number` | нет | побочный эффект job — см. задания `IMPORTPKGUPDATEGIVINGS` | не из класса FTP-среза |
| `UpdateRegistryMain` / `CreateJobUpdateRegistryMain` | `id number` | нет | см. job `UPDATEREGISTRYMAIN` | то же |
| `ImportStation` / `ImportStation2` / `ImportStageStations` / `UpdateRailway` | коды и наименования станций | нет | вывод: `STATIONS` | справочники, не почасовой срез |
| `GetCountryId`, `GetCargoTypeId`, `GetCustomerKtzId`, `GetAffiliationId` | код / id | `number` | вывод: справочники | внутренние (тело не видно, снаружи DLL не подтверждены все) |
| `AddWagonGivingDeparture`, `ProcessWagonGivingDeparture`, `ProcessGivingDepartures`, `UpdateImportGivingLists`, `UpdateImportDepartureLists`, `DeleteDeparturedWagons` | коды или id | нет | вывод: `GIVING*` | тело скрыто |

Исключения и фиксация транзакции в spec отсутствуют. Повтор: клиент защищён только парой «имя файла + type». Повторный вызов `SaveStateReportLine*` при уже записанном файле телом не описан.

Быстродействие: построчный вызов `SaveStateReportLine*` на ленту `STATEREPORTRECORDS` (~1.2×10⁹ строк, без PK и без партиций, много индексов). Блокировки и дедуп внутри процедуры неизвестны. `CreateJob*` может плодить разовые задания (в каталоге jobs такие блоки есть и в основном `DISABLED`).

Проверка в новой системе: не вызывать writers. Сверять сигнатуры spec с `CommandText` DLL. После отдельного разрешения — read-only сравнение `IMPORTFILES` / числа строк среза за час с уже записанным старым импортом.

### 3.2 `DBTC.STATEREPORTPACKAGE`

Заголовок spec: «пакет для работы с отчетами о состоянии вагонов (срезами)».

Факт словаря: зависимость пакета на таблицу `STATEREPORTRECORDS`. Функции `GetNextDestinationStation` / `GetPrevDepartureStation` возвращают `statereportrecords%rowtype`. `GetStateReportId`, `GetPrevDepartureStationId`, `GetPrevDestinationStationId`, `GetPrevDepartureStationName` помечены `DETERMINISTIC`.

| Группа | Члены | Параметры (кратко) | Роль |
|---|---|---|---|
| Чтение | `GetWagonStateReports` | `wagon_id`, `beginDate`, `endDate`, `result out sys_refcursor` | история среза вагона; НУВ/УВП через Remoting |
| Чтение | `GetStateReports`, `LoadStateReports` (4 перегрузки), `LoadStateReportsWagon`, `LoadStateReportsTrain`, `LoadStateReportsTime`, `LoadStateReportInfo` | даты, код вагона/поезда, курсор | списки срезов |
| Запись документа | `SaveWagonStateReport`, `SaveWagonStateReportThird`, `SaveStateReportRecord`, `UpdateWagonStateReport`, `UpdateCurrentWagonStateReport`, `DeleteStateReportRecord`, `DeleteReport` | id, имя, описание, `sysdocument_id`, вагон | ручные/документные срезы, не FTP |
| Пересчёт | `UpdateStateReport` (по `id` и по паре дат), `RefreshStateReport`, `RefreshAdditionalStateReport`, `UpdateLastReport`, `UpdateLastReportId`, `UpdatePrevStations`, `UpdatePreviousLoadingStations`, `UpdateStateReportsSenders`, `RunUpdateJob` | id среза или даты | побочные эффекты только в теле |
| Jobs | `CreateJobUpdateStateReports`, `CreateJobUpdatePrevStations` | дата или `stateReportId` | в каталоге есть разовые `UPDATEPS_*`, сейчас `DISABLED` |
| Разбор кодов | `GetWagonIdByCode`, `GetOperationIdByCode`, `GetStateIdByCode`, `GetCustomerId`, `GetCargoKTZId`, `InsertStateReport`, `InsertStateRecord`, `ImportStateRecord`, `ImportStateRecordXml` | коды, даты, `message out varchar2` у `InsertStateRecord` | альтернативный ввод; FTP идёт через `ImportPkg`, не через эти члены |
| Прочее | `CheckWagon`, `GetReportRecord` (`rowid`), `GetWagonStatus`, `GetCurrentStateReport`, `InsertReportRecordSSGPO` | id, даты | тело скрыто |

Ошибок в spec нет. `InsertStateRecord` отдаёт текст в `message out` — единственный явный канал ошибки на уровне контракта.

Риски: чтение по вагону опирается на индексы `STATEREPORTRECORDS` (`WAGON_ID`, даты). `Refresh*` / `UpdatePrevStations` на большом срезе могут долго держать блокировки; расписание и объём неизвестны. Повтор `DeleteReport` / `Refresh*` без тела считать неидемпотентным нельзя и безопасным тоже.

Проверка: для одного `wagon_id` и суток сравнить курсор `GetWagonStateReports` со старым клиентом (только чтение). Writers (`DeleteReport`, `Refresh*`, `Save*`) не запускать.

### 3.3 `DBTC.WAGONSTATEREPORTSPKG` и `DBTC.STATEREPORTSWAGONPACKAGE`

`WagonStateReportsPkg` (14 членов): документные срезы. Подтверждённый вызов DLL: `GetPublishWagonsThird`. Остальные: `SaveWagonStateReport*`, `LoadWagonStateReports`, `UpdateStateReportsKtz` / `Third`, `CreateJobUpdateWagonStateReports`. Таблицы — вывод: `WAGONSTATEREPORTS`, `WAGONSTATEREPORTSRECORDS`. Триггер `TR_WAGONSTATEREPORTS_ID` только выдаёт id.

`StateReportsWagonPackage` (3 члена): `FillStateReportsWagonStations`, `GetRouteStationDistance`, `ImportRouteStations`. Job `FILLSTATEREPROTSWAGONSTATIONS` вызывает `FillStateReportsWagonStations`, статус **FAILED**, `ENABLED=FALSE`. Таблица-приёмник по имени — вывод: `STATEREPORTSWAGON` (индексов в профиле этапа 2 нет).

## 4. Вагоны и отчёты

### 4.1 `DBTC.WAGONPACKAGE`

Константа `OPERATOR_TK = 679`. Чтение, которое зовут клиенты: `GetWagonList`, `GetWagonByCode`, `GetWagonByCodeSafe`, `GetActiveWagonList`, `GetContainerList`, `GetContainerByCode`, `GetMonthDistances`, `GetWagonsInventoryData`, `GetWagonsInventoryDataOLAP`, `GetWagonsInventoryColumns`, `GetStatusCount`, `GetWagonStatusCount`, `GetWagonType`, `GetRaPairsInfo`, `LoadWagon`, `LoadWagonModel`.

Запись (тело скрыто, по именам это DML): `SaveWagon` (длинный список полей карточки, включая исключение и пробег), `SaveWagonModel`, `DeleteWagon`, `DeleteWagonModel`, `ClearWagon`, `AddToInventoryPark`, `RemoveFromInventoryPark`, `UpdateWagonStatus`, `UpdateAllWagonStatus`, `UpdateRegisterDate`, `MoveWagonHistory`, `ImportBodySize`.

Таблицы — вывод: `WAGON` и справочники. Триггер `TR_WAGON_ID` — только id при INSERT. Обновление дислокации на `WAGON` этим пакетом из spec не следует: поля положения в сигнатуре `SaveWagon` не выделены отдельно как «текущая станция среза».

Проверка чтения: тот же код вагона в старом НУВ и в новом UI. `SaveWagon` / `DeleteWagon` не вызывать.

### 4.2 Отчётные пакеты

| Пакет | Члены | Сценарий | Таблицы |
|---|---|---|---|
| `REPORTSPKG` | `GetApprovedValue`, `GetDirectionDeparture`, `GetLastRepairs`, `GetLastTors`, `GetMovementWagons`, `GetTranspositionDeparture` | отчёты движения и последних ремонтов | вывод, тело скрыто |
| `REPORTINGPACKAGE` | `Get*Fields/Groups/Tables` для downtime, giving, planner, state, warehouse | метаданные колонок отчётов в клиенте | скорее справочник отчётов, не факт вагона |
| `DYNAMICREPORTPACKAGE` | `CurrentWagonParts` | отчёт текущих деталей | вывод: `CURRENTPARTS` |
| `DOWNTIMEPKG` | `GetWagonDownTime`, `AddWagonDownTime`, `UpdateWagonDownTime`, `FillWagonDownTime`, `CreateJobWagonDownTime` | простои; чтение — отчёты | вывод: `WAGONDOWNTIME` (~184×10⁶) |
| `REPORTDATESPKG`, `REPORTEXECUTION`, `REPORTROUTES` | 4 / 1 / 1 | даты и маршруты отчётов | не разбирались построчно |
| `RESOURCEPKG` | `GetResource`, `GetWorkPark`, `GetMonthDistance`, `WriteWagonsDistanceDayli`, `UpdateAvailabilityView`, … | парк и пробеги; jobs ниже | вывод: месячные пробеги, availability |
| `KPIPKG` | `FillKpi`, `FillKpiOldPeriod`, `SaveKpi` | job `FILLKPI` ежедневно в 02:00 | вывод: KPI |

`GetWagonDownTime` и `GetWagonsInventoryDataOLAP` — кандидаты на сверку отчётов. `Fill*` и `Write*` — запись, не вызывать.

## 5. Ремонты, детали, склад, состояние

### 5.1 `DBTC.REPAIRSPKG`

Константы видов ремонта в spec: деповской `0`, капитальный `1`, КРП `2`, текущий `3`, исключение `4`, отстой `5`, упразднённый текущий `6`, подготовка под погрузку `7`, ТР-3 `8`. Сообщения `2610` и `2612`.

Чтение УВП: `GetRepairList`, `GetWagonRepairs`, `LoadMileAgeWagons`, `LoadMileAgeWagonsEveryDay`, `LoadMileAgeWagons2610`, `LoadCurrentRepair`, `LoadRegularRepair`, `LoadExtendedCapitalRepair`, `LoadMaintenanceThreeRepair`, `LoadExclusionRepair`, `LoadTrashRepair`, `GetNextPlanRepairDate`, `Get*RepairId`.

Запись и импорт (не запускать): `Register`, `Unregister`, `Save*Repair`, `PostSave*`, `ImportMileage`, `ImportMileageWeb`, `ImportMileageWeb2612`, `ImportWagonParts`, `ImportWagonParts2731`, `ImportInstalledWagonParts`, `ImportPrice`, `UpdateAllRepairStates`, `UpdateAllLastRepairs`, `UpdateAllDoneRepairs`, `SendMileageWagon`, `DeleteRepair`, `ConvertToRepair`.

Таблицы — вывод: `REPAIRS`, `WAGONMILEAGE`, `WAGONPARTS`. Триггеры на `REPAIRS`: `TR_REPAIR_ID` (id) и `TR_REPAIRS_TIME` (см. §8). Зависимость пакета на эти таблицы в `ALL_DEPENDENCIES` **не видна**.

Повтор `Register` / `ImportMileage*` без тела опасен: идемпотентность не доказана. `LoadMileAgeWagonsEveryDay` по имени похож на суточную загрузку, но отдельного ENABLED-job с этим именем нет; суточные даты ремонтов делает `JOBPKG.UPDATEALLREPAIRDATES`.

### 5.2 Смежные пакеты ремонта

| Пакет | Назначение по именам | Замечание |
|---|---|---|
| `REPAIRBUDGETPKG` | бюджет: `GetRepairBudgetData`, `FillRepairBudget`, `SaveRepairBudget*`, `RepairWagon` | чтение — отчёты УВП; запись не запускать |
| `REPAIRDATES_PKG` | плановые даты: `UpdateAllRepairDates`, `FillPlanRepairs*` | вызывается из `JOBPKG` (см. jobs) |
| `REPAIRREPORTSPKG` | заполнение состояний по заводам (`FillRepairPlants*`, `FillRepairState*`) | пакетная запись, тело скрыто |
| `PARTSPACKAGE` | детали: модели 0–3 (количество, колёсная пара, изготовление, крышка люка). `GetHistoryState`, `LoadPartHistory`, `SearchParts` — чтение. `RotateRepairDetails`, `Save*`, `Move*`, `WriteOff*` — запись | job `ROTATEREPAIRDETAILS` обходит `repairs` и вызывает `RotateRepairDetails`; **DISABLED** |
| `WAREHOUSEPKG` | «Пакет работы со складом». Виды документов 0–6: закуп, приход, перемещение, списание, ремонт колёсных пар, наряд, ремонт. `Register*` / `Unregister*` — НУД | триггеры документов в ключевом наборе — в основном id. Проводки — в теле |

### 5.3 Состояние вагона

Отдельного пакета «только состояние» нет. Состояние размазано:

- срез: `StateReportPackage` / `ImportPkg` → `STATEREPORTRECORDS` (вывод);
- карточка: `WagonPackage.SaveWagon` / `UpdateWagonStatus`;
- подача: `GivingPkg.UpdateStateReport`, `WorkListsPkg.UpdateStateReport`, `GraphSheetPackage.UpdateStateReports`;
- текущие детали: `PartsPackage.UpdateWagonCurrentDetails`, `GetHistoryState`.

Какой из них пишет `WAGON.CURRENTSTATE_ID` и `LOCATIONSTATION_ID`, по spec не видно. Job `WAGONUPDATE` (каждые 15 минут, ENABLED) вызывает `WAGONUPDATEDISL.WAGONUPDATEDISLTIS` — объект вызывающему пользователю **не виден**. Это главный пробел по «обновлению состояния».

## 6. Остальные пакеты (кратко)

Члены посчитаны по `ALL_PROCEDURES`. Назначение — по имени. Тела скрыты. Классификация — в [03_BUSINESS_LOGIC.md](03_BUSINESS_LOGIC.md).

| Пакет | Членов | Назначение по имени |
|---|---:|---|
| `GIVINGDEPARTUREPACKAGE` | 69 | подача/уборка, списки, избыточный простой; факт словаря: читает `STATEREPORTRECORDS` |
| `GIVINGPKG` | 15 | подача, сообщения собственнику, `UpdateStateReport` |
| `GIVINGDEPARTUREREPORTS` | 4 | `FillGivingReports2` — job `FILLGIVINGREPORTS` |
| `REMOTEGIVINGDEPARTUREPACKAGE` | 5 | удалённый контур подачи |
| `APPROACHPKG` | 6 | подход вагонов |
| `WORKLISTSPKG` | 16 | наряды; факт словаря: таблица `WORKLISTS` |
| `GRAPHSHEETPACKAGE` | 21 | график; разовые jobs `UPDATEGSP_*` |
| `IMPORTTYPESPKG` | 21 | типы импорта |
| `ASOUPPKG` | 10 | сообщения 2610/2612/2731 |
| `DIRECTORIES` | 70 | справочники |
| `CUSTOMERPKG` | 30 | контрагенты |
| `STATIONSPACKAGE` | 17 | станции |
| `ROUTESPKG` | 37 | маршруты |
| `PLANNINGVOLUMEPKG` | 54 | план объёмов |
| `REQUESTPACKAGE` / `REQUESTPKG` / `REQUESTCALCPACKAGE` | 35 / 9 / 6 | заявки; `REQUESTPACKAGE` зависит от таблицы `REQUEST` |
| `TRANSPORTREQUESTPKG` | 30 | заявки на перевозку |
| `WAYBILLSPKG` | 5 | накладные |
| `STAGEPKG` | 10 | перегоны |
| `RINGSPKG` | 10 | кольца |
| `TIMESPKG` | 6 | нормализация времени; зовут триггеры ремонтов |
| `JOBPKG` | 5 | `UpdateAllRepairDates`, `FillAllWagonDownTime`, `BackupDbtc`, обновление view ремонтов |
| `SECURITY` / `PERMISSIONS` / `ADMIN` нет отдельно | 36 / 22 | доступ; `PERMISSIONS` → `USERS`. `AdminPkg` из DLL в этом списке имён **нет** — в схеме пакет называется `SECURITY` / `PERMISSIONS` |
| `SYS_TRANSPORTPKG` | 6 | `SendDocument`, `RejectDocument` |
| `SYS_DOCUMENTSPKG` / `SYS_CLASSTYPEPKG` | 2 / 3 | документы |
| `LOGSPKG` / `ERRORSPKG` / `UTILSPKG` / `CONSTANTSPKG` | 1 / 1 / 3 / — | служебные |
| `AVAILABILITYPKG` | 2 | наличие |
| `WAGONTHROWINGPKG` | 2 | бросание; job остановлен |
| `GDPACKAGE` | 6 | не разобран построчно |
| `WORKINGSHEETPACKAGE` | 16 | наряды деталей (рядом с `WarehousePkg`) |
| `CALCULATIONSTEPSPKG` | 3 | шаги расчёта |
| `TEST` | 53 | тестовый пакет в промышленной схеме |

Имя `AdminPkg` из декомпиляции DLL на этом контуре отдельным объектом не подтверждено. `LockUser` / `GrantObjects` нужно искать в `SECURITY` и `PERMISSIONS` при следующем чтении spec, не вызывать.

## 7. Автономные процедуры

Исходник виден. Это разовые загрузчики, не API почасового среза. Почти все делают `INSERT`/`UPDATE`. Явный `COMMIT` есть у `SP_UPDATEWAGONGRREGISTRATION`. Обработчиков `EXCEPTION` в этих 19 файлах нет.

| Процедура | Что делает (по тексту) | Таблицы (словарь зависимостей) |
|---|---|---|
| `SP_ADDCONTEINER` | вставка вагона-контейнера, тип 67 | `WAGON` |
| `SP_ADDSTATION` | станция, если кода ещё нет | `STATIONS` |
| `SP_ADDWAGONGIVINGDEPARTURE` / `2` | разбор строки подачи | `CUSTOMERS`, `REPORT1` / `REPORT2` |
| `SP_ARRIVALWAGONPARTS` | приход деталей на склад | `WAGONPARTS`, `PROCUREMENT*`, `TRANSFERDOCUMENTS`, `WAREHOUSES`, `PARTTYPES`, `CUSTOMERS` |
| `SP_ARRIVEWHEELPAIR` | приход колёсной пары | `ARRIVALDOCUMENTS`, `ARRIVALDETAILS`, `WAGONPARTS`, `WAREHOUSES`, `REPAIRPLANTS` |
| `SP_TRANSFERWAGONPART` | перемещение детали | `TRANSFER*`, `WAGONPARTS`, `WAREHOUSES`, `PROCUREMENT`, `REPAIRPLANTS` |
| `SP_CONVERTREPAIRSTOTR1` | смена вида ремонта через `WagonPackage.GetWagonByCode` | `REPAIRS`; есть `DBMS_OUTPUT` |
| `SP_GETMILEAGE` | читает пробег | `WAGONMILEAGE` |
| `SP_IMPORTCARGOKTZ` | справочник грузов КТЗ | `CARGOKTZ` |
| `SP_IMPORTWAGONOWNER` | собственники | `WAGONOWNERS` |
| `SP_INSERTREASON` / `SP_UPDATEREASON` | причины, через `Directories` | `REASONS` |
| `SP_UPDATESTATIONCOORDINATES` | долгота/широта | `STATIONS` |
| `SP_UPDATESTATIONSDISTANCE` | расстояние | `STATIONSDISTANCE` |
| `SP_UPDATEWAGONGROUP` / `SP_UPDATEWAGONGRREGISTRATION` | группа вагона; вторая делает `COMMIT` | `WAGON`, `WAGONGROUPS` |
| `SP_WAGONEXCLUDE` | исключение вагона | `WAGON`, `WAGONEXCLUSION`, `REPAIRS`, `CUSTOMERS` |
| `UPDATEWAGONTARWEIGHT` | тара | `WAGON` |

Проверка: не исполнять. Для новой системы это не контракт UI, пока вызов не найден в DLL.

## 8. Триггеры

137 штук, все `ENABLED`. 134 тела — присвоение `:new.id` из sequence (шаблон `TR_*_ID`, `BEFORE INSERT` FOR EACH ROW).

Исключения, где тело не сводится к sequence:

| Триггер | Таблица | Событие | Тело (факт) |
|---|---|---|---|
| `TR_REPAIRS_TIME` | `REPAIRS` | `BEFORE INSERT OR UPDATE` | `TimesPkg.WriteTime` для `:new.beginingdate` и `:new.endingdate` |
| `TR_PLAN_REPAIRS_TIME` | `PLAN_REPAIRS_T` | `BEFORE INSERT` | `TimesPkg.WriteTime(:new.repair_plan_date)` |
| `TR_STATIONS_CHANGED` | `STATIONS` | `AFTER INSERT OR UPDATE OR DELETE` | `UPDATE LASTCHANGETIMES SET TIME_STAMP = systimestamp WHERE OBJECT_NAME = 'STATIONSVIEW'` |

На `STATEREPORTRECORDS`, `WAGONMILEAGE`, `CURRENTPARTS` триггеров в этой выборке нет. Значит пересчёт дислокации и пробега, если он есть, сидит в пакетах или в job `WAGONUPDATEDISL`, а не в триггере строки среза.

Ключи id, нужные домену: `TR_IMPORTFILES_ID`, `TR_STATE_REPORT_ID`, `TR_WAGON_ID`, `TR_REPAIR_ID`, `TR_WAGONPART_ID`, `TR_WAGONSTATEREPORTS_ID`, `TR_STATION_ID`.

Риск: `TR_STATIONS_CHANGED` пишет `LASTCHANGETIMES` на каждую строку станции — узкое место при массовом `ImportStation`. `TR_REPAIRS_TIME` вызывает пакет на каждый ремонт; что делает `WriteTime`, в теле `TIMESPKG` не видно.

Проверка: после согласованной вставки в тестовой копии смотреть, что id проставлен и что `LASTCHANGETIMES` для `STATIONSVIEW` сдвинулся. На этом контуре DML не делался.

## 9. Представления

96 view в `DBTC`. Текст не разбирался (права на `ALL_VIEWS.TEXT` не использовались для копирования логики). Приоритетные по имени и длине текста:

| View | `TEXT_LENGTH` | Зачем смотреть дальше |
|---|---:|---|
| `WAGONVIEW` | 2208 | карточка вагона |
| `TM_REPAIRSVIEW` | 1526 | ремонты |
| `TM_WAGONVIEW` | 1146 | вагон (вторая проекция) |
| `GIVINGDEPARTURESVIEW` | 1259 | подача |
| `GIVINGLISTCARRIAGESVIEW` | 1053 | вагоны в списке подачи |
| `GIVINGLISTSVIEW` | 772 | списки подачи |
| `GIVING_ROUTE_REPORT_VIEW` | 1141 | отчёт маршрута |
| `LASTREPAIRSVIEW` / `LASTREPAIRVIEW` / `CURRENTREPAIRVIEW` | 902 / 804 / 478 | последний и текущий ремонт |
| `REPAIRSVIEW` / `PLAN_REPAIRS_VIEW` / `REPAIRBUDGETVIEW` | 705 / 678 / 186 | ремонты и бюджет |
| `WAGONSTATEREPORTSVIEW` | 311 | документные срезы |
| `WAGONDOWNTIMEVIEW` | 214 | простои |
| `WAGONMILEAGEVIEW` | 344 | пробег |
| `PARTSAFTERVIEW` / `LIFETIMEWAGONPARTSVIEW` | 450 / 240 | детали |
| `REPORTRECORDS` | 240 | строки отчёта |
| `KPIVALUESVIEW` | 203 | KPI |

`ALL_DEPENDENCIES`: view → table 273, view → view 6, view → package 15. Это единственный массовый граф «объект → таблица», который словарь отдал. Имена связей — в [03_OBJECT_DEPENDENCIES.md](03_OBJECT_DEPENDENCIES.md).

## 10. Задания планировщика

34 job владельца `DBTC`. Ни один job не запускался.

ENABLED (10):

| Job | Расписание | Действие (как в словаре) | Состояние |
|---|---|---|---|
| `WAGONUPDATE` | каждые 15 минут | `WAGONUPDATEDISL.WAGONUPDATEDISLTIS` | SCHEDULED. Объект не виден |
| `AVAILABILITY_VIEW_JOB` | ежедневно 12:00 | `ResourcePkg.UpdateAvailabilityView` | SCHEDULED |
| `FILLGIVINGREPORTS` | ежедневно 01:00 | `GivingDepartureReports.FillGivingReports2` | SCHEDULED |
| `UPDATEALLREPAIRDATES` | ежедневно 01:00 | `JobPkg.UpdateAllRepairDates` | SCHEDULED |
| `UPDATEWAGONMONTHDISTANCESDAILY` | ежедневно 01:00 | `ResourcePkg.WriteWagonsDistanceDayli` | SCHEDULED |
| `FILLKPI` | ежедневно 02:00 | `KpiPkg.FillKpi` | SCHEDULED |
| `UPDATEREGISTRYMAIN` | ежедневно 02, 04, 06 | цикл по `Importfiles` (анонимный блок, автор в комментарии 2019) | SCHEDULED |
| `UPDATEEXCESSINACTIVITY` | ежедневно 23:00 | `UPDATE GIVINGLISTCARRIAGES` через `GivingDeparturePackage.GetExcessInactivity` | SCHEDULED |
| `FILLREF` | пн–пт, часы 9–17 | `referencesfill` — объект не виден | SCHEDULED |
| `FILLREF_WEEKENDS` | сб–вс, остальные часы | тот же `referencesfill` | SCHEDULED |

Не включены, но важны для домена:

| Job | Состояние | Действие |
|---|---|---|
| `FILLSTATEREPROTSWAGONSTATIONS` | FAILED | `StateReportsWagonPackage.FillStateReportsWagonStations` |
| `IMPORTPKGUPDATEGIVINGS` | DISABLED | цикл по временной таблице giving |
| `DELETEROWSIMPOORTFILES` | STOPPED | `WAGONUPDATEDISL.DeleteRowsImportFiles` |
| `PIUPIU` | DISABLED | `WAGONUPDATEDISL.UpdateDislocationLast` |
| `ROTATEREPAIRDETAILS` | DISABLED | цикл по всем `REPAIRS` → `PartsPackage.RotateRepairDetails` |
| `UPDATEPS_*`, `UPDATEGSP_*` | DISABLED | точечные `StateReportPackage.UpdatePrevStations` / `GraphSheetPackage.UpdateStateReports` с конкретным id |
| `FILLKPIOLDPERIOD`, `FILLGIVINGREPORTSOLDPERIOD`, `FILLALLWAGONSOLDPERIODJOB` | DISABLED или FAILED | исторический догон |
| `WAGONTHROWINGCALCMANUALLY` | STOPPED | `WagonThrowingPkg.CalcThrowingPeriodWagons` |
| `UPDATEWAYBILLSMANUALLY` | FAILED | догон накладных за январь 2020 |
| `UPDATEAVAILABILITYVIEWPERIOD`, `UPDATEROTATEDREPAIRSTR1`, `UPDATEGIVINGDEPARTUREAFFILIATION` | SUCCEEDED | разовые, уже отработавшие |

Повторный запуск ENABLED-job на тех же сутках не проверялся. `UPDATEEXCESSINACTIVITY` и `UPDATEREGISTRYMAIN` по тексту действия переписывают данные; идемпотентность не доказана. Одновременный прогон в 01:00 (`FILLGIVINGREPORTS`, `UPDATEALLREPAIRDATES`, `UPDATEWAGONMONTHDISTANCESDAILY`) плюс импорт среза каждые ~45–60 с — конкуренция писателей. Блокировки из словаря не снимались (`V$LOCK` не запрашивался).

Проверка в новой системе: не создавать параллельный планировщик этих имён, пока тела `JobPkg`, `ResourcePkg`, `KpiPkg` и объект `WAGONUPDATEDISL` не прочитаны. Сверка — сравнение результатов чтения (пробег, KPI, даты ремонта) до и после штатного окна, без ручного `RUN`.
