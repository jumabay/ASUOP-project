# Этап 8. Сервис пользовательских отчётов

## Что исследовано

- `ReportingLib.dll`, `WagonLib.Reporting.*`, `PlatformStateReports.dll`.
- Шаблоны `Программы пользователя/НУВ/.../Templates` и `УВП/.../Templates/InventoryStateReport.xltx`.
- Внешний путь отчётов в config DLL (`WagonLib\Reports`) в workspace отсутствует.

Отдельный процесс «сервис отчётов» не найден.

## Подтверждённые факты

Отчёты встроены в клиент и общие библиотеки:

- Конструктор: `ReportConstructorForm`, `Report`, `QueryBuilder`, `StaticalReport`, условия по дате/числу/справочнику.
- Выгрузка: `ReportingLib.Export.ExcelExporter`, трансформации XLS/XLT.
- Удалённый запуск: `RemoteLib.Reporting.ReportThread`, `ReportThreadsList`.
- Срез вагона как отчётный набор: `WagonLib.Reporting.StateReport` и шаблон `WagonStateReports.xlt`.
- `PlatformStateReportsJob` не формирует отчёт: методы бросают `NotImplementedException`.

Шаблоны НУВ:

- `WagonStateReports.xlt`
- `MovementWagonsReport.xsl`
- `Availability.xlt`, `AvailabilityMoths.xlt`
- `CargoWork.xlt`
- `LoadingFact.xlt`, `LoadingPlanFact.xlt`
- `VolumePlans.xlt`, `ExecutionVolumePlans.xlt`
- `RealizationReport.xlt`
- `AgentsPivotReport.xsl`

УВП: `InventoryStateReport.xltx`.

Данные для части отчётов уже видны по процедурам: `StateReportPackage.GetStateReports`, `WagonPackage.GetWagonsInventoryDataOLAP`, `DownTimePkg.GetWagonDownTime`, `RepairBudgetPkg.GetRepairBudgetData`. Полный SQL каждого шаблона не извлекался.

## Бизнес-логика

Пользователь собирает отчёт в `WagonApp` (конструктор или готовый шаблон Excel/XSL). Сервис может выполнять поток отчёта (`ReportThread`), но отдельной службы отчётов нет. Почасовой файл сам отчётом не является: отчёт читает уже сохранённые срезы.

## Связи

- НУВ — основной набор шаблонов.
- УВП — инвентарный шаблон.
- Oracle: пакеты этапа 2, не отдельная база отчётов.

## Неизвестное

- Каталог файлов `TransCom\src\Wagons\WagonLib\Reports` (вне workspace).
- Какие отчёты ходят через `ReportThread`, а какие только локально в Excel.
- Соответствие каждого `.xlt` конкретной процедуре.

## Риски

- Перенос только `ReportingLib` без шаблонов НУВ потеряет пользовательские формы.
- OLAP-вызов `GetWagonsInventoryDataOLAP` может быть тяжёлым. Не выполнялся.

## Следующий этап

Сводка функций и пробелов.
