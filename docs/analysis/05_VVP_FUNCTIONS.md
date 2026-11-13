# УВП: окна, кнопки и цепочки вызовов

УВП в этой поставке — клиент `WagonApp.exe` **2.2.1.26**, подпись окна «Учет вагонного парка (УВП)». Отдельного EXE ремонта нет. Исходников `.sln` нет.

Разбор сделан декомпиляцией **копий** `ilspycmd` 9.1 (`docs/analysis/_vvp_tmp`). Оригиналы в «Старая программа» не менялись. Клиент не запускался. Пакеты Oracle не выполнялись. Тела `PACKAGE BODY` скрыты, поэтому серверные проверки внутри `Register` / `Save*` ниже названы только там, где их видно в spec или в клиенте.

НУВ и НУД ходят в ту же схему `DBTC`.

Клиент не открывает Oracle сам. Сохранение объекта идёт в `DBManager.SaveObject` / `LoadObject` / `DeleteObject` по атрибутам `[SaveProc]` / `[LoadProc]` / `[DeleteProc]`. Проводка, снятие проводки, смена вида и загрузка ремонта по id идут через Remoting `IWagonCall` (порт контракта 5507, URI `IWagonCall`) в `RemoteLib.WagonCall` на «Сервис пользователя».

Право включается флагом `DirectoriesLib.Items.Users.Operations`:

| Код | Имя | Что включает |
| --- | --- | --- |
| 2 | `WriteRepair` | «Добавить ремонт», режим редактирования ремонта |
| 3 | `ReadWagonAndRepair` | пункт «Обозреватель» |
| 7 | `DeleteRepair` | «Удалить ремонт» |
| 23 | `RegisterRepair` | «Провести ремонт» и «Отменить проводку» |
| 40 | `ConvertRepair` | подменю «Перевести ремонт» |
| 41–44 | `RepairBudgetExplorer` / `Add` / `Edit` / `Delete` | планы ремонтов |

## 1. Как открыть ремонт

Главное окно `MainForm`.

| Окно | Кнопка | Метод | Правило | Oracle | Результат |
| --- | --- | --- | --- | --- | --- |
| `MainForm` | Вагон → Обозреватель | `bbiWagonExplorer_ItemClick` | нужно `ReadWagonAndRepair` | — | MDI `WagonExplorerForm` |
| `WagonExplorerForm` | вкладка «Ремонты» | `tabControl_SelectedIndexChanged`, индекс 1 | вагон уже выбран в списке кодов | `RepairsPkg.GetRepairList(WagonId, RepairType)` | сетка всех ремонтов вагона, курсор на последней строке |
| `MainForm` | Планы ремонтов | `bbiRepairBudgetExplorer_ItemClick` | `RepairBudgetExplorer` | `select * from RepairBudgetView` | `RapairBudgetExplorer` |
| `MainForm` | Поиск деталей | `bbiSearchDetails` | — | `PartsPackage.SearchParts` | `SearchPartsFrm` |
| `MainForm` | статистические отчёты → Анализ | `bbiStaticalReportsAnalyze_ItemClick` | диалог `LastRepairsFrm`, кнопка ОК только закрывает диалог | `ReportsPkg.GetLastRepairs` или `GetLastTORs` | `DataGridForm` |
| `WagonExplorerForm` | Вагон → Графики ремонтов | `bbiShowGraphRepairs_ItemClick` | панель видима, есть id вагона | `RepairsPkg.GetWagonRepairs` | график и таблица `GraphRepairsControl` |

Вкладки обозревателя по порядку: «Тех.паспорт», «Ремонты», «Начальные детали», «Текущие детали», «Дополнительные данные». На «Дополнительные данные» сидит `ExclusionInfoPanel` (исключение и продажа вагона, не строка ремонта). «Текущие детали» заполняются при выборе вагона (`wppParts.Wagon`), отдельной ветки в обработчике вкладки нет.

## 2. Список ремонтов

Окно: `RepairsExplorerPanel` на вкладке «Ремонты». Сетка `dataGridView`, ключ раскладки колонок `repairs_list`.

Колонки: №, признак проводки (иконка), тип ремонта, дата отцепки, дата подачи на станцию, дата подачи на РП, дата начала ремонта / дата ВУ-10М, дата окончания / дата исключения, дата выхода на станцию / дата продажи, станция отцепки, ремонтное предприятие, стоимость, причина отцепки. `RepairTypeId` и `RepairId` есть в сетке.

Иконка проводки: `registertype` 0 — `Unregister.png`, 1 — `Register.png`.

Выбор строки вызывает `ShowSelectedRepair`: создаётся объект нужного класса, `DBManager.LoadObject(..., deepLoad: true)`, панель карточки переключается по виду (см. ниже). Если карточка грязная, спрашивается сохранение.

| Окно | Кнопка | Метод | Правило | Oracle | Результат |
| --- | --- | --- | --- | --- | --- |
| список | смена строки | `dataGridView_RowEnter` → `ShowSelectedRepair` | несохранённые правки: Да / Нет / Отмена | `LoadRegularRepair` / `LoadExtendedCapitalRepair` / `LoadCurrentRepair` / `LoadTrashRepair` / `LoadExclusionRepair` / `LoadMaintenanceThreeRepair` | карточка выбранного ремонта и его детали |
| список | Обновить (панель вагона) | `UpdateRepairs` | обновляет, только если карточка сохранена | `GetRepairList` | та же строка перечитывается |
| список | контекст «Провести ремонт» | `msRegister_Click` | `RegisterRepair`; id берётся из строки | `IWagonCall.RegisterRepair` → `RepairsPkg.Register(id)` | иконка проводки после повторного чтения списка. Тело `Register` скрыто |
| список | контекст «Отменить проводку» | `msUnRegister_Click` | то же право | `RepairsPkg.UnRegister(id)` | снятие проводки. Тело скрыто |
| карточка | режим редактирования | `SetReadOnly` | если `RegisterType == 1`, карточка принудительно только для чтения | — | проведённый ремонт с экрана не правится, пока проводку не снимут |

## 3. Добавление ремонта

Кнопка панели вагона «Добавить ремонт» (`bbitAddRepair`) активна на вкладке ремонтов при праве `WriteRepair`. Пункты меню все вызывают `repairsExplorerPanel.AddRepair(RepairType.*)`.

| Пункт | Диалог | Класс | Запись |
| --- | --- | --- | --- |
| Деповской (ДР) | «Новый деповской ремонт (ДР)» | `DepotRepair` | `RepairsPkg.SaveDepotRepair` |
| Капитальный (КР) | «Новый капитальный ремонт (КР)» | `CapitalRepair` | `RepairsPkg.SaveCapitalRepair` |
| Капитальный ремонт с продлением (КРП) | `ExtRepairDialog` | `ExtendedCapitalRepair` | `RepairsPkg.SaveExtendedCapitalRepair` |
| Текущий отцепочный ремонт (ТОР) | «Новый текущий отцепочный ремонт (ТОР)» | `CurrentRepair` | `RepairsPkg.SaveCurrentRepair` |
| Отстой (ОТСТ) | `TrashRepairDialog` | `TrashRepair` | `RepairsPkg.SaveTrashRepair`, затем `PostSaveTrashRepair` |
| Исключение (ИСКЛ) | «Исключение вагона» | `ExclusionRepair` | `RepairsPkg.SaveExclusionRepair`, затем `PostSaveExclusionRepair` |
| Подготовка вагонов под погрузку (ТР1) | тот же диалог, что у ТОР | `PreparationLoadingRepair` | `RepairsPkg.SavePreparationLoading`, затем `PostSavePreparationLoading` |
| Техническое обслуживание 3 (ТО3) | тот же диалог, что у ТОР | `MaintenanceThreeRepair` | `RepairsPkg.SaveMaintenanceThreeRepair`, затем `PostSaveMaintenanceThreeRepair` |

Цепочка кнопки «Сохранить» диалога:

`RepairDialog.btnSave_Click` → `RepairInfoPanel.ExceptionSaveChanges` → `ApplyContent` (проверка порядка дат) → `DBManager.SaveObject` → процедура `Save*` из таблицы → `PostSave*` там, где атрибут есть → `DialogResult.OK` → список перечитывается `GetRepairList`, событие `RepairAdded`.

Новый ремонт получает `OwnerId` = id вагона. В «детали до ремонта» кладутся `wagon.CurrentDetails` (`PartsPackage.LoadCurrentWagonDetails`). Это снимок текущих деталей на момент открытия диалога, не отдельный ввод.

Упразднённый текущий ремонт (УТР, код 6) отдельным пунктом «Добавить» не создаётся. Он появляется переводом из ТОР.

Чтение КР идёт процедурой `LoadRegularRepair` (атрибут на `DepotRepair`, отдельного `LoadCapitalRepair` в spec нет). УТР и ТР1 отдельных `Load*` не имеют и читаются как наследники `CurrentRepair` через `LoadCurrentRepair`.

## 4. Карточка ремонта

Общая панель `RepairInfoPanel`. Вкладки: «Ремонт», «Детали», «Перевернутые детали».

Общие поля вкладки «Ремонт»: дата отцепки, станция отцепки, дата подачи на станцию, дата подачи на РП, дата начала ремонта, дата выхода из ремонта, дата выхода на станцию, стоимость ремонта, номер счета, другие работы, простой (сутки/ч:мин), простой в ожидании. Простои только для чтения (`Delay`, у ДР/КР/КРП ещё `WaitingDelay`).

Какая панель показывается:

| Вид | Панель | Свои поля |
| --- | --- | --- |
| ДР, КР | `RegularRepairInfoPanel` | ремонтное предприятие (`REPAIRPLANTS`), станция назначения, выполненные модернизации |
| КРП | `ExtRepairInfoPanel` | то же плюс «Срок службы продлен до» |
| ТОР, ОТСТОЙ, УТР, ТР1, ТО3 | `CurrentRepairInfoPanel` | причина отцепки (`Reason`), место ремонта (`REPAIRPLANTSVIEW`), номер состава, подпись узла вагона по причине |
| ИСКЛ | `ExclusionRepairInfoPanel` | предприятие; подписи дат: «Дата ВУ10», «Дата исключения», «Стоимость разделки», «Дата реализации/разделки». На форме есть покупатель, акт ВУ-10, инвентарный парк, примечание и причина исключения, но `ShowRepairInfo` / `ApplyContent` переносят в объект только предприятие и общие поля. Исключение и продажа как данные вагона пишутся с вкладки «Дополнительные данные» (`ExclusionInfo`, `SaleInfo` → `WagonPackage.SaveWagon`) |

Кнопки вкладки «Детали»:

| Кнопка | Метод | Правило | Куда попадает строка |
| --- | --- | --- | --- |
| Оставить детали на вагоне | `btAddToAfter_Click` | выбранные из «до», которых ещё нет в «снятых» | коллекция после ремонта |
| Оставить все детали на вагоне | `tsbAddAlltoAfter_Click` | все из «до», которых нет в «снятых» | то же |
| Снять детали с вагона | `btAddToRemoved_Click` | выбранные из «до», которых нет в «после» | снятые |
| Снять все детали с вагона | `tsbAddAlltoRemoved_Click` | все из «до», которых нет в «после» | снятые |

Одна и та же деталь не может одновременно быть и оставленной, и снятой: вторая кнопка проверяет `ExistsCountingDetail` в противоположной коллекции.

Сохранение коллекций — атрибуты связи на ремонте (имена процедур в `WagonLib`):

| Коллекция | Процедуры |
| --- | --- |
| до ремонта | `PartsPackage.LoadDetailsBefore` |
| после ремонта | `LoadDetailsAfter`, `DeleteDetailsAfter`, `SaveDetailsAfter`, `UpdateCurrentDetailsAfter` |
| снятые | `LoadDetailsRemoved`, `DeleteDetailsRemoved`, `SaveDetailsRemoved` |
| перевернутый вид | `PartsPackage.LoadRotatedDetails` |

`owner_id` этих процедур — id ремонта. История одной детали с карточки детали: `PartsPackage.LoadPartHistory`, состояние — `GetHistoryState`.

## 5. Удаление и перевод вида

| Окно | Кнопка | Метод | Правило на клиенте | Oracle | Результат |
| --- | --- | --- | --- | --- | --- |
| список | Удалить ремонт | `DeleteRepair` | `DeleteRepair`; вопрос «Вы действительно хотите удалить выбранный …?». Проверки «уже проведён» в этом методе нет | `RepairsPkg.DeleteRepair(id)` | строка исчезает из списка после `GetRepairList`. Отказ сервера возможен, тело скрыто |
| контекст | ТОР в УТР | `msConvertToAbolished_Click` | объект `is CurrentRepair`. Текст отказа: «Только ТОР-ы можно переводить в УТР.» | `RepairsPkg.ConvertToRepair(id, 6)`, затем `LoadRepair` и `SaveObject` (`SaveAbolishedRepair` + `PostSaveAbolishedRepair`) | вид становится 6 |
| контекст | УТР в ТОР | `msConvertAbolishedToCurrent_Click` | объект `is AbolishedRepair` | `ConvertToRepair(id, 3)` | вид 3 |
| контекст | ДР в КР | `tmsDepotToCapital_Click` | объект `is DepotRepair`. Текст: «Только ДР-ы можно переводить в КР.» | `ConvertToRepair(id, 1)` | вид 1 |
| контекст | КР в ДР | `tmsCapitalToDepot_Click` | объект `is CapitalRepair`. Текст отказа в коде: «Только КР-ы можно переводить в КР.» | `ConvertToRepair(id, 0)` | вид 0 |
| контекст | ТОР в ТР1 | `tmsCRtoTR1_Click` | объект `is CurrentRepair`, затем повторное сохранение как `PreparationLoadingRepair` | `ConvertToRepair(id, 7)` + `SavePreparationLoading` | вид 7 |
| контекст | ТР1 в ТОР | `tmsTR1toCR_Click` | объект `is PreparationLoadingRepair` | `ConvertToRepair(id, 3)` | вид 3 |
| контекст | ТОР в ТО3 | `tmsCRtoTO3_Click` | объект `is CurrentRepair`. Текст отказа скопирован про УТР | `ConvertToRepair(id, 8)` | вид 8 |
| контекст | ТО3 в ТОР | `tmsTO3toCR_Click` | объект `is MaintenanceThreeRepair`. Текст отказа тоже про УТР | `ConvertToRepair(id, 3)` | вид 3 |

`CurrentRepair` — базовый класс для ОТСТОЙ, УТР, ТР1 и ТО3. `DepotRepair` — базовый для КР и КРП. Проверка `is` поэтому шире, чем текст сообщения. Отсекает ли лишние пары уже `ConvertToRepair`, по телу пакета не видно.

Отдельная процедура `SP_CONVERTREPAIRSTOTR1(wagonCode, endingDate)` пишет `REPAIRS.TYPE = 7` по коду вагона и дате окончания. В строках `WagonLib` / `WagonApp` её нет: это не кнопка УВП.

## 6. Планы ремонтов

Окно `RapairBudgetExplorer`, заголовок «Обозреватель планов ремонтов».

| Кнопка | Метод | Право | Oracle | Результат |
| --- | --- | --- | --- | --- |
| Обновить / открытие | `LoadData` | — | `RepairBudgetView` | список: план, факт, процент |
| Добавить | `AddBudget` | `RepairBudgetAdd` | — | пустой `RepairBudget`, форма «План ремонтов» |
| Просмотреть | `OpenBudget` | — | `RepairBudgetPkg` load через `LoadObject` | форма только для чтения |
| Изменить / двойной щелчок | `EditBudget` | двойной щелчок редактирует при `RepairBudgetEdit`, иначе просмотр | `SaveRepairBudget`, `SaveRepairBudgetItem` | запись бюджета |
| Удалить | `DeleteBudget` | `RepairBudgetDelete`, вопрос подтверждения | `DeleteObject` бюджета | строка уходит из `RepairBudgetView` |
| Заполнить | `LoadRepairWagons` | — | `RepairBudgetPkg.FillRepairBudget(begin, end)` | строки вагонов: план, вид, дата |
| Сохранить | `RepairBudgetForm.Save` | — | `RepairBudgetPkg.SaveRepairBudget` | id бюджета |
| Сводная таблица | `RepairBudgetControl` | — | `GetRepairBudgetColumns`, `GetRepairBudgetData` | помесячный план/факт |
| Выгрузка в Excel | `ExportToExcel` | — | — | файл, Oracle не меняется |

`RepairBudgetPkg.RepairWagon(repair_id)` и `UpdateBudget` / `UpdateBudgets` в кнопках этой формы не вызываются. Кто их зовёт из проведённого ремонта, тело пакетов не показывает.

## 7. Пробег и следующие даты

Блок «Тип учета ремонтов» (`RepairTypeAccountingControl`) на паспорте вагона.

Флажок: снят — учёт по сроку, установлен — учёт по пробегу. Рядом состояние перехода (`repairtransitionstate`), дата перехода, дата постановки на пробег, допустимый пробег, жёлтый пробег, остаток по пробегу (Ж), перепробег (Ж) и (К), пробег, дата данных по справке, дата обновления, номер справки (код сообщения). Числовые поля пробега на контроле `ReadOnly`. При сохранении вагона `ApplyContent` всё равно переписывает в объект нормы, флажок, состояние и даты перехода. Факт пробега (`MileAge`, `MileAgeRedRerun`, `MileAgeDate`, `ChangeDate`, `MessageCode`) помечен `[LoadOnlyField]` и с этой формы в `SaveWagon` как редактируемое поле не уходит.

Цвет строки состояния обозревателя: срок — `DeepSkyBlue`, пробег — `MediumPurple`. Текст «Учет ремонтов:».

Следующие даты (`NextRepairsInfoCtrl`, группа «Следующие ремонты») только показывают `Wagon.NextDepotRepairDate`, `NextMajorRepairDate`, `NextMajorExtRepairDate`. Это `[LoadOnlyField]`. Считает их пакет `RepairDates_Pkg` (`GetDRDate`, `GetCRDate`, `GetECRDate`, `UpdateNextRepairDates`). Суточный job `UPDATEALLREPAIRDATES` в 01:00 вызывает `JobPkg.UpdateAllRepairDates`. Клиент эти процедуры не вызывает.

Загрузка пробега с линии — сообщения 2610 и 2612 на сервисе (`WagonMessage2610*`, `WagonMessage2612*`) в `RepairsPkg.ImportMileage` / `ImportMileageWeb` / `ImportMileageWeb2612`. Это не кнопка УВП.

## 8. Отчёт «Анализ»

`LastRepairsFrm`, заголовок «Анализ».

Плановый блок: тип вагона, тип ремонта, ремонтное предприятие, период с/по. Блок ТОР: станция, переключатель «Причина» / «Деталь». ОК закрывает диалог, отчёт строит `ReportsBinder.GetAnalyzeReport`. Ветка детали зовёт `ReportsPkg.GetLastTORs`, ветка причины и плановый отчёт — `ReportsPkg.GetLastRepairs`. Оба вызова только читают курсор.

## 9. Что экран не делает

- Не вызывает `RepairsPkg.Register` в момент «Сохранить». Проводка — отдельный пункт меню.
- Не показывает `LOGSREPAIRSVIEW`. История на экране — все строки `GetRepairList` по вагону.
- Не проводит складской документ НУД (`WarehousePkg.RegisterRepairDocument`).
- Не запускает `RotateRepairDetails`, `UpdateAllRepairDates`, `ImportMileage*`, `ImportWagonParts*`.
