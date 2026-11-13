# АСУ-ВП: зависимости Oracle

Источник: [02](02-oracle-schema.md), [03](03-oracle-plsql-jobs.md), [04](04-nuv-reverse.md). Имена ниже — `CommandText` из декомпиляции `RemoteLib.dll` эталона. Схема в config: `dbtc`. Словарь `ALL_SOURCE` / `ALL_TABLES` не открывался. Файлов `.sql`/`.pck` в workspace нет. Секреты подключений не копировались.

## Порядок вызовов текстового среза KZ

Один файл, без клиентской транзакции. Каждый `ExecuteNonQuery` фиксируется так, как это делает сессия Oracle для отдельного вызова (autocommit провайдера `System.Data.OracleClient` в коде не переопределён).

| Шаг | Метод C# | CommandText | Параметры |
|---|---|---|---|
| 1 | `IsImportedFile` | `Importpkg.IsImportedFile` | `filename`, `type` (`0` для KZ), return `result`; истина, если `1` |
| 2 | `SaveImportFile` | `ImportPkg.SaveImportFile` | `filename`, `importdate=DateTime.Now`, `type`, `filesize`, return `result` |
| 3 | `SaveStateReport` | `ImportPkg.SaveStateReport` | `importFilesId`, return `result` |
| 4 | `SaveStateReportLine` × N | `ImportPkg.SaveStateReportLineKz` | `stateReportId` и поля строки, см. конвейер |
| 5 | `UpdateStateReport` | `ImportPkg.UpdateStateReport` | `id` = идентификатор среза |

Ошибка шагов 2–5 логируется внутри метода. Шаги 2 и 3 при ошибке возвращают `0` и не прерывают `ProcessFile`. Шаг 4 пропускает строку. Шаг 5 при ошибке только логируется: строки уже отправлены.

Идемпотентность на стороне клиента — только пара «имя файла + тип» через `IsImportedFile`. Хэша содержимого нет. При исключении самой проверки метод возвращает `false`, то есть файл будет пытаться импортироваться снова.

## Процедуры по каналам

| Класс | Тип | Запись строки / содержимого | Завершение |
|---|---|---|---|
| `ImportStateReportsFtp` | 0 | `ImportPkg.SaveStateReportLineKz` | `ImportPkg.UpdateStateReport(id)` |
| `ImportStateReportsFtpRu` | 1 | `ImportPkg.SaveStateReportLineRu` | та же `UpdateStateReport` базового класса |
| `ImportStateReportsFtpTisRu` | 2 | KZ-процедура базового класса | то же |
| `ImportStateReportsFtpTisThirdParties` | 16 | KZ-процедура базового класса | то же |
| `ImportStateReportsFtpTisContainers` | 3 | `ImportPkg.SaveStateReportLineContainer` | то же |
| `ImportStateReportsXls` | 23 | `ImportPkg.SaveFileContent` внутри `BeginTransaction` | `ImportPkg.UpdateStateReportXml` с `stateReportId` и `importFileId` |

XLS дополнительно читает объект типа импорта `ImportStateReportXlsImportTypeId` (эталон `221`) через `DBManager.LoadObject` и гоняет макрос `Importer.RunMacros`. Это не построчный TXT.

Общие для текстовых каналов: `ImportPkg.SaveImportFile`, `ImportPkg.SaveStateReport`, `Importpkg.IsImportedFile`. Регистр `Importpkg` / `ImportPkg` в коде разный; для Oracle без кавычек это один объект.

Поля RU (`SaveStateReportLineRu`): `wagonCode`, `dislocationStationCode`, `trainIndex`, `weigthState`, `dislocationDate`, `operationCode`, `destinationStationCode`, `receiverCode`, `cargoCode`, `wagonType`, `wagonOwner`.

## Чтение среза (НУВ и тот же API)

| Метод | CommandText | Кто вызывает |
|---|---|---|
| `StateReports.LoadWagonStateReports` | `StateReportPackage.GetWagonStateReports` | `GivingDeparture.GetWagonStateReports` → Remoting `IGivingDepartureCall` (порт 5502) |
| публикация списка третьих сторон | `WagonStateReportsPkg.GetPublishWagonsThird` | `GivingDeparture.PublishRegistryStateReportsThird` |

Параметры чтения среза вагона: `wagon_id`, `beginDate` (00:00:00), `endDate` (23:59:59), курсор `result`. НУВ и УВП ходят в этот API, а не в FTP (этапы 4–5).

Другие пакеты схемы, на которые ссылаются DLL, но которые не пишут почасовой `disl_kz`, перечислены в [02-oracle-schema.md](02-oracle-schema.md): `WagonPackage`, `GivingDeparturePackage`, `GivingPkg`, `ApproachPkg`, `RepairsPkg`, `WarehousePkg`, `AsoupPkg`, `WorkListsPkg.UpdateStateReport`.

Имена, похожие на создание серверных заданий, без тела (этап 3): `ImportPkg.CreateJobUpdateGivingDeparture`, `ImportPkg.CreateJobUpdateRegistryMain`. В классе `ImportStateReportsFtp` этих вызовов нет. Побочный эффект возможен только внутри `SaveStateReportLine*` / `UpdateStateReport`.

## Чего в репозитории нет

- Тексты пакетов, триггеры, индексы, партиции, имена таблиц.
- Подтверждение, что перечисленные процедуры есть на тестовой `dbtc`.
- Правило «вставлять строку, если вагон не двигался».
- Момент, когда `IsImportedFile` начинает возвращать 1.
- Объём и срок хранения срезов. Оценка «18k × 24 × N дней» без словаря не считается.
