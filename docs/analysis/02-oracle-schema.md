# Этап 2. Структура Oracle

## Что исследовано

Имена пакетов и процедур, которые вызывают `RemoteLib.dll`, `WagonLib.dll`, `WarehouseLib.dll` эталона «Сервис приложение» (строки UTF-16 и декомпилированные `CommandText`). Схема в config: `dbtc`.

### Живой каталог (добавлено)

Сессия на **`tc-asuvp-03:1521/RAC.WORLD`**, пользователь `ASUVP_SERVICE`, `CURRENT_SCHEMA=DBTC`.  
Словарь `ALL_*` прочитан (только SELECT). Результаты:

- [02_ORACLE_TABLES.md](02_ORACLE_TABLES.md) — таблицы, ключи, индексы, объёмы (статистика), привязка к компонентам  
- [02_ORACLE_RELATIONSHIPS.md](02_ORACLE_RELATIONSHIPS.md) — FK-карта, writers/readers, current/slice/event  
- [02_ORACLE_PERFORMANCE.md](02_ORACLE_PERFORMANCE.md) — узкие места, jobs, отсутствие партиций  

Кратко: Oracle **12.2.0.1.0**; в `DBTC` **251** таблица, **~29** пакетов (58 object PACKAGE/BODY), **96** view; тела пакетов в `ALL_SOURCE` **не видны**; партиций **нет**; лента срезов — `STATEREPORTRECORDS` (~1.2×10⁹ строк по `NUM_ROWS`).

## Подтверждённые факты

Клиенты и сервис не содержат имён таблиц в вызовах среза. Контракт с БД — пакеты:

| Пакет | Вызовы, найденные в DLL | Назначение по именам |
|---|---|---|
| `ImportPkg` | `SaveImportFile`, `SaveStateReport`, `SaveStateReportLineKz`, `SaveStateReportLineRu`, `SaveStateReportLineContainer`, `UpdateStateReport`, `UpdateStateReportXml`, `IsImportedFile`, `SaveGivingReportLine*`, `SaveRegistryMainReportLine`, `Save*ManufactoryReportLine*`, `CreateJobUpdateGivingDeparture`, `CreateJobUpdateRegistryMain` | приём файлов и срезов |
| `StateReportPackage` | `GetWagonStateReports`, `GetStateReports`, `DeleteReport` | чтение и удаление срезов |
| `WagonPackage` | `GetWagonList`, `GetWagonByCode`, `GetActiveWagonList`, `GetContainerList`, `GetMonthDistances`, `GetWagonsInventoryDataOLAP` | списки вагонов |
| `GivingDeparturePackage` / `RemoteGivingDeparturePackage` / `GivingPkg` / `ApproachPkg` | погрузка списков подачи/уборки и подхода | НУВ / планировщик |
| `RepairsPkg` / `RepairBudgetPkg` | ремонты, пробег, бюджет, `ImportWagonParts2731` | УВП |
| `PartsPackage` | `GetHistoryState`, `LoadPartHistory` | детали |
| `WarehousePkg` | Register/Unregister документов склада, колёсных пар, ремонта | НУД |
| `AsoupPkg` | `LoadWagonsMessages2610/2612`, `SetSendedWagon` | сообщения АСОУП |
| `WorkListsPkg` | `UpdateStateReport` | наряд / срез |
| `DownTimePkg` | `GetWagonDownTime` | простои |
| `AdminPkg` | `LockUser`, `UnLockUser`, `GrantObjects` | доступ |
| `sys_TransportPkg` | `SendDocument`, `RejectDocument` | транспорт документов |

На `tc-asuvp-03` пакеты из списка **присутствуют и VALID** в `ALL_OBJECTS` owner `DBTC` (включая `IMPORTPKG`, `STATEREPORTPACKAGE`, `WAGONPACKAGE`, …). Spec `ImportPkg` / `StateReportPackage` читаются из `ALL_SOURCE`; body — нет.

## Бизнес-логика

Почасовой срез хранится как `IMPORTFILES` → `STATEREPORTS` → `STATEREPORTRECORDS` и читается через `StateReportPackage`. Текущее положение также отражено на `WAGON` и в `STATEREPORTSWAGON`. Оценка «18k × 24 × N дней» согласуется по порядку с ~1.2×10⁹ строк в `STATEREPORTRECORDS` (статистика, не COUNT).

## Связи

Все перечисленные пакеты — схема `dbtc` / `DBTC`. АСУОП к этой схеме не относится (MSSQL `terminal`). Детали FK — в [02_ORACLE_RELATIONSHIPS.md](02_ORACLE_RELATIONSHIPS.md).

## Неизвестное

- Текст `PACKAGE BODY` (`ImportPkg`, триггерная логика записи в `WAGON`).  
- Точное правило full vs delta внутри одного файла среза.  
- Актуальный `COUNT(*)` (не согласовывался).

## Риски

- Хост `tc-asuvp-03` совпадает с активным `ConnectionIndex` сервиса — сессия была только на чтение словаря.  
- Без партиций на `STATEREPORTRECORDS` любой новый writer срезов конкурирует с НУВ/отчётами.

## Следующий этап

При необходимости — чтение body под учёткой с правом на `ALL_SOURCE` body / unwrap, и точечный согласованный `COUNT(*)` на крупных таблицах.
