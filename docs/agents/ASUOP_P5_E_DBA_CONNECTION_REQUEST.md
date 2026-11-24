# Заявка DBA / владельцу: ограниченное RO-подключение для проверки чтения АСУОП (P5-E)

Дата: **2026-11-24**.  
Контекст: [ASUOP_P5_E_TEST_RO_SMOKE_PREP.md](ASUOP_P5_E_TEST_RO_SMOKE_PREP.md), план [ASUOP_P5_E_JDBC_READ_PLAN.md](ASUOP_P5_E_JDBC_READ_PLAN.md), журнал [APPROVALS_TEMPLATE.md](../architecture/APPROVALS_TEMPLATE.md).

**Статус:** запрос параметров. Live JDBC в приложении **не** выполняется, пока строка P5-E / «R1 JDBC against TEST Oracle» не станет APPROVED и ниже не будут заполнены ответы.  
**Пароль в этот документ, git, PR и чат-логи не включать.**

---

## 1. Текст заявки (можно переслать как есть)

Нужно подключение для ограниченной проверки чтения АСУОП (номерной учёт: карточка одного вагона и история за одни сутки).

Просьба предоставить:

| # | Сведение | Ответ DBA / владельца |
| --- | --- | --- |
| 1 | **Host** (имя / VIP / scan) | |
| 2 | **Port** | |
| 3 | **Service** name (или SID) | |
| 4 | **PDB** (если CDB; иначе `n/a`) | |
| 5 | Подтверждение назначения контура: **TEST** или иной (если иной — кратко что именно; Production writer path **не** подходит без отдельного решения владельца) | |
| 6 | **`SESSION_USER`** (техническая RO-учётка) | |
| 7 | Ожидаемый **`CURRENT_SCHEMA`** после logon (ожидание заявителя: `DBTC` или путь с synonym на объекты `DBTC`) | |
| 8 | Способ **безопасной передачи пароля** (не в git/PR): например корпоративный secret store / sealed channel + имя переменной окружения на стороне разработки | |
| 9 | Один **контрольный номер вагона** (`CODE`), для которого есть данные | |
| 10 | Одна **дата** (`YYYY-MM-DD`) с данными по этому вагону на ленте (предпочтительно 1–20 строк за сутки) | |
| 11 | Допустимое **окно времени проверки** (когда можно выполнять SELECT) | |
| 12 | **Срок действия** доступа (дата/время окончания) | |
| 13 | Redacted JDBC URL shape **без пароля** (`jdbc:oracle:thin:@//{host}:{port}/{service}`) | |

### Необходимые права

- Чтение **identity** сессии (`DUAL`, `USERENV` / `SESSION_PRIVS` / `SESSION_ROLES` по необходимости).
- Чтение **метаданных** согласованных объектов: как минимум `ALL_TAB_COLUMNS`, `ALL_CONSTRAINTS` / `ALL_CONS_COLUMNS`, `ALL_INDEXES` / `ALL_IND_COLUMNS`, `ALL_OBJECTS` для `DBTC.WAGON` и `DBTC.STATEREPORTRECORDS` (и при необходимости справочных имён без EXECUTE).
- **`SELECT`** на **`DBTC.WAGON`** и **`DBTC.STATEREPORTRECORDS`**.

### Параметры самой проверки

| Параметр | Значение |
| --- | --- |
| Объём | **один** вагон, **одни** сутки |
| Лимит строк | максимум **501** (`FETCH FIRST 501 ROWS ONLY`) |
| Таймаут запроса | **30** секунд |
| Ось суток | `GETTING_DATE` half-open `[D 00:00, D+1 00:00)` |
| Bind | обязательны `WAGON`/`CODE` → `ID` и `WAGON_ID` + даты |

### Не требуется и не запрашивается

- `EXECUTE` пакетов (`WagonPackage`, `StateReportPackage`, …)
- `EXPLAIN PLAN` / запись в `PLAN_TABLE` (отдельное согласование, если понадобится)
- DDL, DML, Flyway, создание БД / PDB / RAILDB
- JDBC registry / watched
- Маппинг `currentState` до отдельного подтверждения источников

Пароль в ответ на эту заявку в документацию репозитория **не** включать — только канал из п. 8.

---

## 2. Форма ответа (заполняет DBA / владелец)

```text
Host:
Port:
Service (or SID):
PDB:
Contour role (TEST / other — specify):
SESSION_USER:
CURRENT_SCHEMA:
Password transfer method (no password here):
Env var name for password (dev side):
CONTROL_WAGON_CODE:
CONTROL_DAY (YYYY-MM-DD):
Allowed check window:
Access valid until:
JDBC URL shape (no password):
Grants applied (summary):
  - SELECT DBTC.WAGON: yes/no
  - SELECT DBTC.STATEREPORTRECORDS: yes/no
  - dictionary views as listed: yes/no
  - EXECUTE: no
  - DDL/DML: no
Notes:
```

После заполнения владелец вносит одну строку APPROVALS по черновику [prep §7](ASUOP_P5_E_TEST_RO_SMOKE_PREP.md). До APPROVED + заполненной формы live JDBC **запрещён**.

---

## 3. Что уже известно заявителю (не подставлять как TEST)

| Элемент | Факт |
| --- | --- |
| Dedicated TEST URL в репо | **отсутствует** |
| Исторический diagnostic host | `tc-asuvp-03:1521` / `RAC.WORLD` — **не** считать TEST без явной пометки владельца |
| Целевая бизнес-схема | кандидат **`DBTC`** |
| APPROVALS сейчас | P5-E и «R1 JDBC against TEST Oracle» = **DENIED UNTIL APPROVED** |

---

## 4. Ссылки

- Prep / allow-list / STOP: [ASUOP_P5_E_TEST_RO_SMOKE_PREP.md](ASUOP_P5_E_TEST_RO_SMOKE_PREP.md)
- План P5-E: [ASUOP_P5_E_JDBC_READ_PLAN.md](ASUOP_P5_E_JDBC_READ_PLAN.md)
- Журнал: [APPROVALS_TEMPLATE.md](../architecture/APPROVALS_TEMPLATE.md)
