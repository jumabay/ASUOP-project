# 33. P3B-0 — Инструкция: словарь DBTC в Oracle SQL Developer

**Не выполнять**, пока в [APPROVALS_TEMPLATE.md](APPROVALS_TEMPLATE.md) нет строки, разрешающей P3B dictionary probes.  
Этот документ — подготовка к ручному прогону.

Скрипт: [sql/dba/01_dictionary_probes.sql](sql/dba/01_dictionary_probes.sql).  
Аудит: [32_P3B0_DICTIONARY_PROBE_AUDIT.md](32_P3B0_DICTIONARY_PROBE_AUDIT.md).

## 1. Подготовка учётной записи

1. Создать/выделить **read-only** пользователя (рекомендуемые имена: `RAIL_DICT_RO` или существующий `RAILWAY_READONLY`).
2. Минимальные привилегии: `CREATE SESSION`; возможность читать словарь по объектам `DBTC` (как у прежнего smoke: часто достаточно прав видеть `ALL_*` для accessible objects; при необходимости — роль `SELECT_CATALOG_ROLE` **только** если политика заказчика позволяет).
3. **Не** выдавать `DBA`, `RESOURCE`, DDL на `DBTC`, и **не** использовать схему `RAIL_APP` (RAC или будущая) как админскую для этого прогона.
4. Пароль — только secret store / локальный SQL Developer connection; **не** в Git, чат, скриншоты.

## 2. Connection в SQL Developer

| Поле | Значение |
| --- | --- |
| Connection type | Basic |
| Host / Port / Service | по факту TEST/copy (пром `tc-asuvp-03`/`RAC.WORLD` — только при scoped approve) |
| Username | read-only account |
| Role | Default |

Проверка: после Connect выполнить **только Block A**. Сверить `DB_NAME` / `SERVICE_NAME` / `SESSION_USER` с ожидаемой средой. При несовпадении — **Stop**.

## 3. Порядок выполнения

1. Открыть `01_dictionary_probes.sql` в Worksheet.
2. Выделять и Run **один блок** (A → M) за раз (`Ctrl+Enter` на выделенном).
3. Не запускать весь файл целиком (F5), чтобы ограничить нагрузку и проще сохранить результаты.
4. После каждого блока: Query Result → Export → CSV (UTF-8). Имя файла: `p3b_<block>_<YYYYMMDD>.csv`.
5. В тикет/репозиторий класть **только CSV без секретов** (нет JDBC URL с паролем, нет connection screenshots с password).
6. Блоки B1–B6 из аудита (§ BLOCKED) **не** выполнять в этом прогоне.

## 4. Контроль нагрузки

| Признак | Действие |
| --- | --- |
| Запрос > 30–60 с на словаре | Cancel; сообщить DBA |
| Случайный запуск SELECT по `STATEREPORTRECORDS` | немедленно Cancel; не повторять |
| ORA-01031 / нет доступа к ALL_* | остановить; запросить минимальные grants, не эскалировать до DBA роли без approve |

Ожидаемо: словарь-запросы секунды, не минуты. `ALL_ARGUMENTS` / `ALL_SOURCE` (spec) — дольше, но всё ещё metadata.

## 5. Что сохранить для P3B анализа

- CSV блоков B–E (колонки, ключи, индексы ленты).  
- CSV G (stats).  
- CSV I/M (Active list signature / watched clues).  
- CSV K/L (repairs/warehouse columns).  
- Отметить: есть ли строки в Block F (partitions).

## 6. После прогона

1. Передать CSV разработчику платформы (без паролей).  
2. Обновить ER/DDL UK только в отдельном P3B deliverable.  
3. Не apply `V0__draft_confirmed_core.sql`.  
4. Partitioning license check для RAILDB — отдельный вопрос DBA (§4 аудита).

## 7. Чеклист «можно нажимать Run»

- [ ] Строка APPROVALS на P3B / dictionary probes = APPROVED  
- [ ] Среда подтверждена (TEST/copy vs prod)  
- [ ] Учётка read-only, не `RAIL_APP` admin  
- [ ] Block A совпал с ожиданием  
- [ ] План экспорта CSV без секретов  
- [ ] Запрет COUNT(*) / full tape sample согласован  
