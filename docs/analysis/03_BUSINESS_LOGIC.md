# Бизнес-логика Oracle, которую вызывают АСУ-ВП, НУВ, УВП, НУД и отчёты

Источник фактов: спецификации и словарь сессии `tc-asuvp-03` / `DBTC` ([03_ORACLE_PACKAGES.md](03_ORACLE_PACKAGES.md)), вызовы DLL из этапов 1 и 4–8.  
Тела пакетов не читались. Где сказано «внутри пакета», это граница знания, а не описание алгоритма.  
Код в C# не переносился.

АСУОП к этой схеме не относится (MSSQL `terminal`).

## 1. Дислокация

```text
FTP disl_*  →  ApplicationService (разбор файла в C#)
            →  ImportPkg.IsImportedFile
            →  ImportPkg.SaveImportFile          → IMPORTFILES
            →  ImportPkg.SaveStateReport         → STATEREPORTS
            →  ImportPkg.SaveStateReportLine*    → STATEREPORTRECORDS  (вывод)
            →  ImportPkg.UpdateStateReport       → пересчёт, тело скрыто
НУВ / УВП   →  StateReportPackage.GetWagonStateReports
отчёты      →  те же чтения + WagonPackage.GetWagonsInventoryDataOLAP
каждые 15 мин (job WAGONUPDATE) → WAGONUPDATEDISL.WAGONUPDATEDISLTIS
            объект вызывающему пользователю не виден
```

Что уже известно без тела:

- Час среза задаёт файл и аргументы процедур, а не таймер 45 секунд. Таймер только опрашивает FTP.
- Клиент не отбрасывает «неподвижный» вагон до вызова `SaveStateReportLineKz`. Решение «писать строку или нет» может быть только в теле `ImportPkg` или во входном файле. Тело недоступно, поэтому правило не зафиксировано.
- Повтор файла на стороне C# — это `IsImportedFile(filename, type) = 1`. Когда флаг становится 1 (до строк, после `UpdateStateReport`, при ошибке посередине), в spec нет.
- На `STATEREPORTRECORDS` нет триггера. Обновление `WAGON` (станция, `CURRENTSTATE_ID`, дата) из строки среза триггером не делается.
- Публичные переменные `ImportPkg` держат контекст перегона между вызовами одной сессии.

Связанные сценарии: АСУ-ВП пишет; НУВ и УВП читают один и тот же `GetWagonStateReports`; документный срез третьих сторон — `WagonStateReportsPkg.GetPublishWagonsThird`; производная таблица `STATEREPORTSWAGON` должна была наполняться job `FILLSTATEREPROTSWAGONSTATIONS`, job в состоянии FAILED и выключен.

Как проверить в новой системе, не подменяя писателя: для уже импортированного часа сравнить число строк и ключи (`файл`, `вагон`, время дислокации) read-only запросом с тем, что показывает старый НУВ. Новый процесс файл может только читать.

## 2. Отчёты по вагонам

Отдельного хоста отчётов нет. Шаблоны живут в клиенте, данные приходят пакетами.

| Потребность | Объект Oracle | Запись? |
|---|---|---|
| Список и карточка | обозреватель: `GetWagonList`; карточка: `GetWagonByCode`; `GetActiveWagonList` — Remoting API, UI НУВ не подтверждён (R4) | чтение. `SaveWagon` — запись карточки, не отчёт. Evidence: [11-nuff-active-wagon-list.md](11-nuff-active-wagon-list.md) |
| Инвентарный парк / OLAP | `GetWagonsInventoryData`, `GetWagonsInventoryDataOLAP` | чтение |
| История дислокации | `StateReportPackage.GetWagonStateReports` и `LoadStateReports*` | чтение |
| Простой | `DownTimePkg.GetWagonDownTime` | чтение. `FillWagonDownTime` и `JobPkg.FillAllWagonDownTime` — запись |
| Движение, последние ремонты, ТОР | `ReportsPkg.GetMovementWagons`, `GetLastRepairs`, `GetLastTors` | по имени чтение; тело скрыто |
| Колонки конструктора | `ReportingPackage.GetStateFields` и соседние `Get*Fields` | метаданные |
| Пробег за месяц | `WagonPackage.GetMonthDistances`, job `UPDATEWAGONMONTHDISTANCESDAILY` | job пишет |
| KPI | job `FILLKPI` → `KpiPkg.FillKpi`, view `KPIVALUESVIEW` | job пишет |
| Подача в отчёте | `FILLGIVINGREPORTS` → `GivingDepartureReports.FillGivingReports2` | job пишет |
| Представления | `WAGONVIEW`, `WAGONDOWNTIMEVIEW`, `GIVING_ROUTE_REPORT_VIEW` | определение не копировалось |

Тяжёлые чтения сидят на `STATEREPORTRECORDS`, `WAGONDOWNTIME`, `WAGONAPPROACHARCHIVE`. Партиций нет (этап 2).

Проверка отчёта: один и тот же вагон и сутки в старом Excel и в новом читателе. Расхождение по «текущей станции» нельзя толковать, пока не виден `WAGONUPDATEDISL`.

## 3. Ремонты

УВП ходит в `RepairsPkg` и `RepairBudgetPkg`. Виды ремонта заданы константами spec (`0`…`8`), сообщения пробега — `2610` / `2612`, детали — `ImportWagonParts2731`.

Суточный пересчёт дат — не `LoadMileAgeWagonsEveryDay` из клиента, а job `UPDATEALLREPAIRDATES` в 01:00: `JobPkg.UpdateAllRepairDates`. Что именно он меняет в `REPAIRDATES_PKG`, тело не показывает.

При любой вставке/правке `REPAIRS` срабатывает `TR_REPAIRS_TIME` и зовёт `TimesPkg.WriteTime` для начала и конца ремонта. Смысл `WriteTime` скрыт в теле `TIMESPKG`.

Идемпотентность `Register` и `ImportMileage*` не доказана. Повторный импорт того же сообщения 2612 может задвоить ремонт или пробег — это риск, не установленный факт.

Проверка: список `GetWagonRepairs` и бюджет `GetRepairBudgetData` по одному вагону против УВП. `Register`, `ImportMileage*`, `Save*Repair` не вызывать.

## 4. Детали и склад

НУД вызывает `WarehousePkg.Register*` / `Unregister*` по видам документа 0–6 (закуп, приход, перемещение, списание, колёсная пара, наряд, ремонт). УВП и отчёты читают историю через `PartsPackage.GetHistoryState` и `LoadPartHistory`.

Автономные `SP_ARRIVALWAGONPARTS`, `SP_ARRIVEWHEELPAIR`, `SP_TRANSFERWAGONPART` делают то же семейство таблиц (`WAGONPARTS`, документы склада) напрямую, в обход пакета. В DLL почасового сервиса они не фигурируют. Пока нет вызова из приложения, считать их рабочим API нельзя: это загрузочные процедуры с видимым `INSERT`.

`PartsPackage.RotateRepairDetails` завязан на выключенный job, который обходит все строки `REPAIRS`. Запускать его нельзя: полный проход без тела.

Текущие детали на вагоне (`UpdateWagonCurrentDetails`, view-уровень `CURRENTPARTS`) отделены от складского регистра. Какой `Register*` обновляет «состояние детали», spec не говорит.

Проверка: `LoadWarehouse*` / `GetHistoryState` против экрана НУД. `Register*` не вызывать.

## 5. Обновление состояния

Цепочка не одна.

| Шаг | Где | Доказано? |
|---|---|---|
| Строка часа в ленте | `ImportPkg.SaveStateReportLine*` | контракт DLL + имена. Таблица — вывод |
| Признак последнего среза | `StateReportPackage.UpdateLastReport*` | член spec. Колонка `STATEREPORTS.LAST` есть в схеме этапа 2. Связка «этот член пишет LAST» телом не подтверждена |
| Предыдущие станции | `UpdatePrevStations`, `UpdatePreviousLoadingStations` | члены spec; разовые jobs `UPDATEPS_*` выключены |
| Карточка `WAGON` | `WagonPackage.SaveWagon`, `UpdateWagonStatus` | запись карточки, не почасовой срез |
| Оперативное положение | `WAGONUPDATEDISL.*` | только текст ENABLED-job раз в 15 минут. Объект не виден |
| Подача / наряд | `GivingPkg.UpdateStateReport`, `WorkListsPkg.UpdateStateReport` | имена. Пересекаются с дислокацией, тело скрыто |
| Станции справочника | `TR_STATIONS_CHANGED` | факт: штамп `LASTCHANGETIMES` для `STATIONSVIEW` |

Пока `WAGONUPDATEDISL` и тело `ImportPkg.UpdateStateReport` не прочитаны, новая система не должна сама писать `WAGON.LOCATION*` и `CURRENTSTATE_ID`.

## 6. Транзакции, ошибки, повтор

| Контур | Что видно |
|---|---|
| Пакеты | в spec нет `EXCEPTION` и `COMMIT` |
| Автономные процедуры | `EXCEPTION` нет; `COMMIT` только в `SP_UPDATEWAGONGRREGISTRATION` |
| АСУ-ВП, текстовый срез | ошибка строки пропускается, ошибка `UpdateStateReport` только логируется, строки уже отправлены (этап 1) |
| XLS | `SaveFileContent` внутри клиентской транзакции, затем `UpdateStateReportXml` |
| Jobs | явный SQL в `UPDATEEXCESSINACTIVITY` и `UPDATEREGISTRYMAIN`; фиксация транзакции анонимного блока не разбиралась, блоки не запускались |
| Триггеры id | ошибки sequence в теле не обрабатываются |

Повторное исполнение опасно для: `SaveStateReportLine*` при сбое после части строк, `CreateJob*` (разовые jobs уже лежат в каталоге), `Register*` склада, `ImportMileage*`, суточных `Fill*` / `UpdateAllRepairDates`, `RotateRepairDetails`.

## 7. Классификация

Классы:

- **Оставить в Oracle** — писатель или агрегатор, тело не прочитано, замена сломает старые EXE.
- **Кандидат в C#** — тонкое чтение или уже существующая оркестрация файла в сервисе. Сам вызов пакета чтения на первых волнах остаётся в Oracle; переносится оболочка UI/импортёра.
- **Исследовать** — нет тела, объект не виден или побочный эффект не отделён от чтения.

| Объект | Класс | Почему |
|---|---|---|
| `ImportPkg` запись среза, giving, реестра, `Update*` | Оставить в Oracle | единственный согласованный писатель файла, тело закрыто |
| `ImportPkg.CreateJob*` | Оставить в Oracle | создаёт задания; побочный эффект не измерен |
| Разбор FTP/XLS в ApplicationService | Кандидат в C# | оркестрация уже в C#; в Oracle уходит готовая строка |
| `StateReportPackage` `Get*` / `Load*` | Кандидат в C# | контракт курсора ясен; SQL внутри ещё нужен Oracle, пока нет своей модели |
| `StateReportPackage` `Update*` / `Refresh*` / `Delete*` / `CreateJob*` | Оставить в Oracle | пересчёт и удаление |
| `WagonStateReportsPkg` чтение публикации | Кандидат в C# | один подтверждённый read-вызов |
| `WagonStateReportsPkg` `Save*` | Оставить в Oracle | документная запись |
| `StateReportsWagonPackage.Fill*` | Исследовать | job FAILED |
| `WagonPackage.Get*` / inventory OLAP | Кандидат в C# | чтение карточки и отчёта |
| `WagonPackage.Save*` / `Delete*` | Оставить в Oracle | карточка вагона, много полей, тело закрыто |
| `ReportsPkg`, `ReportingPackage`, `DownTimePkg.GetWagonDownTime` | Кандидат в C# | отчётные чтения |
| `DownTimePkg.Fill*`, `KpiPkg.Fill*`, `ResourcePkg.Write*` / `UpdateAvailability*` | Оставить в Oracle | ночные агрегаты |
| `RepairsPkg.Get*` / `Load*` (кроме Import/Save) | Кандидат в C# | экраны УВП |
| `RepairsPkg.Register`, `Save*`, `ImportMileage*`, `ImportWagonParts*` | Оставить в Oracle | проводки ремонта |
| `RepairBudgetPkg.Get*` | Кандидат в C# | отчёт |
| `RepairBudgetPkg.Save*` / `Fill*` | Оставить в Oracle | запись бюджета |
| `RepairDates_Pkg`, `JobPkg.UpdateAllRepairDates` | Оставить в Oracle | ночной пересчёт дат |
| `PartsPackage.GetHistoryState`, `LoadPartHistory`, `SearchParts` | Кандидат в C# | история деталей |
| `PartsPackage` `Save*` / `Rotate*` / `Move*` | Оставить в Oracle | движение детали |
| `WarehousePkg.Load*` | Кандидат в C# | списки НУД |
| `WarehousePkg.Register*` / `Unregister*` | Оставить в Oracle | складские проводки |
| `GivingDeparturePackage`, `GivingPkg`, `ApproachPkg`, `WorkListsPkg` | Исследовать | много членов, часть пишет состояние; для UI чтения — кандидат только после разделения Get/Save |
| `AsoupPkg` | Исследовать | сообщения 2610/2612; пересечение с пробегом |
| `WAGONUPDATEDISL` | Исследовать | объект не виден, job включён |
| `REFERENCESFILL` | Исследовать | объект не виден, два ENABLED-job |
| Автономные `SP_*` | Исследовать | видимый DML, вызов из приложений не подтверждён |
| Триггеры `TR_*_ID` | Оставить в Oracle | выдача ключей |
| `TR_REPAIRS_TIME`, `TR_PLAN_REPAIRS_TIME`, `TR_STATIONS_CHANGED` | Оставить в Oracle | побочный эффект на каждой строке |
| View `WAGONVIEW`, ремонтные и giving-view | Исследовать | текст не разобран; читать через них можно после просмотра SQL |
| Пакет `TEST` | Исследовать | 53 члена в рабочей схеме |

Волна записи из [10-target-architecture-migration.md](10-target-architecture-migration.md) по-прежнему закрыта: нет тел `ImportPkg` и `WAGONUPDATEDISL`.
