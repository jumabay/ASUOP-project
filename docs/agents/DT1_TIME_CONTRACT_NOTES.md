# DT1 — notes to [55_DT1_TIME_CONTRACT](../architecture/55_DT1_TIME_CONTRACT.md)

Дата: **2026-11-24**. Docs PR after P5-E on `main` (`ad1b8e7`). No live Oracle on this step.

## Scope delivered

- Provenance chains for `GETTING_DATE` / `LOCATION_DATE` with CONFIRMED / INFERRED / UNKNOWN.
- Concrete A1 contract: `snapshotTime`, reserved `locationTime`, compatible `dislocationTime`.
- Day axis unchanged: GETTING + half-open; R1-Q-TZ still OPEN.
- Edge cases: retain-all; event before day; null location.
- Point gaps DT1-A…F (no new P5-E plan).

## Not done

- CODE / REST / UI changes
- Closing DT1
- Expanding RO allow-list
- `currentState` / registry / watched / import
