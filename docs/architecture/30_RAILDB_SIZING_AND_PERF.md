# 30. Предварительный sizing и производительность RAILDB (P3A)

Все цифры **предварительные** (статистика DBTC / арифметика 18k×24×365), не замена фактического AWR/`COUNT(*)`.

Источник порядка: [06_DATA_STORAGE_STRATEGY.md](06_DATA_STORAGE_STRATEGY.md), [24_RAILDB_DBA_INSTRUCTION.md](24_RAILDB_DBA_INSTRUCTION.md).

## 1. История дислокации

| Metric | Estimate |
| --- | --- |
| Rows / year (full hour) | **157.7M** |
| AVG_ROW_LEN (DBTC tape) | ~140 B |
| Raw data / 12м | ~21–25 GiB |
| Indexes factor | ×1.5…3 (plan **×2**) |
| Data+index / 12м | **~70–100 GiB** |
| Year 2–3 cumulative (if retained in RAILDB) | +70–100 GiB / year |

## 2. Прочие объекты (порядок)

| Domain | 12м / FULL | Disk order |
| --- | --- | --- |
| Kits/headers | hundreds of k | < 1 GiB |
| Wagons + current | ~50k | < 1 GiB |
| Repairs 12м+OPEN | hundreds of k | 1–5 GiB? |
| Stock/parts | hundreds of k | 1–5 GiB? |
| Ops/audit/intake | | < 5 GiB |
| Quarantine files (OS disk) | | ≥ 100 GiB separate |

## 3. Партиции и индексы

- Monthly RANGE on `RAIL_DISL_EVENT(dislocation_ts)`.
- Local index `(wagon_id, dislocation_ts)` — primary access path (card/day).
- Global or local `(dislocation_ts)` for range scans/migration.
- UK for full-kit lines PROVISIONAL until NK1 closed.

## 4. Bulk load

- External load / batch JDBC with bind arrays; disable indexes during initial M6 optional (DBA).
- Commit per kit or per N hours; restartable watermarks.
- Parallel degree TBD after copy test — start DOP 1–4.

## 5. Host resources (discussion minimum)

| Resource | Preliminary |
| --- | --- |
| Tablespace Y1 + headroom | **≥ 300 GiB** (prefer 500) |
| FRA / RMAN | ≥ 1× full + retention policy (often **≥ 500 GiB**) |
| RAM instance | 16–32 GiB hypothesis |
| Growth 3–5 years | plan **0.4–0.6 TiB** data+index if full hours retained online |

Do **not** use RAC schema 5 GiB quota as guide.

## 6. Backup

ARCHIVELOG; daily incremental + weekly full; restore drill before cutover; DBTC RMAN unchanged.
