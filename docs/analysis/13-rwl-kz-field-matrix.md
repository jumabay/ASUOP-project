# 13. Матрица RWL API ↔ KZ FTP (20/21 полей)

Дата: 2026-11-16. Источники: `RWL-API-Rus.docx`, [12-rwl-api-intake.md](12-rwl-api-intake.md), [01_ASUVP_IMPORT_PIPELINE.md](01_ASUVP_IMPORT_PIPELINE.md), R0 `KzDislocationLine`, fixtures `tools/rwl-import-proto` (синтетические номера). Live RWL **не** вызывался.

Статусы: **MATCH** | **PARTIAL** | **RWL_ONLY** | **KZ_ONLY** | **UNKNOWN**.

## 1. Возможности RWL (capability matrix)

| Потребность | request_type / механизм | Статус | Комментарий |
| --- | --- | --- | --- |
| Список всех отслеживаемых вагонов пользователя | `get_user_vagons` | CONFIRMED (doc) | опц. `group_id` |
| Список по группе | `get_group_vagons` | CONFIRMED (doc) | group login/password |
| Список по client_ids | `view_vagons` | CONFIRMED (doc) | |
| Поиск по номерам | `search_vagons` | CONFIRMED (doc) | `search_in_archive`, `full_history` |
| Sync списка | `sync_vagons`, `sync_vagons_territory` | CONFIRMED (doc) | не эквивалент часового среза |
| Текущая дислокация одного вагона | `view_vagon` | CONFIRMED (doc + proto fixture parse) | |
| Текущая дислокация набора | `get_user_vagons` / `view_vagons` (`all_operations=n` → последняя операция) | CONFIRMED (doc) | bulk limit UNKNOWN |
| История перемещений | `view_vagon_history` (+ group/by_id) | CONFIRMED (doc) | `days_limit`, `calendar_date`, `date_type`; bulk hour UNKNOWN |
| Несколько операций | `all_operations=y` | CONFIRMED (doc) | «все последние операции» — глубина UNKNOWN |
| Окно по минутам | `added_last_minutes` | CONFIRMED (doc) | «только с `all_operations`» (текст обрезан в extract — уточнять live); **не** доказан как full-hour kit |
| Ремонты | `view_vagon_repairs`, 6.8 | CONFIRMED (doc) | формат полей — отдельно; ≠ `RepairsPkg` |
| Грузы | `cargo_name`, `etsng_code`, `weight`, `loaded` в `position` | CONFIRMED (doc/fixture) | ETSNG ≠ обязательно `cargoCode` KZ |
| Станции | `current_position_code`, from/to codes | CONFIRMED | 6-значные коды в примерах |
| Изменения за период | history / `added_last_minutes` | PARTIAL | нет доказанного «час файла» |
| Полный часовой срез ~18 000 | — | **UNKNOWN** | нет request_type «hour kit»; лимиты/квоты UNKNOWN |

## 2. KZ поля 0…20 ↔ RWL

| # | KZ (ImportPkg / R0) | RWL XML (doc/fixture) | Статус | Примечание |
| --- | --- | --- | --- | --- |
| 0 | `wagonCode` | `vagon_info/vagon_no` | MATCH | |
| 1 | `dislocationStationCode` | `position/current_position_code` | MATCH | |
| 2 | `trainIndex` | `position/train_index` | MATCH | |
| 3 | `dislocationDate` | `utc_datetime` / local date fields | PARTIAL | KZ строка даты (формат файла); RWL предпочитает UTC `yyyy-MM-dd HH:mm:ss` |
| 4 | `operationCode` | `operation_asoup_code` **или** `operation_code` | PARTIAL | ASOUP ближе к коду операции ASUVP; текстовый `operation_code` другой |
| 5 | `destinationStationCode` | `vagon_info/to_code` | PARTIAL | маршрут vs текущая дислокация |
| 6 | `departureStationCode` | `vagon_info/from_code` | PARTIAL | |
| 7 | `receiverCode` | — | KZ_ONLY | в извлечённом RWL position/vagon_info **не** найдено 1:1 |
| 8 | `senderCode` | — | KZ_ONLY | |
| 9 | `wagonType` | specs? `add_vagon_specs` | UNKNOWN | без live/sample specs |
| 10 | `wagonOwner` | — | KZ_ONLY / UNKNOWN | |
| 11 | `nodCode` | — | KZ_ONLY | |
| 12 | `weigthState` | `loaded` (y/n) | PARTIAL | семантика «состояние веса» ≠ флаг loaded |
| 13 | `workState` | — | KZ_ONLY | |
| 14 | `docNumber` | — | KZ_ONLY | |
| 15 | `cargoCode` | `etsng_code` | PARTIAL | разные справочники возможны |
| 16 | `cargoWeigth` | `weight` | PARTIAL | единицы/тип UNKNOWN |
| 17 | `capacity` | — | KZ_ONLY | |
| 18 | `messageCode` | — | KZ_ONLY | |
| 19 | `operationDate` | `utc_datetime` / position dates | PARTIAL | одна метка vs две в KZ |
| 20 | `containers` (21-field) | `vagon_info type=container` / `platform_no` | PARTIAL | другой модель контейнера |

### RWL-only (нет в KZ line)

`current_position` (имя), lat/lon, `train_number`, `operation` (текст), `operation_id` (тип, не event id), group/client fields, days_wo_*, ETA, repairs block, roads multilang, …

## 3. Идентичность событий и повторы

| Тема | KZ / R0 | RWL |
| --- | --- | --- |
| Kit idempotency | `(fileName, type)` | нет файла; нужен intake key запроса |
| Строка события | нет PK в DBTC tape | гипотеза proto `event_key` = wagon\|utc\|station\|asoup\|train_index + `content_hash` — **не контракт RWL** |
| Полный час | все строки файла, включая неподвижные | UNKNOWN: текущий snapshot ≠ доказательство «все 18k с часом» |
| Инкремент | отдельный канал RU и др., не delta-flag в ленте | `added_last_minutes` / history — другой контракт |

## 4. Временные зоны

| Источник | Поведение |
| --- | --- |
| KZ FTP | даты как в файле; TZ бизнес-логики R1-Q-TZ open |
| RWL | `utc_datetime` рекомендован; также Latvia/Riga unix stamps в doc | хранить UTC + исходную строку |

## 5. Заключение: замена FTP

**Не подтверждена.** RWL — сильный кандидат на **дополнение** (текущее положение / слежение / точечная история). Для primary full-hour import нужны: лимиты, стабильный bulk ~парка, полевая паритетность B1, семантика «неподвижные вагоны в часе». До этого FTP/`FtpIntake` обязателен как запасной/основной контур часа.
