# modules/dislocation

A1 day-history domain: half-open calendar day, state-report lines, query port.

## Time contract (T1)

- Domain field: `StateReportLine.snapshotTime` — slice / `GETTING_DATE` axis.
- `dislocationTime()` — deprecated alias, always equal to `snapshotTime`.
- Day filter: `DayInterval` half-open `[D 00:00, D+1 00:00)` on snapshot time (naive `LocalDateTime`; R1-Q-TZ OPEN).
- No deduplication of lines with identical times.

See `docs/architecture/55_DT1_TIME_CONTRACT.md`.
