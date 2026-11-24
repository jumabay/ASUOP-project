# DT1 T2 — точечный запрос DBA: BODY `SaveStateReportLineKz`

Дата: **2026-11-24**.  
Контекст: [55_DT1_TIME_CONTRACT.md](../architecture/55_DT1_TIME_CONTRACT.md) §1.2 / §7 (**DT1-B2**, альтернатива **DT1-E**); T1 DONE @ `5fb1878` (PR #35).  
Статус артефакта: **WAITING** (BODY / paired kit export ещё не получены).  
**T2 CODE не начинать.** **DT1** и **R1-Q-TZ** остаются **OPEN**. APPROVALS не расширять этим документом.

Пароли / connection strings в документ, git и чат **не** включать.

---

## 1. Текст заявки (переслать DBA как есть)

Нужен **read-only экспорт исходного текста** (не выполнение) для доказательства записи параметра даты строки KZ в колонку ленты.

### Объект

| Item | Value |
| --- | --- |
| Schema / package | `DBTC.IMPORTPKG` |
| Subprogram | `SaveStateReportLineKz` |
| Need | **PACKAGE BODY** fragment for this procedure **and** any directly called private helpers that perform the `INSERT`/`UPDATE` into `DBTC.STATEREPORTRECORDS` (or equivalent write path) |

### Что показать в тексте

Путь параметра **`dislocationDate`** (в SPEC / `ALL_ARGUMENTS` также как `DISLOCATIONDATE`) → колонка **`STATEREPORTRECORDS.LOCATION_DATE`** (или явное опровержение: запись в другую колонку / не используется).

Достаточно вырезки BODY с:

1. списком формальных параметров процедуры (имена дат);
2. местом присвоения / bind в DML по `STATEREPORTRECORDS`;
3. именами колонок в DML, куда уходит `dislocationDate` (и, если видно в том же фрагменте, куда уходит `operationDate` — без расширения скоупа на другие пакеты).

### Способ передачи (предпочтительно)

1. Export BODY (или redact-safe file share) → аналитик кладёт в gitignored `docs/analysis/_oracle_tmp/` **или**
2. Временный `SELECT` на `ALL_SOURCE` / эквивалент **только** для `IMPORTPKG` BODY — отдельным grant; **не** постоянный Production allow-list.

### Запрещено этим запросом

- `EXECUTE` / вызов `SaveStateReportLineKz` и любых ImportPkg процедур  
- DDL / DML / изменение пакетов  
- Полный dump всех пакетов DBTC без нужды  
- Новые live business SELECT вне уже согласованного allow-list (этот запрос — **export текста**, не повтор CONTROL smoke)

### Критерий приёмки артефакта

Один из вариантов:

- **A (предпочтительно):** в BODY видно `dislocationDate` / `DISLOCATIONDATE` → `LOCATION_DATE` (или однозначный промежуточный assign → та же колонка);  
- **A′:** BODY однозначно показывает **другую** целевую колонку — тоже принимается как proof (mapping иначе).

---

## 2. Альтернатива, если BODY недоступен (DT1-E)

Без EXECUTE процедур импорта. Нужна **одна** согласованная тройка:

| # | Артефакт | Назначение |
| --- | --- | --- |
| 1 | Исходный файл `disl_kz*.txt` (тот, что реально импортирован) | field **[3]** = `dislocationDate` ([01_ASUVP_IMPORT_PIPELINE](../analysis/01_ASUVP_IMPORT_PIPELINE.md)) |
| 2 | Цепочка kit: `IMPORTFILES` → `STATEREPORTS` → `STATEREPORTRECORDS` для **этого** файла | имя файла / `IMPORTFILES_ID` / `REPORT_ID` |
| 3 | Строка(и) ленты контрольного вагона с `LOCATION_DATE` (и для сверки `GETTING_DATE`) | binary/string compare field[3] ↔ `LOCATION_DATE` |

### Уже известные якоря (не заменяют DT1-E)

| Якорь | Значение | Ограничение |
| --- | --- | --- |
| CONTROL wagon | `54787361` (`WAGON_ID` `119465`) | [P5_E_RO_SMOKE_EVIDENCE](P5_E_RO_SMOKE_EVIDENCE.md) |
| Day (GETTING axis) | `2026-11-24` | half-open slice |
| Sample `REPORT_ID` | `588495`, `588498`, `588503` | **нет** paired `IMPORTFILES.FILENAME` / исходного `disl_kz` в ASUVP |
| getting ≠ location (пример) | getting `2026-11-24 00:00`, location `2026-11-21 01:25` | доказывает **различие осей**, **не** file[3]→column |
| SPEC / `ALL_ARGUMENTS` | параметр `DISLOCATIONDATE` есть | **≠** column mapping ([55](../architecture/55_DT1_TIME_CONTRACT.md) §7) |

Кандидат файла вне ASUVP (не evidence до pairing): `LMS_TNC` … `disl_kz2026-11-17_08-21-18.txt` — **другой** час/сутки, чем CONTROL `2026-11-24`; без kit-цепочки **не** использовать как DT1-E.

Минимальный RO export для альтернативы (только если владелец отдельно разрешит scoped SELECT; **не** исполнять из этого документа):

```text
-- INTENT ONLY — do not run without new APPROVALS / allow-list
-- 1) IMPORTFILES by FILENAME = <disl_kz name>
-- 2) STATEREPORTS by IMPORTFILES_ID → REPORT_ID / UK
-- 3) STATEREPORTRECORDS by REPORT_ID + WAGON_ID (CONTROL) → LOCATION_DATE, GETTING_DATE
-- FETCH FIRST small; bind parameters; no EXECUTE
```

---

## 3. После получения артефакта (чеклист — сейчас WAITING)

| Check | Status now | Rule |
| --- | --- | --- |
| Маппинг `dislocationDate` → `LOCATION_DATE` | **WAITING** — артефакта нет | CONFIRMED только по BODY (A/A′) **или** DT1-E compare |
| Бизнес-смысл «факт дислокации» | **не CONFIRMED** | Даже при CONFIRMED column mapping ярлык «факт» требует отдельного product/legacy UI evidence ([55](../architecture/55_DT1_TIME_CONTRACT.md) §1.2 / DT1-F); различие getting≠location **не** закрывает ярлык |
| DT1 / R1-Q-TZ | **OPEN** | T2 mapping proof ≠ закрытие DT1 |

Пока WAITING: **доказательства маппинга нет**; повторный поиск по репозиториям и live вне allow-list **не** проводить.

---

## 4. Конкретное предложение T2 (после proof — не CODE сейчас)

Если маппинг станет **CONFIRMED** (BODY или DT1-E):

1. **Отдельная** строка APPROVALS: `DT1 T2 CODE (locationTime as fact)` — сейчас **DENIED**.  
2. CODE (после approve): additive A1 `locationTime` ← `LOCATION_DATE`; `snapshotTime` / `dislocationTime` alias **без изменений** (T1); day filter остаётся на `GETTING_DATE`.  
3. UI: колонка «Время дислокации» **только** если owner подтвердил бизнес-ярлык; иначе raw/`INFERRED` per [55](../architecture/55_DT1_TIME_CONTRACT.md) §2.2 — не подписывать «факт» самовольно.  
4. Тесты: alias parity T1; `locationTime` null-safe; нет remap `dislocationTime`→LOCATION.  
5. **Не** закрывать DT1 / R1-Q-TZ этим срезом; **не** auto-merge.

Если BODY покажет **другую** колонку — остановить предложение «locationTime = LOCATION_DATE» и пересмотреть контракт 55 до CODE.

---

## 5. Out of scope

- T2 / любой Java·React CODE  
- EXECUTE ImportPkg / повторный P5-E CONTROL live  
- Расширение APPROVALS этим файлом  
- Закрытие DT1, R1-Q-TZ, G31, NK1  
- Create RAILDB / Flyway / import writer
