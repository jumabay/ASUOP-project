# АСУ-ВП: архитектура сервиса

Источник: этапы [01](01-asuvp-hourly-import.md), [02](02-oracle-schema.md), [04](04-nuv-reverse.md). В этом документе цепочка импорта KZ и имена процедур перепроверены декомпиляцией `ilspycmd` 9.1 по `RemoteLib.dll` эталона «Сервис приложение» (только чтение). Исходников `.sln`/`.cs` в workspace нет. Потребитель среза — **НУВ**.

## Платформа

| Что | Значение |
|---|---|
| Хост | `ApplicationService.exe` 1.0.3.37, Windows Service |
| Журнал | `LogName=ASUVPService`, `SourceName=ApplicationService.exe` |
| Импорт и Remoting | `RemoteLib.dll` (PDB рядом) |
| FTP | `FtpLib.FtpClient` |
| Конфиг | `config.json` рядом с EXE, схема `dbtc` |
| Клиент Oracle | `System.Data.OracleClient`, вызовы `CommandType.StoredProcedure` |

Каталог эталона-импортёра: `Старая программа/Сервис приложение/ApplicationService/ApplicationService/`.

Каталог API для НУВ: `Старая программа/Сервис пользователя/ApplicationService/ApplicationService/`.

## Две копии одного EXE

Один и тот же набор сборок, разный `config.json`.

| Копия | Импорт срезов `ITaskStateReports*Enabled` | Сообщения 2610/2612/2654/2731 | Роль |
|---|---|---|---|
| Сервис приложение | все `True` | выключены, кроме приёмника 2612 | писатель FTP → `ImportPkg` |
| Сервис пользователя | все `False` | все `True` | Remoting для НУВ |

Подменять копии нельзя: у пользователя другая маска RU (`disl_cis*.txt` вместо `disl_operations*.txt`) и другой `FtpDirectoryPath` (`//Infoservice//` вместо `///tisservice//disl_vagons_tmp//`).

## Запуск

По этапу 1 (хост в этом проходе не декомпилировался повторно):

```text
ApplicationService.Program.Main
  → SettingsJsonManager.LoadConfig
  → AppService.OnStart
  → ModulesSearcher.ScanLibraries / StartModules
       (DLL с IRemoteCall и [ObjectUri], только если Uri+"Enabled" = true)
  → поток MethodRunPulseTimer → ITaskScheduler.Run()
```

Для среза KZ точка входа задачи — `RemoteLib.GivingDepartures.ImportStateReportsFtp.Run`:

1. Читает маску, каталоги FTP, флаги копии, `FtpSaveDaysFile`.
2. `InitEventLog` / `InitConnection` / `InitFtp` / `InitTimerSettings` / `CreateDirectory`.
3. `System.Threading.Timer(ReceiveFiles, due=0, period=_seconds*1000)`.

`InitTimerSettings` берёт `FtpStateReportsReceiveInterval` как **секунды**. Эталон: **45**. Копия пользователя: **60**. Это период опроса FTP, не «раз в час». Час — свойство файла, который кладёт внешняя система.

Повторный вход в `ReceiveFiles` отсекается флагом `_startedProcess` на экземпляре задачи. Каждая включённая задача — свой поток, свой таймер и свой пользователь Oracle (`ImportStateReportsFtp_UserName=asuvp_thread_01` и далее `_02`…`_32`).

## Компоненты

```mermaid
flowchart LR
  FTP[FTP_TIS_disl]
  AppSvc[ApplicationService_import]
  Ora[Oracle_dbtc_ImportPkg]
  Api[ApplicationService_user]
  Nuv[NUV_WagonApp]
  FTP --> AppSvc --> Ora
  Ora --> Api --> Nuv
```

- Писатель: только «Сервис приложение», классы `ImportStateReportsFtp*`.
- Читатель: НУВ `WagonApp` / `WagonPlanner` вызывает Remoting `IGivingDepartureCall` на «Сервис пользователя». `GivingDeparture.GetWagonStateReports` делегирует в `StateReports.LoadWagonStateReports`.
- НУВ сам FTP-срез не забирает (этап 4).

## Задачи среза

Класс, атрибут `[ObjectUri]`, маска эталона, тип среза, процедура строки, учётка.

| Класс | Uri | Маска | Тип | Строка | Учётка |
|---|---|---|---|---|---|
| `ImportStateReportsFtp` | `ITaskStateReportsImporter` | `DirSearchPatternKZ` = `disl_kz*.txt` | `STATEREPORTS_KZ_TYPE` не инициализирован, фактически **0** | поле `_saveReportLineProcedure` = `ImportPkg.SaveStateReportLineKz` | `asuvp_thread_01` |
| `ImportStateReportsFtpRu` | `ITaskStateReportsRuImporter` | `disl_operations*.txt` | **1** | `ImportPkg.SaveStateReportLineRu` | `asuvp_thread_02` |
| `ImportStateReportsFtpTisRu` | `ITaskStateReportsTisRuImporter` | `disl_vagons_cis*.txt` | **2** | наследник KZ, та же `SaveStateReportLineKz` | `asuvp_thread_03` |
| `ImportStateReportsFtpTisContainers` | `ITaskStateReportsContainerImporter` | `disl_containers_*.txt` | **3** | `ImportPkg.SaveStateReportLineContainer`, первая строка пропускается | `asuvp_thread_04` |
| `ImportStateReportsFtpTisThirdParties` | `ITaskStateReportsTisThirdPartiesImporter` | `disl_vagons_all_*.txt` | **16** | наследник KZ | `asuvp_thread_21` |
| `ImportStateReportsXls` | `ITaskStateReportsXlsImporter` | `*_disl_kz.xls` | **23** | не построчно: Excel → макрос типа импорта `221` → XML → `ImportPkg.SaveFileContent` + `ImportPkg.UpdateStateReportXml` | `asuvp_thread_32` |

Режим Remoting у импортёров: `WellKnownObjectMode.SingleCall`. Подтверждённый порт контракта подачи: `GivingDeparture`, Uri `IGivingDepartureCall`, `Port = 5502`. Остальные контракты задаются тем же атрибутом `ObjectUri.Port`; полный перечень портов в этом проходе не снимался.

`PlatformStateReports.PlatformStateReportsJob` к почасовому срезу вагонов не относится: `Run` не реализован (этап 1).

## Список на слежение

`GivingDeparture.PublishRegistryStateReports` собирает файл `_applicationPath + _tmpPath + "wagons.txt"` из загруженного `WagonStateReport.WagonStateRecords` и при `CopyTisServiceWagons=True` заливает его на `TisServiceWagonsPath` (`//tisservice//IN//`).

`PublishRegistryStateReportsThird` пишет тот же локальный `wagons.txt` из `WagonStateReportsPkg.GetPublishWagonsThird` и заливает на `TisServiceWagonsPathThird`.

На диске копии пользователя: `...\Сервис пользователя\...\tmp\wagons.txt`, 18334 строки. У копии приложения файл пустой. Это публикация списка, не хранение почасового среза.

## Журналирование

`ARemoteCall.WriteMessageSyslog` / `WriteErrorSyslog`: журнал событий Windows (`ASUVPService`) и Syslog, если `EnableSyslog=true`. Ошибки строки и файла пишутся туда и не пробрасываются наружу из `ProcessFile` / `SaveStateReportLine`.
