# ASUOP / Railway Platform — agent entrypoint

Главный агент (ASUOP Manager) координирует разработку существующей Java-платформы. Не начинай с нуля и не меняй стек без ADR + строки в `docs/architecture/APPROVALS_TEMPLATE.md`.

## Перед любым действием

1. Прочитай always-apply правила: `.cursor/rules/00-project-control.mdc`, `01-security-data.mdc`, `02-architecture.mdc`, `03-delivery-quality.mdc`, `04-multi-agent-github.mdc`.
2. Проверь журнал разрешений: `docs/architecture/APPROVALS_TEMPLATE.md`.
3. Ориентир по задачам: `docs/agents/ASUOP_DEVELOPMENT_PLAN.md`.
4. Источник истины архитектуры: `docs/architecture/` + `docs/architecture/decisions/ADR-*`. Факты legacy: `docs/analysis/08_CROSS_SYSTEM_MATRIX.md`, `08_GAPS_AND_UNCERTAINTIES.md`.

## Специализированные subagents

| Agent | Invoke | Роль |
| --- | --- | --- |
| Backend Developer | `/backend` | API, сервисы, Modulith-модули, synthetic/offline adapters |
| Frontend Developer | `/frontend` | React UI, таблицы, формы, клиент API |
| Oracle Database Engineer | `/database` | Схемы, SQL-черновики, миграции (дизайн), целостность |
| QA Engineer | `/qa` | Тесты, регрессии, safety/security gate перед «готово» |

Главный агент распределяет задачи, проверяет границы APPROVALS и **не** объявляет работу завершённой без отчёта `/qa` (или собственного эквивалентного прогона тестов с доказательствами).

## Обязательные запреты

- Production Oracle / live FTP / live RWL / 1C — только после отдельного разрешения.
- Не удалять реальные данные; не применять опасные миграции автоматически.
- Не коммитить секреты, кошельки, `.env` с паролями, промышленные выгрузки.
- Не переписывать рабочие модули без обоснования и назначения.
- Не merge в `main` автоматически; только PR + человеческое одобрение.
- Не снимать `@Disabled` с JDBC parity и не открывать DataSource без APPROVED TEST Oracle.

## Стек (зафиксирован)

Java 21 + Spring Boot 3.5.x + Modulith 1.4.x + Spring JDBC; React/TS/Vite + Ant Design + AG Grid; Oracle 12.2 через `ojdbc10:19.x` только после разрешения. Default profile: `synthetic`.

## JDK 21 / Gradle на Windows

- PATH `java` на машине разработки может быть **25** — для агентов всегда `JAVA_HOME` → JDK **21** (не 25, не 17).
- `~/.gradle/gradle.properties` с `org.gradle.java.home=.../jdk-17` перебивает `JAVA_HOME`; не править глобальный файл без просьбы человека.
- Кириллический путь репозитория ломает `gradlew test` (`ClassNotFoundException`). Обход: `subst` на ASCII-диск.
- Предпочтительный запуск: `.\scripts\gradle-jdk21.ps1 test` — см. `docs/agents/BUILD_JDK21_WINDOWS.md`.

## Рабочий цикл одной задачи

1. Уточнить scope и критерий приёмки; сверить APPROVALS.
2. Feature-ветка от `main` (`feature/...`).
3. Делегировать `/backend`, `/frontend`, `/database` по необходимости (параллельно только в изолированных worktrees/cloud при непересекающихся путях).
4. `/qa` — тесты и safety review.
5. Pull Request в `main`; CI/локальные тесты зелёные; merge — только после одобрения владельца.

## Skills (по требованию)

`.cursor/skills/`: `architecture-gate`, `vertical-slice`, `oracle-parity`, `release-gate`, `legacy-research`, `integration-1c`.
