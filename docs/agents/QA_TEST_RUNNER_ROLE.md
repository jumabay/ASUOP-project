# QA Test Runner — изолированная роль (конфиг)

Дата: **2026-11-20**.  
Файлы: `.cursor/agents/qa.md` (audit, **readonly: true**), `.cursor/agents/qa-test-runner.md` (**readonly: false**, bans).

## Зачем два агента

| Agent | Frontmatter | Назначение |
| --- | --- | --- |
| `/qa` | `readonly: true` | Документный/статический audit; не исполняет Shell |
| `/qa-test-runner` | `readonly: false` | Только offline/synthetic прогоны тестов + git/gh **read** |

## Что фактически открывает `readonly: false`

| Открывает (в Cursor) | Не открывает |
| --- | --- |
| Shell (команды локально) | Доступ к Oracle / Production |
| Возможность Write/StrReplace инструментов агента | Снятие DENIED в APPROVALS |
| | Flyway/DDL/live FTP·RWL |
| | Права merge в `main` |

Реальные границы: Hard bans в `qa-test-runner.md` + OS/DB permissions + журнал APPROVALS.

## Политика доказательств

- Independent PASS только если **Test Runner** реально выполнил команды.
- `/qa` Ask/BLOCKED ≠ FAIL продукта.
- Lead/CI — отдельные каналы; не переименовывать в Independent QA.
