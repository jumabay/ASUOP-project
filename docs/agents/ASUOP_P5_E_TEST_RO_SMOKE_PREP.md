# P5-E — prep к ограниченному TEST RO smoke

Дата: **2026-11-24**.  
База плана: [ASUOP_P5_E_JDBC_READ_PLAN.md](ASUOP_P5_E_JDBC_READ_PLAN.md) (PR #31).  
Wave 2A DONE @ `3f1739c`.

**Статус этого документа:** PREP **COMPLETE**; identity/CONTROL **заполнены владельцем** (2026-11-24). Доп. планы не плодить.  
**Контур M5:** **не TEST** — «Рабочий контур legacy; только чтение» (`tc-asuvp-03` / `RAC.WORLD`).  
**Live JDBC:** строка §7 **APPROVED** владельцем 2026-11-24 (time-boxed до `2026-11-25 18:00 +05:00`).  
RO smoke разрешён в объёме §2/§7; отчёт отдельно identity / metadata / business SELECT.  
Успех `CODE` / `GETTING_DATE` ≠ полная сверка A2/A1; прочие поля — `SKIPPED_UNKNOWN_MAPPING`.  
**Не делает:** Create RAILDB, DDL/DML/Flyway, registry/watched JDBC, auto-merge; постоянный Production allow-list.  
**Сохранить DENIED:** P5-F, P5-H, Test Oracle write, Create RAILDB (пока не APPROVED отдельно).  
Пароли / полные credential connect-string — **вне git** (`env:ASUOP_ORACLE_PASSWORD`); не печатать в логи evidence.

---

## 0. Суженный объём (фиксированный)

| In                                                 | Out                                                                              |
| -------------------------------------------------- | -------------------------------------------------------------------------------- |
| **A2** паспорт **одного** CONTROL-вагона           | JDBC registry / `GetWagonList`                                                   |
| **A1** история **одного** вагона за **одни** сутки | JDBC watched / G31                                                               |
| `JdbcParityIT` (после APPROVED, profile gate)      | P5-G Oracle LIKE parity                                                          |
| Identity + dictionary metadata (RO) для UNKNOWN    | NK1/DT1 deep probes                                                              |
|                                                    | `currentState` **/ E-CUR** — **OFF** до отдельного подтверждения источников (§4) |
|                                                    | `EXECUTE` пакетов — **не** в минимальном разрешении (§3)                         |
|                                                    | `EXPLAIN PLAN` / запись в `PLAN_TABLE` — **отдельное** согласование (§5)         |

Успех smoke = `SMOKE_PASS` / `SMOKE_FAIL` / `SKIPPED_UNKNOWN_MAPPING`.  
Не писать «NK1/DT1/G31 CLOSED».

---

## 1. TEST identity — что известно и чего не хватает

### 1.1 Факт репозитория

| Параметр                 | Значение в репо          | Примечание                                                                                                                     |
| ------------------------ | ------------------------ | ------------------------------------------------------------------------------------------------------------------------------ |
| Dedicated TEST service   | **ОТСУТСТВУЕТ**          | [APPROVALS](../architecture/APPROVALS_TEMPLATE.md): «R1 JDBC against TEST Oracle» = DENIED; «TEST URL отсутствует»             |
| Dedicated TEST PDB / SID | **ОТСУТСТВУЕТ**          | Create RAILDB TEST тоже DENIED; не путать с P5-E DBTC read                                                                     |
| Schema (бизнес)          | Целевой кандидат: `DBTC` | Подтверждён как рабочая схема legacy ([28](../architecture/28_DBTC_OBJECT_CATALOG.md)) — **не** как утверждённый TEST endpoint |
| `SESSION_USER` (TEST RO) | **ОТСУТСТВУЕТ**          | Не фиксировать prod-учётки как TEST                                                                                            |

### 1.2 Исторические подключения (не являются P5-E target)

| Когда | Host / service | User | Что разрешалось |
| --- | --- | --- | --- |
| 2026-11-14 JDBC smoke | `tc-asuvp-03:1521` / `RAC.WORLD` | `RAILWAY_READONLY` | Только DUAL / sys views; **не** бизнес-таблицы; постоянный allow-list **не** создан ([21](../architecture/21_JDBC_SMOKE_CHECK.md)) |
| P3B/P3C dictionary | тот же контур (prod-class RAC) | `DBTC_READONLY` (в findings) | SELECT словаря / ограниченные probes; packages BODY **empty** для RO ([36](../architecture/36_P3B_PARTIAL_DICTIONARY_FINDINGS.md), [39](../architecture/39_P3C1_SEMANTICS_FINDINGS.md)) |

**Правило:** `tc-asuvp-03` / `RAC.WORLD` / `RAILWAY_READONLY` / `DBTC_READONLY` **не** подставлять в строку P5-E как TEST, пока владелец явно не объявит «TEST = clone of …» отдельной формулировкой. По умолчанию это **не** TEST.

### 1.3 Фактическая identity / CONTROL (заполнено владельцем 2026-11-24)

Пароль **не** хранить в документе. Источник ответа: чат владельца → этот prep.

| # | Поле | Факт | Примечание |
| --- | --- | --- | --- |
| M1 | Host | `tc-asuvp-03` | docs/tools + владелец |
| M2 | Port | `1521` | docs/tools + владелец |
| M3 | Service | `RAC.WORLD` | **не** помечать TEST |
| M4 | PDB | `n/a` (non-CDB) | владелец |
| M5 | Contour role | **Рабочий контур legacy; только чтение** | **≠ TEST** |
| M6 | `SESSION_USER` | `Dbtc` (нормализация Oracle обычно `DBTC`) | владелец; не `RAILWAY_READONLY` / не `DBTC_READONLY` |
| M7 | JDBC URL shape | `jdbc:oracle:thin:@//tc-asuvp-03:1521/RAC.WORLD` | без пароля |
| M8 | Password store | `env:ASUOP_ORACLE_PASSWORD` | значение вне git |
| M9 | `CONTROL_WAGON_CODE` | `54787361` | один вагон |
| M10 | `CONTROL_DAY` | **`2026-11-24`** | нормализовано из `24.11.2026` → `YYYY-MM-DD` |
| M11 | `CURRENT_SCHEMA` | `DBTC` | владелец |
| M12 | Access valid until | **`2026-11-25 18:00 +05:00`** | владелец |
| — | CONTROL notes | один вагон / одни сутки / max 501 / timeout 30s | |

**Live = BLOCKED** до явного APPROVED §7. Заявка: [ASUOP_P5_E_DBA_CONNECTION_REQUEST.md](ASUOP_P5_E_DBA_CONNECTION_REQUEST.md).

---

## 2. Окончательный allow-list (минимальное разрешение)

### 2.1 Объекты

#### A. Identity / session (обязательно)

| Object / view | Операция |
| --- | --- |
| `DUAL` | `SELECT` |
| `SYS_CONTEXT('USERENV', …)` via `DUAL` | `SELECT` |
| `SESSION_USERS` / `USER` | через context |
| `SESSION_PRIVS` | `SELECT` |
| `SESSION_ROLES` | `SELECT` |
| `ALL_USERS` | `SELECT` (наличие схемы `DBTC`) |

#### B. Dictionary metadata для UNKNOWN (обязательно до маппинга passport joins)

| Object | Операция | Зачем |
| --- | --- | --- |
| `ALL_TAB_COLUMNS` | `SELECT` | колонки `WAGON`, `STATEREPORTRECORDS`, `STATIONS`, `OPERATIONS` |
| `ALL_CONSTRAINTS` / `ALL_CONS_COLUMNS` | `SELECT` | FK/UK только для **чтения имён**; не строить join в smoke без совпадения имени |
| `ALL_INDEXES` / `ALL_IND_COLUMNS` | `SELECT` | подтвердить `IDX_REC_WAGON_ID` видимость (без EXPLAIN) |
| `ALL_OBJECTS` | `SELECT` | тип/статус `WAGON`, `STATEREPORTRECORDS`, packages **наличие** |
| `ALL_PROCEDURES` / `ALL_ARGUMENTS` | `SELECT` | сигнатуры `GetWagonByCode`, `GetWagonStateReports` |
| `ALL_SOURCE` | `SELECT` | **только** TYPE=`PACKAGE` (SPEC); BODY — см. §3 |

#### C. Business SELECT (A2 / A1 smoke)

| Object | Операция | Примечание |
| --- | --- | --- |
| `DBTC.WAGON` | `SELECT` | только колонки, подтверждённые metadata + §2.2 Q-A2 |
| `DBTC.STATEREPORTRECORDS` | `SELECT` | только wagon-scoped + date bind; §2.2 Q-A1 |
| `DBTC.STATEREPORTS` | `SELECT` | **optional**; только по `REPORT_ID` из уже полученных строк A1 |
| `DBTC.STATIONS` | `SELECT` | **только после** metadata подтвердит FK-имя на `WAGON` / tape; иначе **не** в working map |
| `DBTC.OPERATIONS` | `SELECT` | то же |

#### D. Явно вне allow-list этого разрешения

| Запрещено |
| --- |
| `EXECUTE` любого PL/SQL (включая `WagonPackage.*`, `StateReportPackage.*`) |
| `EXPLAIN PLAN` / `DBMS_XPLAN` / запись в `PLAN_TABLE` |
| Любой DML/DDL/MERGE/TRUNCATE |
| `STATEREPORTRECORDS` без bind `WAGON_ID` + date range |
| Registry page / watched / `WAGONSTATEREPORTS*` |
| Warehouse, repairs, mileage, TMP, jobs |
| Production host, если M5 ≠ TEST |
| `currentState` business SELECT beyond dictionary names (до §4) |

### 2.2 Запросы (параметры, лимит, таймаут)

Общий **statement timeout: 30 s**. При превышении — STOP (§5).  
Binds только named / JDBC `?` с явным списком ниже.  
Пароль не логировать; URL в логе — redacted.

#### Q-G0 — identity

```text
SELECT
  USER                              AS session_user,
  SYS_CONTEXT('USERENV','CURRENT_SCHEMA') AS current_schema,
  SYS_CONTEXT('USERENV','DB_NAME')  AS db_name,
  SYS_CONTEXT('USERENV','SERVICE_NAME') AS service_name,
  SYS_CONTEXT('USERENV','SERVER_HOST') AS server_host
FROM DUAL
```

| | |
| --- | --- |
| Params | none |
| Limit | 1 row |
| Timeout | 30 s |
| STOP if | `session_user` ≠ M6; `service_name`/`server_host` не соответствуют M1–M3; schema ≠ M11 |

#### Q-G0b — privileges snapshot

```text
SELECT privilege FROM SESSION_PRIVS ORDER BY 1
-- + SELECT role FROM SESSION_ROLES ORDER BY 1
```

| | |
| --- | --- |
| Limit | unbounded small (session privs) |
| STOP if | видны INSERT/UPDATE/DELETE/DDL admin, не объяснённые owner как RO anomaly |

#### Q-META-WAGON — колонки паспорта (UNKNOWN → имена)

```text
SELECT column_name, data_type, nullable
FROM all_tab_columns
WHERE owner = 'DBTC' AND table_name = 'WAGON'
ORDER BY column_id
```

| | |
| --- | --- |
| Params | none (owner/table literals fixed) |
| Limit | all columns of one table (~75) |
| Timeout | 30 s |
| Use | сопоставить DTO только если **точное** имя колонки найдено; иначе поле = `SKIPPED_UNKNOWN_MAPPING` |

Повторить аналогично:

| ID | table_name |
| --- | --- |
| Q-META-SRR | `STATEREPORTRECORDS` |
| Q-META-ST | `STATIONS` |
| Q-META-OP | `OPERATIONS` |

#### Q-META-FK — не угадывать joins

```text
SELECT c.constraint_name, c.constraint_type, cc.column_name,
       r.owner AS r_owner, r.table_name AS r_table, rc.column_name AS r_column
FROM all_constraints c
JOIN all_cons_columns cc
  ON cc.owner = c.owner AND cc.constraint_name = c.constraint_name
LEFT JOIN all_constraints r
  ON r.owner = c.r_owner AND r.constraint_name = c.r_constraint_name
LEFT JOIN all_cons_columns rc
  ON rc.owner = r.owner AND rc.constraint_name = r.constraint_name
 AND rc.position = cc.position
WHERE c.owner = 'DBTC'
  AND c.table_name = :table_name   -- 'WAGON' then 'STATEREPORTRECORDS'
  AND c.constraint_type IN ('R','P','U')
ORDER BY c.constraint_name, cc.position
```

| | |
| --- | --- |
| Params | `:table_name` ∈ {`WAGON`,`STATEREPORTRECORDS`} |
| Limit | constraints of one table |
| Rule | **Working mapping join только если** FK column → `STATIONS`/`OPERATIONS`/`WAGONTYPES`/… **явно** в результате. Иначе join **не** добавлять в smoke SQL |

#### Q-A2 — passport smoke (минимальный CONFIRMED core)

До metadata: разрешён **только** UK lookup:

```text
SELECT w.id AS wagon_id, w.code AS wagon_code
FROM dbtc.wagon w
WHERE w.code = :wagon_code
FETCH FIRST 1 ROW ONLY
```

| | |
| --- | --- |
| Params | `:wagon_code` = M9 |
| Limit | 1 |
| Timeout | 30 s |
| Maps to DTO | `wagonCode` **CONFIRMED**; `wagon_id` internal only |
| Other A2 fields | **не SELECT** пока Q-META-* не подтвердит точные имена колонок; в IT: `SKIPPED_UNKNOWN_MAPPING` |

После metadata (тот же Q-A2 **расширяется только** подтверждёнными column names из ALL_TAB_COLUMNS / FK) — отдельный evidence appendix «columns unlocked», без догадок.

#### Q-A1 — day lines (ось GETTING_DATE)

```text
SELECT
  r.report_id,
  :wagon_code AS wagon_code,
  r.getting_date AS dislocation_time
  -- station/operation columns: ONLY after Q-META-SRR + Q-META-FK name them
FROM dbtc.statereportrecords r
WHERE r.wagon_id = :wagon_id
  AND r.getting_date >= :day_from
  AND r.getting_date <  :day_to
ORDER BY r.getting_date ASC
FETCH FIRST 501 ROWS ONLY
```

|                  |                                                                                                             |
| ---------------- | ----------------------------------------------------------------------------------------------------------- |
| Params           | `:wagon_id` from Q-A2; `:wagon_code` = M9; `:day_from` = M10 00:00:00; `:day_to` = M10+1 00:00:00           |
| Limit            | **501** (если 501 → приложение/IT трактует как overflow / 422 path)                                         |
| Timeout          | 30 s                                                                                                        |
| Axis             | `GETTING_DATE` **only**; `LOCATION_DATE` не фильтр                                                          |
| NK1              | retain all; no DISTINCT                                                                                     |
| Station/op codes | добавлять в SELECT **только** после подтверждения имён колонок + FK; иначе keys = `report_id` + time + code |

Optional Q-A1b: `SELECT … FROM dbtc.statereports WHERE uk_report_id = :report_id FETCH FIRST 1` — только для report ids, уже вернувшихся в Q-A1 (≤20 для CONTROL).

### 2.3 `JdbcParityIT` (после APPROVED)

| Step | Assert |
| --- | --- |
| 1 | Q-G0 identity match M6/M3/M11 |
| 2 | Q-A2 returns exactly one row for M9 (or documented 404) |
| 3 | Q-A1 returns N∈[0,500] or detects 501 |
| 4 | Passport fields beyond `wagonCode`: pass only if unlocked by metadata; else skip |
| 5 | No registry/watched calls |
| 6 | No `currentState` assert until §4 unlocked |
| 7 | Default profile `synthetic` tests remain green; IT gated `@EnabledIfEnvironmentVariable` / profile `oracle-test` |

---

## 3. EXECUTE — не в минимальном разрешении

### 3.1 Известное по SPEC (offline)

| Member | Spec role (name-level) | BODY | Side effects |
| --- | --- | --- | --- |
| `WagonPackage.GetWagonByCode` | read / cursor | **недоступен** RO historically | **UNKNOWN** — нельзя доказать отсутствие DML |
| `StateReportPackage.GetWagonStateReports` | read (`wagon_id`, `beginDate`, `endDate`, `result OUT`) | **недоступен** | **UNKNOWN** |
| Same packages: `Save*`, `Delete*`, `Refresh*`, `Update*` | writers by name | hidden | **запрещены** навсегда в P5-E |

Evidence: [03_ORACLE_PACKAGES.md](../analysis/03_ORACLE_PACKAGES.md); P3C-1: package text empty for RO ([39](../architecture/39_P3C1_SEMANTICS_FINDINGS.md)).

### 3.2 Политика

1. Минимальная строка APPROVALS (§7) = **SELECT-only** (§2).
2. `EXECUTE` **не** включать автоматически.
3. Перед любым будущим EXECUTE-дополнением DBA должен подтвердить одно из:
   - экспорт **BODY** (или эквивалент) без скрытого DML/autonomous tx для двух read-членов; **или**
   - письменное DBA attestation: «GetWagonByCode / GetWagonStateReports — pure read on TEST».
4. До attestation — parity только через table SELECT §2.2.

### 3.3 Запрос DBA (если понадобится EXECUTE позже)

Отдельная строка APPROVALS (не эта): grant `EXECUTE` на 2 члена + SPEC/BODY visibility для `ASUOP` RO user; запрет writers; evidence без секретов.

---

## 4. UNKNOWN-поля — точная metadata-проверка (без joins в working map)

| DTO field                  | Working map until unlocked           | Unlock check                                                                                                                                                             |
| -------------------------- | ------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `wagonCode`                | `WAGON.CODE`                         | Q-META-WAGON contains `CODE` (**CONFIRMED** historically)                                                                                                                |
| `model`                    | **SKIP**                             | column/FK name present in Q-META-WAGON + Q-META-FK → model table; **exact** name recorded                                                                                |
| `wagonKind`                | **SKIP**                             | same → types table                                                                                                                                                       |
| `ownerName`                | **SKIP**                             | same → owners/customers                                                                                                                                                  |
| `homeStationCode` / `Name` | **SKIP**                             | FK from `WAGON` → `STATIONS` explicit in Q-META-FK                                                                                                                       |
| `inventoryPark`            | **SKIP**                             | exact column on `WAGON` in Q-META-WAGON                                                                                                                                  |
| A1 `stationCode`           | **SKIP**                             | tape column + FK → `STATIONS` in Q-META-SRR/FK                                                                                                                           |
| A1 `operationCode`         | **SKIP**                             | `OPERATION_ID` or code col + `OPERATIONS`                                                                                                                                |
| A1 `reportId`              | `REPORT_ID` if present in Q-META-SRR | else SKIP                                                                                                                                                                |
| A1 `dislocationTime`       | `GETTING_DATE` if present            | else STOP (cannot run A1)                                                                                                                                                |
| `currentState.*`           | **OUT OF SCOPE**                     | Unlock only after separate owner note: which `WAGON` columns = station/op/time/train; anti-rule vs day last line. Then **new** APPROVALS delta — not silent expand of §7 |

**Запрет:** подставлять «вероятно MODEL_ID → WAGONMODELS» в JDBC adapter / IT assert как CONFIRMED.

---

## 5. Критерии остановки (STOP)

Любой STOP → немедленно disconnect; не retry с расширением окна/объектов; IT fail; evidence «STOP reason=…» без секретов.

| Code | Trigger |
| --- | --- |
| **STOP-ID** | Q-G0: `SESSION_USER` ≠ M6 (`Dbtc`/`DBTC`); service/host ≠ M1–M3; PDB unexpected; `CURRENT_SCHEMA` ≠ M11; контур вне заявленного legacy RO окна / после M12 |
| **STOP-WRITE** | Попытка/факт DML/DDL; privilege snapshot показывает write; SQL вне SELECT allow-list |
| **STOP-TIMEOUT** | Statement > 30 s; cancel |
| **STOP-SCOPE** | SQL object не из §2.1; registry/watched/currentState business read; `EXECUTE`; package call |
| **STOP-PLAN** | Любой `EXPLAIN PLAN` / `PLAN_TABLE` / `DBMS_XPLAN` **без** отдельной APPROVALS строки на EXPLAIN |
| **STOP-HEAVY** | (Если EXPLAIN позже APPROVED) DBA/IT видит `TABLE ACCESS FULL` на `STATEREPORTRECORDS` или examined rows > согласованного cap (рекомендация 5 000) → STOP; **без** самовольного EXPLAIN |
| **STOP-VOLUME** | Day query без `WAGON_ID` bind или date range шире одних суток M10 |
| **STOP-SECRET** | Пароль/полный credential URL попал в лог → redact, rotate per DBA, abort run |

**Отдельно:** согласование **EXPLAIN PLAN** = своя мини-строка (не входит в §7), потому что Oracle может писать в `PLAN_TABLE` (DML-like). До неё опираться только на наличие индекса в dictionary (`ALL_INDEXES`), не на runtime plan.

---

## 6. Контрольный вагон / период (слоты)

| Slot | Binding | Rule |
| --- | --- | --- |
| CONTROL-1 | **`54787361`** + **`2026-11-24`** | Один код; одни сутки; max 501; timeout 30s; ось `GETTING_DATE` |
| Empty / overflow | — | Не в минимальном smoke этого окна |

UI сверка (после APPROVED live): card passport unlocked fields + day grid keys — без current block.

---

## 7. Заполненная строка APPROVALS — **APPROVED** владельцем 2026-11-24

> Зафиксировано в [APPROVALS_TEMPLATE.md](../architecture/APPROVALS_TEMPLATE.md): P5-E + scoped Production RO override.  
> Пароль не включён. Контур **не TEST** — scoped RO на рабочем legacy.  
> Окно до `2026-11-25 18:00 +05:00`.

```text
| P5-E JDBC read A1/A2 on TEST Oracle | APPROVED WITH CONSTRAINTS | Owner (chat ASUOP Manager, 2026-11-24):
  Scoped RO smoke — NOT dedicated TEST. Contour: working legacy read-only.
  Target: host tc-asuvp-03, port 1521, service RAC.WORLD, PDB n/a (non-CDB);
  SESSION_USER Dbtc (expect CURRENT_SCHEMA DBTC); password only env ASUOP_ORACLE_PASSWORD;
  URL shape jdbc:oracle:thin:@//tc-asuvp-03:1521/RAC.WORLD (no secrets in git).
  Operations SELECT-only per ASUOP_P5_E_TEST_RO_SMOKE_PREP.md §2: identity DUAL/USERENV;
  dictionary ALL_TAB_COLUMNS/ALL_CONSTRAINTS/ALL_INDEXES (+ ALL_PROCEDURES/ARGUMENTS/SOURCE SPEC as needed);
  business SELECT DBTC.WAGON (CODE/ID + only metadata-unlocked columns) and
  DBTC.STATEREPORTRECORDS (WAGON_ID + GETTING_DATE half-open day FETCH FIRST 501), timeout 30s.
  Control: wagon_code 54787361, day 2026-11-24. Optional JdbcParityIT under env/profile gate only.
  Validity until 2026-11-25 18:00 +05:00; then access ends / no standing allow-list.
  FORBIDDEN: EXECUTE; EXPLAIN PLAN/PLAN_TABLE (separate row); registry/watched JDBC; currentState
  until source unlock; Create RAILDB; DDL/DML/Flyway; FTP/RWL live; P5-F/H; writes; auto-merge;
  closing G31/NK1/DT1; inventing UNKNOWN joins; treating CODE/GETTING_DATE success as full A2/A1 parity.
  Does NOT open permanent Production access. Companion: one-time scoped override vs DENIED
  «Production access» / «R1 JDBC against TEST Oracle» for this window only.
  Prep: ASUOP_P5_E_TEST_RO_SMOKE_PREP.md §1.3/§7. | <owner-approve-date> |
```

**Companion:** при утверждении — одна scoped строка; «R1 JDBC against TEST Oracle» **не** переименовывать в TEST (контур legacy). Permanent Production access остаётся DENIED.

**Отдельные будущие строки (не смешивать):**

| Title | Когда |
| --- | --- |
| P5-E EXPLAIN PLAN on TEST (PLAN_TABLE) | Если нужен plan gate |
| P5-E EXECUTE GetWagonByCode/GetWagonStateReports | После BODY/attestation |
| P5-E currentState source unlock | После подтверждения колонок WAGON |
| NK1 / DT1 / G31 probes | Research tracks |

---

## 8. Порядок до live (checklist)

1. ~~Owner заполняет M1–M12~~ **DONE** (§1.3).
2. Owner явно утверждает §7 в `APPROVALS_TEMPLATE.md` (статус → APPROVED WITH CONSTRAINTS).
3. Только после шага 2: RO smoke Q-G0 → Q-META-* → Q-A2 → Q-A1; отчёт identity / metadata / business отдельно.
4. Evidence redacted; synthetic regression green.
5. Истечение M12 (`2026-11-25 18:00 +05:00`) → окно закрыто; без постоянного allow-list.

---

## 9. Связь с PR #31

| Doc | Role |
| --- | --- |
| [ASUOP_P5_E_JDBC_READ_PLAN.md](ASUOP_P5_E_JDBC_READ_PLAN.md) | Общий план / field matrix / gaps |
| **This file** | Операционный prep: identity gaps, final allow-list, no-EXECUTE, STOP, одна строка APPROVALS |
| APPROVALS | P5-E execution остаётся DENIED до §7 |

---

## 10. Residual

- Без M1–M12 live невозможен — это ожидаемый STOP, не дефект плана.  
- SPEC read-by-name ≠ proof of pure SELECT inside BODY.  
- Half-open day vs legacy 23:59:59 может дать boundary delta — не «баг» без R1-Q-TZ.  
- Index existence ≠ plan quality without approved EXPLAIN.
