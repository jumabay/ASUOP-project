# Approved decisions and stage gates

Журнал решений по утверждению плана «Java Architecture Plan» (промпт №11), 2026-11-13. Строка `APPROVED` разрешает указанное решение. Строка `DENIED UNTIL APPROVED` остаётся запретом. Код, схема Oracle, 1С и FTP этим журналом не меняются.

Источник истины целевой платформы: `docs/architecture/01`…`20` и `docs/architecture/decisions/ADR-0001`…`ADR-0007`. Черновик C# / Blazor (`docs/architecture/_archive_csharp/`, `docs/analysis/10-target-architecture-migration.md`) стеком не является. Матрица `docs/analysis/09-function-gap-matrix.md` для проектирования устарела; опора по фактам старого контура — `docs/analysis/08_CROSS_SYSTEM_MATRIX.md` и `docs/analysis/08_GAPS_AND_UNCERTAINTIES.md`.

| Decision | Status | Evidence / approver | Date |
|---|---|---|---|
| Java runtime / Spring Boot versions | APPROVED | Java 21 LTS, Spring Boot 3.5.x, Spring Modulith 1.4.x. Java 25 не база до smoke-test JDBC к Oracle 12.2. ADR-0001 | 2026-11-13 |
| Oracle version + compatible JDBC driver | APPROVED | База 12.2.0.1.0, схема `DBTC`. Драйвер `ojdbc11` линии 19c. JDBC 23.x к 12.2 не берётся. Первое соединение — только `SELECT` версии на копии. ADR-0003 | 2026-11-13 |
| Backend/frontend UI libraries | APPROVED | Spring JDBC / `JdbcClient`, без JPA на ленте срезов. React, TypeScript, Vite, Ant Design, AG Grid Community. ADR-0002, ADR-0005 | 2026-11-13 |
| Modular monolith, no broker | APPROVED | Один процесс. Kafka, RabbitMQ, Redis, Kubernetes на старте не вводятся. ADR-0004 | 2026-11-13 |
| Single writer, read-first | APPROVED | Старый контур — единственный писатель среза, ремонта и склада до тестов группы B. ADR-0007 | 2026-11-13 |
| Flyway scope | APPROVED | Flyway только для схемы метаданных приложения. DDL ленты `DBTC` не выполняется. ADR-0006 | 2026-11-13 |
| Expedition, finance, contracts rules | DENIED UNTIL APPROVED | Требований в исследовании нет. Модули остаются границами без правил | |
| First feature to implement | APPROVED | Только R0: библиотека `platform-disl-parser`, без JDBC, Oracle, сети, хоста и React. Состав — `20_FIRST_IMPLEMENTATION_PLAN.md`. Исходники этим журналом не создаются | 2026-11-13 |
| Test Oracle write permissions | DENIED UNTIL APPROVED | | |
| Production access | DENIED UNTIL APPROVED | | |
| 1C integration endpoint | DENIED UNTIL APPROVED | Конфигурации 1С нет. Формат обмена не задаётся | |
| Live import cutover | DENIED UNTIL APPROVED | | |
