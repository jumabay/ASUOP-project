# Каталог отчётов, найденных в поставке

Список собран по формам УВП (`WagonApp`), НУВ (`WagonPlanner`), НУД (`WarehouseApp`) и классам `Report` / `StaticalReport`. Сохранённые пользователями файлы `.rep` / `.brp` и отчёты информационной панели в поставке отсутствуют: в каталоге только типы, которые открывает программа.

Поля конструктора приходят из `ReportingPackage.GetFields(applicationId)`. Этот курсор не выполнялся, поэтому перечень колонок конструктора здесь не выдуман. Ниже — корень `FROM` и номер приложения из `WagonLib.Reporting.Enums.Applications`.

Расчёт готового отчёта, у которого вызвается процедура, находится в скрытом теле пакета. В клиенте видна привязка параметров и подписи колонок.

## 1. Конструктор

Общий алгоритм для всех строк таблицы: пользователь выбирает поля и условия, `QueryBuilder` собирает один `SELECT`, `DbManagerCall.GetData` исполняет текст. Сортировки в этом `SELECT` нет. Группировка — `SUM` / `MAX` / `MIN` / `AVG` / `COUNT` и `GROUP BY` остальных выводимых полей. Арифметика — только для числового поля.

| Пункт меню | Где | Класс | Корень | `applicationId` | Право на пункт |
| --- | --- | --- | --- | --- | --- |
| Конструктор → Вагоны | УВП | `WagonReport` | `FROM Wagon` | `WagonApp` = 1 | `CreateDynamicReports` |
| Конструктор → Срезы | УВП и НУВ | `StateReport` | `FROM STATEREPORTRECORDS` | `StateApp` = 5 | УВП: `CreateDynamicReports`. НУВ: `PlannerReportConstructor` |
| Конструктор → Движение по срезам | УВП и НУВ | `StateReportsWagon` | `FROM STATEREPORTSWAGON` | `StateReportsWagon` = 11 | то же |
| Конструктор → Маршруты | УВП, обе кнопки с подписью «Маршруты» | `GivingRouteReport` | `FROM GIVING_ROUTE_REPORT_VIEW` | `GivingRouteApp` = 9 | `CreateDynamicReports` |
| Конструктор → подача (`msGivingReport`, `msGivingDepartureReports`) | НУВ | `GivingReport` | `FROM GIVINGLISTSVIEW` | `PlannerApp` = 2 | `PlannerReportConstructor` |
| Конструктор → маршруты подачи | НУВ | `GivingRouteReport` | `FROM GIVING_ROUTE_REPORT_VIEW` | 9 | `PlannerReportConstructor` |
| Конструктор → простои | НУВ | `DownTimeReport` | `FROM WAGONDOWNTIMEVIEW` | `DownTimeApp` = 6 | `PlannerReportConstructor` |
| Конструктор отчётов | НУД | `WarehouseReport` | `FROM WAREHOUSESVIEW` | `WarehouseApp` = 4 | `WarehouseReportConstructor` |

`PlannerReport` (`FROM Wagon`, приложение `PlannerApp`) в сборке есть. Вызова `new PlannerReport` в формах УВП, НУВ и НУД нет.

Номера `Directories` = 3, `TransportRequestApp` = 7, `RecordTrafficApp` = 8, `ManagerApp` = 10 в конструкторах этих форм не используются. В spec `ReportingPackage` остаются старые процедуры без `applicationId` и отдельные `GetPlanner*`, `GetWarehouse*`, `GetGiving*`, `GetState*`, `GetDownTime*`. Текущий клиент зовёт только тройку с `applicationId`.

Запуск с информационной панели (УВП и НУВ) берёт уже собранный `Report` из реестра и при флаге `ViewDynamicReports` показывает `ReportViewerForm`. Если у полей стоит «запрашивать значение», перед показом открывается `FieldRequestDialog`. В УВП можно слить условия с группой вагонов обозревателя (`MergeReportConditions`).

## 2. Готовые отчёты с процедурой

Форма `StaticalReportForm` по кнопке получения вызывает `GetData()`, затем вызывающий код выгружает Excel. Параметры формы строятся по свойствам с атрибутом `Property`. В процедуру уходят параметры команды, не склеенный SQL.

| Пункт | Класс | Параметры клиента | Процедура | Что делает клиент помимо выборки | Шаблон |
| --- | --- | --- | --- | --- | --- |
| План погрузки | `CalendarLoadingReport` | дата, тип груза, станция погрузки, отправитель (пустой справочник → `DBNull`) | `RequestPackage.GetLoadingCalendarReport` и `RequestPackage.GetLoadingEvenReport` | режет календарь на таблицы по грузу, станции и отправителю; строка «ИТОГО» — сумма дней в клиенте. Вторая таблица — «Грузы с равномерной погрузкой» | нет, книга создаётся пустой |
| Планируемые объёмы | `TotalPlanReport` | дата, оператор вагонов | `ReportRoutes.GetReportPlan` | колонки: маршрут, тип маршрута, тип вагона, тип груза, количество, оборот, объём по кольцу, объём. Заголовок листа — месяц даты | `Templates\VolumePlans.xlt`, иначе пустая книга |
| Выполнение плана | `ExecutionPlanReport` | дата, оператор | `ReportExecution.GetReportExecution` | колонки расчётного, планового и фактического числа вагонов, оборотов и объёмов, плюс три отношения «к расчёту / к плану» как отдельные поля курсора. Клиент их не пересчитывает | `Templates\ExecutionVolumePlans.xlt` |
| Реализация | `DailyRealizationReport` | день, оператор; оператор перед вызовом догружается `LoadObject` | `ReportsPkg.GetDailyRealizationReport` | подписи в шаблон: собственник или «по всем вагонам», дата. Колонки — как вернул курсор | `Templates\RealizationReport.xlt` |
| Сводный отчёт по движению | `MovementWagonsReport` | дата начала, дата конца | `ReportsPkg.GetMovementWagons` | колонки: направление, тип колеи, станция, дата, число вагонов, индекс станции, отделение. Заголовок: «Справка по движению вагонов ЖРС на Китай…» | ищет `Templates\MovementWagonsReport.xlt`. В папке НУВ есть только `MovementWagonsReport.xsl` |
| Справка о состоянии инвентарного парка | `InventoryStateReport` | дата только в заголовке «о состоянии инвентарного парка на …» | см. таблицу ниже, все вызовы без параметров | раскладывает курсоры по листам шаблона | `Templates\InventoryStateReport.xltx` |
| Анализ | `ReportsBinder.GetAnalyzeReport` | тип вагона, тип ремонта, предприятие, период; затем на каждую строку код вагона, узел или причина, станция, дата ремонта | `ReportsPkg.GetLastRepairs`, затем `ReportsPkg.GetLastTORs` | ТОР дописываются вправо новыми колонками с суффиксом номера. Ветки «только ТОР» без первого вызова нет | сетка `DataGridForm`, не Excel-шаблон |

Вызовы справки инвентаря, в этом порядке:

| Лист / таблица клиента | Процедура |
| --- | --- |
| `InventoryStateReport` | `RepairReportsPkg.FillStateInventory` |
| `CurrentRepairState` | `RepairReportsPkg.FillRepairStateCurrent` |
| `DepotRepairState` | `RepairReportsPkg.FillRepairStateDepot` |
| `CapitalRepairState` | `RepairReportsPkg.FillRepairStateCapital` |
| `RepairPlantRF` | `RepairReportsPkg.FillRepairPlantsRF` |
| `RepairPlantTrv` | `RepairReportsPkg.FillRepairPlantsTrv` |
| `RepairPlantBereke` | `RepairReportsPkg.FillRepairPlantsBereke` |

Колонки листа инвентаря, которые клиент ждёт от курсора: собственник, инвентарный парк, отстой, разрешение на курсирование, исключён из инвентаря, в ожидании исключения, ДР, КР, КРП, ТОР, всего нерабочий парк, рабочий парк. Скрытые: `owner_id`, `row_index`.

Лист ремонтного предприятия ждёт счётчики ТОР / ДР / КР по полувагонам, цементовозам, окатышевозам и платформам.

В spec `RepairReportsPkg` есть ещё `FillRepairPlantsZiksto`, `FillRepairPlantsBogatyrTrans`, `FillTransitionDates`. Из `InventoryStateReport` они не вызываются.

`ReportsPkg` в spec также содержит `GetApprovedValue`, `GetDirectionDeparture`, `GetTranspositionDeparture` и конвейерную `GetMovementWagons` без курсора. Готовые формы их не вызывают. `GetDailyRealizationReport` в этом spec-файле не объявлен, клиент имя процедуры передаёт строкой.

## 3. Книги Excel, которые программа только открывает

НУВ, меню отчётов, `Process.Start` на файл. Параметров из C# нет. Текст запроса внутри книги не извлекался.

| Пункт | Файл |
| --- | --- |
| Наличие | `templates\Availability.xlt` |
| Наличие по месяцам | `templates\AvailabilityMoths.xlt` |
| Грузовая работа | `templates\CargoWork.xlt` |
| Факт погрузки | `templates\LoadingFact.xlt` |
| План-факт погрузки | `templates\LoadingPlanFact.xlt` |

Рядом лежит `WagonStateReports.xlt` и `AgentsPivotReport.xsl`. Ссылок на эти имена из форм конструктора и готовых отчётов нет. `WagonStateReports` в НУВ — документ среза (`WagonStateReportsPkg`), не этот шаблон.

## 4. Что в этот каталог не входит

- Документы среза вагона и импорт FTP. Они пишут и читают `STATEREPORTS` / `STATEREPORTRECORDS`, но это не конструктор.
- `PlatformStateReportsJob` — методы не реализованы.
- `DynamicReportPackage.CurrentWagonParts` — в формах отчётов вызова нет.
- Задания `FillGivingReports2` и `KpiPkg.FillKpi` — запись витрин, не экран отчёта.
- Бюджет ремонта (`RepairBudgetView`) — обозреватель УВП, отдельный от этого конструктора.
