# Предложение: минимальное изменение QA `readonly` (без применения до одобрения)

Дата: **2026-11-20**.  
Связано: [QA_ASK_MODE_DIAGNOSIS.md](QA_ASK_MODE_DIAGNOSIS.md), [QA_ASK_MODE_REMEDIATION_PLAN.md](QA_ASK_MODE_REMEDIATION_PLAN.md), `.cursor/agents/qa.md`.

## 1. Факт

Файл `.cursor/agents/qa.md` содержит `readonly: true` → субагент `/qa` в Ask mode → Shell block → Independent QA **BLOCKED**.

## 2. Минимальное предложение (патч **не применён**)

| Change | Before | After (proposed) |
| --- | --- | --- |
| Frontmatter | `readonly: true` | `readonly: false` |
| Description | «Readonly verification preferred; may run tests» | «May run offline git/tests/CI checks; Oracle/Production remain denied by Hard bans + APPROVALS» |
| Hard bans section | keep | **keep unchanged** (no Production Oracle, no live import, no auto-merge, no weakening asserts) |

**Не предлагается:** credentials, JDBC allow-list, Create RAILDB rights, снятие DENIED в APPROVALS, доступ к DBTC.

## 3. Permission mode / least privilege

| Capability | After proposed change |
| --- | --- |
| Local Shell: git, Gradle, npm, Playwright synthetic, `gh` read | Yes (needed for Independent PASS) |
| Oracle TEST/Production connect | **No** (Hard bans + APPROVALS) |
| Flyway / migrate / FTP·RWL live | **No** |
| Merge to `main` | **No** (human only) |

## 4. Gate before applying

Требуется **явное одобрение владельца** на правку `.cursor/agents/qa.md` (процессный docs/config, не Create RAILDB).  
До одобрения: Lead PASS / CI ≠ Independent QA PASS.

## 5. Verification after approval (future)

1. Apply one-line/`readonly` change in a tiny PR.  
2. Invoke `/qa` on a docs-only or synthetic slice.  
3. Expect Shell success + verdict with commands/exit codes.  
4. Confirm Hard bans still in file.

**Status now:** proposal only — **not applied**.
