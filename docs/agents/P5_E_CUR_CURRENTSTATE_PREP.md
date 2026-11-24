# P5-E-CUR — prep: Oracle `currentState` на карточке (без live)

Дата: **2026-11-24**.  
База: `main` @ `5fb1878` (после DT1 T1).  
Тип: **анализ + подготовка разрешения**. Live `currentState` / JDBC mapping **не** выполнять до APPROVED строки ниже.  
**Не** расширяет P5-E / P5-E allow-list delta (там `currentState` **FORBIDDEN**).  
Вне скоупа: T2, import, registry/watched JDBC, writes, Create RAILDB, auto-merge.

Связанные факты: [ASUOP_P5_E_JDBC_READ_PLAN.md](ASUOP_P5_E_JDBC_READ_PLAN.md) §4; [P5_E_MAPPING_FOLLOWUP.md](P5_E_MAPPING_FOLLOWUP.md) §2.2; [P5_E_RO_SMOKE_EVIDENCE.md](P5_E_RO_SMOKE_EVIDENCE.md); P5-C DTO `WagonCurrentState`.

---

## 1. Master-поля текущего положения

Источник: **`DBTC.WAGON`** (master), **не** `STATEREPORTRECORDS` выбранных суток.

| API `currentState.*` | Master / join | Смысл | Grade | Доказательство |
| --- | --- | --- | --- | --- |
| `stationCode` | `WAGON.LOCATIONSTATION_ID` → soft `STATIONS.ID` → `STATIONS.CODE` | Текущая станция на карточке | Колонка **CONFIRMED**; soft ref (нет DB FK) **CONFIRMED**; join pattern как A1 location **INFERRED** | Smoke columns [P5_E_RO_SMOKE_EVIDENCE](P5_E_RO_SMOKE_EVIDENCE.md); soft refs [P5_E_MAPPING_FOLLOWUP](P5_E_MAPPING_FOLLOWUP.md) §2.2; catalog «master + current» [28](../architecture/28_DBTC_OBJECT_CATALOG.md); **не** `HOMESTATION_ID` (passport home — A2 CONFIRMED) |
| `operationCode` | `WAGON.OPERATION_ID` → soft `OPERATIONS.ID` → **`IDCODE`** (fallback `CODE`) | Текущая операция | Колонка **CONFIRMED**; soft ref **CONFIRMED**; display `IDCODE` **CONFIRMED** на A1 CONTROL | Same + [P5_E_MAPPING_FOLLOWUP](P5_E_MAPPING_FOLLOWUP.md) §2.1 op join |
| `dislocationTime` | **`WAGON.LOCATIONDATE` only** for API map | Время на блоке «Текущее состояние» (UI label уже «Время дислокации») | Колонка **CONFIRMED**; назначение UI **PROVISIONAL** (G3/G4 OPEN: writer `WAGON.LOCATION*` не доказан) | Columns seen in smoke; provisional [JDBC plan §4.3](ASUOP_P5_E_JDBC_READ_PLAN.md). **`REPORTDATE`** — только diagnostic рядом, **не** silent fallback. **Запрещено** подставлять `GETTING_DATE`, last day line, `SYSDATE` |
| `trainIndex` | **нет** подтверждённой колонки на `WAGON` | Индекс поезда | **UNKNOWN** → API **`null`** | Interesting WAGON set в smoke без `TRAIN*`; tape `STATEREPORTRECORDS.TRAIN_CODE` **CONFIRMED**, но взять его с суток = нарушение anti-rule |

### Не путать

| Поле | Почему не в P5-C `currentState` |
| --- | --- |
| `WAGON.CURRENTSTATE_ID` | Вероятно `WAGONSTATES` (рабочее состояние), не станция/операция/поезд |
| `WAGON.HOMESTATION_ID` | Приписка (passport), уже в A2 |
| Last A1 line / `GETTING_DATE` | Ось суток / срез; P5-C + plan §4.1 **запрещают** derive current |

### Anti-rules (обязательны)

1. **Не** брать последнюю строку выбранных суток (`ORDER BY GETTING_DATE DESC FETCH 1` и т.п.).  
2. Смена `?date=` суток **не** меняет `currentState`.  
3. Пустой день **не** очищает `currentState`.  
4. Null/orphan soft ref / null date → JSON `null` / UI «—».  
5. Неподтверждённое время **не** заменять `GETTING_DATE` или «сейчас».

---

## 2. SQL (черновик — **не исполнять** без APPROVED E-CUR)

Файл: [../architecture/sql/dba/p5e/E_CUR_wagon_current_state.sql](../architecture/sql/dba/p5e/E_CUR_wagon_current_state.sql)

```sql
-- E-CUR DRAFT — DO NOT RUN without APPROVALS row «P5-E-CUR …» APPROVED.
-- CONTROL wagon only. Soft joins. No STATEREPORTRECORDS day derive.
SELECT
    w.code                                              AS wagon_code,
    w.locationstation_id                                AS location_station_id,
    ls.code                                             AS station_code,
    w.operation_id                                      AS operation_id,
    NVL(op.idcode, op.code)                             AS operation_code,
    w.locationdate                                      AS location_date,   -- sole candidate → currentState.dislocationTime
    w.reportdate                                        AS report_date,     -- diagnostic only; NOT mapped unless owner picks
    CAST(NULL AS VARCHAR2(64))                          AS train_index_placeholder  -- no CONFIRMED WAGON train col
FROM dbtc.wagon w
LEFT JOIN dbtc.stations ls
       ON ls.id = w.locationstation_id
LEFT JOIN dbtc.operations op
       ON op.id = w.operation_id
WHERE w.code = :wagon_code   -- bind '54787361'
FETCH FIRST 1 ROW ONLY;
```

Optional metadata (same gate, no invent):  
`ALL_TAB_COLUMNS` where `table_name='WAGON'` and `column_name LIKE '%TRAIN%'` — если 0 rows, `trainIndex` остаётся null.

**Запрещено в том же smoke:** SELECT day tape ради current; EXECUTE; DDL/DML; registry/watched.

---

## 3. Маппинг → существующий `currentState`

| DB / join | → JSON | Null policy |
| --- | --- | --- |
| `station_code` | `currentState.stationCode` | null if ID null or join miss |
| `operation_code` (`IDCODE`/`CODE`) | `currentState.operationCode` | null if ID null or join miss |
| `location_date` → ISO-like string (naive, no TZ invent; same style as A1) | `currentState.dislocationTime` | **null** if `LOCATIONDATE` null — **no** REPORTDATE/GETTING/now fallback |
| (none) | `currentState.trainIndex` | **always null** until CONFIRMED WAGON column |
| whole block | `currentState` | if all four null → either `currentState: null` **or** object of nulls (prefer object of nulls only if UI needs keys; synthetic today uses object or null per fixture — JDBC: **`null` block** if station+op+time+train all absent) |

Passport A2 fields **unchanged**. Day A1 **unchanged**.

---

## 4. Пример JSON (ожидаемая форма; значения — placeholders)

```json
{
  "wagonCode": "54787361",
  "model": "…",
  "wagonKind": "…",
  "ownerName": "…",
  "homeStationCode": "…",
  "homeStationName": "…",
  "inventoryPark": true,
  "currentState": {
    "stationCode": "<STATIONS.CODE or null>",
    "operationCode": "<OPERATIONS.IDCODE or null>",
    "dislocationTime": "<WAGON.LOCATIONDATE as ISO-like or null>",
    "trainIndex": null
  }
}
```

Проверка независимости от суток (после CODE):  
`GET .../wagons/54787361` при `state-reports?date=2026-11-24` и при другой/пустой дате → **тот же** `currentState`; last day line ≠ current (или равенство задокументировано evidence).

---

## 5. Строка APPROVALS (для утверждения владельцем)

Сейчас в журнале: **DENIED UNTIL APPROVED**. После решения владельца — сменить статус на **APPROVED WITH CONSTRAINTS** (не править текст P5-E).

| Решение | Статус (сейчас) | Детали |
| --- | --- | --- |
| P5-E-CUR JDBC `currentState` CONTROL | **DENIED UNTIL APPROVED** | Владелец (черновик 2026-11-24): **отдельная** строка — **не** расширение P5-E / allow-list delta. **Разрешено после APPROVED:** SELECT-only master current для **одного** CONTROL `54787361` на том же контуре `tc-asuvp-03:1521/RAC.WORLD`, user `Dbtc`, schema `DBTC`, password `env:ASUOP_ORACLE_PASSWORD` (не в git); timeout 30s; `FETCH FIRST 1`. Объекты: `DBTC.WAGON` (колонки `LOCATIONSTATION_ID`,`OPERATION_ID`,`LOCATIONDATE`,`REPORTDATE` diagnostic); soft join **по PK** `DBTC.STATIONS`,`DBTC.OPERATIONS` только для ID с этой строки WAGON; metadata `ALL_TAB_COLUMNS` WAGON `%TRAIN%`. Маппинг: `stationCode`←`STATIONS.CODE`; `operationCode`←`OPERATIONS.IDCODE` (fallback `CODE`); `dislocationTime`←**только** `WAGON.LOCATIONDATE` (null→null; **запрет** GETTING_DATE / SYSDATE / last day / silent REPORTDATE); `trainIndex`←null пока нет CONFIRMED колонки. **Разрешено** JDBC→API→UI проверка карточки CONTROL; default profile `synthetic`. **Запрещено:** derive current из `STATEREPORTRECORDS`/A1; registry/watched JDBC; T2/`locationTime`; import/EXECUTE/DDL/DML/EXPLAIN; Create RAILDB/Flyway; writes; закрытие DT1/R1-Q-TZ/G31/NK1; auto-merge. Срок: указать при approve (рекомендация: time-box ≤ 48h). Prep: [P5_E_CUR_CURRENTSTATE_PREP.md](P5_E_CUR_CURRENTSTATE_PREP.md) |

---

## 6. Out of scope

T2, import writer, registry/watched, DB writes, расширение старых строк P5-E, закрытие DT1/R1-Q-TZ, CODE до APPROVED.
