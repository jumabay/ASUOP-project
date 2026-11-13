# 09. УВП — управление вагонным парком

УВП в этой поставке — клиент `WagonApp.exe` 2.2.1.26, заголовок «Учет вагонного парка (УВП)». Отдельного EXE ремонта нет. Клиент Oracle не открывает. Запись объекта — `DBManager.SaveObject` по атрибуту `[SaveProc]`. Проводка идёт через Remoting `IWagonCall`, порт контракта 5507.

Источник: [05_VVP_FUNCTIONS.md](../analysis/05_VVP_FUNCTIONS.md), [05_VVP_REPAIRS.md](../analysis/05_VVP_REPAIRS.md).

## Что подтверждено

Виды `RepairsPkg`: 0 ДР, 1 КР, 2 КРП, 3 ТОР, 4 исключение, 5 отстой, 6 УТР, 7 ТР1, 8 ТО3. Предприятия — `REPAIRPLANTS`. Таблица `REPAIRS` около 411 тыс. строк. На каждой вставке и правке срабатывает `TR_REPAIRS_TIME` → `TimesPkg.WriteTime`. Смысл `WriteTime` скрыт.

Чтение, которое модуль `repairs` может вызвать:

| Экран | Процедура |
| --- | --- |
| Список | `GetRepairList(WagonId, RepairType)` |
| Карточка | `LoadRegularRepair`, `LoadExtendedCapitalRepair`, `LoadCurrentRepair`, `LoadTrashRepair`, `LoadExclusionRepair`, `LoadMaintenanceThreeRepair` |
| Детали | `LoadDetailsBefore`, `LoadDetailsAfter`, `LoadDetailsRemoved`, `LoadRotatedDetails` |
| Текущие и начальные детали | `PartsPackage.LoadCurrentWagonDetails`, `LoadInstalledDetails` |
| График | `GetWagonRepairs` |
| Бюджет, чтение | `RepairBudgetView`, `GetRepairBudgetColumns`, `GetRepairBudgetData` |
| Следующие даты и факт пробега | поля паспорта `[LoadOnlyField]` |

Даты следующего ремонта считает job `UPDATEALLREPAIRDATES` в 01:00. Пробег с линии пишет служба сообщений, не форма. Новый код эти процедуры не вызывает. Поля пробега показываются как прочитанные.

Вкладки обозревателя: «Тех.паспорт», «Ремонты», «Начальные детали», «Текущие детали», «Дополнительные данные». Исключение и продажа вагона как данные карточки пишутся с вкладки «Дополнительные данные» через `SaveWagon`, не только через `SaveExclusionRepair`.

## Что остаётся у старого клиента до своей волны

`SaveDepotRepair`, `SaveCapitalRepair`, `SaveExtendedCapitalRepair`, `SaveCurrentRepair`, `SaveTrashRepair` и `PostSave*`, `SaveExclusionRepair`, `SavePreparationLoading`, `SaveMaintenanceThreeRepair`, `Register` / `UnRegister`, `DeleteRepair`, `ConvertToRepair`.

Пары перевода, которые шлёт клиент: ТОР↔УТР (6↔3), ДР↔КР (0↔1), ТОР↔ТР1 (3↔7), ТОР↔ТО3 (3↔8). Отсекает ли пакет лишнюю пару, не подтверждено (M4).

УТР пунктом «Добавить» не создаётся. Проводка не вызывает `Save`: в процедуру уходит уже записанный id. Проведённая карточка на старой форме только для чтения. Клиент при удалении не смотрит признак проводки; запрет на стороне пакета не доказан (G7).

Одна деталь не может быть одновременно в «после» и в «снятых» — правило клиента. `PartsPackage.RotateRepairDetails` с кнопки не вызывается, job выключен, новый код его не запускает.

Бюджет: Excel с формы `RapairBudgetExplorer` Oracle не меняет. Кто вызывает `RepairBudgetPkg.RepairWagon` и `UpdateBudget`, не найдено (G12). Эти члены не переносятся как кнопки.

## Новая форма

Модуль `repairs` в том же монолите. Связь с вагоном — через публичный интерфейс `fleet`. Связь со складом — общий ключ детали, не общий документ проводки. Документ склада вида 6 не является строкой `REPAIRS`.

Волна чтения: A3, A4, A5. Волна записи: B4 (новый ТОР без `Register`), затем B5 (проводка после наблюдения G6). `Register` в тестах чтения не вызывается.

Риски переноса: скрытое тело `Register` (G6), триггер времени на каждой правке, общий `WAGONPARTS` со складом, невидимый писатель локации карточки, расхождение двух описаний отчёта «Анализ» (M18).
