# 25. Матрица миграции DBTC → RAILDB (окно 12 месяцев)

Статус: **проектирование**. Не выполнять выгрузки и загрузки. Источники фактов: [02_ORACLE_TABLES.md](../analysis/02_ORACLE_TABLES.md), [06_DATA_STORAGE_STRATEGY.md](06_DATA_STORAGE_STRATEGY.md), [11-nuff-active-wagon-list.md](../analysis/11-nuff-active-wagon-list.md), [ADR-0009](decisions/ADR-0009-raildb-primary-store.md).

## 1. Правила окна

| Правило | Значение |
| --- | --- |
| Cutoff | `cutover_ts − 12 calendar months` (динамически от фактической даты переключения) |
| Ориентир на дату плана | ~16.11.2025 … 16.11.2026 при cutover 16.11.2026 |
| История ≥ cutoff | переносится в `RAILDB` |
| История < cutoff | остаётся в `DBTC` (архив) |
| Актуальные сущности | **FULL**, независимо от даты создания |
| Открытые операции / остатки | **FULL / OPEN**, независимо от давности |
| Справочники | **REF_FULL** (или минимальный набор для FK) |
| Bootstrap | для каждого вагона — последнее известное состояние **≤ cutoff**, если иначе «дыра» на старте окна |
| DBTC | read-only для мигратора; без DDL/DML |
| Идемпотентность | повтор батча не создаёт дублей (natural key / staging keys) |

Теги фильтра: `12M` | `FULL` | `OPEN` | `REF_FULL` | `BOOTSTRAP` | `ARCHIVE_DBTC` | `SKIP`.

## 2. Список слежения (watched) — состояние исследования

| Кандидат | Статус | Вывод для миграции |
| --- | --- | --- |
| Отдельная таблица «слежение» в `DBTC` | **не найдена** в словаре | не на что маппить 1:1 |
| `WagonPackage.GetActiveWagonList` | Remoting API **CONFIRMED**; UI НУВ call-site **не найден**; тело/предикат **UNKNOWN** | кандидат «Active»; нужен read-only снимок курсора на cutover |
| `WagonPackage.GetWagonList` | Обозреватель НУВ «Все вагоны» | это **весь реестр**, не «слежение» |
| `wagons.txt` (~18 334 кода) | Публикация FTP из document state-reports; **≠** Active | кандидат «publish list»; другой смысл |
| R3 synthetic `watched.json` | Тестовая фикстура | **не** источник миграции |

**Рабочая политика миграции (до закрытия G31):**

1. Перенести **все** актуальные вагоны (`WAGON` **FULL**) в `RAIL_WAGON`.
2. Членство «на слежении» зафиксировать отдельной таблицей `RAIL_WAGON_WATCHED` **снимком на cutover** из **явно выбранного** источника после approve:
   - предпочтительно: курсор `GetActiveWagonList` (если бизнес подтвердит, что это «действующий список»);
   - альтернатива: актуальный `wagons.txt` / document publish list;
   - запрещено молча подставлять synthetic R3.
3. Пока источник не утверждён — миграция membership = **BLOCKED**; реестр вагонов не блокируется.

## 3. Матрица объектов

### 3.1. Справочники — `REF_FULL`

| DBTC | Критерий | Поле даты | → RAILDB (logical) | Confidence |
| --- | --- | --- | --- | --- |
| `STATIONS` | REF_FULL | — | `RAIL_STATION` | CONFIRMED name |
| `CUSTOMERS` | REF_FULL | — | `RAIL_CUSTOMER` | CONFIRMED |
| `OWNERS` / `WAGONOWNERS` | REF_FULL | — | owner ref | CONFIRMED/partial |
| `WAGONTYPES`, `WAGONMODELS`, `WAGONSTATES`, `WAGONSTATUSES` | REF_FULL | — | type/state refs | CONFIRMED names |
| `OPERATIONS`, `CARGOTYPES`, `CARGOKTZ` | REF_FULL | — | ops/cargo refs | CONFIRMED |
| `REPAIRTYPES`, `REPAIRPLANTS` | REF_FULL | — | repair refs | CONFIRMED |
| `PARTTYPES`, `WAREHOUSETYPES`, `WAREHOUSES` | REF_FULL | — | parts/wh refs | CONFIRMED |
| `USERS` (legacy) | TBD mapping to `RAIL_APP_USER` | — | app users | UNKNOWN mapping |

### 3.2. Реестр и текущее состояние — `FULL`

| DBTC | Критерий | Date field | → RAILDB | Notes |
| --- | --- | --- | --- | --- |
| `WAGON` | FULL | N/A (snapshot) | `RAIL_WAGON` + `RAIL_WAGON_CURRENT` | ~50k; location fields current |
| `GIVING` | FULL | N/A | current giving | ~9k |
| `WAGONAPPROACH` | FULL | N/A | current approach | ~27k |
| `CURRENTPARTS` | FULL | N/A | installed parts | ~168k |
| `WAREHOUSEDETAILS` | FULL stock | N/A | `RAIL_STOCK_BALANCE` | grain UNKNOWN |
| `STATEREPORTS` where `LAST` | FULL markers | — | kit/current flags | body UpdateLast UNKNOWN |

### 3.3. История дислокации / комплекты — `12M` + `BOOTSTRAP`

| DBTC | Критерий | Date field | → RAILDB | Notes |
| --- | --- | --- | --- | --- |
| `STATEREPORTRECORDS` | 12M | **`GETTING_DATE`** (CONFIRMED index axis) | `RAIL_DISL_EVENT` | ~157,7M rows/year order; **no PK** |
| same | BOOTSTRAP | last row per wagon with `GETTING_DATE` ≤ cutoff | seed current/history edge | обязательно |
| `STATEREPORTS` | 12M parents | **`GETTING_DATE`** / `OPERATING_DATE` | `RAIL_IMPORT_KIT` (header part) | PK `ID` |
| `IMPORTFILES` | 12M | **`IMPORTDATE`** | kit identity | UK business `(FILENAME, TYPE)` via client rule |
| `STATEREPORTRECORDS_TMP` | SKIP | — | — | ephemeral |
| `IMPORTFILESCONTENT_TMP` | SKIP | — | — | ephemeral |
| Hour calendar | NEW | — | `RAIL_HOUR_EXPECTATION` | в DBTC таблицы нет |

Полный vs инкрементальный срез: в `STATEREPORTRECORDS` **нет** флага full/delta (CONFIRMED словарь). Канал/тип файла (`KIT_TYPE` 0 KZ, 1 RU, …) сохранять; не схлопывать полные часы в дельту.

### 3.4. Ремонты — `12M` + `OPEN`

| DBTC | Критерий | Date field | → RAILDB | Notes |
| --- | --- | --- | --- | --- |
| `REPAIRS` | 12M closed ∪ OPEN | `beginingdate` / `endingdate` (trigger cols CONFIRMED) | `RAIL_REPAIR` | open predicate (null end / `CURRENTREPAIR_ID`) **UNKNOWN** |
| Types linked to TR/KR/DR | REF | — | via `REPAIRTYPES` | types 0 ДР … 7 ТР1 … CONFIRMED in pkg docs |

### 3.5. Склад / документы — `FULL` остатки + `12M` документы

| DBTC | Критерий | Date field | → RAILDB | Notes |
| --- | --- | --- | --- | --- |
| `WAGONPARTS` | FULL as needed by FK | — | `RAIL_PART` | UK partnumber/type UNKNOWN |
| `VIRTUALWAREHOUSEDETAILS` | OPEN/FULL queue | — | TBD | ~659k; writer UNKNOWN |
| Document headers (procurement/arrival/transfer/writeoff/…) | 12M | `DELIVERYDATE` where known | `RAIL_WH_DOCUMENT*` | иные date cols UNKNOWN |
| Line tables | 12M with parents | — | details | часть имён UNKNOWN |

### 3.6. Архив — остаётся в DBTC (`ARCHIVE_DBTC`)

| DBTC | Почему |
| --- | --- |
| `STATEREPORTRECORDS` / parents с датой < cutoff | вне окна |
| `WAGONAPPROACHARCHIVE`, `GIVINGARCHIVE`, `WAGONDOWNTIME` | явные архивы; огромные |
| `WAGONMILEAGE` старше политики | date col name UNKNOWN → фильтр блокируется до словаря |
| Старые document slice `WAGONSTATEREPORTSRECORDS` | вне окна / отдельное решение |

### 3.7. Ошибки импорта

В `DBTC` отдельной таблицы ошибок разбора FTP **не документировано** (syslog/Event Log). В `RAILDB` ошибки — новые таблицы ops (`RAIL_IMPORT_ERROR`, quarantine), не миграция из DBTC.

## 4. Порядок загрузки (логический)

1. REF_FULL  
2. `WAGON` FULL + current satellites  
3. OPEN repairs / stock / parts  
4. BOOTSTRAP dislocation edge at cutoff  
5. 12M kits (`IMPORTFILES` → `STATEREPORTS`)  
6. 12M `STATEREPORTRECORDS` batches  
7. 12M warehouse documents  
8. Watched membership snapshot (когда источник утверждён)  
9. Delta loop до cutover  
10. Final reconcile + freeze

## 5. Сверка (минимум)

| Проверка | Метод |
| --- | --- |
| Counts | по суткам `GETTING_DATE` / по `KIT_TYPE` |
| Checksums | hash выборки ключей (не PII/секреты) |
| Current wagon | сравнение полей location/state sample |
| Open repairs | set compare |
| Stock | qty per warehouse/part sample |
| Orphan FK | = 0 в RAILDB |
| Kit idempotency | повтор `(filename, type)` → 1 kit |
| Watched | count + sample codes vs выбранный источник |

## 6. Delta-синхронизация

- Watermark per table (`RAIL_MIGRATION_WATERMARK`).
- Resume-safe batches.
- До cutover при конфликте ключа: **побеждает DBTC**.
- После cutover: writer только `RAILDB`; delta из DBTC останавливается.

## 7. OPEN вопросы (блокируют финальный DDL/SQL)

1. Утверждение источника watched (G31).  
2. Natural key `STATEREPORTRECORDS`.  
3. `GETTING_DATE` vs `LOCATION_DATE` как ось часа.  
4. Open-repair predicate.  
5. Полный column dump REPAIRS / warehouse / mileage.  
6. Нужен ли перенос `STATEREPORTSWAGON` (derived, job broken).  
7. Права SELECT migration user: tables vs packages only.
