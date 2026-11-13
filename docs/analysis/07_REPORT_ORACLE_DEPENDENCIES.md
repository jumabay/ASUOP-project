# Oracle-зависимости пользовательских отчётов

Источники: spec пакетов в `docs/analysis/_oracle_tmp/source`, вызовы из декомпилированных клиентов, уже снятый словарь (`02_ORACLE_TABLES.md`, `03_ORACLE_PACKAGES.md`, `03_OBJECT_DEPENDENCIES.md`). Тела пакетов не читались и не выполнялись. Имена таблиц внутри `GetFields` / `GetGroups` / `GetTables` в spec не указаны.

Схема клиента и службы — `dbtc`. Отчётное соединение — сессия службы, не отдельный пользователь «reports».

## 1. Метаданные конструктора

Клиент читает три курсора `ReportingPackage` с параметром `applicationId`.

| Процедура | Колонки, которые читает клиент |
| --- | --- |
| `GetGroups` | `ID`, `TITLE`, `DESCRIPTION`, `PARENT_ID` |
| `GetTables` | `Id`, `Name`, `JoinString`, `Name_For_Query` |
| `GetFields` | `Title`, `Description`, `Name`, `FieldType`, `Table_Id`, `Group_id` |

`JoinString` вставляется в `FROM` как есть, после перевода в верхний регистр. `Name`, если начинается с `(`, вставляется в список `SELECT` как выражение.

В том же пакете есть процедуры без параметра приложения и пары `GetPlanner*`, `GetWarehouse*`, `GetGiving*`, `GetState*`, `GetDownTime*`. Разобранные формы их не вызывают.

Номера приложений, которые реально передаёт конструктор: 1 (вагоны), 2 (подача НУВ), 4 (склад), 5 (срезы), 6 (простои), 9 (маршруты), 11 (движение по срезам).

## 2. Корни конструктора и таблицы под ними

Зависимость view → таблица взята из уже снятого `ALL_DEPENDENCIES`. Тело view здесь не повторяется.

| Корень в клиенте | Куда смотрит словарь | Замечание по объёму |
| --- | --- | --- |
| `Wagon` | таблица `WAGON` (~50 тыс., много индексов) | конструктор может присоединить любые таблицы из `JoinString` |
| `STATEREPORTRECORDS` | та же таблица | больше 1 млрд строк, PK нет, партиций нет. Писатель — импорт срезов |
| `STATEREPORTSWAGON` | та же таблица, FK на вагон, станции, маршруты, заказчиков, грузы | ~8,2 млн, в key-set индексов не было |
| `GIVINGLISTSVIEW` | `GIVINGLISTS` | view короткая (текст ~772) |
| `GIVING_ROUTE_REPORT_VIEW` | в зависимостях объект `GIVING_ROUTES_VIEW` помечен как таблица | текст view ~1141 |
| `WAGONDOWNTIMEVIEW` | `WAGONDOWNTIME` | ~184 млн, текст view ~214 |
| `WAREHOUSESVIEW` | складской контур НУД | корень конструктора склада |

Представление `REPORTRECORDS` зависит от `STATEREPORTRECORDS`. Конструктор открывает таблицу `STATEREPORTRECORDS` напрямую, не это представление.

`WagonReport.GetCodesTable` добавляет `WAGONTYPES` по `WAGON.WAGONTYPE_ID`, если такого соединения ещё нет.

## 3. Процедуры готовых отчётов

Чтение. Параметры — как их именует клиент.

| Процедура | Параметры клиента | Кто вызывает | Что известно без тела |
| --- | --- | --- | --- |
| `RequestPackage.GetLoadingCalendarReport` | `report_date`, `cargotype_id`, `loadingstation_id`, `sender_id` | план погрузки | календарные колонки дней плюс имена груза, станции, отправителя |
| `RequestPackage.GetLoadingEvenReport` | те же | план погрузки | равномерная погрузка: отправитель, груз, станции, согласованные вагоны и тонны, среднесуточная, тип ПС |
| `ReportRoutes.GetReportPlan` | `execution_date`, `operator_id_` | планируемые объёмы | spec: курсор или конвейер `reportroutes$report_set` |
| `ReportExecution.GetReportExecution` | `execution_date`, `operator_id_` | выполнение плана | spec: тот же тип набора `reportroutes$report_set` |
| `ReportsPkg.GetDailyRealizationReport` | `Operator_Id`, `ReportDay` | реализация | в файле spec `ReportsPkg` этой процедуры нет; клиент передаёт имя строкой |
| `ReportsPkg.GetMovementWagons` | `beginDate`, `endDate` | движение | spec: курсор и конвейер `reportspkg$movementwagons_set`. Клиент зовёт курсор |
| `ReportsPkg.GetLastRepairs` | `wagontype_id_`, `repairtype_id`, `repairplant_id`, `begin_date`, `end_date` | анализ, первый вызов | курсор `directories.ref_cursor_t`. Клиент считает, что колонка 0 — код вагона, колонка 4 — дата |
| `ReportsPkg.GetLastTORs` | `wagon_code`, `wagonunit_id`, `reason_id`, `station_id`, `lastrepair_date` | анализ, на каждую строку | тот же тип курсора |
| `RepairReportsPkg.FillStateInventory` | нет | инвентарь | spec: конвейер `repairspkg$inventorystate_set` и курсор |
| `RepairReportsPkg.FillRepairStateCurrent` | нет | инвентарь | `repairspkg$repairstate_set` |
| `RepairReportsPkg.FillRepairStateDepot` | нет | инвентарь | то же |
| `RepairReportsPkg.FillRepairStateCapital` | нет | инвентарь | то же |
| `RepairReportsPkg.FillRepairPlantsRF` | нет | инвентарь | `repairreports$repairplant_set` |
| `RepairReportsPkg.FillRepairPlantsTrv` | нет | инвентарь | то же |
| `RepairReportsPkg.FillRepairPlantsBereke` | нет | инвентарь | то же |

В spec есть и клиент не вызывает: `FillRepairPlantsZiksto`, `FillRepairPlantsBogatyrTrans`, `FillTransitionDates`, `ReportsPkg.GetApprovedValue`, `GetDirectionDeparture`, `GetTranspositionDeparture`, `DynamicReportPackage.CurrentWagonParts` (конвейер `CURRENTPARTS`).

`ReportDatesPkg` (`SavePeriod`, `DeletePeriod`, `LoadPeriod`, `GetBeginPeriod`) к кнопкам этого разбора не подключён. Таблица `REPORTDATES` в словаре есть; кто её пишет из этих экранов — не видно.

## 4. Запись, которая кормит отчёты

Эти процедуры из конструктора не вызываются. Они меняют данные, которые конструктор и книги Excel потом читают.

| Кто | Процедура | Куда, по уже снятому словарю |
| --- | --- | --- |
| job `FILLGIVINGREPORTS`, ежедневно 01:00 | `GivingDepartureReports.FillGivingReports2` | производные данные подачи. В пакете также `FillGivingReportsOldPeriod`, `RefreshGivingReports`, `RunRefreshJob` |
| job `FILLKPI`, ежедневно 02:00 | `KpiPkg.FillKpi` | `KPIVALUES`. Представление `KPIVALUESVIEW` |
| job `AVAILABILITY_VIEW_JOB`, ежедневно 12:00 | `ResourcePkg.UpdateAvailabilityView` | витрина наличия. Книга `Availability.xlt` открывается отдельно, связь с этой витриной в C# не прописана |
| job `UPDATEALLREPAIRDATES`, ежедневно 01:00 | `JobPkg.UpdateAllRepairDates` | даты ремонтов |
| импорт службы, каждые 45–60 с | `ImportPkg.SaveStateReportLine*` | `STATEREPORTS`, `STATEREPORTRECORDS` |
| простой, не из конструктора | `DownTimePkg.FillWagonDownTime` и соседние `Fill*` / `Add*` | `WAGONDOWNTIME`. Конструктор простоев только читает `WAGONDOWNTIMEVIEW` |

`DownTimePkg.GetWagonDownTime` — отдельное чтение простоя. Конструктор его не вызывает: он читает представление своим `SELECT`.

## 5. Исполнение текста конструктора

Произвольный `SELECT` конструктора не завёрнут в процедуру. Его принимает `DbManagerCall.GetData`: `CommandType.Text` на соединении сессии. Рядом так же исполняются `ExecuteScalar` и `ExecuteQuery`. Ограничение «только представления отчёта» в этом методе не задано.

Таймаут выборки конструктора — 60 секунд на стороне службы (таймер, затем `Cancel`, закрытие соединения сессии, `Thread.Abort`). Предела числа строк нет.

## 6. Объекты, которые легко спутать с отчётом

| Объект | Почему не этот сервис |
| --- | --- |
| `STATEREPORTS` | заголовок среза импорта (~412 тыс.), не шаблон пользователя |
| `WAGONSTATEREPORTS` / `WAGONSTATEREPORTSRECORDS` | документные срезы НУВ, пакет `WagonStateReportsPkg` |
| `SYSTEMREPORTS`, `REPORT1`, `REPORT2` | имена есть в `ALL_TABLES`. Конструктор и готовые формы их не называют |
| `PlatformStateReports` | сборка .NET, методы задания не реализованы, таблица Oracle с таким именем в этом разборе не вызывалась |
