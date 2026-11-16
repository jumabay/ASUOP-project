# Сводка сверки пяти компонентов

Контроль качества исследования перед проектированием. Новых проектов, форм и вызовов Oracle в этом документе нет.

Источники: [01_ASUVP_ARCHITECTURE.md](01_ASUVP_ARCHITECTURE.md), [01_ASUVP_IMPORT_PIPELINE.md](01_ASUVP_IMPORT_PIPELINE.md), [02_ORACLE_TABLES.md](02_ORACLE_TABLES.md), [02_ORACLE_RELATIONSHIPS.md](02_ORACLE_RELATIONSHIPS.md), [03_BUSINESS_LOGIC.md](03_BUSINESS_LOGIC.md), [03_OBJECT_DEPENDENCIES.md](03_OBJECT_DEPENDENCIES.md), [04-nuv-reverse.md](04-nuv-reverse.md), [05_VVP_FUNCTIONS.md](05_VVP_FUNCTIONS.md), [05_VVP_ORACLE_MAP.md](05_VVP_ORACLE_MAP.md), [06_NUUD_FUNCTIONS.md](06_NUUD_FUNCTIONS.md), [06_NUUD_ORACLE_MAP.md](06_NUUD_ORACLE_MAP.md), [07_REPORT_SERVICE.md](07_REPORT_SERVICE.md), [07_REPORT_CATALOG.md](07_REPORT_CATALOG.md), [07_REPORT_ORACLE_DEPENDENCIES.md](07_REPORT_ORACLE_DEPENDENCIES.md).

Ранняя матрица [09-function-gap-matrix.md](09-function-gap-matrix.md) этим файлом не заменяется как архив этапа, но для проектирования она устарела: словарь `DBTC` после неё снимался, виды ремонта и документы склада разобраны по кнопкам. Где этап 9 расходится с этапами 2–7, ниже стоит этап 2–7.

Имена в задании: НУВ (`WagonApp` 2.2.1.25 и `WagonPlanner` 1.2.2.85), УВП (`WagonApp` 2.2.1.26), НУД (`WarehouseApp` 1.2.0.29). АСУОП (`cncClient`, MSSQL `terminal`) в пять компонентов не входит.

## Граница знания

Клиенты не запускались. Пакеты не выполнялись. `PACKAGE BODY` скрыт. Поэтому колонка «подтверждение» ниже не означает прогон формы.

| Статус | Что он значит |
| --- | --- |
| код | Цепочка видна в декомпиляции DLL/EXE или в spec |
| поведение C# | Ветвление видно в методе. Живой щелчок не делался |
| словарь | Объект есть в снимке `DBTC`. Текст тела пакета не читался |
| не подтверждено | Объяснения в материалах нет. Алгоритм здесь не достраивается |

## Контур, который сходится у всех пяти

```text
FTP disl_*  →  ApplicationService «Сервис приложение»  →  ImportPkg  →  DBTC
сообщения 2610/2612/2731  →  та же служба, копия «Сервис пользователя»  →  AsoupPkg / RepairsPkg
НУВ, УВП, НУД, конструктор отчётов  →  Remoting той же службы  →  пакеты чтения и записи
отдельного процесса «сервис отчётов» нет
```

Две копии одного `ApplicationService.exe` 1.0.3.37 различаются только `config.json`. Подменять их нельзя: у импортёра маска RU `disl_operations*.txt` и каталог `///tisservice//disl_vagons_tmp//`, у пользовательской копии импорт выключен, маска RU `disl_cis*.txt`, каталог `//Infoservice//`.

## Матрица

Пустая ячейка результата означает: клиент вызывает процедуру, а что она меняет в таблицах, тело не показывает.

### АСУВП — служба импорта и сообщений

| Функция | Где | Код / метод | Oracle | Вход | Результат, который уже виден | Подтверждение |
| --- | --- | --- | --- | --- | --- | --- |
| Приём текстового среза KZ | «Сервис приложение» | `ImportStateReportsFtp.Run` → `ReceiveFiles` → `ProcessFile` → `SaveLines` | `ImportPkg.IsImportedFile`, `SaveImportFile`, `SaveStateReport`, `SaveStateReportLineKz`, `UpdateStateReport` | файл маски `disl_kz*.txt`, тип фактически 0; строка — 20 или 21 поле через табуляцию | каждая непустая строка уходит в процедуру; пустой файл удаляется и в Oracle не пишется; таблицы по контракту `IMPORTFILES`, `STATEREPORTS`, `STATEREPORTRECORDS` | код и поведение C#. Правило «писать ли неподвижный вагон» — не подтверждено |
| Срез RU | тот же хост | `ImportStateReportsFtpRu` | `SaveStateReportLineRu` | `disl_operations*.txt`, тип 1, 11 полей | отдельный канал, не дельта KZ | код. Смысл полей файла — не подтверждено: образца нет |
| TIS RU и третьи стороны | тот же хост | `ImportStateReportsFtpTisRu`, `ImportStateReportsFtpTisThirdParties` | та же `SaveStateReportLineKz` | `disl_vagons_cis*.txt` тип 2; `disl_vagons_all_*.txt` тип 16 | наследники KZ | код |
| Контейнеры | тот же хост | `ImportStateReportsFtpTisContainers` | `SaveStateReportLineContainer` | `disl_containers_*.txt`, тип 3, первая строка пропускается | не срез вагонов | код |
| Срез из Excel | тот же хост | `ImportStateReportsXls` | `SaveFileContent`, `UpdateStateReportXml` | `*_disl_kz.xls`, тип 23, макрос типа 221, затем XML | единственный путь с клиентской транзакцией вокруг LOB | код. Макрос и образец xls — не подтверждались в этом своде построчно |
| Повтор файла | тот же класс | `IsImportedFile` до `ProcessFile` | `ImportPkg.IsImportedFile(filename, type)` | имя и тип | при ошибке проверки C# считает файл неимпортированным и не блокирует повтор | код. Момент, когда пакет ставит признак 1, — не подтверждено |
| Обрыв посередине файла | `ProcessFile` / `SaveLines` | ошибка строки логируется и цикл идёт дальше; `SaveImportFile` / `SaveStateReport` при ошибке возвращают 0 и не пробрасывают исключение | те же процедуры | частичный файл | транзакции на TXT нет; файл уже в `processing` следующим опросом входного каталога не читается | поведение C#. Судьба признака импорта — не подтверждено |
| Опрос FTP | `InitTimerSettings` | период = `FtpStateReportsReceiveInterval` секунд | — | эталон импортёра 45, копия пользователя 60 | час задаёт файл, не таймер | поведение C# |
| Список на слежение | `GivingDeparture` | `PublishRegistryStateReports`, `PublishRegistryStateReportsThird` | `WagonStateReportsPkg.GetPublishWagonsThird` у третьего списка | локальный `wagons.txt`; при флаге заливка на `//tisservice//IN//` | на диске копии пользователя 18334 строки; у импортёра файл пустой. Это не почасовой срез | код. R4: кнопка «Закачать на сервер» на форме реестра среза (`PlannerLib`); не `GetActiveWagonList`. См. [11-nuff-active-wagon-list.md](11-nuff-active-wagon-list.md) |
| Заглушка платформ | `PlatformStateReportsJob` | `Run` не реализован | — | — | к срезу вагонов не относится | код |
| Сообщения 2610 / 2612 / 2731 | «Сервис пользователя», задачи включены; у импортёра выключены, кроме приёмника 2612 | `WagonMessage2610*`, `WagonMessage2612*`, приём 2731 | `AsoupPkg.LoadWagonsMessages2610/2612`, `SetSendedWagon`; пробег `RepairsPkg.ImportMileage` / `ImportMileageWeb` / `ImportMileageWeb2612`; детали `ImportWagonParts2731` | сообщение АСОУП, не файл `disl_*` | запись пробега и номерных деталей. Это не кнопка УВП и не кнопка НУД | вызов из службы — код. Идемпотентность повтора — не подтверждено. `cncClient` к этому пакету не привязан |

Учётки строк импорта: `asuvp_thread_01` … `asuvp_thread_04`, третьи стороны `_21`, Excel `_32`. Порт контракта подачи `IGivingDepartureCall` — 5502. Полный перечень портов в разборе службы не снимался.

### НУВ

Построчно разобраны чтение среза и общие библиотеки. Кнопки `WagonPlanner` (наряды, подача, уборка, маршруты, заявки, бюджеты планировщика, транспортные заявки) подтверждены списком классов и именами пакетов, не каждой кнопкой.

| Функция | Где | Код / метод | Oracle | Вход | Результат, который уже виден | Подтверждение |
| --- | --- | --- | --- | --- | --- | --- |
| История среза вагона за сутки | `WagonApp` 2.2.1.25 | `GivingDeparture.GetWagonStateReports` → `StateReports.LoadWagonStateReports` | `StateReportPackage.GetWagonStateReports` | вагон, интервал 00:00:00–23:59:59 | курсор `result`. Что вернёт пакет на неполном часе — не подтверждено | код. Пакет в словаре зависит от `STATEREPORTRECORDS` |
| Список и карточка вагона | тот же клиент и общий `WagonLib` | обозреватель: `GetWagonList` через `WagonCodeCollection`; карточка: `GetWagonByCode` / `LoadWagon`; `GetActiveWagonList` — Remoting без найденного UI call-site в НУВ; `GetContainerList` | `WagonPackage` | для Active на клиенте только session/connection; фильтр UI у списка обозревателя не найден | чтение `WAGON` и справочников. `SaveWagon` — запись карточки, не отчёт | R4 IL: [11-nuff-active-wagon-list.md](11-nuff-active-wagon-list.md). Критерий Active в body — не подтверждено |
| Пробег за месяц | библиотека | `GetMonthDistances` | `WagonPackage.GetMonthDistances` | вагон | чтение. Пишет job `UPDATEWAGONMONTHDISTANCESDAILY` → `ResourcePkg.WriteWagonsDistanceDayli` | вызов чтения — код. Формула job — не подтверждено |
| Подход | библиотека | `ApproachPkg.GetApproachWagons` | `ApproachPkg` | — | чтение подхода | имя вызова. Экран и полный контракт — не подтверждено |
| Наряд и состояние среза наряда | библиотека | `WorkListsPkg.UpdateStateReport` | `WorkListsPkg`; пакет словарём связан с `WORKLISTS` | — | по имени это запись, не отчёт | вызов подтверждён. Что меняется — не подтверждено |
| Удаление среза | библиотека | `StateReportPackage.DeleteReport` | тот же пакет | — | запись | вызов подтверждён. Условия удаления — не подтверждено |
| Документный срез третьих сторон | библиотека | `WagonStateReportsPkg.GetPublishWagonsThird` | `WagonStateReportsPkg` | — | чтение списка публикации | код |
| Подача и уборка | `WagonPlanner` 1.2.2.85, только в папке НУВ | классы `GivingDepartures` | `GivingPkg`, `GivingDeparturePackage` | файлы подачи пишет ещё и импортёр (`SaveGivingReportLine*`) | пакет словарём зависит от `STATEREPORTRECORDS`. Планировщик строку `REPAIRS` с формы наряда не редактирует | классы и пакет — код. Кнопки формы — не подтверждено |
| Планы объёмов и отчёты планировщика | `WagonPlanner` | меню «Отчёт» и конструктор, право `PlannerReportConstructor` (36) | см. матрицу отчётов | сессия открыта; отдельного флага на готовые отчёты нет | — | код форм отчётов. Остальные экраны планировщика — не подтверждено |
| Ремонт внутри НУВ | тот же `WagonApp`, класс `RepairExplorer` | те же процедуры, что у УВП | `RepairsPkg`, `PartsPackage`, `RepairBudgetPkg` | — | отдельного ремонтного продукта НУВ нет. Бюджет ремонтов живёт в `WagonApp`, не в `WagonPlanner` | код сборок. Отличия 2.2.1.25 от УВП 2.2.1.26 — не подтверждено |
| Стык подачи с датами ремонта | spec, не кнопка планировщика | в разобранных кнопках наряда вызова нет | `RepairsPkg.UpdateGivingDepartureRepair` | тип подачи, код вагона, даты станции и предприятия, вид, предприятие, станция, номер поезда | запись | член spec. Кто вызывает из живого контура — не подтверждено |

НУВ сам FTP-срез не забирает. Excel-макрос в поставке умеет превратить лист дислокации в XML по всем строкам листа; это не замена `ImportStateReportsFtp`.

### УВП

Клиент Oracle не открывает. Запись объекта — `DBManager.SaveObject` по атрибуту `[SaveProc]`. Проводка, снятие, смена вида и загрузка ремонта по id — `IWagonCall`, порт 5507.

Виды `RepairsPkg`: 0 ДР, 1 КР, 2 КРП, 3 ТОР, 4 исключение, 5 отстой, 6 УТР, 7 ТР1, 8 ТО3.

| Функция | Где | Код / метод | Oracle | Вход | Результат, который уже виден | Подтверждение |
| --- | --- | --- | --- | --- | --- | --- |
| Обозреватель вагона | `MainForm` | `bbiWagonExplorer_ItemClick` | — | право `ReadWagonAndRepair` (3) | MDI `WagonExplorerForm` | код |
| Список ремонтов вагона | вкладка «Ремонты» | `tabControl_SelectedIndexChanged` | `RepairsPkg.GetRepairList(WagonId, RepairType)` | выбранный вагон | все виды, курсор на последней строке. Колонки перечислены в разборе УВП | код. Серверный состав курсора без тела — не подтверждено |
| Открыть карточку | сетка | `ShowSelectedRepair` | `LoadRegularRepair` (ДР и КР), `LoadExtendedCapitalRepair`, `LoadCurrentRepair` (ТОР, УТР, ТР1), `LoadTrashRepair`, `LoadExclusionRepair`, `LoadMaintenanceThreeRepair` | id строки | карточка и детали. Отдельного `LoadCapitalRepair` нет | код |
| Добавить ДР / КР / КРП / ТОР / ОТСТ / ИСКЛ / ТР1 / ТО3 | «Добавить ремонт» | `AddRepair` → `RepairDialog.btnSave_Click` → `ApplyContent` → `SaveObject` | `SaveDepotRepair`, `SaveCapitalRepair`, `SaveExtendedCapitalRepair`, `SaveCurrentRepair`, `SaveTrashRepair`+`PostSaveTrashRepair`, `SaveExclusionRepair`+`PostSaveExclusionRepair`, `SavePreparationLoading`+`PostSavePreparationLoading`, `SaveMaintenanceThreeRepair`+`PostSaveMaintenanceThreeRepair` | `OwnerId` вагона; «до ремонта» = снимок `PartsPackage.LoadCurrentWagonDetails` | список перечитывается. УТР пунктом «Добавить» не создаётся | код и проверки дат на клиенте. Что пишет пакет и триггер `TR_REPAIRS_TIME` (`TimesPkg.WriteTime`) — не подтверждено |
| Провести / снять проводку | контекст списка | `msRegister_Click` / `msUnRegister_Click` | `RepairsPkg.Register` / `UnRegister` | право `RegisterRepair` (23), id | сохранение в этот момент не вызывается. Проведённая карточка на форме только для чтения | код. Тело `Register` — не подтверждено |
| Удалить ремонт | список | `DeleteRepair` | `RepairsPkg.DeleteRepair` | право `DeleteRepair` (7), вопрос. `RegisterType` клиент не смотрит | строка уходит после повторного `GetRepairList`, если сервер не отказал | код. Запрет проведённого удаления — не подтверждено |
| Перевод вида | контекст | `ConvertToRepair(id, новый код)` и для УТР/ТР1 повторный `Save*` | `RepairsPkg.ConvertToRepair` | право `ConvertRepair` (40). Пары: ТОР↔УТР (6↔3), ДР↔КР (0↔1), ТОР↔ТР1 (3↔7), ТОР↔ТО3 (3↔8) | id строки сохраняется | код клиента. Отсекает ли пакет лишние пары — не подтверждено |
| Детали ремонта | вкладки «Детали» и «Перевернутые детали» | кнопки переноса в памяти, затем `Save*` | `Load/Save/DeleteDetailsBefore|After|Removed`, `UpdateCurrentDetailsAfter`, `LoadRotatedDetails` | id ремонта. Одна деталь не может быть и в «после», и в «снятых» | `PartsPackage.RotateRepairDetails` с кнопки не вызывается (job выключен) | код |
| Начальные и текущие детали | вкладки обозревателя | `LoadInstalledDetails` / `LoadCurrentWagonDetails` | `PartsPackage` | id вагона | «Текущие детали» заполняются при выборе вагона | код |
| Планы ремонтов | `RapairBudgetExplorer` | `LoadData`, `AddBudget`, `EditBudget`, `DeleteBudget`, `LoadRepairWagons` | `RepairBudgetView`, `RepairBudgetPkg.SaveRepairBudget`, `SaveRepairBudgetItem`, `FillRepairBudget`, `GetRepairBudgetColumns`, `GetRepairBudgetData` | права 41–44 | Excel бюджета Oracle не меняет. `RepairWagon` / `UpdateBudget` с этой формы не вызываются | код. Кто вызывает `UpdateBudget` — не подтверждено |
| Пробег и следующие даты | паспорт | показ полей; `MileAge*` помечены `[LoadOnlyField]` | даты считает `RepairDates_Pkg` и job `UPDATEALLREPAIRDATES` → `JobPkg.UpdateAllRepairDates`. Пробег с линии пишет служба, не эта форма | флажок срок/пробег и нормы уходят в `SaveWagon` | клиент `GetDRDate` / `ImportMileage*` не вызывает | код |
| График ремонтов | меню вагона | `bbiShowGraphRepairs_ItemClick` | `RepairsPkg.GetWagonRepairs` | id вагона | график и таблица. Текст ошибки на контроле говорит «пробегов» | код. Несовпадение подписи и процедуры — см. пробелы |
| Анализ | `LastRepairsFrm` | `ReportsBinder.GetAnalyzeReport` | `ReportsPkg.GetLastRepairs`, `ReportsPkg.GetLastTORs` | тип вагона, вид, предприятие, период; ветка ТОР — станция и «причина / деталь» | только чтение. Расхождение веток между разбором УВП и разбором отчётов — не подтверждено, см. пробелы | код вызова есть в обоих разборах, состав веток расходится |
| Поиск деталей | `MainForm` | `bbiSearchDetails` | `PartsPackage.SearchParts` | тип, номер, клеймо, год, чертёж | тот же класс, что в НУД | код |
| Чего экран не делает | — | — | `Register` склада, `RotateRepairDetails`, `UpdateAllRepairDates`, `ImportMileage*`, `ImportWagonParts*`, `LOGSREPAIRSVIEW`, `SP_CONVERTREPAIRSTOTR1` | — | эти объекты в кнопках УВП не найдены | код |

Порядок дат на карточке проверяется только при заполненной дате отцепки и только между соседними заполненными датами цепочки из шести. Это правило клиента, не пакет.

### НУД

Клиент ходит в `IDbManagerCall`. Карточку ремонта вагона читает через `IWagonCall`. Виды документов 0…6.

| Функция | Где | Код / метод | Oracle | Вход | Результат, который уже виден | Подтверждение |
| --- | --- | --- | --- | --- | --- | --- |
| Реестр | `DocumentExplorerFrm` | `LoadData` | `WarehousePkg.GetDocumentList(begin, end)` | период, по умолчанию 31 день; право `ViewWarehouseDocuments` (24) | колонки `ID`, `REGISTER_TYPE`, `DOCTYPENAME`, склад, поставщик, дата, тип детали, количество, примечание | код. Представление под курсором spec не называет |
| Создать семь видов | меню «Создать» | `OpenNewDocument` | шапка `Save*Document`, строки `Delete*Details` затем `Save*Details` затем `Update*` | право `CreateWarehouseDocument` (26) | 0 покупка, 1 приход, 2 перемещение, 3 списание, 4 ремонт колёсной пары, 5 оборотная ведомость, 6 документ ремонта склада | код. Эффект `Register*` — не подтверждено |
| Открыть / копировать / удалить | реестр | `OpenSelectedDocument`, `tsbCopy_Click`, `DeleteDocument` | `Load*` / `Delete*` | открытие — право 25, удаление — 27. Клиент не смотрит, проведён ли документ | копия обнуляет `Id` и оставляет `Register_Type` исходного | код. Последствие копии проведённого — не подтверждено |
| Провести / снять | реестр и карточка | `RegisterDocument` / `UnregisterDocument` | `Register*` / `Unregister*` по виду, аргумент только id | право 28. Карточка перед вызовом не сохраняется. Повторная проводка проведённого на форме закрыта | список перечитывается | код. Какие строки `WAREHOUSEDETAILS` меняются — не подтверждено |
| Номерная деталь | карточка детали | `CountingDetailItemView.SaveItem` | `SaveDetail` / `SaveWheelPair` / `SaveManufactoringDetail` / `SaveHatchCover` | модель 0 количество, 1 колёсная пара, 2 изготовленная, 3 крышка люка | таблица экземпляра `WAGONPARTS` | код. Уникальность номера — не подтверждено |
| Покупка | `ProcurementDocumentFrm` | поля шапки и строки `BuyingDetail` | `SaveProcurementDetails` | склад, дата, контрагент; у пары номер ≤ 15, клеймо ≤ 5. Толщину обода этот наследник в объект не переносит | — | код |
| Приход | `ArrivalDocumentFrm` | набор по `WAREHOUSEITEMSVIEW` | `SaveArrivalDetails` | деталь или металл (`ObjectType = 1` → справочник `Metal`, не `WAGONPARTS`) | — | код |
| Перемещение | `TransferDocumentFrm` | подбор остатка отправителя | `LoadWarehouseDetails`, `SaveTransferDetails` | два склада. Смена отправителя при непустых строках спрашивает очистку | — | код |
| Списание | `WritingOffDocumentFrm` | шапка и `WritingOffWarehouseDetailsFrm` | `SaveWritingOffDetails` | типы пакета 1…5. Клиент связывает объект шапки только с продажей (партнёр). Ремонт и вагон задаются строкой через `IWagonCall.LoadRepair` | единственная ссылка документа склада на id `REPAIRS` | код. Пустой `AssociatedObject_Id` для типов 2–5 — поведение клиента; как его читает пакет — не подтверждено |
| Ремонт колёсной пары | `WheelPairRepairDocumentFrm` | подбор пары с остатка | `RegisterWheelPairRepair`, `SaveWheelPairDetails` | типы пакета 1 смена оси, 2 смена колеса. Это не ремонт вагона | подписи строк справочника клиент не задаёт | код |
| Документ ремонта склада | `RepairDocumentFrm` | строки с остатка склада отгрузки | `SaveRepairDocument`, `SaveRepairDetails`, `RegisterRepairDocument` | склад, дата, склад отгрузки | `RepairsPkg.Register` не вызывается, поля id ремонта вагона на шапке нет | код |
| Оборотная ведомость | `DetailsWorkingSheetFrm` | «Добавить детали» | `PartsPackage.LoadVirtualDetails`, `SaveDetailsWorkingSheet`, `SaveDetailsWorkingSheetPart` | склад, даты. Очистку строк перед повторным сохранением клиент не вызывает | `AddBeforeToVirtualWarehouse` из НУД не вызывается | код. Кто наполняет виртуальный склад — не подтверждено |
| Остатки и история | `WarehouseExplorerFrm` | фильтр склада и типа | `LoadWarehouseDetails`, `GetHistoryState`, `LoadPartHistory` | id детали | значок: клиент берёт число как индекс картинки 0/1/2. Пустой курсор истории помечается `New` (0) | код. Совпадает ли код функции с 0/1/2 — не подтверждено |
| Конструктор склада | меню | `ShowReportConstructor` | корень `FROM WAREHOUSESVIEW`, приложение 4 | право 30 | см. отчёты | код |
| Справочники меню | `Program.InitDirectories` | склады, предприятия, типы склада, контрагенты, причины, металл, типы и состояния деталей | таблицы и `Save*` из разбора НУД | ячейки и места внутри склада в формах нет | `WritingOffType` и `WheelPairRepairType` в меню не регистрируются, они на документах | код |
| Чего в клиенте нет | — | — | `SP_ARRIVALWAGONPARTS`, `SP_ARRIVEWHEELPAIR`, `SP_TRANSFERWAGONPART`, возврат как вид документа, `RepairsPkg.Register` | — | автономные процедуры пишут те же семейства таблиц напрямую. Вызова из НУД нет | код клиента и текст процедур. Расписание вызова SP — не подтверждено |

### Сервис отчётов

Отдельного хоста нет. SQL конструктора исполняет `RemoteLib.DbManagerCall.GetData` (`CommandType.Text`, порт `IDbManagerCall` 5505) на соединении сессии. Готовый отчёт идёт в `ExecuteNonQueryCursor` (`CommandType.StoredProcedure`).

| Функция | Где | Код / метод | Oracle | Вход | Результат, который уже виден | Подтверждение |
| --- | --- | --- | --- | --- | --- | --- |
| Конструктор | УВП, НУВ, НУД | `ReportConstructorForm`, `QueryBuilder`, `Report.GetResultTable` | `ReportingPackage.GetGroups/GetTables/GetFields(applicationId)` затем произвольный `SELECT` | корни: `Wagon` (1), `GIVINGLISTSVIEW` (2), `WAREHOUSESVIEW` (4), `STATEREPORTRECORDS` (5), `WAGONDOWNTIMEVIEW` (6), `GIVING_ROUTE_REPORT_VIEW` (9), `STATEREPORTSWAGON` (11) | файл `.rep` / `.brp` локально, не строка Oracle. `ORDER BY` у запроса данных нет. Таймаут службы 60 с, лимита строк нет | код сборки SQL. Курсор полей не выполнялся, перечень колонок — не подтверждено |
| Запуск с панели | УВП и НУВ | реестр Windows `staticInfoPanel` | тот же `GetData` | право `ViewDynamicReports` (5) | сохранённых `.rep` / `.brp` в поставке нет | код |
| План погрузки | НУВ | `CalendarLoadingReport` | `RequestPackage.GetLoadingCalendarReport`, `GetLoadingEvenReport` | дата, тип груза, станция, отправитель | строка «ИТОГО» суммируется в клиенте | код. Тело пакета — не подтверждено |
| Планируемые объёмы | НУВ | `TotalPlanReport` | `ReportRoutes.GetReportPlan` | дата, оператор | шаблон `VolumePlans.xlt` или пустая книга | код |
| Выполнение плана | НУВ | `ExecutionPlanReport` | `ReportExecution.GetReportExecution` | дата, оператор | отношения «к расчёту / к плану» клиент не пересчитывает, берёт из курсора | код |
| Реализация | НУВ | `DailyRealizationReport` | `ReportsPkg.GetDailyRealizationReport` | день, оператор | шаблон `RealizationReport.xlt`. В снятом spec `ReportsPkg` этой процедуры нет, имя передаётся строкой | код клиента. Член spec — не подтверждено |
| Движение | НУВ | `MovementWagonsReport` | `ReportsPkg.GetMovementWagons` | период | код ищет `MovementWagonsReport.xlt`, в папке НУВ лежит `.xsl` | код и файл. Запрос внутри книги — не подтверждено |
| Инвентарный парк | клиент отчётов | `InventoryStateReport` | шесть `RepairReportsPkg.Fill*` без параметров | дата только в заголовке Excel | шаблон `InventoryStateReport.xltx`. `FillRepairPlantsZiksto`, `FillRepairPlantsBogatyrTrans`, `FillTransitionDates` не вызываются | код вызовов в разборе отчётов. Совпадение с меню УВП — не подтверждено, см. пробелы |
| Пять книг Excel | меню НУВ | `Process.Start` | запрос в C# отсутствует | файлы `Availability.xlt`, `AvailabilityMoths.xlt`, `CargoWork.xlt`, `LoadingFact.xlt`, `LoadingPlanFact.xlt` | связь с job `AVAILABILITY_VIEW_JOB` в C# не прописана | код открытия. SQL книг — не подтверждено |
| Анализ | УВП | см. строку УВП | `GetLastRepairs`, затем в разборе отчётов `GetLastTORs` на каждую строку | — | N+1 чтений описан в сервисе отчётов | расхождение с ветками УВП — не подтверждено |
| Витрины, которые кормят отчёты | jobs, не кнопка | — | `FillGivingReports2` 01:00, `KpiPkg.FillKpi` 02:00, `ResourcePkg.UpdateAvailabilityView` 12:00, `JobPkg.UpdateAllRepairDates` 01:00, импорт в `STATEREPORTRECORDS` | — | конструктор эти процедуры не вызывает | словарь jobs. Тела — не подтверждено |
| Асинхронный контур | служба | `ExecuteReportData`, `CancelReportData`, `CheckReportData`, `ReportThreadsList.Add` | — | — | `NotImplementedException`; в список потоков никто не добавляет | код |

Права конструктора закрывают пункт меню: УВП `CreateDynamicReports` (4), НУВ `PlannerReportConstructor` (36), НУД `WarehouseReportConstructor` (30). `GetData` флаг операции не читает.

## Общие модули

| Модуль | Кто использует | Что общее |
| --- | --- | --- |
| `ApplicationService.exe` + `RemoteLib.dll` | обе копии службы | Remoting, `DbManagerCall`, импорт, сообщения |
| `WagonLib.dll` | НУВ, УВП, отчёты | вагон, ремонт, конструктор, `QueryBuilder`, `SearchParts` |
| `ReportingLib.dll` | обе службы, НУВ, УВП, НУД | готовые отчёты и COM Excel |
| `WarehouseLib.dll` | НУД | документы склада и корень конструктора |
| `PlannerLib.dll` | НУВ | корни конструктора планировщика и четыре готовых отчёта |
| `DirectoriesLib` | все клиенты | один enum `Operations`, коды прав сквозные |
| `PartsPackage` | УВП, НУД, поиск в обоих | `WAGONPARTS`, история, поиск |
| `REPAIRPLANTS` | УВП и склад НУД | предприятие ремонта и привязка склада |
| `WAGON` | все, кроме чистого склада как писателя ремонта | центр FK |
| `STATIONS`, `CUSTOMERS` | все контуры | справочники наибольшей связности |
| `STATEREPORTRECORDS` | импорт пишет, НУВ и УВП читают, конструктор «Срезы» читает, `GivingDeparturePackage` зависит словарём | одна лента, без флага «полный / дельта» |

Один `WagonApp` двух версий. Класс `RepairExplorer` есть и в НУВ, и в УВП. Границу продуктов нельзя проводить по имени папки.

## Дублирование, которое уже видно без тел пакетов

| Две дороги | Чем отличаются | Чего нельзя утверждать |
| --- | --- | --- |
| `SaveStateReportLineKz` для KZ, TIS RU и третьих сторон | разные маски и тип файла, одна процедура строки | что тип на стороне Oracle не меняет разбор |
| `ImportPkg.UpdateStateReport`, `GivingPkg.UpdateStateReport`, `WorkListsPkg.UpdateStateReport`, job `WAGONUPDATEDISL` | четыре имени обновления состояния | что они пишут одни и те же колонки `WAGON` |
| Кнопки УВП `ConvertToRepair(..., 7)` и `SP_CONVERTREPAIRSTOTR1` | экран меняет одну строку по id; процедура ищет по коду вагона и дню окончания | что результат совпадает |
| Документы НУД и `SP_ARRIVALWAGONPARTS` / `SP_ARRIVEWHEELPAIR` / `SP_TRANSFERWAGONPART` | клиент зовёт пакет; SP делает `INSERT` в те же семейства таблиц | что SP — рабочий API приложений |
| Проводка `RepairsPkg.Register` и `WarehousePkg.RegisterRepairDocument` | разные документы. Общие только детали | что одна заменяет другую |
| Исключение с вкладки ремонта и `SP_WAGONEXCLUDE` | экран исключения переносит в объект не все нарисованные поля; продажа вагона пишется с «Дополнительных данных» через `SaveWagon`. SP пишет `WAGON`, `WAGONEXCLUSION`, `REPAIRS`, `CUSTOMERS` | что это один сценарий |
| Пробег: поля паспорта, `ImportMileage*` службы, `SP_GETMILEAGE` | паспорт только показывает факт; служба пишет; SP читает `WAGONMILEAGE` | формулу |
| Три выгрузки Excel | сетка `ExportToXlsx`, COM `ExcelExporter`, `Process.Start` на `.xlt` | что книги `.xlt` считают то же, что процедуры |
| Конструктор `SELECT` и готовый отчёт-процедура | разный канал исполнения | что набор строк совпадёт |

## Порядок переноса

Порядок задают писатели и общие ключи, не удобство экрана. Волна записи из [10-target-architecture-migration.md](10-target-architecture-migration.md) закрыта, пока не прочитаны `ImportPkg` и объект `WAGONUPDATEDISL`.

| Шаг | Что переносится | Почему раньше следующего | Что остаётся в старом контуре |
| --- | --- | --- | --- |
| 0 | Ничего не писать. Наблюдать файл и уже записанный срез | второй писатель в `ImportPkg` может задвоить ленту или создать `CreateJob*` | «Сервис приложение» — единственный писатель `disl_*` |
| 1 | Справочники `STATIONS`, `CUSTOMERS`, `WAGONTYPES`, `REPAIRPLANTS`, `PARTTYPES` | на них входящие FK почти всех документов | старые клиенты продолжают их править |
| 2 | Чтение вагона и среза: `GetWagonList` / `GetWagonByCode` / `GetWagonStateReports` | карточка и отчёты сидят на `WAGON` и `STATEREPORTRECORDS` | тот же пакет, старый НУВ не выключается |
| 3 | Чтение ремонта и деталей: `GetRepairList`, `Load*`, `GetHistoryState`, `LoadPartHistory`, `LoadWarehouseDetails` | запись ремонта и склада делит `WAGONPARTS` | `Register` обоих контуров не вызывать |
| 4 | Готовые отчёты по одному, после ночных jobs (`FILLGIVINGREPORTS`, `FILLKPI`, `UPDATEALLREPAIRDATES`, `AVAILABILITY_VIEW_JOB`) | отчёт читает витрину, которую пишет job | конструктор с произвольным SQL не переносить как есть |
| 5 | Подача, наряды, подход | `GivingDeparturePackage` зависит от `STATEREPORTRECORDS`; кнопки планировщика не разобраны | `UpdateStateReport` нарядов и `SaveGiving*` импортёра |
| 6 | Запись ремонта, затем запись склада | триггер `TR_REPAIRS_TIME` на каждой правке `REPAIRS`; складской документ вида 6 не есть строка ремонта | оба `Register*` |
| 7 | Замена импортёра | только после сверки числа строк и ключей на одном файле со старым `ImportPkg` | до этого новый процесс файл только читает |

АСОУП и MSSQL `terminal` в эти шаги не входят. Сообщения 2610/2612/2731 остаются на старой службе, пока не прочитаны `ImportMileage*` и `ImportWagonParts2731`.

Jobs, которые нельзя запускать ради проверки: `ROTATEREPAIRDETAILS`, выключенный `FILLSTATEREPROTSWAGONSTATIONS`, разовые `CreateJob*` / `UPDATEPS_*`.
