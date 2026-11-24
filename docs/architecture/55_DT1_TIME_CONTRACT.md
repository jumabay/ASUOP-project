# 55. DT1 — контракт времени A1 (`snapshotTime` / `locationTime`)

Дата: **2026-11-24**.  
Статус: **T1 APPROVED (CODE)** — `snapshotTime` additive; **T2 DENIED** until BODY or file↔Oracle.  
База: `main` после P5-E (`ad1b8e7`).  
Скоуп: семантика `GETTING_DATE` / `LOCATION_DATE` + REST/UI контракт для **суток (A1)**.  
**Вне скоупа:** `currentState`, registry/watched, импорт/writer, Create RAILDB, расширение RO allow-list, live-пробы.

Связано: [37](37_P3C0_SEMANTICS_RESEARCH.md), [39](39_P3C1_SEMANTICS_FINDINGS.md), [49](49_NK1_DT1_G31_OFFLINE_RESEARCH.md), [23](23_R1_A1_A2_IMPLEMENTATION_PLAN.md), [40](40_P4_0_RAILDB_STORAGE_CONTRACTS.md), [41](41_P4_0_R1_R3_FIELD_MAPPING.md), evidence [P5_E_MAPPING_FOLLOWUP](../agents/P5_E_MAPPING_FOLLOWUP.md), [P5_E_JDBC_CONTROL_EVIDENCE](../agents/P5_E_JDBC_CONTROL_EVIDENCE.md), pipeline [01_ASUVP_IMPORT_PIPELINE](../analysis/01_ASUVP_IMPORT_PIPELINE.md).

### Вердикт (кратко)

| Поле Oracle | Назначение в контракте | Статус назначения |
| --- | --- | --- |
| `STATEREPORTRECORDS.GETTING_DATE` | **`snapshotTime`** — время среза / ось суток | **CONFIRMED** (ось фильтра + UI «Время среза») |
| `STATEREPORTRECORDS.LOCATION_DATE` | кандидат на **`locationTime`** | mapping file→колонка **INFERRED**; «факт дислокации» **не CONFIRMED** |
| API `dislocationTime` (текущий) | = `GETTING_DATE` (P5-E) | **PROVISIONAL alias** → при CODE = `snapshotTime`; смысл **не** менять молча |

**DT1 не закрыт.** Различие значений на CONTROL **не** доказывает назначение `LOCATION_DATE`.

---

## 1. Цепочка происхождения (с доказательствами)

### 1.1 `GETTING_DATE` → `snapshotTime`

```text
файл часа (kit) → ImportStateReportsFtp.ProcessFile
  → ImportPkg.SaveImportFile / SaveStateReport(importFilesId)
  → STATEREPORTS.GETTING_DATE (шапка комплекта)
  → SaveStateReportLineKz(stateReportId, …) × каждая строка файла
  → STATEREPORTRECORDS.GETTING_DATE
  → A1 filter + API snapshotTime
```

| Шаг | Утверждение | Grade | Evidence |
| --- | --- | --- | --- |
| C# не передаёт отдельный «getting» в строке | В `SaveStateReportLineKz` из файла уходят `dislocationDate` [3] и `operationDate` [19]; параметра getting в списке C# нет | **CONFIRMED** | [01_ASUVP_IMPORT_PIPELINE](../analysis/01_ASUVP_IMPORT_PIPELINE.md) § формат строки |
| Шапка среза | `SaveStateReport(importFilesId)` → `STATEREPORTS`; колонка `GETTING_DATE` TIMESTAMP(2) | **CONFIRMED** | [03_ORACLE_PACKAGES](../analysis/03_ORACLE_PACKAGES.md) §3.1; [36](36_P3B_PARTIAL_DICTIONARY_FINDINGS.md) §3.1 |
| Колонка на ленте + индексы | `GETTING_DATE` на `STATEREPORTRECORDS`; `IDX_REC_GETTING_DATE`, `(WAGON_ID, GETTING_DATE)` | **CONFIRMED** | [36](36_P3B_PARTIAL_DICTIONARY_FINDINGS.md) §3.2; [02_ORACLE_TABLES](../analysis/02_ORACLE_TABLES.md) |
| Ось суток / 12м / A1 | Фильтр half-open по `GETTING_DATE` only | **CONFIRMED** (policy) | [23](23_R1_A1_A2_IMPLEMENTATION_PLAN.md); [49](49_NK1_DT1_G31_OFFLINE_RESEARCH.md) §2; P5-E SQL/UI |
| Точное тело: откуда PL/SQL пишет tape.GETTING | SYSDATE / копирование с `STATEREPORTS` / `IMPORTDATE` / час из имени файла | **UNKNOWN** | тело `ImportPkg` не читалось ([08_GAPS](../analysis/08_GAPS_AND_UNCERTAINTIES.md)) |
| Выравнивание tape vs header | час `srr.GETTING` = час `sr.GETTING` | **UNKNOWN** (probe DT1.3 не исполнялся) | [39](39_P3C1_SEMANTICS_FINDINGS.md) §3; [sql/dba/p3c/DT1_*](sql/dba/p3c/DT1_date_semantics_probes.sql) |

**Вывод для контракта:** `snapshotTime` ← `GETTING_DATE` как **время среза (kit/day axis)**. Не называть «фактом дислокации».

### 1.2 `LOCATION_DATE` → кандидат `locationTime`

```text
disl_kz*.txt field[3] «dislocationDate»
  → C# SaveStateReportLine → ImportPkg.SaveStateReportLineKz(…, dislocationDate, …)
  → ? STATEREPORTRECORDS.LOCATION_DATE
  → (будущий) locationTime
```

| Шаг | Утверждение | Grade | Evidence |
| --- | --- | --- | --- |
| Поле файла [3] | Имя параметра C# `dislocationDate` | **CONFIRMED** | [01_ASUVP_IMPORT_PIPELINE](../analysis/01_ASUVP_IMPORT_PIPELINE.md); `KzDislocationLine.dislocationDate` |
| Колонка на ленте | `LOCATION_DATE` существует; индексы INFO/DATA | **CONFIRMED** | [36](36_P3B_PARTIAL_DICTIONARY_FINDINGS.md) §3.2 |
| `dislocationDate` → именно `LOCATION_DATE` | Гипотеза по имени/роли | **INFERRED** | [37](37_P3C0_SEMANTICS_RESEARCH.md) §2; **не** доказано телом пакета / binary compare |
| «Факт дислокации» | Бизнес-смысл UI | **не CONFIRMED** | CONTROL: значения ≠ `GETTING_DATE`, но это **не** proof назначения ([P5_E_MAPPING_FOLLOWUP](../agents/P5_E_MAPPING_FOLLOWUP.md) §3) |
| Поле [19] `operationDate` | → `DISPATCH_DATE` / иное | **UNKNOWN** | [37](37_P3C0_SEMANTICS_RESEARCH.md) §2 |

**Контроль (не расширяет RO, уже снято):** wagon `54787361`, day `2026-11-24`, 14 строк: `getting == location` = **0**; пример getting `2026-11-24 00:00`, location `2026-11-21 01:25` ([P5_E_MAPPING_FOLLOWUP](../agents/P5_E_MAPPING_FOLLOWUP.md)).

---

## 2. Контракт API (A1 day line) — целевой

Эндпоинт без смены пути: `GET /api/v1/wagons/{wagonCode}/state-reports?date=YYYY-MM-DD`.

### 2.1 Поля времени

| JSON field | Источник DBTC | Смысл | Null | Когда в ответе |
| --- | --- | --- | --- | --- |
| **`snapshotTime`** | `GETTING_DATE` | Время среза / принадлежность суткам | Строка без getting **не** попадает в A1 (фильтр); если когда-либо NULL в выборке — `null` + retain row | **всегда** после CODE |
| **`locationTime`** | `LOCATION_DATE` | Время фактической дислокации | `null` если колонка NULL | **только** после подтверждения назначения (см. §2.2); до этого — **не** эмитить как факт |
| **`dislocationTime`** | то же, что `snapshotTime` | **Deprecated alias** для совместимости | как `snapshotTime` | пока есть клиенты; **запрещено** переназначать на `LOCATION_DATE` |

Остальные поля строки без изменения смысла: `reportId`, `wagonCode`, `stationCode`, `operationCode`.

### 2.2 Правило включения `locationTime`

Пока mapping/purpose **не CONFIRMED**:

1. Документ резервирует имя `locationTime`.
2. CODE **не** добавляет поле с UI-ярлыком «факт дислокации».
3. Допустимый промежуточный шаг (отдельное approve): additive `locationTime` = raw `LOCATION_DATE` + `locationTimeConfidence: "INFERRED"` **или** отдельный feature-flag; UI колонка «LOCATION_DATE (сырое)» / скрыта — **не** «Факт дислокации».

После точечных доказательств §7 и approve владельца: `locationTime` = факт; UI «Время дислокации»; confidence снимается.

### 2.3 Пример ответа (после CODE + при CONFIRMED location)

```json
{
  "wagonCode": "54787361",
  "date": "2026-11-24",
  "lines": [
    {
      "reportId": 123456789,
      "wagonCode": "54787361",
      "snapshotTime": "2026-11-24T00:00:00",
      "locationTime": "2026-11-21T01:25:00",
      "dislocationTime": "2026-11-24T00:00:00",
      "stationCode": "69260",
      "operationCode": "81"
    },
    {
      "reportId": 123456790,
      "wagonCode": "54787361",
      "snapshotTime": "2026-11-24T01:00:00",
      "locationTime": null,
      "dislocationTime": "2026-11-24T01:00:00",
      "stationCode": "69260",
      "operationCode": "04"
    }
  ]
}
```

`dislocationTime` **обязан** совпадать с `snapshotTime` (не с `locationTime`).

### 2.4 Пример до подтверждения location (совместимый переход)

```json
{
  "reportId": 123456789,
  "wagonCode": "54787361",
  "snapshotTime": "2026-11-24T00:00:00",
  "dislocationTime": "2026-11-24T00:00:00",
  "stationCode": "69260",
  "operationCode": "81"
}
```

Нет `locationTime` → нет ложного «факта». Клиенты, читающие только `dislocationTime`, видят прежний срез.

---

## 3. Сутки, null, точность, TZ

| Тема | Правило | Grade |
| --- | --- | --- |
| Ось фильтра | **только** `GETTING_DATE` / `snapshotTime` | **CONFIRMED** |
| Границы | half-open `[D 00:00:00, D+1 00:00:00)` naive `LocalDateTime` | **CONFIRMED** policy ([23](23_R1_A1_A2_IMPLEMENTATION_PLAN.md), `DayInterval`) |
| `LOCATION_DATE` в фильтре суток | **запрещено** | **CONFIRMED** hold |
| Событие раньше календарных суток D | строка **остаётся**, если `GETTING_DATE ∈ [D, D+1)` | **CONFIRMED** intent (CONTROL: location 21.11 при getting 24.11) |
| Null `LOCATION_DATE` | строка сохраняется; `locationTime: null` (когда поле включено) | policy |
| Null / missing `GETTING_DATE` | не в day window; retain-all на уровне kit/миграции отдельно (NK1) | policy |
| Точность Oracle | `STATEREPORTS.GETTING_DATE` = TIMESTAMP(2); тип `LOCATION_DATE` на ленте — уточнить dictionary (§7) | PARTIAL |
| Сериализация API | ISO-8601 **без** суффикса зоны (`2026-11-24T00:00:00`) как сейчас | current |
| Смещение / TZ | **не** приписывать `Asia/Almaty` / UTC к Oracle DATE/TIMESTAMP без правила | **R1-Q-TZ OPEN** |
| Legacy closed `23:59:59` | возможный boundary delta — не баг smoke | OPEN disclosure |

---

## 4. UI (A1 grid)

| Колонка (RU) | API field | Когда |
| --- | --- | --- |
| **Время среза** | `snapshotTime` (fallback `dislocationTime`) | сразу при CODE контракта |
| **Время дислокации** | `locationTime` | только если назначение CONFIRMED; иначе не показывать / не подписывать как факт |
| ID отчёта / Станция / Операция / Вагон | без смены | — |

Текущий P5-E label «Время среза» на поле `dislocationTime` **согласован** с этим контрактом (смысл среза уже раскрыт в UI).

`currentState.dislocationTime` — **вне** этого документа (отдельный источник; P5-C synthetic).

---

## 5. Совместимый переход от `dislocationTime`

| Фаза | API | UI | Approve |
| --- | --- | --- | --- |
| **Сейчас (P5-E)** | `dislocationTime` = GETTING | «Время среза» | done |
| **T1 (после approve CODE)** | additive `snapshotTime` = GETTING; `dislocationTime` **копия** `snapshotTime`; без `locationTime` или raw+INFERRED | колонка «Время среза» → bind `snapshotTime` | отдельная строка |
| **T2 (после CONFIRMED location)** | additive `locationTime` = LOCATION | + «Время дислокации» | отдельная строка |
| **T3 (опционально)** | deprecate / remove `dislocationTime` | — | отдельная строка; breaking для клиентов |

**Запрещено:** переименовать смысл `dislocationTime` с GETTING на LOCATION без dual-field периода.

---

## 6. Граничные случаи (все строки среза сохраняются)

| Случай | Поведение |
| --- | --- |
| Несколько строк с одинаковым `snapshotTime` (и/или одинаковым `locationTime`) | **Retain-all**; не DISTINCT/UK; stable order PROVISIONAL: `snapshotTime ASC`, затем `reportId` / tech id ([49](49_NK1_DT1_G31_OFFLINE_RESEARCH.md) NK1 hold) |
| `locationTime` ≪ суток D, `snapshotTime` ∈ D | строка в ответе D; UI показывает оба времени (когда location включён) |
| `locationTime` IS NULL | строка в ответе; колонка дислокации «—» |
| Дубликат содержимого среза | два `reportId` / две строки — обе |

Это **не** закрывает NK1 (частота дублей UNKNOWN).

---

## 7. Недостающие доказательства (точечно — не новый план P5-E)

Без расширения действующего RO-окна и без EXECUTE. Нужны **отдельные** scoped SELECT / exports:

| ID | Запрос / артефакт | Зачем | Уже есть черновик |
| --- | --- | --- | --- |
| **DT1-A** | `ALL_TAB_COLUMNS` для `STATEREPORTRECORDS.LOCATION_DATE` / `GETTING_DATE` (DATA_TYPE, DATA_SCALE) | точность, DATE vs TIMESTAMP | dictionary; уточнить если не в CSV |
| **DT1-B** | `ALL_ARGUMENTS` / SPEC `ImportPkg.SaveStateReportLineKz` — имена/типы параметров дат | **только сигнатура** (имена входа); **не** доказательство INSERT в `LOCATION_DATE` | [38](38_P3C0_DBA_PACKAGE_SOURCE_REQUEST.md); сейчас 0 visibility у RO |
| **DT1-B2** | **BODY** `SaveStateReportLineKz` (или эквивалент export) | доказательство записи параметра → колонка | отдельный DBA grant / file-share |
| **DT1-C** | Bounded hour sample: EQUAL / WITHIN_1H / GT_24H / NULL_SIDE (≤5000) | распределение, не proof purpose | [sql/dba/p4-2/DT1_tape_date_axis_probes.sql](sql/dba/p4-2/DT1_tape_date_axis_probes.sql) |
| **DT1-D** | Join `STATEREPORTS`↔tape: same_hour_as_report | GETTING = kit hour | DT1.3 в том же SQL |
| **DT1-E** | Сверка одного импортированного файла: field[3] ↔ `LOCATION_DATE` строки Oracle (по `REPORT_ID` / kit) | закрыть INFERRED mapping **без** BODY | образец `disl_kz*.txt` + REPORT_ID |
| **DT1-F** | Что показывает legacy НУВ в колонке времени суток (`GetWagonStateReports` cursor cols) | UI «факт» vs срез | Remoting/UI research; пакет text OPEN |

**T1 (`snapshotTime`) — APPROVED CODE:** ось GETTING CONFIRMED; live Oracle re-run не нужен.

**T2 (`locationTime` = факт):** SPEC/`ALL_ARGUMENTS` **недостаточно** — подтверждает сигнатуру, не маппинг параметра в колонку. Нужен **BODY** (DT1-B2) **или** сверка исходного файла с соответствующей строкой Oracle (DT1-E), плюс бизнес-ярлык «факт».

---

## 8. Out of scope для T1 CODE

- `locationTime` / confidence / UI «Время дислокации»
- Live Oracle / повтор CONTROL
- Смена day axis на LOCATION
- Закрытие DT1 / R1-Q-TZ → CLOSED
- `currentState`, registry, watched, import writer

---

## 9. Gates

| Gate | Status |
| --- | --- |
| T1 docs + CODE (`snapshotTime` additive) | **APPROVED** (см. APPROVALS) |
| T2 CODE (`locationTime` as fact) | **DENIED** until BODY or file↔Oracle |
