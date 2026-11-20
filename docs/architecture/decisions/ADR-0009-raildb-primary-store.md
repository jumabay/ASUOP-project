# ADR-0009. RAILDB как основное хранилище; DBTC — миграция и архив

## Статус

**ACCEPTED — architecture direction only** (2026-11-20). Владелец проекта (чат ASUOP Manager).

Пакет готовности: [42_ADR0009_DECISION_READINESS.md](../42_ADR0009_DECISION_READINESS.md).  
Фиксация Accept и открытых условий: [APPROVALS_TEMPLATE.md](../APPROVALS_TEMPLATE.md).  
DBA checklist перед Create: [43_ADR0009_DBA_CREATE_CHECKLIST.md](../43_ADR0009_DBA_CREATE_CHECKLIST.md).

### Что Accept разрешает

- Целевое архитектурное направление: отдельная Oracle **`RAILDB`** + схема **`RAIL_APP`**; `DBTC` = миграция + архив после cutover.
- Планирование и docs (контракты, DDL drafts, DBA checklist) в рамках уже APPROVED offline-срезов.

### Что Accept **не** разрешает (нужны отдельные строки APPROVALS)

- Создание instance/PDB `RAILDB`
- Flyway apply / исполнение DDL
- JDBC connect (TEST RO, write, Production)
- Миграция данных DBTC → RAILDB
- Live FTP / RWL
- Смена бизнес-кода / REST под connect
- Объявление закрытыми **NK1**, **DT1**, **G31**, **R1-Q-TZ**

## Контекст

Ранее документы фиксировали схему `DBTC` как единственное бизнес-хранилище на переносе: [05_ORACLE_INTEGRATION.md](../05_ORACLE_INTEGRATION.md), [06_DATA_STORAGE_STRATEGY.md](../06_DATA_STORAGE_STRATEGY.md), [ADR-0006](ADR-0006-flyway-app-schema-only.md), [ADR-0007](ADR-0007-single-writer-read-first.md). После Accept **целевой** store supersede’ится этим ADR; текущая эксплуатация legacy на `DBTC` продолжается до cutover.

Заказчик утвердил **вариант 2**: собственная основная Oracle-база; после переключения повседневная работа не зависит от `DBTC`. `DBTC` — источник первичной миграции, сверки и архив старше окна переноса.

Схема-пользователь `RAIL_APP` в существующей RAC (квота 5 ГиБ) **не** является целевым store.

## Решение (принято как направление)

1. Целевое хранилище — **отдельная Oracle Database `RAILDB`** (совместимость с 12.2 на этапе внедрения; апгрейд до 19c — отдельное окно DBA) на хосте-кандидате `TC-ASUVP-03` **только после** DBA checklist и явного APPROVED Create.
2. Рабочая схема приложения внутри `RAILDB`: **`RAIL_APP`**.
3. После cutover новые бизнес-данные пишутся только в `RAILDB`. Обычная работа приложения не требует `DBTC`.
4. `DBTC` на миграции **не изменяется** проектом (нет DDL/DML). После cutover — архив.
5. Первичная миграция (когда будет APPROVED): **12 календарных месяцев** истории + FULL/OPEN/REF + bootstrap на cutoff. Детали — [25](../25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md). **Сейчас миграция DENIED.**
6. Квота 5 ГиБ RAC `RAIL_APP` не ориентир sizing.
7. RAC schema `RAIL_APP` не prod store.
8. Java: modular monolith, порты R0–R3, REST; адаптеры Synthetic / (future) LegacyDbtcRead / RailDbJdbc / RwlApiClient / FtpIntake.
9. RWL — кандидат intake, не замена FTP до B1.
10. Flyway (когда APPROVED) только на `RAILDB.RAIL_APP`, никогда на `DBTC`.

### Утверждённые предпочтения владельца (2026-11-20)

| Тема | Решение |
| --- | --- |
| Топология | Предпочтительно **A** (отдельный instance); A/B и service name — **DBA confirm** до Create |
| Charset | Предпочтительно **AL32UTF8**; DBA confirm + совместимость с DBTC `CL8MSWIN1251` |
| DDL path | **V0** если Partitioning licensed; иначе **V0b** после проверки логической эквивалентности |
| Create RAILDB | **Не сейчас** |
| JDBC TEST RO / write / Production | **Не сейчас** / write отдельно / Production запрещён |
| Migrator DBTC read + RAILDB write | **Не сейчас** |
| G31 | **OPEN**; `rail_wagon_watched` пуст до бизнес-источника |
| R1-Q-TZ | **OPEN**; временно half-open сутки на `dislocation_ts` (GETTING) |
| Rollback | Целевая политика **≤ 72 ч** + quarantine replay; drill обязателен до cutover |
| NK1 / DT1 | Остаются **BLOCKED/OPEN** — не закрыты этим Accept |

## Отношение к принятым ADR

| ADR | Действие после Accept ADR-0009 |
| --- | --- |
| ADR-0001 | Контуры платформы живут на `RAILDB`, не «одна схема DBTC» как целевой store |
| ADR-0002 | JDBC/не-JPA; целевые таблицы — `RAILDB`; пакеты `DBTC` — миграция/архивное чтение |
| ADR-0003 | `ojdbc10:19.33.0.0` (+ `orai18n`) до решения по версии `RAILDB` |
| ADR-0006 | Scope Flyway (когда разрешён) → бизнес-схема `RAILDB.RAIL_APP`; запрет DDL на `DBTC` |
| ADR-0007 | После cutover writer → `RAILDB`; до cutover legacy writer в `DBTC` |
| ADR-0008 | DIP/порты без изменения |

Историю прежних ADR **не удалять**.

## Инфраструктура (выбор DBA до Create)

- **A.** Отдельный instance (предпочтение владельца): SID/service например `RAILDB` / `RAILDB.WORLD`.
- **B.** PDB `RAILDB` — если DBA не может A.

`RAC.WORLD` не переименовывается под `RAILDB`.

## Критерии готовности к переключению (минимум)

- Сверка counts / checksums / current / open repairs / stock / FK.
- Идемпотентность комплектов `(file_name, kit_type)`.
- Успешный rollback drill на копии.
- Default runtime без обязательного `DBTC`.
- Явная строка APPROVALS на cutover.

## План отката после переключения

Простого «переключить datasource на DBTC» недостаточно.

1. **Soak:** legacy writer в `DBTC`; `RAILDB` наполняется миграцией + delta; app может читать для сверки.
2. **Cutover:** writer → `RAILDB`; quarantine входных payloads ≥ окна отката.
3. **Rollback window (≤ 72 ч, политика принята; уточняет ops):** stop RAILDB writers → restore legacy intake → **replay** quarantine в legacy → app на LegacyDbtcRead/synthetic → `RAILDB` сохранить (RMAN/forensics).
4. Потери UI-only операций без reverse path — журнал исключений.
5. После окна — откат на DBTC primary = чрезвычайный отдельный проект.

## Последствия

- 05/06/15: пометка superseded for **target store** (см. эти документы).
- Create / Flyway / JDBC / migrate — только новые строки APPROVALS.
- Sizing/backup: [24](../24_RAILDB_DBA_INSTRUCTION.md), checklist [43](../43_ADR0009_DBA_CREATE_CHECKLIST.md).

## Ссылки

- Decision readiness: [42](../42_ADR0009_DECISION_READINESS.md)
- Migration matrix: [25](../25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md)
- RWL: [12-rwl-api-intake.md](../../analysis/12-rwl-api-intake.md)
- Watched research: [11-nuff-active-wagon-list.md](../../analysis/11-nuff-active-wagon-list.md)
