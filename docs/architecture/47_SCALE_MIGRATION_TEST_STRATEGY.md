# 47. Стратегия тестов масштаба (~18k) и сверки миграции

Статус: **DRAFT / docs-only** (2026-11-20).  
Не заменяет [16_TESTING_STRATEGY.md](16_TESTING_STRATEGY.md).  
Связь: [44](44_MIGRATION_VERIFICATION_MODEL_OFFLINE.md), [45](45_API_SCALE_READINESS_18K.md), [25](25_DBTC_TO_RAILDB_MIGRATION_MATRIX.md), [APPROVALS_TEMPLATE.md](APPROVALS_TEMPLATE.md).

**Правило доказательств:** пороги p95 из doc 16 — договорные. PASS только с записанным измерением. Не изобретать зелёные результаты.

---

## 1. Fixture tiers

| Tier | Size | Purpose | CI |
| --- | --- | --- | --- |
| T0 | current synthetic | PR smoke | every PR |
| T1 | ≥ ~18 334 registry codes | paging/search/totals | `@Tag("scale")` opt-in |
| T2 | 18 000 KZ lines | R0 parse &lt; 1h | opt-in |
| T3 | 501+ day lines | 422 limit | can be small fixture |
| T4 | T1 via UI | Playwright `@scale` | opt-in |

Watched **не** раздувать до 18k без G31. Дубликаты ленты retain. Только SYN-* коды.

---

## 2. Offline checks to add (after CODE APPROVED)

| Area | Examples |
| --- | --- |
| API | last page, totalElements, size 201→400, day 501→422, record card/list timings |
| R0 | 18k parse; idempotent filename; calendar hole |
| FE Vitest | pagination math; 422 handling; no full-array render |
| Playwright | keep T0 smoke; `@scale` optional |
| Migrator L0 | selection tags 12M/FULL/BOOTSTRAP; retain-dup; kit UK replay |

Default `gradlew test` не обязан гонять T1/T2 без решения владельца.

---

## 3. Migration risk detectors

| Risk | Offline detector | Later (APPROVED) |
| --- | --- | --- |
| Data loss | N in → N out golden | counts by day/kit |
| Duplicates / false dedup | identical payloads → 2 events | re-run batch delta=0 |
| Chronology | getting axis / half-open | A1 compare; DT1 when open |
| Cutoff hole | bootstrap max ≤ cutoff | per-wagon check |
| Wrong watched | assert M8 blocked | G31 source |
| Perf | record timings | AWR on TEST |
| Secrets | redacted reports | same |

---

## 4. Gates map

| Gate | Opens tests |
| --- | --- |
| P0-5 / P1-4 / P4-0 | T0 regression |
| **Proposed:** Scale offline fixtures CODE | T1–T4 + L0 harness |
| Accept ADR-0009 (direction — already in journal with constraints) | policy only |
| Create / Flyway / DBTC read / RAILDB write / JDBC TEST | L1–L2, parity |
| G31 / NK1 / DT1 | M8 / dedup / axis |

**Proposed APPROVALS row (owner must add):**  
`Scale offline fixtures T1–T4 + tagged tests | APPROVED | generated catalog/KZ; @Tag("scale"); timing record only | no Oracle; no claim contractual p95 without measure`

---

## 5. Executable-now vs BLOCKED

| Now (after CODE APPROVED) | BLOCKED |
| --- | --- |
| Keep T0 green; generators T1/T2; L0 migrator unit; fail-fast stubs; secret-hygiene | Create RAILDB, Flyway, JDBC/parity, cutover, G31/NK1/DT1 close |

**QA verdict on this pack:** strategy **READY TO DOCUMENT**; scale/migration **PASS claims** — not yet (no runs invented).
