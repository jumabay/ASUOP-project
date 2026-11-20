-- P3C DT1 — GETTING_DATE vs LOCATION_DATE semantics (bounded sample)
-- DO NOT RUN until P3C-1. Prerequisite: 00_explain_plan_gates.sql PASS.
-- Hypothesis (INFERRED from import C#): file field[3]=dislocationDate → likely LOCATION_DATE;
--   STATEREPORTS.GETTING_DATE / tape GETTING_DATE ≈ import/hour axis (CONFIRMED index for 12м).
-- Timeout: 60s. Max rows: 5000 sample / 20 aggregate buckets.
-- Expected load: INDEX RANGE one hour; no full tape scan.
-- Do NOT change approved 12м GETTING_DATE migration axis without evidence from these probes.

-- =============================================================================
-- DT1.1 Pairwise delta sample (anonymized ids only)
-- =============================================================================
SELECT srr.wagon_id,
       srr.report_id,
       srr.getting_date,
       srr.location_date,
       CASE
         WHEN srr.getting_date IS NULL OR srr.location_date IS NULL THEN 'NULL_SIDE'
         WHEN srr.getting_date = srr.location_date THEN 'EQUAL'
         WHEN srr.getting_date > srr.location_date THEN 'GETTING_AFTER_LOC'
         ELSE 'GETTING_BEFORE_LOC'
       END AS relation,
       (CAST(srr.getting_date AS DATE) - CAST(srr.location_date AS DATE)) * 24 AS delta_hours
FROM dbtc.statereportrecords srr
WHERE srr.getting_date >= :d_from
  AND srr.getting_date <  :d_to
  AND ROWNUM <= 5000;
-- Export: p3c_DT1_sample_pairs_<date>.csv

-- =============================================================================
-- DT1.2 Aggregate relation counts inside same hour (from inline sample pattern)
-- Note: nested ROWNUM sample then aggregate — avoids GROUP BY entire hour if desired.
-- Prefer offline aggregate of DT1.1 CSV; optional DB aggregate below on same filter.
-- =============================================================================
SELECT relation, COUNT(*) AS cnt
FROM (
  SELECT CASE
           WHEN srr.getting_date IS NULL OR srr.location_date IS NULL THEN 'NULL_SIDE'
           WHEN srr.getting_date = srr.location_date THEN 'EQUAL'
           WHEN ABS(CAST(srr.getting_date AS DATE) - CAST(srr.location_date AS DATE)) * 24 <= 1
             THEN 'WITHIN_1H'
           WHEN ABS(CAST(srr.getting_date AS DATE) - CAST(srr.location_date AS DATE)) * 24 <= 24
             THEN 'WITHIN_24H'
           ELSE 'GT_24H'
         END AS relation
  FROM dbtc.statereportrecords srr
  WHERE srr.getting_date >= :d_from
    AND srr.getting_date <  :d_to
    AND ROWNUM <= 5000
)
GROUP BY relation
ORDER BY 1;
-- Export: p3c_DT1_relation_buckets_<date>.csv

-- =============================================================================
-- DT1.3 Align tape GETTING_DATE to parent STATEREPORTS.GETTING_DATE (kit hour)
-- Join via REPORT_ID → UK_REPORT_ID; filter parent by indexed GETTING_DATE day.
-- STATEREPORTS ~411k — day filter OK. Child limited by same hour binds.
-- =============================================================================
SELECT sr.id AS report_id,
       sr.getting_date AS report_getting_ts,
       sr.importfiles_id,
       srr.wagon_id,
       srr.getting_date AS tape_getting,
       srr.location_date AS tape_location,
       CASE WHEN TRUNC(CAST(sr.getting_date AS DATE),'HH24')
                 = TRUNC(srr.getting_date,'HH24')
            THEN 'Y' ELSE 'N' END AS same_hour_as_report
FROM dbtc.statereports sr
JOIN dbtc.statereportrecords srr
  ON srr.report_id = sr.id
WHERE sr.getting_date >= :d_from
  AND sr.getting_date <  :d_to
  AND ROWNUM <= 2000;
-- Export: p3c_DT1_report_vs_tape_<date>.csv
-- Load note: ensure start from STATEREPORTS filter; if plan walks tape first — STOP and rewrite.

-- =============================================================================
-- DT1.4 Null rates inside hour sample
-- =============================================================================
SELECT SUM(CASE WHEN getting_date IS NULL THEN 1 ELSE 0 END) AS null_getting,
       SUM(CASE WHEN location_date IS NULL THEN 1 ELSE 0 END) AS null_location,
       COUNT(*) AS sample_n
FROM (
  SELECT srr.getting_date, srr.location_date
  FROM dbtc.statereportrecords srr
  WHERE srr.getting_date >= :d_from
    AND srr.getting_date <  :d_to
    AND ROWNUM <= 5000
);
-- Export: p3c_DT1_null_rates_<date>.csv
