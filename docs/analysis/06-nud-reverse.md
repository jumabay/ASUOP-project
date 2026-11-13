# Этап 6. Реверс НУД

## Что исследовано

- `Программы пользователя/НУД/НУД/WarehouseApp.exe` 1.2.0.29.
- Список классов и процедуры `WarehouseLib.dll` / `PartsPackage` в `WagonLib`.

Приложение не запускалось, Oracle не открывалась.

## Подтверждённые факты

`WarehouseApp` — отдельный клиент склада. Классы: `MainForm`, `WarehouseExplorer`, `Documents` и связанные контролы, `dsDetailsWorkingSheet`.

`WarehousePkg` регистрирует и снимает регистрацию документов:

- приход `RegisterArrivalDocument` / `UnregisterArrivalDocument`
- закупка `RegisterProcurementDocument`
- ремонт `RegisterRepairDocument`
- перемещение `RegisterTransferDocument`
- списание `RegisterWritingOffDocument`
- ведомость деталей `RegisterDetailsWorkingSheet`
- ремонт колёсной пары `RegisterWheelPairRepair`

История состояния детали читается через `PartsPackage.GetHistoryState` и `LoadPartHistory`. В `WagonLib` есть формы `StateWheelPairFrm`, `StateManufactoringDetailFrm`, `StateHatchCoverFrm`, `StateDetail`.

Связь с ремонтом вагона со стороны сервиса: `RepairsPkg.ImportWagonParts2731` и задачи сообщений 2731.

## Бизнес-логика

НУД учитывает номерные детали складскими документами и привязывает их к ремонту и ведомостям. Карточка вагона при этом остаётся в `WagonApp`, не в `WarehouseApp`.

## Связи

- Схема `dbtc`, пакет `WarehousePkg`.
- УВП/НУВ показывают состояние деталей через `WagonLib.RailwayUnits.Parts`.
- Общий Remoting: в эталоне включены `ITransportCall` и `IDbManagerCall`.

## Неизвестное

- Имена таблиц склада и уникальность номера детали.
- Вызывает ли `WarehouseApp` Oracle напрямую или только через сервис. По конфигу клиентов нормальный путь — `ApplicationServers`, но каждый экран не трассировался.

## Риски

- Двойная регистрация документа (`Register*` / `Unregister*`) при параллельной новой системе без разбора пакета.

## Следующий этап

Клиент АСУОП и граница с Oracle.
