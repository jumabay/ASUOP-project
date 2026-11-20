# QA subagent Ask-mode diagnosis (P4-2)

Дата: **2026-11-20**. Контекст: на P4-1 FINAL INTEGRATION `/qa` неоднократно возвращал **BLOCKED** с текстом Ask/readonly mode (Shell отклонён).

## Observed pattern

1. Lead запускает `Task` / `/qa` с инструкцией «MUST use shell; if Ask mode → BLOCKED».
2. Субагент получает отказ на первый Shell (`git rev-parse`, `gradle`, `npm`, `gh`).
3. По собственному CRITICAL-правилу субагент корректно ставит **BLOCKED** и не изобретает PASS.

Это **не** доказательство дефекта продукта и **не** Independent QA PASS.

## Likely causes (hypothesis — not root-cause confirmed in Cursor product internals)

| Hypothesis | Notes |
| --- | --- |
| H1 | Субагент `/qa` в данной сессии/конфигурации стартует в **Ask/readonly** tool policy |
| H2 | Parent Agent mode ≠ child tool policy (lead имеет Shell; child нет) |
| H3 | Skill/agent definition marks QA as readonly-preferred and harness enforces Ask |

## Operational rule (project)

| Role | Authority |
| --- | --- |
| Independent `/qa` | PASS only if **that** agent executed tests/diff |
| Lead re-run | May be recorded as **Lead PASS** separately |
| Never | Relabel Lead evidence as «Independent QA PASS» |

## Mitigation for future gates

1. Re-invoke `/qa` only when child Agent/shell is available; otherwise accept Lead evidence + CI URLs.
2. Prefer CI check conclusions on `main` as third evidence channel.
3. Do **not** expand QA privileges to Oracle/Production to «fix» Ask mode.

## P4-2 implication

Docs-only pack: QA may review files read-only. If Shell blocked again → **QA BLOCKED**; Lead may still open docs PR after static review.
