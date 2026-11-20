-- P3C-0 / gate — EXPLAIN PLAN ONLY (no data fetch from tape)
-- Run BEFORE any STATEREPORTRECORDS diagnostic in P3C-1.
-- Account: DBTC_READONLY (or TEST copy). Timeout suggest: 15s.
-- Expected load: dictionary + optimizer; no tape row fetch.
-- Pass criteria: PLAN shows INDEX RANGE/UNIQUE SCAN on
--   IDX_REC_GETTING_DATE or IDX_REC_WAGON_ID (or equivalent),
--   NOT TABLE ACCESS FULL on STATEREPORTRECORDS.

-- Bind example (SQL Developer):
--   :d_from = TIMESTAMP '2026-11-01 00:00:00'
--   :d_to   = TIMESTAMP '2026-11-01 01:00:00'

EXPLAIN PLAN FOR
SELECT /*+ INDEX(srr IDX_REC_GETTING_DATE) */
       srr.wagon_id, srr.report_id, srr.getting_date, srr.location_date
FROM dbtc.statereportrecords srr
WHERE srr.getting_date >= :d_from
  AND srr.getting_date <  :d_to
  AND ROWNUM <= 100;

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, NULL, 'BASIC +PREDICATE +INDEX'));

-- Second shape: wagon-scoped (bootstrap-like), still capped
EXPLAIN PLAN FOR
SELECT /*+ INDEX(srr IDX_REC_WAGON_ID) */
       srr.wagon_id, srr.report_id, srr.getting_date
FROM dbtc.statereportrecords srr
WHERE srr.wagon_id = :wagon_id
  AND srr.getting_date <= :cutoff_ts
  AND ROWNUM <= 50;

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, NULL, 'BASIC +PREDICATE +INDEX'));
