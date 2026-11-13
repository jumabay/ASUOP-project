# Этап 5. Реверс УВП

## Что исследовано

- `Программы пользователя/УВП/.../WagonApp.exe` 2.2.1.26.
- Сравнение списка классов и размеров прикладных DLL с НУВ.
- Строки пакетов `RepairsPkg`, `RepairBudgetPkg`, `PartsPackage`, `RepairsPkg.ImportWagonParts2731` в `RemoteLib` / `WagonLib`.
- VBA `MacrosTemp/Ремонты. Импорт деталей*.bas` (два файла одного размера 10593).
- Шаблон `Templates/InventoryStateReport.xltx`.

UI не запускался.

## Подтверждённые факты

Список классов `WagonApp` УВП **совпадает** со списком классов `WagonApp` НУВ (в том числе `RepairExplorer`, `StateReportImporter`, `Reports`). Отдельного EXE «управление ремонтом» нет.

Отличия поставки:

- EXE: НУВ 569344 байт / 2.2.1.25, УВП 570888 байт / 2.2.1.26.
- В папке УВП нет `WagonPlanner.exe`.
- В папке НУВ есть `FtpLib`, `RemoteLib`, `TransportLib`, `SyslogNet.Client`; в списке размеров УВП их нет, зато есть `Microsoft.Build.Conversion.v3.5.dll`.
- Шаблоны отчётов НУВ в папку УВП не скопированы, кроме `InventoryStateReport.xltx`.

Ремонтный контур в общих библиотеках: `RepairsPkg.GetRepairList`, `GetWagonRepairs`, `LoadMileAgeWagons`, `LoadMileAgeWagonsEveryDay`, `ImportMileageWeb`, `Register`, `ImportWagonParts2731`; бюджет — `RepairBudgetPkg`; история детали — `PartsPackage`.

Сообщения 2731 на сервисе — отдельные задачи `WagonMessage2731SenderTask` / `ReceiverTask` (в эталоне импорта выключены, во включённой копии пользователя включены).

## Бизнес-логика

УВП в этой поставке — тот же клиент вагонов более новой сборки, без планировщика, с макросом импорта деталей ремонта. Текущий, деповской и капитальный ремонт как отдельные классы в списке типов не выделены; они, если есть, спрятаны в формах `RepairExplorer` / данных `RepairsPkg`.

## Связи

- Общий `WagonLib` с НУВ.
- Детали уходят в НУД через `WarehousePkg` и `PartsPackage` (этап 6).
- Срезы вагона те же: `StateReportPackage`.

## Неизвестное

- Чем именно отличается IL `WagonApp` 2.2.1.26, кроме размера.
- Справочник видов ремонта (ТР/ДР/КР) — таблицы не читались.
- Какой из двух одинаковых `.bas` актуален.

## Риски

- Считать УВП отдельной системой по имени папки — ошибка. Паритет новой системы надо снимать с функций `RepairExplorer` + `RepairsPkg`, а не с «второго продукта».

## Следующий этап

Склад и номерные детали (`WarehouseApp`).
