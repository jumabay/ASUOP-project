# ADR-0009. RAILDB как основное хранилище; DBTC — миграция и архив

## Статус

**PROPOSED** (2026-11-16). Не Accepted. Не открывает JDBC, DDL, DML, создание `RAILDB`, Flyway apply или Production access.

Пакет готовности к решению (без Accept): [42_ADR0009_DECISION_READINESS.md](../42_ADR0009_DECISION_READINESS.md) (2026-11-20).

## Контекст

Утверждённые ранее документы фиксируют схему `DBTC` как единственное бизнес-хранилище на всём переносе («второй базы нет»): [05_ORACLE_INTEGRATION.md](../05_ORACLE_INTEGRATION.md), [06_DATA_STORAGE_STRATEGY.md](../06_DATA_STORAGE_STRATEGY.md), [ADR-0006](ADR-0006-flyway-app-schema-only.md), [ADR-0007](ADR-0007-single-writer-read-first.md).

Заказчик утвердил **вариант 2**: новая платформа NUFF / ВВП / NUUD должна иметь **собственную основную** Oracle-базу и после переключения не зависеть от `DBTC` в повседневной работе. `DBTC` остаётся источником первичной миграции, сверки и архивом данных старше окна переноса.

В существующей RAC уже создана схема-пользователь `RAIL_APP` (квота 5 ГиБ). Это **не** целевое самостоятельное хранилище и **не** отдельная база.

## Решение (предлагаемое)

1. Создать **отдельную Oracle Database `RAILDB`** (совместимую с 12.2 на этапе внедрения; апгрейд до 19c — отдельное окно DBA) на хосте `TC-ASUVP-03` **только после** проверки ресурсов, лицензии, бэкапов и явного разрешения в журнале APPROVALS.
2. Рабочая схема приложения внутри `RAILDB`: **`RAIL_APP`**.
3. После cutover все **новые** бизнес-данные пишутся только в `RAILDB`. Обычная работа Java-приложения **не требует** подключения к `DBTC`.
4. `DBTC` на этапе миграции **не изменяется** (нет DDL/DML от этого проекта). После подтверждённого переключения `DBTC` — архив (история старше окна + fallback forensics).
5. Первичная миграция: история **последних 12 календарных месяцев** до даты cutover (`cutoff = cutover_ts − 12 months`) **плюс** все актуальные сущности, открытые операции, остатки, справочники и bootstrap начальных состояний на границе cutoff. Детали — [25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md](../25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md).
6. Квота 5 ГиБ схемы `RAIL_APP` в RAC **не** используется как ориентир sizing `RAILDB` (см. оценку объёмов в инструкции DBA).
7. Схема `RAIL_APP` внутри RAC не является prod store; возможный staging только по отдельной строке журнала.
8. Java сохраняет modular monolith, порты R0–R3 и REST-контракты; адаптеры: Synthetic / LegacyDbtcRead / RailDbJdbc / RwlApiClient / FtpIntake.
9. RWL API — кандидат источника дислокации, **не** доказанная замена FTP до B1-сверки полноты, лимитов и периодичности ([12-rwl-api-intake.md](../../analysis/12-rwl-api-intake.md)).
10. Flyway (когда будет разрешён) применяется только к `RAILDB.RAIL_APP`, никогда к ленте `DBTC`.

## Отношение к принятым ADR

| ADR | Действие при Accept ADR-0009 |
| --- | --- |
| ADR-0001 | Уточнить: контуры платформы живут на `RAILDB`, не «одна схема DBTC» |
| ADR-0002 | JDBC/не-JPA сохраняется; целевые таблицы — `RAILDB`; пакеты `DBTC` — только миграция/архивное чтение |
| ADR-0003 | Драйвер `ojdbc10:19.33.0.0` (+ `orai18n`) остаётся до решения по версии `RAILDB` |
| ADR-0006 | Scope Flyway расширяется до **бизнес-схемы `RAILDB.RAIL_APP`**; запрет DDL на `DBTC` сохраняется |
| ADR-0007 | После cutover единственный writer бизнес-данных — новое приложение → `RAILDB`; до cutover legacy остаётся writer в `DBTC` |
| ADR-0008 | Без изменения DIP/портов |

Историю прежних ADR **не удалять**.

## Инфраструктура (выбор DBA)

Допустимые целевые формы (решение DBA, зафиксировать в журнале до создания):

- **A.** Отдельный instance: `ORACLE_SID=RAILDB`, service name например `RAILDB` / `RAILDB.WORLD`, listener shared или dedicated.
- **B.** CDB на том же хосте, PDB `RAILDB`, service = PDB service name.

Существующий сервис `RAC.WORLD` остаётся для текущей RAC/`DBTC` и **не** переименовывается под `RAILDB`.

## Критерии готовности к переключению (минимум)

- Сверка counts / контрольных сумм / текущих состояний вагонов / open repairs / остатков / FK.
- Идемпотентность комплектов R0 `(file_name, kit_type)`.
- Успешный drill отката (см. ниже) на копии.
- Default runtime profile без обязательного `DBTC`.
- Явная строка APPROVALS на cutover.

## План отката после переключения (реалистичный)

Простого «переключить datasource обратно на DBTC» **недостаточно**: после cutover в `RAILDB` появляются данные, которых нет в архиве `DBTC`.

1. **До cutover (soak):** legacy writer в `DBTC` ещё работает; `RAILDB` наполняется миграцией + delta; приложение может читать для сверки, но не является единственным writer.
2. **Cutover:** остановить запись новых бизнес-данных в `DBTC` со стороны нового контура; включить writer → `RAILDB`; сохранить все входные файлы/ответы API в карантине ≥ срока отката.
3. **Rollback window (рекомендуемо ≤ 72 ч, уточняет ops):**
   - остановить writers в `RAILDB`;
   - восстановить приём legacy importer в `DBTC`;
   - **replay** сохранённых FTP/API payloads с момента cutover в legacy-контур (предпочтительно) **или** контролируемый reverse-export из `RAILDB` только для событий, которые legacy умеет принять;
   - переключить приложение на `LegacyDbtcRead` / synthetic;
   - `RAILDB` не уничтожать — forensics + RMAN.
4. **Потери при откате:** ручные операции, выполненные только в новом UI и не имеющие обратного контура в legacy, требуют журнала исключений и ручной сверки.
5. **После закрытия rollback window:** откат на `DBTC` как primary считается чрезвычайным и отдельным проектом.

## Последствия

- Нужны новые строки APPROVALS на создание `RAILDB`, read `DBTC`, write `RAILDB`, cutover.
- Документы 05/06/15 требуют пометки «superseded for target store by ADR-0009» после Accept (не в этом PROPOSED-срезе обязательно править текст целиком).
- Sizing, backup и charset — в [24_RAILDB_DBA_INSTRUCTION.md](../24_RAILDB_DBA_INSTRUCTION.md).

## Ссылки

- Матрица миграции: [25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md](../25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md)
- RWL: [12-rwl-api-intake.md](../../analysis/12-rwl-api-intake.md)
- Watched list research: [11-nuff-active-wagon-list.md](../../analysis/11-nuff-active-wagon-list.md)
