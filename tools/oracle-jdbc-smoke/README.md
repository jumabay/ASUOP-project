# Oracle JDBC smoke (one-shot)

Минимальная проверка Java 21 → Oracle. Не Spring, не `platform-oracle`, не часть `platform-disl-parser`.

## Утверждённые артефакты (ADR-0003, 2026-11-14)

| Артефакт | Координат |
|---|---|
| JDBC | `com.oracle.database.jdbc:ojdbc10:19.33.0.0` |
| NLS / charset | `com.oracle.database.nls:orai18n:19.33.0.0` |

`orai18n` нужен для кодировки БД `CL8MSWIN1251` (иначе JDBC `17056`). Параметры NLS на сервере не меняем.

Не использовать `ojdbc11:23.x`. Формулировка «ojdbc11 линии 19c» снята: на Central у `ojdbc11` нет `19.x`.

## Риск хоста

`tc-asuvp-03:1521/RAC.WORLD` — **потенциально промышленная** база. Постоянного allow-list нет.

Одноразовый connect:

```powershell
.\gradlew.bat run --args="--connect --allow-potential-prod-smoke-once"
```

## Переменные окружения (Windows)

Не коммитить. Не слать пароль в чат.

```powershell
$env:ORACLE_JDBC_URL  = 'jdbc:oracle:thin:@//tc-asuvp-03:1521/RAC.WORLD'
$env:ORACLE_USER      = 'RAILWAY_READONLY'
$env:ORACLE_PASSWORD  = '***'   # только локально
```

Проверка без пароля:

```powershell
'URL=' + $env:ORACLE_JDBC_URL
'USER=' + $env:ORACLE_USER
'PWD set=' + [bool]$env:ORACLE_PASSWORD
```

## Запуск

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12'
.\gradlew.bat verifyApprovedDriver
.\gradlew.bat run --args='--dry-run'
# только при заданных env и scoped-разрешении:
.\gradlew.bat run --args='--connect --allow-potential-prod-smoke-once'
```

Область connect: сессия, привилегии, `SELECT 1 FROM DUAL`, версия, наличие схемы `DBTC` в `ALL_USERS`. Без бизнес-таблиц, DML/DDL/PL/SQL, R1 и API.

## Классификация привилегий

`READ ANY TABLE` — чтение (не STOP). `SELECT ANY TABLE` — широкое чтение (NOTE, не STOP).  
`INHERIT PRIVILEGES` (объектная, USER / IR PL/SQL) — NOTE, не STOP; не даёт DML по таблицам.  
`INHERIT ANY PRIVILEGES` (системная) — STOP.  
STOP также: `INSERT/UPDATE/DELETE/ALTER/DROP ANY TABLE`, `CREATE/EXECUTE ANY PROCEDURE`, admin-роли и т.п. Неизвестные привилегии — STOP.

Unit-тесты: `.\gradlew.bat test` (на Windows при кириллице в пути проекта удобнее junction на ASCII-путь).
