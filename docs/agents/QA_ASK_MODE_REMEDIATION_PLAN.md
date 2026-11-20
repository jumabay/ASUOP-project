# План устранения блокировки QA Ask mode (минимальные права)

Дата: **2026-11-20**. Связано: [QA_ASK_MODE_DIAGNOSIS.md](QA_ASK_MODE_DIAGNOSIS.md), `.cursor/agents/qa.md`.

## 1. Корневая причина (подтверждена конфигурацией репо)

В `.cursor/agents/qa.md` фронтматтер содержит:

```yaml
readonly: true
```

При `readonly: true` субагент `/qa` получает Ask/readonly tool policy → **Shell запрещён** → любой gate, требующий `git`/`gradle`/`npm`/`gh`, закономерно даёт **BLOCKED**.

Это **не** требует и **не** оправдывает доступ QA к Oracle/Production.

## 2. Цель remediation

| Разрешить | По-прежнему запретить |
| --- | --- |
| Shell для `git status/diff/log`, Gradle/Vitest/Playwright **offline**, `gh` checks | Production/TEST Oracle connect |
| Чтение CI logs / артефактов без секретов | Flyway apply, live FTP/RWL |
| | Ослабление assertions «чтобы зелёное» без Owner |
| | Auto-merge `main` |

## 3. Минимальный патч (предложение Owner)

1. В `.cursor/agents/qa.md` сменить `readonly: true` → `readonly: false` (или удалить ключ, если default = agent).
2. **Сохранить** блок Hard bans в том же файле (Production Oracle, live import, merge, weakening tests).
3. Уточнить description: «May run offline tests; Oracle/Production remain denied by project rules + APPROVALS».
4. Docs: обновить diagnosis — H3 confirmed via `readonly: true`.
5. Проверка: один dry-run `/qa` на docs-only или synthetic slice → ожидание Shell OK и PASS/FAIL с evidence.

**Не** добавлять: DB credentials, allow-list JDBC, права на Create RAILDB.

## 4. Операционный fallback (пока патч не принят)

| Evidence channel | Использование |
| --- | --- |
| Lead re-run | «Lead PASS/FAIL» отдельной строкой |
| GitHub Actions | Третий канал |
| Independent `/qa` | BLOCKED при Ask — **не** переименовывать в PASS |

## 5. Acceptance

- [ ] `/qa` выполняет хотя бы `git rev-parse` + один тестовый прогон без Ask block
- [ ] Hard bans в `qa.md` на месте
- [ ] APPROVALS Create/JDBC/Production без изменений этим патчем
- [ ] Один регрессионный отчёт QA содержит команды и exit codes

## 6. Статус

| Item | Status |
| --- | --- |
| Diagnosis | DONE |
| Config root cause | **CONFIRMED** (`readonly: true`) |
| Apply patch to `qa.md` | **PENDING Owner** (не применён этим docs-only пакетом handoff, если не согласован отдельно) |
