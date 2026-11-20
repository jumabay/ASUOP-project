-- P3C NK1 — Natural key / duplicate probes for STATEREPORTRECORDS
-- DO NOT RUN until P3C-1 approve. Prerequisite: 00_explain_plan_gates.sql PASS.
-- Goal: evidence whether candidate keys collide inside a bounded hour window.
-- NEVER: full-table COUNT/GROUP BY on STATEREPORTRECORDS.
-- Timeout suggest: 60s per statement. Max rows returned: 200 (aggregates) / 500 (samples).
-- Expected load: INDEX RANGE on GETTING_DATE for ONE hour (~order 10k–30k rows touched, not 1.2B).

-- =============================================================================
-- NK1.1 Candidate collision: (REPORT_ID, WAGON_ID) within one hour of GETTING_DATE
-- Returns only groups with cnt > 1 (duplicates). Capped outer result.
-- Bind: :d_from, :d_to = one hour; prefer recent low-traffic hour after plan check.
-- =============================================================================
SELECT *
FROM (
  SELECT srr.report_id,
         srr.wagon_id,
         COUNT(*) AS row_cnt,
         COUNT(DISTINCT srr.location_date) AS distinct_location_dates,
         COUNT(DISTINCT srr.operation_id) AS distinct_operations,
         COUNT(DISTINCT srr.location_station_id) AS distinct_loc_stations
  FROM dbtc.statereportrecords srr
  WHERE srr.getting_date >= :d_from
    AND srr.getting_date <  :d_to
  GROUP BY srr.report_id, srr.wagon_id
  HAVING COUNT(*) > 1
  ORDER BY COUNT(*) DESC
)
WHERE ROWNUM <= 200;
-- Export: p3c_NK1_report_wagon_dups_<date>.csv
-- Interpretation:
--   0 rows → no collision in THIS hour for (REPORT_ID,WAGON_ID); still not proof globally.
--   >0 → CONFIRMED duplicates possible; RAILDB must keep all with technical PK.

-- =============================================================================
-- NK1.2 Candidate collision: (WAGON_ID, GETTING_DATE) — must NOT assume unique
-- =============================================================================
SELECT *
FROM (
  SELECT srr.wagon_id,
         srr.getting_date,
         COUNT(*) AS row_cnt,
         COUNT(DISTINCT srr.report_id) AS distinct_reports
  FROM dbtc.statereportrecords srr
  WHERE srr.getting_date >= :d_from
    AND srr.getting_date <  :d_to
  GROUP BY srr.wagon_id, srr.getting_date
  HAVING COUNT(*) > 1
  ORDER BY COUNT(*) DESC
)
WHERE ROWNUM <= 200;
-- Export: p3c_NK1_wagon_getting_dups_<date>.csv

-- =============================================================================
-- NK1.3 Content diversity sample for duplicate groups (no PII beyond ids)
-- Only if NK1.1 returned rows; pick one report_id/wagon_id binds.
-- =============================================================================
SELECT srr.report_id,
       srr.wagon_id,
       srr.getting_date,
       srr.location_date,
       srr.location_station_id,
       srr.operation_id,
       srr.current_state_id,
       srr.train_code,
       ORA_HASH(
         NVL(TO_CHAR(srr.location_date,'YYYYMMDDHH24MISS'),'N') || '|' ||
         NVL(TO_CHAR(srr.location_station_id),'N') || '|' ||
         NVL(TO_CHAR(srr.operation_id),'N') || '|' ||
         NVL(srr.train_code,'N') || '|' ||
         NVL(TO_CHAR(srr.current_state_id),'N')
       ) AS content_hash32
FROM dbtc.statereportrecords srr
WHERE srr.getting_date >= :d_from
  AND srr.getting_date <  :d_to
  AND srr.report_id = :report_id
  AND srr.wagon_id = :wagon_id
  AND ROWNUM <= 50;
-- Export: p3c_NK1_dup_content_sample_<date>.csv

-- =============================================================================
-- NK1.4 Hour volume estimate (bounded) — COUNT only inside indexed hour
-- =============================================================================
SELECT COUNT(*) AS rows_in_hour
FROM dbtc.statereportrecords srr
WHERE srr.getting_date >= :d_from
  AND srr.getting_date <  :d_to;
-- Export: p3c_NK1_hour_volume_<date>.csv
-- Load: acceptable IF plan uses IDX_REC_GETTING_DATE (verify via 00_*).
