# ASUOP — следующий этап разработки (после PR #4)

Дата снимка: **2026-11-20**. База: `main` @ `224c3eb` (Merge PR #4 `feature/p0-synthetic-runtime-smoke`).  
Репозиторий: [jumabay/ASUOP-project](https://github.com/jumabay/ASUOP-project).  
Этот документ — **docs-only** результат аудита менеджера + субагентов `/backend`, `/frontend`, `/database`, `/qa`. Он **не** заменяет `APPROVALS_TEMPLATE.md` и ADR.

**Метод:** факты из кода и тестов; функции не считаются реализованными только по описанию в архитектуре.

---

## 0. Состояние репозитория (этап 1)

| Проверка | Результат |
| --- | --- |
| `git pull origin main` | Already up to date |
| Working tree | clean |
| HEAD | PR #4 merged: synthetic REST smoke + Playwright CI + runbook |
| Ветка отчёта | `docs/next-development-stage-after-pr4` |

Ключевые источники: `AGENTS.md`, `docs/architecture/APPROVALS_TEMPLATE.md`, ADR-0001…0009, `docs/agents/ASUOP_DEVELOPMENT_PLAN.md`, `docs/architecture/24`–`39`, код `platform-app` / `modules/*` / `platform-oracle` / `frontend`.

---

## 1. Что реально работает

### 1.1. Продуктовый контур (только profile `synthetic`)

| Возможность | API | UI | Доказательство |
| --- | --- | --- | --- |
| Карточка вагона (R1/A2) | `GET /api/v1/wagons/{code}` | `WagonCardPanel` | MockMvc IT/smoke, Vitest, Playwright |
| История суток (R1/A1) | `GET /api/v1/wagons/{code}/state-reports?date=` | `StateReportGrid` | то же; лимит 500 → 422 |
| Реестр (R2) | `GET /api/v1/wagons?page&size&sort&search` | `WagonRegistryGrid` + пагинация | то же |
| Слежение (R3) | `GET /api/v1/wagons/watched?...` | `WatchedWagonGrid` | то же; **≠** legacy `GetActiveWagonList` |

Единственный REST-контроллер: `WagonController`. Только GET. Default profile: `synthetic`. `DataSourceAutoConfiguration` исключён; `NoOracleDataSourceTest` фиксирует отсутствие DataSource.

### 1.2. Offline-библиотеки (не продукт Boot)

| Компонент | Статус | Связь с app |
| --- | --- | --- |
| R0 `platform-disl-parser` | JUnit acceptance | **не** dependency Boot, нет REST |
| RWL offline `platform-rwl` + `modules/intake` | pipeline + fixtures | **не** на classpath Boot |
| Live RWL client | fail-fast stub | без сокетов |

### 1.3. Документация / дизайн RAILDB (без исполнения)

- ADR-0009 **PROPOSED**; ER `27`, матрица `25`, план 12м `29`, DDL draft `sql/raildb/V0__draft_confirmed_core.sql`
- **P4-0 Storage Contracts (offline) APPROVED** — overview [40_P4_0_RAILDB_STORAGE_CONTRACTS.md](../architecture/40_P4_0_RAILDB_STORAGE_CONTRACTS.md), R1–R3 field map [41_P4_0_R1_R3_FIELD_MAPPING.md](../architecture/41_P4_0_R1_R3_FIELD_MAPPING.md); V0b non-partitioned fallback + `sql/raildb/read/*` stubs (DO NOT EXECUTE)
- P3B/P3C dictionary + semantics: RP1/WH1 **CONFIRMED**; NK1/DT1 **BLOCKED**; G31 **OPEN**
- DBA worksheets offline; apply / create instance — **DENIED**

### 1.4. CI / QA (offline)

Jobs в `.github/workflows/gradle-jdk21-test.yml`: `unit-tests` (Gradle + explicit `SyntheticRuntimeSmokeTest`), `frontend-unit` (Vitest + build), `playwright-smoke` (bootRun synthetic + Chromium).  
Вердикт QA: **PASS** для «synthetic demo / offline CI»; **BLOCKED** для ops / Oracle / legacy parity.  
**CI evidence (follow-up):** push на `main` после merge PR #4 (`224c3eb`) — workflow `Gradle JDK21 test` **success** (run `44690124705`, 2026-11-20). Docs-only PR #5 CI — регрессия того же offline suite.

---

## 2. Что отсутствует

| Область | Факт в коде |
| --- | --- |
| JDBC adapters к Oracle / RAILDB | Нет (кроме fail-fast stub карточки) |
| Создание `RAILDB`, Flyway apply, миграция данных | DENIED; не начато |
| REST/UI ремонтов, склада, отчётов, auth | Нет контроллеров/экранов |
| Live FTP / RWL intake в app | Нет; live DENIED |
| Parity IT vs legacy | `JdbcParityIT` `@Disabled` |
| Полный REF DDL / parts / WH docs в V0 | Неполный относительно ER `27` |
| Источник membership «слежение» | G31 OPEN — миграция M8 blocked |
| Deep-link URL, debounce search, retry UI | UX-долг внутри R1–R3 (не блокер эксплуатации БД) |

Не путать: «DONE (synthetic)» в плане ≠ готовность к промышленной эксплуатации.

---

## 3. Что подготовлено, но не подключено

| Артефакт | Готовность | Почему не подключено |
| --- | --- | --- |
| Порты `WagonCardPort`, `WagonRegistryPort`, `WatchedWagonPort`, `StateReportDayPort` | Стабильны | Адаптеры только synthetic |
| Profile `raildb-test` + `RailDbWagonPortAdapterStub` | Scaffold | Stub только на `WagonCardPort`; остальные порты при активации профиля **не удовлетворены** → Boot не стартует |
| Draft DDL `V0__draft_confirmed_core.sql` | Логическое ядро | Не Flyway; Partitioning license open; NK1/DT1 open |
| Правила open-repair / stock grain | CONFIRMED P3C-1 | Нет Java API/UI и нет целевых таблиц в живой БД |
| R0 parser + RWL normalize + `ImportIntakePort` | Offline tested | Не в Boot; writer в RAILDB «not implemented» |
| `application.yml` секция `raildb-test` | Placeholder | Нет URL/credentials (намеренно) |

---

## 4. Основные блокирующие проблемы (ранжировано)

1. **ADR-0009 не Accepted** + строки Create RAILDB / Flyway / DBTC read / RAILDB write = **DENIED**.
2. **Нет APPROVED TEST Oracle** (и отдельного RAILDB) → JDBC adapters и parity IT закрыты.
3. **NK1 + DT1** (нет DBA tape access plan) → финальная уверенность по ключам/оси времени ленты.
4. **G31** — источник списка слежения не утверждён (не блокирует реестр/карточку).
5. **Profile gap `raildb-test`** — неполный набор stub/adapters.
6. **Натяжение ADR-0006/0007** с целевым store до Accept ADR-0009.
7. Вторично: Partitioning license, неполный V0 vs ER, watched ≠ legacy, RWL≠FTP до B1.

---

## 5. Следующий рекомендуемый функциональный срез

### Выбор

**Срез S1 — «RAILDB storage contracts + read-path design для реестра / карточки / суток» (offline)**  
Код-имя: **P4-0 Storage Contracts (R1–R2 read path)**.

Соответствует приоритету №1 заказчика (собственная RAILDB и контракты хранения), **не** дублирует уже работающий synthetic UI/API и не требует Create/Flyway/JDBC live.

| Кандидат | Почему не сейчас |
| --- | --- |
| Повтор R2/R1 UI | Уже работает на synthetic (PR #4) |
| История как новый экран | Уже есть; нужен store, не второй грид |
| Импорт дислокации | Offline libs есть; live DENIED; writer зависит от store |
| Ремонты/склад | Правила RP1/WH1 готовы, но ниже приоритета store; нет read API |

### Цель среза S1

Зафиксировать и (после отдельного CODE APPROVED) реализовать **offline**:

1. Контракт таблиц `RAIL_APP` для чтения **реестра, карточки, текущего состояния, суточной истории** (подмножество ER, согласованное с REST R1–R2).
2. SQL-черновики bind + page limits (без EXECUTE).
3. Полный набор `@Profile("raildb-test")` **fail-fast stubs** на все четыре порта (Boot стартует, JDBC всё ещё DENIED).
4. Матрица поле↔DTO↔колонка для R1–R2; явные UNKNOWN.
5. Чеклист Accept ADR-0009 + Create RAILDB (для человека/DBA), без выполнения.

**Вне scope S1:** Accept ADR-0009 исполнение, create instance, Flyway apply, ojdbc connect, снятие `@Disabled` с parity, live RWL/FTP, UI ремонтов, смена REST-контрактов R1–R3, G31 resolve (отдельный мини-срез).

### Зависимости и ADR

| Зависимость | Статус |
| --- | --- |
| ADR-0001…0008 Accepted (стек) | OK |
| ADR-0009 | PROPOSED — Accept **не** входит в реализацию S1, только подготовка пакета решений |
| ADR-0002 (JDBC not JPA) | Сохраняется |
| ADR-0006 Flyway scope | После Accept → бизнес-схема RAIL_APP; в S1 только draft |
| Synthetic R1–R3 green | Регрессионный gate — не ломать |

---

## 6. План реализации по агентам

### 6.1. Offline сейчас (после CODE APPROVED на docs+stubs S1)

| Агент | Задачи |
| --- | --- |
| **Lead** | Строка APPROVALS «P4-0 Storage Contracts offline»; ветка `feature/p4-0-raildb-storage-contracts`; PR; не merge без человека |
| **/database** | Уточнить V0/подмножество DDL для wagon+current+disl_event+station (read path); Partitioning fallback DDL; SQL drafts `sql/raildb/read/*` (bind, page); обновить mapping `31` для R1–R2; список OPEN (NK1/DT1) без «закрытия» выдумкой |
| **/backend** | Stubs `@Profile("raildb-test")` на `WagonRegistryPort`, `WatchedWagonPort`, `StateReportDayPort` (+ оставить card stub); тест «контекст `raildb-test` поднимается и порты fail-fast»; **не** добавлять DataSource/URL; не менять synthetic adapters/REST |
| **/frontend** | В S1 — **no product UI**. Опционально позже: debounce/retry (отдельный micro-slice) |
| **/qa** | Регрессия synthetic R0–R3 + P0-5/P1-4; новый тест fail-fast `raildb-test`; secret-hygiene; verdict PASS/BLOCKED |

### 6.2. После S1 — gated срезы (не начинать без APPROVED)

| Срез | Содержание | Блокер APPROVALS |
| --- | --- | --- |
| **S2** | Accept ADR-0009 + DBA create `RAILDB` (A/B) + charset/backup/sizing | Accept ADR-0009; Create Oracle RAILDB |
| **S3** | Flyway apply V0 (или non-partitioned) на пустой RAIL_APP | RAILDB Flyway apply |
| **S4** | JDBC read adapters R1–R2 (+ optional watched) на TEST RAILDB; bounded IT | RAILDB read JDBC / TEST URL; не Production |
| **S5** | DBTC→RAILDB migrator M1–M6 (12м + FULL) | DBTC read + RAILDB write |
| **S6** | Import path (R0/RWL) → RAIL_IMPORT_KIT / events | Writer + B1 policy; live отдельно |
| **S7** | Repairs/warehouse read screens | Research G6+; новый product APPROVED |

---

## 7. Необходимые approvals

### 7.1. Предложение для **немедленного** offline S1 (ещё не в журнале)

| Decision | Предлагаемый статус | Разрешает | Не разрешает |
| --- | --- | --- | --- |
| P4-0 RAILDB storage contracts (offline) | **APPROVED** (журнал); docs `40`/`41` | Docs DDL/SQL drafts; fail-fast stubs всех read-портов под `raildb-test`; mapping R1–R3; тесты без Oracle | JDBC connect; Flyway apply; create RAILDB; DBTC/RAILDB DML; live FTP/RWL; смена REST R1–R3; Accept ADR-0009 |

*Этот docs-only PR **не** добавляет строку в `APPROVALS_TEMPLATE.md` без явного согласия владельца — ниже только текст для копирования.*

### 7.2. Заблокировано до отдельных строк (предложения, **не выполнять**)

| Decision | Зачем |
| --- | --- |
| Accept ADR-0009 | Целевой store; снять натяжение 05/06/15, ADR-0006/0007 |
| Create Oracle RAILDB (instance/PDB) | После DBA checklist `24` |
| RAILDB Flyway apply / business DDL | После Accept + create |
| DBTC read for migration | Выгрузки 12м + FULL |
| RAILDB write (app or migrator) | Наполнение и cutover writer |
| R1 JDBC against TEST Oracle **или** RAILDB TEST read | Реальные adapters + IT |
| DBA tape access plan + NK1/DT1 SELECT | Закрытие семантики ленты |
| G31 SPEC / business pick Active vs wagons.txt | M8 watched membership |
| RWL live / FTP cutover | После store + B1 |
| Production access | Всегда отдельно |

---

## 8. API-контракты и схемы данных (S1 якорь)

### 8.1. REST (без изменения в S1)

Сохранить:

- `GET /api/v1/wagons` → registry page DTO  
- `GET /api/v1/wagons/{code}` → card DTO  
- `GET /api/v1/wagons/{code}/state-reports?date=YYYY-MM-DD` → day DTO (half-open сутки; R1-Q-TZ open)  
- `GET /api/v1/wagons/watched` → watched page (источник membership — UNKNOWN до G31)

### 8.2. Целевые таблицы чтения (логическая)

| Use-case | Таблицы RAIL_APP | Примечание |
| --- | --- | --- |
| Registry / card | `rail_wagon`, `rail_station` (+ later refs) | UK `wagon_code` |
| Current on card | `rail_wagon_current` | Не путать с полной лентой |
| Day history | `rail_disl_event` | Filter by wagon + getting_ts day; page/limit ≤500; **retain duplicates** |
| Watched | `rail_wagon_watched` | Пусто до утверждённого snapshot |

Черновик: `docs/architecture/sql/raildb/V0__draft_confirmed_core.sql`.  
До закрытия DT1 ось миграции/фильтра истории = **getting_ts / GETTING_DATE**.

---

## 9. Тестовый план и критерии завершения

### 9.1. Тесты S1 (offline)

1. Существующий `./gradlew test` + frontend Vitest/build + Playwright CI — зелёные (регрессия).  
2. Новый тест: profile `raildb-test` → все read-порты бросают ожидаемый fail-fast; **нет** DataSource.  
3. `/qa`: secret-review diff; `JdbcParityIT` остаётся `@Disabled`.  
4. Docs: mapping R1–R2 ↔ колонки; список OPEN/BLOCKED без ложного CLOSED.

### 9.2. Definition of Done — S1

- [x] Строка APPROVED P4-0 в журнале  
- [ ] DDL/SQL drafts + mapping committed (`40`/`41`, V0b, `sql/raildb/read/*`); V0 не «применён»  
- [ ] Stubs всех четырёх портов под `raildb-test`; synthetic не сломан  
- [ ] QA verdict PASS  
- [ ] PR в `main`; merge только человеком  
- [ ] Следующий gate (Accept ADR-0009 / Create) оформлен предложением, не выполнен

### 9.3. Критерии «ближе к эксплуатации» (после S2–S4, не S1)

- Принятый ADR-0009; существующий `RAILDB`; Flyway schema; JDBC read R1–R2 на TEST с bounded queries; counts сверки на копии.

---

## 10. Оценка рисков

| Риск | Влияние | Митигация |
| --- | --- | --- |
| Считать synthetic = prod-ready | Ложная готовность | Явная маркировка в UI/docs; этот отчёт |
| Ранний Create RAILDB до Accept/sizing | Стоимость отката | Держать DENIED до checklist `24` |
| Включить DataSource «на всякий случай» | Утечка к RAC/prod | Fail-fast stubs; exclude auto-config до APPROVED URL |
| Закрыть NK1/DT1 догадкой | Порча миграции ленты | Оставить BLOCKED; DBA plan |
| Параллельная смена REST при JDBC | Ломает FE/CI | S1–S4 не меняют контракты R1–R3 |
| Watched JDBC = synthetic 60 | Неверный смысл | G31 / business pick до M8 |

---

## 11. Сводка аудитов субагентов

| Агент | Вердикт (сжато) |
| --- | --- |
| [Backend](029a8370-ca5c-4d06-a946-985f87d90f4e) | R1–R3 synthetic E2E; JDBC отсутствует; `raildb-test` неполный; R0/RWL вне Boot |
| [Frontend](fdde0be8-dd60-40ff-b65f-ae0900aedb45) | 4 экрана на одном SPA; API match; UX-долг мелкий; repairs UI не начинать |
| [Database](c87fc4eb-61fa-4d61-8841-b19ecfa612fa) | P3C-1 partial-ready design; P4 create/Flyway blocked; RP1/WH1 OK; NK1/DT1/G31 open |
| [QA](41932a24-582b-4556-82f2-029bed742d22) | Synthetic CI posture PASS; ops/parity BLOCKED |

---

## 12. Следующее действие владельца

1. ~~P4-0~~ — **DONE** (PR #6).  
2. ~~Accept ADR-0009 direction~~ — **DONE** (2026-11-20); checklist Create: [43](../architecture/43_ADR0009_DBA_CREATE_CHECKLIST.md).  
3. Далее — DBA checklist → отдельные APPROVALS на Create / Flyway / TEST JDBC.  
4. NK1/DT1/G31/R1-Q-TZ **OPEN**; не снимать `@Disabled` с JDBC; Production запрещён.

---

## Ссылки

- План: [ASUOP_DEVELOPMENT_PLAN.md](ASUOP_DEVELOPMENT_PLAN.md)  
- Approvals: [../architecture/APPROVALS_TEMPLATE.md](../architecture/APPROVALS_TEMPLATE.md)  
- ADR-0009: [../architecture/decisions/ADR-0009-raildb-primary-store.md](../architecture/decisions/ADR-0009-raildb-primary-store.md)  
- P4-0 contracts: [40](../architecture/40_P4_0_RAILDB_STORAGE_CONTRACTS.md), [41](../architecture/41_P4_0_R1_R3_FIELD_MAPPING.md)  
- ER / 12м / matrix: [27](../architecture/27_RAILDB_ER_MODEL.md), [29](../architecture/29_RAILDB_MIGRATION_12M_PLAN.md), [25](../architecture/25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md)  
- Smoke: [SYNTHETIC_RUNTIME_SMOKE.md](SYNTHETIC_RUNTIME_SMOKE.md)
