# УВП: карта Oracle, НУВ и НУД

Сессия словаря в этом проходе не открывалась. Имена процедур взяты из уже снятых spec (`docs/analysis/_oracle_tmp/source`) и из атрибутов копий `WagonLib` / `RemoteLib.WagonCall`. Тела пакетов по-прежнему скрыты: список таблиц у `RepairsPkg` в `ALL_DEPENDENCIES` не виден, ниже таблицы указаны по spec, триггерам, представлениям и аргументам. Запросы `Register`, `Save*`, `Import*`, `Delete*` не выполнялись.

Схема `DBTC`. Клиент УВП доходит до неё так:

`WagonApp` → `DBManager` (save/load/delete по атрибуту) или `IWagonCall` → «Сервис пользователя» `RemoteLib.WagonCall` → пакет.

НУВ — `WagonApp` 2.2.1.25 и `WagonPlanner`. НУД — `WarehouseApp`.

## 1. Виды и запись ремонта

Константы `RepairsPkg`: 0 ДР, 1 КР, 2 КРП, 3 ТОР, 4 исключение, 5 отстой, 6 упразднённый ТОР, 7 подготовка под погрузку, 8 ТО-3. Сообщения пробега: `MESSAGE_2612 = 2612`, `MESSAGE_2610 = 2610`.

| Процедура | Кто вызывает | Аргументы записи (сокращённо) | Экран |
| --- | --- | --- | --- |
| `SaveDepotRepair` | `DBManager` | владелец, начало, конец, стоимость, работы, счёт, предприятие, подача на РП, id, модернизации, отцепка, станция отцепки, станция назначения, подача на станцию, выход на станцию | ДР |
| `SaveCapitalRepair` | тот же набор, без отдельного `LoadCapitalRepair` | КР |
| `SaveExtendedCapitalRepair` | тот же набор + `lastservicedate` | КРП |
| `SaveCurrentRepair` | владелец, даты, стоимость, работы, счёт, станция отцепки, причина, id, номер поезда, место ремонта, станция назначения, подача/выход/прибытие | ТОР |
| `SaveTrashRepair` + `PostSaveTrashRepair` | как ТОР | отстой |
| `SaveAbolishedRepair` + `PostSaveAbolishedRepair` | как ТОР | УТР |
| `SavePreparationLoading` + `PostSavePreparationLoading` | как ТОР | ТР1 |
| `SaveMaintenanceThreeRepair` + `PostSaveMaintenanceThreeRepair` | как ТОР | ТО3 |
| `SaveExclusionRepair` + `PostSaveExclusionRepair` | владелец, начало, конец, стоимость, предприятие, прибытие, id, отцепка, станция отцепки, подача и выход | исключение |
| `PostSaveProcedure` | атрибут базового `Repair` | id | вызывается каркасом после save, если слой читает атрибут базы |
| `LoadRegularRepair` | id, курсор | ДР и КР |
| `LoadExtendedCapitalRepair` | id, курсор | КРП |
| `LoadCurrentRepair` | id, курсор | ТОР; УТР и ТР1 отдельных Load не имеют |
| `LoadTrashRepair` | id, курсор | отстой |
| `LoadExclusionRepair` | id, курсор | исключение |
| `LoadMaintenanceThreeRepair` | id, курсор | ТО3 |
| `DeleteRepair` | id | кнопка «Удалить ремонт» |
| `GetRepairList` | три перегрузки: вагон+вид, вагон, период | сетка истории |
| `Register` / `UnRegister` | id, через `IWagonCall` | контекстное меню |
| `ConvertToRepair` | id, новый вид, через `IWagonCall` | «Перевести ремонт» |
| `GetRepairType` | id → number | `WagonCall.LoadRepair` выбирает класс |

Чтение без карточки:

| Процедура | Назначение на экране |
| --- | --- |
| `GetWagonRepairs(wagon_id, begin, end, usePeriod)` | график ремонтов |
| `GetSetoutCount(wagonId)` / `GetRepairSetoutCount(repairId)` | отцепки после планового |
| `GetLastRepairId` / `GetLastClosedRepairId` / `GetLastPeriodRepairId` / `GetPreviousRepairId` / `GetNextPlanRepairDate` / `GetCurrentRepairId` / `GetLatestRepairId` | последние и текущий; клиент показывает готовые поля вагона, сами функции с кнопки не зовёт |
| `GetLastRepairs` / `GetLastTors` в `ReportsPkg` | отчёт «Анализ» |

Запись, которой на кнопках УВП нет и которую не запускать для проверки:

`UpdateAllLastRepairs`, `UpdateRepairState`, `UpdateAllRepairStates`, `UpdateAllDoneRepairs`, `ImportPrice`, `UpdateGivingDepartureRepair`, `ImportWagonParts`, `ImportWagonParts2731`, `ImportInstalledWagonParts`, `ImportMileage`, `ImportMileageWeb`, `ImportMileageWeb2612`, `LoadMileAgeWagons*`, `SendMileAgeWagon`.

`UpdateGivingDepartureRepair` связывает подачу/уборку (контур НУВ) с датами ремонта: тип подачи, код вагона, дата станции, дата предприятия, вид, код предприятия, станция назначения, номер поезда.

`SP_CONVERTREPAIRSTOTR1` — отдельная процедура, не пакет. Ищет `REPAIRS` по `wagon_id` и дню `endingdate`, ставит `type = 7`. В клиенте УВП не найдена.

## 2. Детали

`PartsPackage`. Модели: 0 количество, 1 колёсная пара, 2 номерная изготовленная, 3 крышка люка. Типы: 0 колёсная пара, 1 автосцепка, 2 надрессорная балка, 3 боковая рама.

| Процедура | Владелец | Роль |
| --- | --- | --- |
| `LoadDetailsBefore` | id ремонта | список «до» |
| `LoadDetailsAfter` / `DeleteDetailsAfter` / `SaveDetailsAfter` / `UpdateCurrentDetailsAfter` | id ремонта | список «после»; последняя обновляет текущие после установки |
| `LoadDetailsRemoved` / `DeleteDetailsRemoved` / `SaveDetailsRemoved` | id ремонта | снятые |
| `LoadRotatedDetails` | id ремонта | вкладка «Перевернутые детали» |
| `RotateRepairDetails` | id ремонта | job `ROTATEREPAIRDETAILS`, в каталоге выключен |
| `LoadCurrentWagonDetails` | id вагона | вкладка «Текущие детали» |
| `UpdateWagonCurrentDetails` | id вагона | spec: с последнего ремонта, иначе с предустановленных |
| `LoadInstalledDetails` / `DeleteInstalledDetails` / `SaveInstalledDetails` | id вагона | «Начальные детали» |
| `WritingOffInstalledDetails` / `ArrivalRemovedDetails` / `ArriveInstalledDetails` / `WriteOffRemovedDetails` | id | складское движение снятых и установленных. С кнопки УВП не вызываются |
| `AddBeforeToVirtualWarehouse` | id ремонта | виртуальный склад |
| `LoadPartHistory` / `GetHistoryState` | id детали | история и значок состояния |
| `SearchParts` | тип, номер, клеймо, год, чертёж | «Поиск деталей» |
| `MoveDetailsRemoved` / `MoveDetailsAfter` | id ремонта | перенос строк между списками на сервере. Кнопки УВП делают это в памяти и затем `Save*` |

Аргументы `SaveDetailsAfter` / `SaveDetailsRemoved`: `owner_id`, `detail_id`, количество, состояние, причина отклонения, клеймо завода, дата освидетельствования, толщина обода, вторая толщина.

Импорт 2731 пишет те же номерные позиции пачками `tb1`–`tb2`, `sb1`–`sb4`, `wp1`–`wp4` по коду вагона.

## 3. Предприятия, пробег, бюджет

| Объект | Процедуры | Экран |
| --- | --- | --- |
| `REPAIRPLANTS` | `Directories.SaveRepairPlant`, `LoadRepairPlant`, `DeleteRepairPlant` | предприятие ДР/КР/КРП/исключения. Поле `Depot` |
| `REPAIRPLANTSVIEW` | чтение справочника | место ремонта ТОР |
| `REPAIRTYPES` | 9 строк справочника | код 0…8 на строке ремонта |
| `repairtransitionstate` | справочник без правки (`DirectoryOperations.None`) | состояние перехода на пробег |
| `PARTTYPES` | `PartsPackage.SaveDetailType` / `LoadDetailType` / `DeleteDetailType` | тип детали |
| `MANUFACTORINGPLANTS` | справочник | завод-изготовитель, не ремонтное предприятие |
| `REPAIRBUDGET` | `RepairBudgetPkg.SaveRepairBudget` | план: имя, тип, период, документ, примечание |
| строки бюджета | `SaveRepairBudgetItem`, `LoadRepairBudgetItem`, `DeleteRepairBudgetItem`, `FillRepairBudget`, `LoadWagons`, `UpdateWagons` | вагон, плановый вид и дата, фактические вид и дата |
| свод | `GetRepairBudgetColumns`, `GetRepairBudgetData`, `GetRepairBudgetRecordPlan`, `GetRepairBudgetRecordFact` | сводная таблица |
| `RepairBudgetView` | прямой `select *` из обозревателя планов | список планов |
| `RepairBudgetPkg.RepairWagon` / `UpdateBudget` / `UpdateBudgets` | в кнопках УВП не найдены | связь проведённого ремонта с фактом бюджета, тело скрыто |

Даты следующих ремонтов — `RepairDates_Pkg`: `GetDRDate`, `GetCRDate`, `GetECRDate`, `GetLastServiceDate`, `UpdateNextRepairDates`, `UpdateLastServiceDate`, `UpdateAllRepairDates`, `FillPlanRepairs`, `FillPlanRepairsByTerms`, `FillPlanRepairsByMileage`, `FillAllPlanRepairs`. Сутки: job `UPDATEALLREPAIRDATES` → `JobPkg.UpdateAllRepairDates`.

Пробег вагона — таблица `WAGONMILEAGE` (объём в прежнем снимке около 33,6 млн). Поля карточки: норма, конец нормы, жёлтый, конец жёлтого, перепробег жёлтый, перепробег красный, текущий пробег, даты, код сообщения. Пишет импорт 2610/2612, не форма.

## 4. Таблицы и представления

Подтверждены зависимостями представлений, триггерами или атрибутом `[Table]`. Колонки `ALL_TAB_COLUMNS` в этом проходе не перечитывались. Контракт экрана — поля классов и аргументы `Save*`.

| Объект | Связь |
| --- | --- |
| `REPAIRS` | строка ремонта. Триггеры `TR_REPAIR_ID` (id) и `TR_REPAIRS_TIME` (`TimesPkg.WriteTime` на начало и конец). Объём снимка около 411 тыс. |
| `PLAN_REPAIRS_T` | плановые даты, триггер `TR_PLAN_REPAIRS_TIME` |
| `WAGON` | владелец. Поля последних ремонтов и `CurrentRepairState` приезжают с `LoadWagon` |
| `WAGONMILEAGE` | пробег |
| `WAGONPARTS` | номерная деталь. Триггер id `TR_WAGONPART_ID` |
| `CURRENTPARTS` | текущая установка на вагоне |
| `INSTALLEDPARTS` | начальные детали |
| `PARTSAFTER` / `PARTSREMOVED` | после ремонта и снятые. Имена таблиц следуют из процедур `SaveDetailsAfter` / `SaveDetailsRemoved` и снимка схемы; текст DDL здесь не открывался |
| `REPAIRPLANTS` / `REPAIRTYPES` / `PARTTYPES` | справочники |
| `REPAIRBUDGET` / `REPAIRBUDGETWAGONS` | план |
| `REPAIRDOCUMENTS` / `REPAIRDETAILS` | документ ремонта склада НУД, не строка `REPAIRS` |
| `WAGONEXCLUSION` | документ исключения вагона (`SP_WAGONEXCLUDE` пишет ещё `WAGON`, `REPAIRS`, `CUSTOMERS`) |
| `LOGS` | журнал, представление `LOGSREPAIRSVIEW` |

Представления, которые зависят от `REPAIRSPKG` и/или `REPAIRS`: `REPAIRSVIEW`, `LASTREPAIRVIEW`, `LASTREPAIRSVIEW`, `LASTPLANREPAIRSVIEW`, `CURRENTREPAIRVIEW`, `REGULARREPAIRSVIEW`, `PLAN_REPAIRS_VIEW`, `TM_REPAIRSVIEW`, `LOGSREPAIRSVIEW`, `REPAIRBUDGETVIEW`. `WAGONVIEW` формально смотрит таблицу `WAGON` и пакет `REPAIRSPKG`: колонки ремонта приходят из функции пакета. Текст представлений в этом проходе не выгружался.

`RepairReportsPkg` наполняет справки состояния парка и положения по предприятиям (`FillStateInventory`, `FillRepairStateCurrent` / `Depot` / `Capital`, `FillRepairPlants*`, `FillTransitionDates`). В меню разобранного `WagonApp` вызовов нет.

## 5. НУВ

НУВ и УВП — один клиент `WagonApp` двух сборок (2.2.1.25 и 2.2.1.26). Класс `RepairExplorer` есть в обеих. Отдельного ремонтного продукта НУВ нет.

Общее:

- те же `WagonLib` процедуры `RepairsPkg` / `PartsPackage` / `RepairBudgetPkg`;
- тот же Remoting `IWagonCall`: `LoadRepair`, `RegisterRepair`, `UnRegisterRepair`, `ConvertRepair`;
- паспорт вагона с пробегом и следующими датами;
- чтение среза `StateReportPackage.GetWagonStateReports` рядом с ремонтом, но это другая вкладка («Движение по срезу»).

Только контур НУВ, не карточка УВП:

- `WagonPlanner` (в папке УВП его нет) ведёт наряды, подачу и уборку;
- `RepairsPkg.UpdateGivingDepartureRepair` стыкует дату станции и дату предприятия подачи/уборки с ремонтом;
- сервис приложения принимает 2610, 2612 и 2731 (`WagonMessage2610SenderTask` / `ReceiverTask`, то же для 2612 и 2731) и вызывает `ImportMileage*` и `ImportWagonParts2731`.

Планировщик строку `REPAIRS` с формы наряда не редактирует. Бюджет ремонтов живёт в `WagonApp`, не в `WagonPlanner`.

## 6. НУД

НУД — `WarehouseApp` и `WarehousePkg`. Виды документов склада 0…6: закуп, приход, перемещение, списание, ремонт колёсных пар, наряд, ремонт.

Стыки с ремонтом вагона:

| Точка НУД | Точка УВП | Общий ключ |
| --- | --- | --- |
| `RegisterRepairDocument` / `UnregisterRepairDocument` | `RepairsPkg.Register` | разные документы. Общие только детали |
| `SaveRepairDocument(warehouse, delivery, warehousefrom)` + `SaveRepairDetails` | `SaveDetailsAfter` / `SaveDetailsRemoved` | `detail_id`, клеймо, дата, толщина |
| `RegisterWheelPairRepair` | модель 1 колёсной пары на карточке ремонта | id колёсной пары |
| `PartsPackage.GetHistoryState` / `LoadPartHistory` | та же история на форме детали УВП | id детали |
| `AddBeforeToVirtualWarehouse`, `VIRTUALWAREHOUSEDETAILS` | список «до» ремонта | id ремонта |
| сообщение 2731 `ImportWagonParts2731` | текущие и начальные детали вагона | код вагона и номер детали |

Проводить документ склада из карточки вагона нельзя: это изменит остатки `WAREHOUSEDETAILS`. Читать историю детали можно.

## 7. Что не доказано

- Текст `RepairsPkg.Register`: меняет ли он `CURRENTPARTS`, бюджет и `CurrentRepairState`.
- Запрещает ли `DeleteRepair` проведённую строку.
- Идемпотентность импорта 2612 и 2731.
- Полный список колонок `REPAIRS` из `ALL_TAB_COLUMNS`.
- Отличаются ли тела `Save*` УВП 2.2.1.26 от вызовов НУВ 2.2.1.25. Имена процедур в копиях совпадают.
