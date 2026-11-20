# 53. P5 NUFF — тестовая матрица (synthetic vs Oracle)

Статус: **docs-only / PLAN**. Дата: 2026-11-20.  
Связанный план: [ASUOP_P5_NUFF_FUNCTIONAL_PLAN.md](../agents/ASUOP_P5_NUFF_FUNCTIONAL_PLAN.md).  
Стратегии: [16_TESTING_STRATEGY.md](16_TESTING_STRATEGY.md), [47_SCALE_MIGRATION_TEST_STRATEGY.md](47_SCALE_MIGRATION_TEST_STRATEGY.md).

**Правило:** зелёный synthetic = regression / UX / contract. **Не** = legacy parity, G31 truth, contractual p95, cutover readiness.

---

## 1. Матрица

| Feature | Scenario | Layer | Synthetic? | Needs Oracle? | Current coverage | Gap |
| --- | --- | --- | --- | --- | --- | --- |
| Card (A2) | Happy passport | unit/API/UI/e2e | Yes | No for contract | Query/adapters/MockMvc/smoke/Vitest/Playwright | — |
| Card | 404 unknown | API/UI/e2e | Yes | No | MockMvc + FE + e2e | — |
| Card | ≡ GetWagonByCode | IT parity | **No** | **Yes** TEST RO | `JdbcParityIT` @Disabled | Group A2 |
| Card | Current vs last day line | — | Partial stubs | Semantics | Out of A2 | Do not invent equality |
| Day (A1) | Lines / empty | unit/API/UI/e2e | Yes | No | DayInterval + MockMvc + FE | — |
| Day | Half-open [D,D+1) | unit | Yes (naive LDT) | Later TZ | DayIntervalTest | R1-Q-TZ OPEN |
| Day | >500 → 422 | unit/API/FE/scale | Yes | No | MockMvc + Vitest + scale tag | Soft timing ≠ p95 |
| Day | ≡ GetWagonStateReports | IT parity | **No** | **Yes** | Disabled | DT1/NK1 affect truth |
| Day | Duplicate retain (NK1) | unit + later IT | Partial L0 | Yes tape | L0 retain; API provisional | NK1 BLOCKED |
| Registry | page/size/sort/search | all | Yes | No for REST | R2 suite + FE | Oracle predicate unknown |
| Registry | ~18k paging math | @Tag("scale") | Yes T1 | No for math | Scale IT / FE mock totals | Default CI excludes scale |
| Registry | ≡ GetWagonList + p95 | IT/load | Soft only | **Yes** | None live | Doc 16 p95 unmeasured |
| Search UX | debounce/Enter/Clear | UI/e2e | Yes | No | Vitest + Playwright | — |
| Deep-link | tab/wagon/date | unit/UI | Yes | No | deepLink tests + e2e | List params P5-A |
| Watched | page/search; ⊂ catalog | all | Yes fixture 60 | No for smoke | R3 suite | ≠ Active (G31) |
| Watched | Migrate M8 / Active | L0 + IT | Empty/SKIP | Yes + G31 | L0 empty watched | Stay empty |
| Bulk ops | batch save/publish | — | N/A | If written | No API | Out of P5 wave 1 |
| Errors | 400/404/422/empty | API/UI | Yes | No | Strong | ORA-* unmapped |
| Concurrency | 20–30 users / import | load | Soft only | Yes | None automated | No false PASS |
| raildb-test | fail-fast / no DS | unit | Yes | No | RailDb fail-fast tests | Not parity |

---

## 2. QA gates по срезам P5

| Slice | Must pass | Must not claim |
| --- | --- | --- |
| P5-0 | Gradle JDK21 + FE + Playwright T0 | Oracle readiness |
| P5-A / P5-B | Vitest + Playwright; size≤200; day explicit load | A1/A2 parity |
| P5-C / P5-G | MockMvc + FE contract; additive docs | Oracle LIKE parity |
| P5-D | Docs + tests: FIXTURE ≠ Active; L0 empty | G31 closed |
| P5-E | Parity IT after APPROVED TEST | Prod JDBC; write |
| P5-F | Only after prefs APPROVED | Cross-browser sync |

**Evidence:** Independent PASS via `/qa-test-runner` или Lead/CI с явной меткой канала. Ask-mode `/qa` BLOCKED ≠ product FAIL.

---

## 3. Остаётся `@Disabled` до APPROVED TEST Oracle

- `JdbcParityIT` и любой live DataSource/ojdbc в app tests  
- Group A1–A12 equivalence; Group B* writes  
- Watched migrate populate / M8  
- Contractual p95 claims  
- Production / live FTP / RWL as proof  

Default `gradlew test` excludes `@Tag("scale")` unless owner opts in.

---

## 4. Риски ложной уверенности

1. Green R1–R3 ≠ legacy package results.  
2. Watched 60 codes ≠ GetActiveWagonList.  
3. Half-open LDT ≠ wall-clock tape (DT1/TZ).  
4. Contains search ≠ Oracle collation/index.  
5. T1 18k ≠ DB I/O latency.  
6. Soft timings ≠ contractual PASS.  
7. No bulk/concurrency suite.  
8. Filling watched from fixture = false Active set.
