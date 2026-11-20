# 37. P3C-0 — Семантика DBTC для RAILDB (offline)

Статус: **SUPERSEDED in part by P3C-1** — см. [39_P3C1_SEMANTICS_FINDINGS.md](39_P3C1_SEMANTICS_FINDINGS.md).  
База offline: P3B-2 CSV `20261116_054752` + docs 11/01/02/03/08/25–36.  
Диагностические SQL: [sql/dba/p3c/](sql/dba/p3c/); executable subset: [sql/dba/p3c/run/](sql/dba/p3c/run/).

Доступ: учётка dictionary RO с расширенными SELECT на объекты DBTC — **OK** (без секретов в этом документе). Пароль считать скомпрометированным до ротации DBA.

---

## 1. NK1 — естественный ключ ленты

### Evidences (CONFIRMED P3B)

- PK на `STATEREPORTRECORDS` **нет**.
- NOT NULL: `WAGON_ID`, `REPORT_ID`, `CURRENT_STATE_ID`.
- Индекс `(WAGON_ID, REPORT_ID, …)` **NONUNIQUE**.
- `(WAGON_ID, GETTING_DATE)` **NONUNIQUE** — **нельзя** считать UK без доказательства.

### INFERRED (C# import)

- Клиент шлёт **каждую** строку файла в `SaveStateReportLine*` ([01_ASUVP_IMPORT_PIPELINE.md](../analysis/01_ASUVP_IMPORT_PIPELINE.md)).
- Дедуп на стороне Oracle **не доказан** → дубликаты в ленте возможны и должны переноситься.

### Модель RAILDB (решение P3C-0)

| Поле | Назначение |
| --- | --- |
| `event_id` | **технический PK** (IDENTITY / sequence), всегда |
| `legacy_report_id` | `REPORT_ID` |
| `legacy_wagon_id` | `WAGON_ID` |
| `source_ordinal` | порядок строки внутри report при миграции (0..n) |
| `provenance_key` | `reportId:wagonId:ordinal` — идемпотентность батча |
| `content_hash` | SHA-256 полезной нагрузки — детекция, **не** удаление |
| `kit_id` | FK на `RAIL_IMPORT_KIT` (nullable если orphan) |

**Политика дубликатов:** не схлопывать и не удалять исходные строки автоматически. Две строки с одинаковым hash → два `event_id`.

Уникальность `(kit_id, wagon_id)` в RAILDB — **запрещена** до B1/P3C-1 доказательств.

Диагностика: [sql/dba/p3c/NK1_natural_key_probes.sql](sql/dba/p3c/NK1_natural_key_probes.sql).

---

## 2. DT1 — GETTING_DATE / LOCATION_DATE

### CONFIRMED

- Обе колонки на ленте; индексы по обеим осям.
- 12м фильтр миграции: **`GETTING_DATE`** (индексная ось) — **не менять** без доказательств P3C-1.
- `STATEREPORTS.GETTING_DATE` TIMESTAMP(2) — час/срез шапки.

### INFERRED

| Поле файла KZ | Параметр C# | Вероятная колонка ленты |
| --- | --- | --- |
| `[3] dislocationDate` | dislocationDate | скорее **`LOCATION_DATE`** (факт дислокации) |
| `[19] operationDate` | operationDate | `DISPATCH_DATE` / др. — UNKNOWN |
| — | момент среза/импорта | **`GETTING_DATE`** ≈ час комплекта |

### Проверки (P3C-1)

- Выборка ≤5000 строк за **один час** `GETTING_DATE`.
- Доли EQUAL / WITHIN_1H / GT_24H / NULL.
- Совпадение часа tape.GETTING с parent STATEREPORTS.GETTING.

`dislocation_ts` в RAILDB: хранить **оба**; partition/watermark = getting-equivalent; UI «факт» может показывать location после подтверждения.

Диагностика: [sql/dba/p3c/DT1_date_semantics_probes.sql](sql/dba/p3c/DT1_date_semantics_probes.sql).

---

## 3. G31 — список слежения

### CONFIRMED (R4 + P3B)

| Утверждение | Status |
| --- | --- |
| Полный реестр обозревателя = `GetWagonList` | CONFIRMED |
| Remoting `GetActiveWagons` → `WagonPackage.GetActiveWagonList` | CONFIRMED |
| UI НУВ вызывает Active | **не найден** |
| `wagons.txt` ≠ Active | CONFIRMED (PublishRegistry*) |
| `WAGONSTATEREPORTS*` существуют | CONFIRMED P3B |
| Тело/предикат Active | **UNKNOWN** (пакеты не видны RO) |

### Различие реестр vs слежение

- **Реестр** ≈ все вагоны (`WAGON` / `GetWagonList`).
- **Слежение** — отдельный membership; эталон для cutover **не зафиксирован**.

### Запрос DBA

[38_P3C0_DBA_PACKAGE_SOURCE_REQUEST.md](38_P3C0_DBA_PACKAGE_SOURCE_REQUEST.md) — SPEC/ALL_SOURCE, **без EXECUTE**.

Диагностика dictionary: [sql/dba/p3c/G31_watched_dictionary.sql](sql/dba/p3c/G31_watched_dictionary.sql).

---

## 4. RP1 — незавершённые ремонты

### CONFIRMED

- `REPAIRS.BEGININGDATE` / `ENDINGDATE`.
- `WAGON.CURRENTREPAIR_ID` → FK `PK_REPAIR_ID`.
- Индекс `IDX_ENDINGDATE`; индекс `IX_WAGON_CURRENTREPAIR`.

### Гипотезы

| Hyp | Status |
| --- | --- |
| Open = `ENDINGDATE IS NULL` | **UNVERIFIED** — проверить vs CURRENTREPAIR |
| Open = row referenced by `CURRENTREPAIR_ID` | **PRIMARY migration rule (draft)** |
| Оба совпадают всегда | UNKNOWN → RP1.2 |

### Правила переноса (draft)

1. **Все** строки `WAGON.CURRENTREPAIR_ID IS NOT NULL` → `RAIL_REPAIR` с `is_open='Y'`, **даже если** `BEGININGDATE` старше 12м.
2. Закрытые за 12м: `ENDINGDATE ∈ [cutoff, cutover)` → `is_open='N'`.
3. Не удалять «лишние» open с null end вне CURRENT без бизнес-решения после RP1.3.

Диагностика: [sql/dba/p3c/RP1_open_repair_probes.sql](sql/dba/p3c/RP1_open_repair_probes.sql).

---

## 5. WH1 — склад

### CONFIRMED

- UK `(WAREHOUSE_ID, OBJECT_ID, OBJECT_STATE)`.
- Колонки: `OBJECT_TYPE`, `OBJECT_COUNT`.
- FK `OBJECT_STATE` → `BUYINGTYPES` (имя constraint); `WAREHOUSE_ID` → `WAREHOUSES`.
- ~8983 строк — FULL snapshot дешёв.

### UNKNOWN → SQL

- Значения `OBJECT_TYPE` (деталь / тип / документ?).
- `OBJECT_ID` → `WAGONPARTS.ID` vs `PARTTYPES.ID`.
- Семантика Register* (не вызывать).

### Миграция

- **Актуальные остатки:** FULL `WAREHOUSEDETAILS` (+ `WAREHOUSES`) на cutover, **независимо от давности**.
- Документы: 12м по `DELIVERYDATE` (отдельно).

Диагностика: [sql/dba/p3c/WH1_warehouse_semantics_probes.sql](sql/dba/p3c/WH1_warehouse_semantics_probes.sql).

---

## 6. KIT_FK — STATEREPORTS ↔ IMPORTFILES

### CONFIRMED

- Логическая цепочка C#: `SaveImportFile` → id → `SaveStateReport(importFilesId)` → lines.
- Колонка `STATEREPORTS.IMPORTFILES_ID` есть.
- **Физического FK нет** → возможны NULL / битые id.

### Модель

```
RAIL_IMPORT_KIT (legacy_importfiles_id, file_name, kit_type, …)
    ↑ optional FK
RAIL_DISL_EVENT.kit_id  +  legacy_report_id
```

- Orphan report: `kit_id` NULL, флаги `ORPHAN_REPORT`, сохранить `legacy_importfiles_id` как было.
- Orphan file: kit без событий — для сверки watermark.

Диагностика: [sql/dba/p3c/KIT_FK_import_link_probes.sql](sql/dba/p3c/KIT_FK_import_link_probes.sql).

---

## 7. Архитектура RAILDB (уточнение)

### Лента

1. Технический PK `event_id`.
2. Полный час (full kit) и «повтор» — **обе** строки хранятся.
3. `ImportKit → DislEvent` через `kit_id` + всегда `legacy_report_id`.
4. Bootstrap на cutoff: last event per wagon by `getting_ts <= cutoff` (не удаляя историю 12м).
5. Инкремент после cutover: новые kit’ы с provenance_key идемпотентности батча.

### Ремонты / склад

- CURRENT + 12м closed; stock FULL.

См. обновлённые [27_RAILDB_ER_MODEL.md](27_RAILDB_ER_MODEL.md), [29_RAILDB_MIGRATION_12M_PLAN.md](29_RAILDB_MIGRATION_12M_PLAN.md).

---

## 8. Оценка нагрузки SQL (P3C-1)

| File | Touch set | Risk | Timeout |
| --- | --- | --- | --- |
| `00_explain_plan_gates` | optimizer | низкий | 15s |
| `NK1_*` | 1 hour tape via GETTING index | средний | 60s |
| `DT1_*` | ≤5k sample / 1 hour | средний | 60s |
| `G31_*` | ALL_* | низкий | 30s |
| `RP1_*` | WAGON 50k + REPAIRS by PK/date | низкий–средний | 60s |
| `WH1_*` | ≤10k | низкий | 30s |
| `KIT_FK_*` | STATEREPORTS day/week | низкий–средний | 60s |

Обязателен PASS плана до NK1/DT1 data SELECT.

---

## 9. Оставшиеся UNKNOWN

| ID | Unknown |
| --- | --- |
| NK1 | Глобальная частота дублей (REPORT,WAGON) / (WAGON,GETTING) |
| DT1 | Доказанный mapping file[3]→колонка; бизнес-ось UI |
| G31 | Предикат Active; колонки курсора; связь с R3 watched |
| RP1 | Совпадение CURRENT vs ENDINGDATE IS NULL |
| WH1 | OBJECT_TYPE domain; OBJECT_ID target table |
| KIT_FK | Доля orphan за 12м |
| PKG | Тела ImportPkg/RepairsPkg/WarehousePkg |

---

## 10. Stop

P3C-0 завершён. **P3C-1 / Oracle SELECT / P4 / RAILDB create / Flyway — не начинать** без нового approve.
