# Этап 4. Реверс НУВ

## Что исследовано

- `Программы пользователя/НУВ/.../WagonApp.exe` 2.2.1.25, `WagonPlanner.exe` 1.2.2.85.
- Списки классов ilspycmd (без запуска UI и без FTP).
- VBA импорта срезов в `MacrosTemp`.
- Шаблоны `Templates\`.
- Пакеты, на которые ссылается общий `WagonLib` / `RemoteLib` (этап 2).

## Подтверждённые факты

`WagonApp` — клиент номерного учёта. Классы: `WagonExplorer`, `NewWagonEditor`, `AsyncWagonLoader`, `StateReportImporter`, `RepairExplorer`, `Asoup`, `Reports`.

`WagonPlanner` — отдельный EXE планировщика (только в папке НУВ): наряды (`WorkingSheetExplorer`), подача/уборка (`GivingDepartures`), маршруты, заявки, бюджеты, транспортные заявки, плановые объёмы.

Чтение среза вагона идёт в `RemoteLib.GivingDepartures.StateReports.LoadWagonStateReports` → `StateReportPackage.GetWagonStateReports` (дата с 00:00:00 по 23:59:59).

Списки и подача на стороне библиотек: `WagonPackage.GetWagonList`, `GivingDeparturePackage`, `RemoteGivingDeparturePackage`, `ApproachPkg.GetApproachWagons`.

Клиент подключается к Remoting `ApplicationService` (копия «Сервис пользователя», порты контрактов `IWagonCall`, `IGivingDepartureCall`, `IDirectoryCall`, `IDbManagerCall`). Прямой обход сервиса в этом этапе не доказывался по каждому экрану.

## Бизнес-логика

НУВ показывает дислокацию и историю срезов, ведёт карточки вагонов, подачу/уборку и планирование. Сам почасовой файл с FTP забирает не клиент, а `ImportStateReportsFtp` на сервисе. Excel-макрос в поставке НУВ умеет превратить лист дислокации в XML по всем строкам листа.

## Связи

- Oracle `dbtc`: `StateReportPackage`, `WagonPackage`, `Giving*`, `ApproachPkg`.
- Общие DLL с УВП: `WagonLib`, `RemoteLib`.
- Отчёты: шаблоны в `Templates` (этап 8).

## Неизвестное

- Точная resource-подпись пункта меню, открывающего форму реестра перед «Закачать на сервер» (сама кнопка публикации `wagons.txt` — R4 / [11-nuff-active-wagon-list.md](11-nuff-active-wagon-list.md)).
- Кто вызывает `GetActiveWagons` / серверный критерий `GetActiveWagonList` (UI НУВ в охвате R4 не найден; body Oracle не читался).
- Portable-поставка, отдельная от ClickOnce, в workspace не найдена.
- Отличия поведения `WagonApp` 2.2.1.25 от УВП 2.2.1.26 при одинаковом списке классов.

## Риски

- Запуск ClickOnce-клиента может сходить на сервер обновлений или FTP. Не запускался.
- Ремонтные формы есть и в НУВ: границу с УВП нельзя проводить только по имени папки.

## Следующий этап

Сравнение поставки УВП с этим клиентом.
