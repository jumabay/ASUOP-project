# Этап 11 / R4. Call-site `GetActiveWagonList` в поставке НУВ

Локальный research-gate R4 (2026-11-16). Цель: цепочка UI → `WagonPackage.GetActiveWagonList` и состав списка по доступным материалам. Oracle/JDBC/FTP и запуск legacy с БД не выполнялись. Оригиналы в «Старая программа» не менялись. Декомпиляция — `ilspycmd` 9.1.0.7988 по **копиям** в `docs/analysis/_nuff_tmp/` (каталог в `.gitignore`).

Источник поставки НУВ: `Старая программа/Программы пользователя/НУВ/wago..tion_75733403170c1f93_0001.0002_9dc6a92e4bc8e039/`.

## 1. Исследованные бинарники

| Файл | FileVersion | Size | SHA-256 |
| --- | --- | --- | --- |
| `WagonApp.exe` | 2.2.1.25 | 569344 | `51FB88E757E7B172DF2D06814CD107077CF3EC0BE55B15BAF06D1EA0370AD37C` |
| `WagonLib.dll` | 2.1.7673.41652 | 696320 | `B5DB9487B799059F16E13E957E2D90FFB92A290026DB5E768CD6FF8DDA141D7B` |
| `RemoteLib.dll` (рядом с НУВ) | 1.0.0.0 | 190464 | `754AC36608B57D2DD7564D95EF327ED62F10E3BB0981E287A22F057AB25EE142` |
| `IRemoteLib.dll` | 1.0.0.0 | 52736 | `7465F35E179E467F61211B5E5C107EB01BFF04E0D78A7A4DFFA610A66AEC06C2` |
| `RemoteSessionLib.dll` | 1.0.0.0 | 17920 | `FEFF02D649D6AB1A9E2735E225E6855AA42ED54006BBB91CF2551C2E4A41391D` |
| `PlannerLib.dll` | 1.0.2.0 | 328704 | `243ADC6D5A4B434741BF24DBC1D62B4E356A191C58513FF3AF552D9CB5ADAE72` |
| `WagonPlanner.exe` | 1.2.2.85 | 2355208 | `73275A25911B15C2201747692D6EBAC391E8965DC301EE8DC76835617695584E` |
| `RemoteLib.dll` («Сервис пользователя») | 1.0.0.0 | 194048 | `77AC2E5596E597D1658BE976737B6074CA5F3782D6D4359FAB2C6EE4B0300300` |

Прежние артефакты: списки типов/строк и выборочный `decomp/` в `_nuff_tmp` (этап 4). Для R4 заново сняты полные проекты `decomp_WagonApp`, `decomp_WagonLib`, `decomp_PlannerLib`, `decomp_WagonPlanner` и точечные типы Remoting.

Литералы `GetActiveWagons` / `GetActiveWagonList` в дереве «Старая программа» найдены только в `RemoteLib` / `IRemoteLib` (и совпадающих копиях службы/НУД/УВП для интерфейса), **не** в `WagonApp.exe`, `WagonLib.dll`, `PlannerLib.dll`, `WagonPlanner.exe`.

## 2. Серверная цепочка `GetActiveWagonList` (CONFIRMED)

```
IRemoteLib.IGivingDepartureCall.GetActiveWagons(Guid sessionCode)
  → RemoteLib.GivingDeparture.GetActiveWagons(sessionCode)
    → GivingDepartureLoader.GetActiveWagonList(OracleConnection)
      → CommandText = "WagonPackage.GetActiveWagonList"
      → единственный параметр: result OUT Cursor
      → OracleDataAdapter.Fill(DataTable); без клиентской фильтрации строк
```

Клиентский Remoting-прокси (без найденного UI-caller):

```
RemoteSessionLib.Calls.RemoteGivingDeparture.GetActiveWagons()
  → IGivingDepartureCall.GetActiveWagons(_sessionCode)
```

Доказательства IL (`ilspycmd`):

- `IRemoteLib.IGivingDepartureCall` — объявление `DataTable GetActiveWagons(Guid sessionCode);`
- `RemoteLib.GivingDeparture.GetActiveWagons` — делегирует в loader
- `RemoteLib.GivingDepartures.GivingDepartureLoader.GetActiveWagonList` — `CommandText = "WagonPackage.GetActiveWagonList"`, только `result` cursor
- `RemoteSessionLib.Calls.RemoteGivingDeparture.GetActiveWagons` — тонкий proxy

Аргументы отбора на клиенте: **нет** (только `sessionCode` для выбора соединения). Поля курсора: клиент читает `DataTable` целиком, имён колонок в этом методе **нет** (UNKNOWN до выполнения/spec body).

## 3. UI НУВ: список в «Обозревателе» (CONFIRMED — это не Active)

| Шаг | Сборка / тип / метод | Факт |
| --- | --- | --- |
| Меню | `WagonApp.MainForm`, `bbiWagonExplorer`, caption «Обозреватель», право `Operations` 3 | открывает `WagonExplorerForm` |
| Панель кодов | `WagonExplorerForm` → `WagonCodeCollectionCtrl` → `WagonCodeCollection` | dock «Список вагонов» |
| Загрузка | `WagonLib.RailwayUnits.Groups.WagonCodeCollection.LoadData` | `Session.Current.DbManager.ExecuteNonQueryCursor("WagonPackage.GetWagonList")` |
| Подпись | `UpdateCaption` | `"Все вагоны (" + table.Rows.Count + ")"` |
| Колонки таблицы клиента | `InitTable` | `ID`, `CODE`, `WagonOwner`, `WagonType` |
| Фильтр клиента | `LoadData` / базовый `CodeCollection` | нет отсева строк после Fill; caption отражает полный курсор |

**CONFIRMED:** видимый список кодов обозревателя НУВ завязан на `GetWagonList`, не на `GetActiveWagonList`.

## 4. Call-site UI → `GetActiveWagonList` (не найден)

Охват поиска:

1. UTF-16/ASCII литералы `GetActiveWagons` / `GetActiveWagonList` по всем `*.dll`/`*.exe` под `Старая программа`.
2. Полная декомпиляция `WagonApp` 2.2.1.25, `WagonLib`, `PlannerLib`, `WagonPlanner` 1.2.2.85 — поиск `GetActive` / `GetActiveWagons` в `.cs`: совпадений **нет**.
3. Использование `Session.Current.GivingDeparture` в этих сборках есть (импорт XML, `PublishRegistryStateReports*`), но вызова `.GetActiveWagons` нет.

Ограничения:

- Отсутствие ссылки в этих сборках **не доказывает** отсутствие вызова из другого хоста, скрипта, необнаруженной поставки или поздней/динамической привязки.
- `ISessionLib.dll` содержит байтовую строку `GetActiveWagons` (метаданные/соседний heap); отдельного UI-типа с вызовом не восстановлено.
- Тела `PACKAGE BODY` Oracle не читались: серверный критерий «Active» **UNKNOWN** (предел локального анализа).

**Статус:** UI call-site в поставке НУВ ClickOnce **не подтверждён**. Remoting API существует.

## 5. Связь с `wagons.txt` (CONFIRMED: другой механизм)

Публикация списка на слежение/FTP **не** вызывает `GetActiveWagonList`:

| Путь | Источник кодов | Файл |
| --- | --- | --- |
| `GivingDeparture.PublishRegistryStateReports` | строки `WagonStateReport.WagonStateRecords`, поле `WAGONCODE` | `_tmpPath + "wagons.txt"`, затем FTP / опционально TIS |
| `PublishRegistryStateReportsThird` | `WagonStateReportsPkg.GetPublishWagonsThird`, поле `CODE` | тот же локальный `wagons.txt`, заливка на third-path |

UI-кнопка (PlannerLib): `WagonStateReportForm.UploadFile` / `WagonStateReportThirdForm.UploadFile` — `tsbUploadFile`, текст «Закачать на сервер», вызов `Session.Current.GivingDeparture.PublishRegistryStateReports*` (G9 частично закрыт; см. `08_GAPS_AND_UNCERTAINTIES.md`).

**CONFIRMED:** `wagons.txt` ≠ курсор `GetActiveWagonList`. Связь R3 synthetic `watched` с Active или с `wagons.txt` по IL **не доказана**.

## 6. Сводка статусов

| Утверждение | Статус |
| --- | --- |
| Oracle-имя `WagonPackage.GetActiveWagonList` вызывается из `GivingDepartureLoader` | CONFIRMED |
| Remoting: `IGivingDepartureCall.GetActiveWagons` → loader | CONFIRMED |
| Клиентский proxy `RemoteGivingDeparture.GetActiveWagons` | CONFIRMED |
| Меню/форма НУВ вызывает Active | UNKNOWN (в охвате НУВ не найдено) |
| Критерий отбора Active на сервере | UNKNOWN (body скрыт) |
| Колонки курсора Active | UNKNOWN |
| Обозреватель «Список вагонов» = `GetWagonList` | CONFIRMED |
| `wagons.txt` строится из Active | опровергнуто для найденных путей (CONFIRMED иной источник) |
| R3 watched ≡ Active | не подтверждено (как и до R4) |

## 7. Рекомендуемый следующий шаг

Локальный предел по call-site Active в клиенте НУВ достигнут. **Один следующий шаг:** получить на TEST Oracle (отдельный gate, read-only) текст/метаданные `WagonPackage.GetActiveWagonList` и рядом `GetWagonList` — `ALL_ARGUMENTS` / доступный SPEC или BODY, имена колонок курсора `result` — чтобы зафиксировать серверный критерий «Active» и отличие от списка обозревателя.

План сверки UI↔Oracle для «Вагоны на слежении» **пока не предлагается**: эталонная процедура для R3 Active не привязана к экрану; путать её с `GetWagonList` или `wagons.txt` нельзя без новых доказательств.
