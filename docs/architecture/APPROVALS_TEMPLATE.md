# Approved decisions and stage gates

Журнал решений по утверждению плана «Java Architecture Plan» (промпт №11), 2026-11-13. Строка `APPROVED` разрешает указанное решение. Строка `DENIED UNTIL APPROVED` остаётся запретом. Код, схема Oracle, 1С и FTP этим журналом не меняются без отдельной строки.

Источник истины целевой платформы: `docs/architecture/01`…`23` и `docs/architecture/decisions/ADR-0001`…`ADR-0008`. Черновик C# / Blazor (`docs/architecture/_archive_csharp/`, `docs/analysis/10-target-architecture-migration.md`) стеком не является. Матрица `docs/analysis/09-function-gap-matrix.md` для проектирования устарела; опора по фактам старого контура — `docs/analysis/08_CROSS_SYSTEM_MATRIX.md` и `docs/analysis/08_GAPS_AND_UNCERTAINTIES.md`.

| Decision | Status | Evidence / approver | Date |
|---|---|---|---|
| Java runtime / Spring Boot versions | APPROVED | Java 21 LTS, Spring Boot 3.5.x, Spring Modulith 1.4.x. Java 25 не база. Smoke JDBC к 12.2 пройден (см. строку JDBC smoke). ADR-0001 | 2026-11-13 |
| Oracle version + compatible JDBC driver | APPROVED | База 12.2.0.1.0, схема `DBTC`. Драйвер `com.oracle.database.jdbc:ojdbc10:19.33.0.0` + NLS `com.oracle.database.nls:orai18n:19.33.0.0`. `ojdbc11:23.x` к 12.2 не берётся. ADR-0003, [21_JDBC_SMOKE_CHECK.md](21_JDBC_SMOKE_CHECK.md) | 2026-11-13 / уточн. 2026-11-14 |
| Backend/frontend UI libraries | APPROVED | Spring JDBC / `JdbcClient`, без JPA на ленте срезов. React, TypeScript, Vite, Ant Design, AG Grid Community. ADR-0002, ADR-0005 | 2026-11-13 |
| Modular monolith, no broker | APPROVED | Один процесс. Kafka, RabbitMQ, Redis, Kubernetes на старте не вводятся. ADR-0004 | 2026-11-13 |
| Single writer, read-first | APPROVED | Старый контур — единственный писатель среза, ремонта и склада до тестов группы B. ADR-0007 | 2026-11-13 |
| Flyway scope | APPROVED | Flyway только для схемы метаданных приложения. DDL ленты `DBTC` не выполняется. ADR-0006 | 2026-11-13 |
| Expedition, finance, contracts rules | DENIED UNTIL APPROVED | Требований в исследовании нет. Модули остаются границами без правил | |
| First feature to implement (R0) | APPROVED / DONE | Библиотека `platform-disl-parser`, без JDBC в самом модуле. [20_FIRST_IMPLEMENTATION_PLAN.md](20_FIRST_IMPLEMENTATION_PLAN.md) | 2026-11-13 |
| JDBC smoke (one-time diagnostic) | COMPLETED | Разовые scoped-разрешения на диагностику к потенциально промышленному `tc-asuvp-03:1521/RAC.WORLD` под `RAILWAY_READONLY`. Только DUAL / системные представления. Постоянный allow-list **не** создан. Доказательства: [21_JDBC_SMOKE_CHECK.md](21_JDBC_SMOKE_CHECK.md). Повтор без новой строки журнала запрещён | 2026-11-14 |
| R1 scope A1+A2 (read-only intent) | APPROVED WITH CONSTRAINTS | Только A1/A2; synthetic; без Oracle/JDBC; без A3–A8/FTP/импорта; без пром allow-list; сутки half-open; R1-Q-TZ open. Детали: [23_R1_A1_A2_IMPLEMENTATION_PLAN.md](23_R1_A1_A2_IMPLEMENTATION_PLAN.md) | 2026-11-14 |
| ADR-0008 Gradle multi-project + DIP | APPROVED | [ADR-0008](decisions/ADR-0008-gradle-multi-project.md). DIP: порты в fleet/dislocation; oracle = adapter; Modulith 1.4 — один Boot | 2026-11-14 |
| R1 synthetic scaffold code | APPROVED | Промпт №15: разрешена реализация A1+A2 на synthetic (Java + React) по плану 23. JDBC/Oracle/tc-asuvp-03 по-прежнему запрещены. JDBC parity `@Disabled` | 2026-11-14 |
| R1 CODE IMPLEMENTATION (synthetic A1+A2) | APPROVED | Явное разрешение промпта №15 (2026-11-14): код R1 только synthetic, по ADR-0008 и плану 23. Не снимает запреты JDBC/Oracle/Production/legacy-parity | 2026-11-14 |
| R1 JDBC against TEST Oracle | DENIED UNTIL APPROVED | TEST URL отсутствует; parity `@Disabled` | |
| R1 implementation (full card+day vs legacy on copy) | DENIED UNTIL APPROVED | Сверка с legacy на копии Oracle — вне synthetic R1. [22_R1_PREPARATION_PLAN.md](22_R1_PREPARATION_PLAN.md), [23_R1_A1_A2_IMPLEMENTATION_PLAN.md](23_R1_A1_A2_IMPLEMENTATION_PLAN.md) | |
| Test Oracle write permissions | DENIED UNTIL APPROVED | | |
| Production access | DENIED UNTIL APPROVED | Разовое диагностическое подключение **не** считается постоянным Production access. Промышленный JDBC для разработки и R1 по умолчанию закрыт | |
| 1C integration endpoint | DENIED UNTIL APPROVED | Конфигурации 1С нет. Формат обмена не задаётся | |
| Live import cutover | DENIED UNTIL APPROVED | | |
