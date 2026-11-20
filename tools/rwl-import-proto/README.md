# RWL import prototype

Одноразовый CLI для проверки чтения дислокации из RailWagonLocation (RWL) и записи в **отдельные staging-таблицы** новой Oracle **RAILDB**.

Не является частью runtime `platform-app`. Профиль `synthetic` не затрагивается.

**Целевая БД:** новая RAILDB (основная база новой системы).  
**Не целевая:** старая RAC/DBTC (архив после перехода), в т.ч. ранее созданный `RAIL_APP` в `RAC.WORLD`.

## Что делает

- Live: только `view_vagon` по номерам из `RWL_VAGON_NOS` (максимум 5). Полный `get_user_vagons` по умолчанию **не** вызывается.
- Формат ответа: документированный **XML** (default API). JSON не выдумывается по XML-примерам.
- Staging: `rwl_import_run`, `rwl_staging_operation`, `rwl_staging_current`, `rwl_staging_reject` в схеме подключенного пользователя RAILDB.
- Перед DDL/DML печатаются: `USER`, `CURRENT_SCHEMA`, `DB_NAME`, `SERVICE_NAME`, `CON_NAME` (+ banner при доступе). База не определяется только по имени пользователя.
- Legacy `RAC.WORLD` / `DB_NAME=RAC` по умолчанию **отклоняется** (нужен `--allow-archive-oracle` только для явной работы с архивом).
- Дедуп: гипотетический `event_key` + `content_hash`; повтор → unchanged; изменение содержимого при том же ключе → ревизия.

## Переменные окружения

Java **не** читает файл `.env` сама. Варианты:

1. Задать переменные в текущей сессии PowerShell:

```powershell
$env:RWL_NAME = "..."
$env:RWL_PASSWORD = "..."
$env:RWL_VAGON_NOS = "12345678;87654321"
$env:RAILDB_JDBC_URL = "jdbc:oracle:thin:@//HOST:1521/RAILDB_SERVICE"
$env:RAILDB_USER = "<user from RAILDB config>"
$env:RAILDB_PASSWORD = "..."
# Optional pin (recommended):
$env:EXPECTED_ORACLE_SERVICE = "RAILDB_SERVICE"
$env:EXPECTED_ORACLE_DB_NAME = "RAILDB"
```

2. Либо передать файл явно: `--env-file=C:\path\to\raildb-rwl.env`  
   (KEY=VALUE, без секретов в Git; файл вне репозитория).

| Переменная | Назначение |
|---|---|
| `RWL_NAME` / `RWL_PASSWORD` | Учётка RWL |
| `RWL_VAGON_NOS` | До 5 номеров через `;` |
| `RWL_BASE_URL` | Опционально, default export.php |
| `RAILDB_JDBC_URL` или `ORACLE_JDBC_URL` | Полный JDBC URL **новой RAILDB** |
| `RAILDB_USER` или `ORACLE_USER` | Пользователь/схема **новой** RAILDB из фактической конфигурации. По ADR-0009 целевая схема — `RAIL_APP` **внутри RAILDB** (это не `RAIL_APP` в старой RAC) |
| `RAILDB_PASSWORD` или `ORACLE_PASSWORD` | Пароль |
| `EXPECTED_ORACLE_SERVICE` | Опциональная сверка `SERVICE_NAME` |
| `EXPECTED_ORACLE_DB_NAME` | Опциональная сверка `DB_NAME` |

`RAC.WORLD` — сервис старого архива, не URL и не цель этого импорта.

## Команды

Требуется **JDK 21** (`JAVA_HOME`). На Windows, если путь проекта содержит кириллицу, используйте `subst` на ASCII-диск.

**PowerShell** (важно: `.\gradlew.bat`, не `gradlew.bat`; переменные через `$env:...`, не `set`):

```powershell
subst R: (Get-Location).Path
Set-Location R:\
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat test
.\gradlew.bat run --args="--dry-run --fixture=src/test/resources/fixtures/success-two-wagons.xml"
.\gradlew.bat run --args="--import --db=h2 --init-schema --fixture=src/test/resources/fixtures/success-two-wagons.xml"
$envFile = Join-Path $env:USERPROFILE 'raildb-rwl.env'
$saveDir = Join-Path $env:USERPROFILE 'rwl-captures'
.\gradlew.bat run --args="--dry-run --env-file=$envFile --save-dir=$saveDir"
```

Повтор сохранённого live-пакета (вне Git). После dry-run возьмите **реальное** имя из `$env:USERPROFILE\rwl-captures`:

```powershell
$fixture = Join-Path $env:USERPROFILE 'rwl-captures\rwl-view_vagon-20261116T050000Z-1.xml'
.\gradlew.bat run --args="--import --init-schema --fixture=$fixture --env-file=$envFile"
```

**cmd.exe** (если не PowerShell):

```bat
subst R: "%CD%"
R:
set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.12"
set "PATH=%JAVA_HOME%\bin;%PATH%"
gradlew.bat run --args="--dry-run --env-file=%USERPROFILE%\raildb-rwl.env --save-dir=%USERPROFILE%\rwl-captures"
```

Явные фикстуры (не live RWL) — ревизия веса и FAILED:

```powershell
.\gradlew.bat run --args="--import --db=h2 --fixture=src/test/resources/fixtures/success-corrected-weight.xml"
.\gradlew.bat run --args="--import --db=h2 --force-write-failure --fixture=src/test/resources/fixtures/success-two-wagons.xml"
```

### Таблица соответствия полей (staging)

| Staging | RWL XML |
|---|---|
| `wagon_no` | `vagon_info/vagon_no` |
| `station_code` / `station_name` | `position/current_position_code` / `current_position` |
| `operation_time_utc` | `position/utc_datetime` (TIMESTAMP + UTC Calendar) |
| `operation_code` / `operation_asoup_code` / `operation_name` | `operation_code` / `operation_asoup_code` / `operation` |
| `train_index` / `train_number` | `train_index` / `train_number` |
| `from_*` / `to_*` | `vagon_info/from_code|from_name` / `to_code|to_name` |
| `loaded_flag` / `cargo_name` / `etsng_code` / `weight_tons` | `loaded` / `cargo_name` / `etsng_code` / `weight` |
| `latitude` / `longitude` | `current_position_latitude` / `longitude` |
| `source_code` / `fetched_at_utc` | константа `RWL` / время получения ответа |

UNKNOWN: `operation_id` (тип, не event id); `position/from_station|to_station` vs `vagon_info` from/to; `state` vs `loaded`; `update_time` (Riga).

### Event key (гипотеза) и ограничения

`wagon_no|utc_datetime|station_code|operation_asoup_code|train_index` + `content_hash`.

- Повтор одинакового содержимого → `unchanged`, 0 новых операций.
- Изменение веса/груза при том же ключе → новая **ревизия**.
- Исправление `station_code`, кода операции или `train_index` → **другой** `event_key` (новая запись), не ревизия.
- Без обязательных полей идентичности → `rwl_staging_reject`.

## Статусы run

| Status | Смысл |
|---|---|
| `SUCCESS` | Данные обработаны |
| `EMPTY` | Корректный ответ без операций |
| `API_ERROR` | Прикладная ошибка RWL / HTTP |
| `PARSE_ERROR` | Не XML / неизвестная структура |
| `FAILED` | Ошибка записи; операции откатены; insert/update = 0 |

## Oracle DDL

См. `src/main/resources/ddl/oracle-staging.sql` (схема подключенного пользователя RAILDB).  
H2 DDL только для автотестов и не подтверждает совместимость Oracle.
