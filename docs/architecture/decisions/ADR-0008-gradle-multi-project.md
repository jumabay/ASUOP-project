# ADR-0008. Структура Gradle multi-project для R1

## Статус

**Accepted** — реализация R1 synthetic (промпт №15, 2026-11-14) разрешена журналом. JDBC/Oracle по-прежнему вне scope.

## Контекст

- R0: `platform-disl-parser` без JDBC; поведение не менять.
- R1: только A1/A2; synthetic; без Oracle/JDBC до отдельного разрешения.
- Стек: Java 21, Spring Boot 3.5.x, Spring Modulith 1.4.x, React/Vite ([02_TECHNOLOGY_STACK.md](../02_TECHNOLOGY_STACK.md)).
- JDBC позже: только `platform-oracle`, `ojdbc10:19.33.0.0` + `orai18n` ([ADR-0003](ADR-0003-oracle-jdbc-19c-against-12-2.md)).
- Production / `tc-asuvp-03`: DENIED; allow-list не создавать.

## Альтернативы корня сборки

| # | Вариант | Выбор |
|---|---|---|
| A | Один Gradle-корень в workspace | **выбран** |
| B | Отдельный корень `railway-platform/` | отклонён (две сборки) |
| C | Только разрозненные проекты | отклонён |

## Решение: Gradle + инверсия зависимостей

### Gradle include (R1)

`platform-disl-parser`, `platform-oracle`, `platform-app`, `modules:fleet`, `modules:dislocation`.  
`frontend/` в дереве репо, **не** JVM-subproject.  
Остальные бизнес-модули **не** создаются.  
`tools/oracle-jdbc-smoke` — опциональный include утилиты, не runtime app.

### Направление зависимостей (обязательно)

Бизнес-модули **владеют** портами доступа к данным.  
`platform-oracle` — **инфраструктурный адаптер**, реализует эти порты.

```text
modules:fleet          → (нет зависимости на oracle / JDBC)
modules:dislocation    → (нет зависимости на oracle / JDBC)
platform-oracle        → modules:fleet, modules:dislocation   (implements ports)
platform-app           → fleet, dislocation, platform-oracle  (wiring + REST)
frontend               --HTTP--> platform-app
platform-disl-parser   ↛ R1 runtime classpath app
```

Запрещено:

- `fleet` / `dislocation` → `platform-oracle`, ojdbc, классы `…synthetic` / `…jdbc` адаптеров;
- прямые JDBC-вызовы из controllers / use-cases;
- второй Spring Boot / отдельные контейнеры «ради Modulith».

### Совместимость со Spring Modulith 1.4.x

Modulith моделирует **логические модули по пакетам** под одним Boot-приложением; Gradle-модули — ортогональны и поддерживаются (обсуждения spring-modulith #151 / #500).

Минимальная схема **без** дополнительных Spring-контейнеров:

| Логический модуль | Пакет | Тип Modulith | Содержимое |
|---|---|---|---|
| `fleet` | `platform.fleet` | CLOSED | use-case, DTO, **порт** `WagonCardPort` (имя уточняется в коде) |
| `dislocation` | `platform.dislocation` | CLOSED | use-case, DTO, **порт** суток |
| `oracle` | `platform.oracle` | OPEN (infra) | adapters (synthetic сейчас; JDBC позже), реализуют порты fleet/dislocation |
| bootstrap | `platform.app` | приложение | единственный `@SpringBootApplication`, REST, `ApplicationModules.verify()` |

- Один процесс, один Boot jar/`platform-app`.
- `scanBasePackages = "platform"` (или эквивалент), чтобы типы из всех jar попали в контекст.
- `ApplicationModules.of(Application.class).verify()` в тестах `platform-app`.
- `oracle` объявляется `@ApplicationModule(type = OPEN)` и зависит только от **публичных** API `fleet`/`dislocation` (порты + DTO).
- Не вводить Event Publication Registry / actuators Modulith «на всякий случай» в R1.

**Если** при первой реализации `verify()` конфликтует с multi-jar detection — **остановиться** и согласовать один из минимальных fallback (не делать заранее):

1. явная `ApplicationModuleDetectionStrategy` в **test** sources; или  
2. уточнение `allowedDependencies` на `package-info` модулей.

Не решать fallback усложнением инфраструктуры до факта ошибки verify.

### Профили

- Default: **`synthetic`** — нет DataSource, нет сети к Oracle.
- `oracle-test` — не активировать без журнала.

### Версии

`gradle/libs.versions.toml`: Java 21, Boot 3.5.x, Modulith 1.4.x, JUnit 5.  
Patch-версии фиксируются при первом коммите каркаса.

## Дерево

```text
АСУВП/
  settings.gradle.kts
  build.gradle.kts
  gradle/libs.versions.toml
  platform-disl-parser/
  platform-oracle/          # adapters only
  platform-app/             # Boot + REST + Modulith verify
  modules/
    fleet/                  # A2 + port
    dislocation/            # A1 + port
  frontend/
  tools/oracle-jdbc-smoke/
  docs/
```

## Последствия

- DIP соблюдён; JDBC изолирован в адаптере.
- Modulith проверяет границы без второго контейнера.
- Откат: убрать include новых модулей; R0 не затронут по поведению.

## Запрещено

Менять сборку/код до отдельного разрешения на реализацию. Брокер, вторая БД, пром host в default config, модули вне A1/A2.
