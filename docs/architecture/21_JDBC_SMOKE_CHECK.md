# 21. JDBC smoke-check — результаты

Дата прогона (заключительный успешный): **2026-11-14**.  
Инструмент: `tools/oracle-jdbc-smoke` (не Spring, не `platform-oracle`, не часть `platform-disl-parser`).

## Статус

**Успешно** в пределах scoped-разрешения на одноразовую диагностику.  
Это **не** открытие постоянного доступа к промышленной Oracle и **не** старт R1.

## Цель

Проверить связку Java 21 + JDBC 19c (`ojdbc10`) + NLS (`orai18n`) к Oracle 12.2 и учётку `RAILWAY_READONLY` короткими read-only `SELECT` по DUAL и системным представлениям.

## Разрешение на подключение

- Журнал [APPROVALS_TEMPLATE.md](APPROVALS_TEMPLATE.md): **Production access = DENIED UNTIL APPROVED** остаётся в силе.
- Отдельно выдавались **разовые** разрешения на диагностический JDBC smoke к хосту `tc-asuvp-03:1521/RAC.WORLD` (потенциально промышленная база; тестовой копией не считается).
- Постоянный allow-list для `tc-asuvp-03` **не создаётся**.
- Повторные подключения к этому хосту без нового явного разрешения **запрещены**.

## Стек прогона

| Компонент | Версия / значение |
|---|---|
| Java | 21.0.12 |
| JDBC | `com.oracle.database.jdbc:ojdbc10:19.33.0.0` |
| NLS | `com.oracle.database.nls:orai18n:19.33.0.0` (нужен для `CL8MSWIN1251`) |
| Oracle | Database 12c Enterprise Edition **12.2.0.1.0** 64-bit |
| Service | `RAC.WORLD` (`DB_NAME=RAC`, `SERVER_HOST=tc-asuvp-03`) |
| Пользователь | `RAILWAY_READONLY` |

Секреты и полные строки с паролем в документ **не** включаются. Учётные данные только из локальных переменных окружения `ORACLE_*`.

## Результаты проверок

| Проверка | Результат |
|---|---|
| Подключение JDBC | успех |
| `CL8MSWIN1251` через `orai18n` | успех (иначе ранее был JDBC `17056`) |
| `SELECT 1 FROM DUAL` | OK |
| Версия БД | 12.2.0.1.0 |
| Схема `DBTC` в `ALL_USERS` | YES |
| Чтение бизнес-таблиц `DBTC` | **не выполнялось** |
| DML / DDL / PL/SQL | **не выполнялось** |

## Права `RAILWAY_READONLY` (снимок smoke)

| Источник | Наблюдение | Классификация smoke |
|---|---|---|
| `SESSION_PRIVS` | `CREATE SESSION`, `READ ANY TABLE` | чтение |
| `SESSION_ROLES` | отсутствуют | — |
| `USER_TAB_PRIVS` (виды) | `INHERIT PRIVILEGES=1` | NOTE (не DML; IR / USER), не STOP |
| Прямые INSERT/UPDATE/DELETE/ALTER/DROP / admin | не обнаружены в снимке | — |

Классификатор: `PrivilegeClassifier` в `tools/oracle-jdbc-smoke` (локальные тесты 10/10 на момент фиксации).

## Что smoke не доказывает

- Готовность промышленного чтения для UI.
- Совместимость вызовов пакетов `DBTC` (курсоры, `EXECUTE`).
- Наличие **тестовой** копии / обезличенных данных для R1.
- Закрытие пробелов G1 и группы A/B.

## Артефакты кода

- `tools/oracle-jdbc-smoke/` — одноразовый инструмент.
- ADR драйвера: [decisions/ADR-0003-oracle-jdbc-19c-against-12-2.md](decisions/ADR-0003-oracle-jdbc-19c-against-12-2.md).
- Интеграция: [05_ORACLE_INTEGRATION.md](05_ORACLE_INTEGRATION.md).

## Ограничения процесса

- Не извлекать секреты из памяти или окружения чужих процессов.
- Не сохранять пароли в Git, исходниках, логах и архитектурных документах.
- Не использовать `ojdbc11:23.x` к Database 12.2.
