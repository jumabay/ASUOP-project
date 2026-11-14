# 23. План минимальной реализации R1: A1 и A2

Статус: **согласован с уточнениями** (DIP, HTTP 422, Modulith) — код не писать до отдельного разрешения.  
См. [ADR-0008](decisions/ADR-0008-gradle-multi-project.md).

## Ограничения (сохранены)

| Правило | Значение |
|---|---|
| Scope | только A1 + A2 |
| Profile | только **synthetic** |
| Oracle / JDBC | запрещены; `tc-asuvp-03` запрещён |
| JDBC parity | `@Disabled` |
| A3–A8 / FTP / импорт / доп. модули | out of scope |
| `platform-disl-parser` | поведение не менять |
| Пром allow-list | не создавать |
| Сутки | полуоткрытый интервал |
| TZ | открытый вопрос **R1-Q-TZ** |

---

## 1. Критерии A1 / A2

### A2 — карточка

Поля: `wagonCode`, `model`, `wagonKind`, `ownerName`, `homeStationCode`, `homeStationName`, `inventoryPark`.  
Не входят: текущая станция, `CURRENTSTATE_ID`, дата дислокации.  
Синтетика: GET = golden fixture; неизвестный код → 404.  
Будущий Oracle: `WagonPackage.GetWagonByCode`.

### A1 — сутки среза

Ключи строки: `reportId`, `wagonCode`, `dislocationTime`, `stationCode`, `operationCode`.  
Пустой день: `lines: []`. Порядок не сравнивается.  
Будущий Oracle: `StateReportPackage.GetWagonStateReports`.

A2 не требует равенства со станцией последней строки A1.

---

## 2. Интервал суток

```text
beginInclusive = D 00:00:00
endExclusive   = (D + 1 day) 00:00:00
входит ⇔ beginInclusive <= dislocationTime < endExclusive
```

Не использовать `BETWEEN … 23:59:59`. Дроби секунды до `endExclusive` входят в день `D`.

### R1-Q-TZ (открыто)

Legacy вызывал 00:00:00–23:59:59 wall-clock без доказанной TZ.  
Synthetic: naive `LocalDateTime`. Не зашивать `Asia/Almaty` / UTC как факт системы.

---

## 3. Окончательная структура проекта

```text
АСУВП/
  settings.gradle.kts
  build.gradle.kts
  gradle/libs.versions.toml
  platform-disl-parser/                 # R0, без изменения поведения
  modules/
    fleet/                              # A2: domain, use-case, PORT
      .../platform/fleet/
        WagonCard.java
        WagonCardQuery.java
        WagonCardPort.java              # интерфейс доступа к данным (владелец — fleet)
    dislocation/                        # A1: domain, use-case, PORT
      .../platform/dislocation/
        StateReportDay.java
        StateReportLine.java
        DayStateReportQuery.java
        StateReportDayPort.java
  platform-oracle/                      # INFRA adapters only
    .../platform/oracle/
      synthetic/
        SyntheticWagonCardAdapter.java      # implements WagonCardPort
        SyntheticStateReportDayAdapter.java # implements StateReportDayPort
      jdbc/                             # не активировать
  platform-app/                         # единственный Spring Boot
    .../platform/app/
      RailwayPlatformApplication.java
      api/...
  frontend/
  tools/oracle-jdbc-smoke/
  docs/
```

---

## 4. Граф зависимостей (исправленный)

```text
frontend --HTTP--> platform-app

platform-app --> modules:fleet
platform-app --> modules:dislocation
platform-app --> platform-oracle          # регистрирует adapters как Spring beans

platform-oracle --> modules:fleet         # implements WagonCardPort
platform-oracle --> modules:dislocation   # implements StateReportDayPort

modules:fleet          ↛ platform-oracle, ojdbc, adapter classes
modules:dislocation    ↛ platform-oracle, ojdbc, adapter classes
platform-disl-parser   ↛ R1 app runtime
```

Слои:

```text
React → REST → fleet/dislocation Query
             → Port (в том же бизнес-модуле)
             → Adapter в platform-oracle (synthetic)
```

---

## 5. Modulith 1.4.x

- Один Boot-контейнер: `platform-app`.
- Логические модули: `fleet` / `dislocation` (CLOSED), `oracle` (OPEN infra).
- `ApplicationModules.verify()` в тестах app.
- Без лишних Modulith runtime-фич в R1.
- При конфликте verify с multi-jar — стоп и согласование минимального fallback (detection strategy / allowedDependencies), без новых контейнеров.

---

## 6. DTO / API

### WagonCardDto (A2)

`wagonCode`, `model`, `wagonKind`, `ownerName`, `homeStationCode`, `homeStationName`, `inventoryPark`

### StateReportLineDto / StateReportDayDto (A1)

Line: `reportId`, `wagonCode`, `dislocationTime`, `stationCode`, `operationCode`  
Day: `wagonCode`, `date`, `lines[]`

### REST

| Метод | Путь | Успех |
|---|---|---|
| GET | `/api/v1/wagons/{wagonCode}` | 200 + card |
| GET | `/api/v1/wagons/{wagonCode}/state-reports?date=YYYY-MM-DD` | 200 + day (`lines` может быть `[]`) |

### Ошибки

| HTTP | code | Когда |
|---|---|---|
| 400 | `INVALID_WAGON_CODE` / `INVALID_DATE` / `MISSING_DATE` | валидация входа |
| 404 | `WAGON_NOT_FOUND` | нет вагона в каталоге |
| **422** | **`RESULT_LIMIT_EXCEEDED`** | строк суток **> 500**; **без** тихой обрезки |
| 500 | только неожиданный сбой | не для лимита |

Тело ошибки (без секретов):

```json
{ "error": "RESULT_LIMIT_EXCEEDED", "message": "...", "wagonCode": "...", "date": "..." }
```

Лимит: **500** строк на ответ суток.

---

## 7. Синтетические fixtures

`platform-oracle/.../synthetic/`:

- `wagons/catalog.json`, `wagons/SYN-00001.card.json`
- `state-reports/SYN-00001.20240115.json` (≥2 строки)
- `state-reports/SYN-00001.20240116.empty.json`
- тесты границ: `T23:59:59.5` ∈ дня D; `D+1T00:00:00` ∉ дня D

Только `SYN-*`.

---

## 8. Критерии тестирования (синтетика)

| Уровень | Критерии |
|---|---|
| Unit | DTO/mapping; полуоткрытый интервал; ключи A1 |
| Module | Query через Port с fake/in-memory double **в модуле** или synthetic adapter через app wiring |
| MockMvc | 200 card/day/empty; 404; 400; **422** при >500 |
| Modulith | `verify()` без циклов; fleet/dislocation не тянут oracle types |
| Frontend | минимум: card + table на fixture/MSW |
| Negative | нет DataSource в `synthetic` |
| Архитектура Gradle | `fleet`/`dislocation` compile classpath **без** ojdbc |
| JDBC parity | `@Disabled` — не запускать |

Merge synthetic фазы: все включённые тесты зелёные **без сети**; нет пром/JDBC URL в config; `platform-disl-parser` без изменения поведения; нет секретов.

---

## 9. Этапы

| Этап | Содержание | Код? |
|---|---|---|
| R1-S0 | Документы + разрешение на код | нет (сейчас) |
| R1-S1 | Gradle + ports + synthetic adapters + REST + UI | после отдельного «можно код» |
| R1-S2 | Полный synthetic test pack | после S1 |
| R1-S3 | TEST JDBC | отдельное разрешение |

---

## 10. Журнал

После разрешения на код (не автоматически):

- ADR-0008 → APPROVED (с DIP + Modulith notes)
- R1 synthetic scaffold → APPROVED  
- R1 JDBC TEST / Production → DENIED UNTIL APPROVED  
- R1-Q-TZ → OPEN
