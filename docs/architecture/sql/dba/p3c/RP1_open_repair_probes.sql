-- P3C RP1 — Open repair semantics (WAGON.CURRENTREPAIR_ID vs ENDINGDATE)
-- DO NOT RUN until P3C-1. No package EXECUTE.
-- Prefer access via WAGON (~50k) → REPAIRS by PK (indexed), not full REPAIRS scan.
-- Timeout: 60s. Max rows: 500 samples / few aggregate rows.
-- Expected load: FULL SCAN WAGON acceptable (~50k) OR index on CURRENTREPAIR_ID
--   (IX_WAGON_CURRENTREPAIR exists per P3B). REPAIRS by PK_REPAIR_ID.

-- =============================================================================
-- RP1.1 How many wagons point to a current repair
-- =============================================================================
SELECT COUNT(*) AS wagons_with_currentrepair
FROM dbtc.wagon w
WHERE w.currentrepair_id IS NOT NULL;
-- Export: p3c_RP1_wagon_current_count_<date>.csv
-- Load: low–medium (50k rows).

-- =============================================================================
-- RP1.2 Consistency: CURRENTREPAIR_ID row ENDINGDATE null vs not
-- =============================================================================
SELECT CASE
         WHEN r.endingdate IS NULL THEN 'ENDING_NULL'
         ELSE 'ENDING_SET'
       END AS ending_bucket,
       COUNT(*) AS cnt
FROM dbtc.wagon w
JOIN dbtc.repairs r
  ON r.id = w.currentrepair_id
GROUP BY CASE WHEN r.endingdate IS NULL THEN 'ENDING_NULL' ELSE 'ENDING_SET' END;
-- Export: p3c_RP1_current_vs_ending_<date>.csv
-- If ENDING_SET > 0 → hypothesis "open = ENDINGDATE IS NULL" alone is FALSE/incomplete.

-- =============================================================================
-- RP1.3 Open-by-null-end but NOT referenced as CURRENTREPAIR (orphan opens)
-- Bounded: only repairs with BEGININGDATE >= :rep_from (e.g. cutover-400d).
-- Uses index including BEGININGDATE (P3B). STOP if plan FTS all REPAIRS.
-- =============================================================================
SELECT COUNT(*) AS ending_null_not_current
FROM dbtc.repairs r
WHERE r.endingdate IS NULL
  AND r.beginingdate >= :rep_from
  AND NOT EXISTS (
        SELECT 1 FROM dbtc.wagon w WHERE w.currentrepair_id = r.id
      );
-- Export: p3c_RP1_null_end_not_current_<date>.csv
-- Load: medium when :rep_from bounds the range.

-- =============================================================================
-- RP1.4 Sample open repairs for migration rule (started > 12м ago, still current)
-- =============================================================================
SELECT w.id AS wagon_id,
       w.code AS wagon_code,
       r.id AS repair_id,
       r.type AS repair_type,
       r.beginingdate,
       r.endingdate,
       r.registertype
FROM dbtc.wagon w
JOIN dbtc.repairs r ON r.id = w.currentrepair_id
WHERE r.beginingdate < :cutoff_minus_12m
  AND ROWNUM <= 200;
-- Export: p3c_RP1_open_started_before_window_<date>.csv
-- Migration rule draft: migrate ALL w.currentrepair_id rows regardless of begin age.

-- =============================================================================
-- RP1.5 Closed repairs inside 12м window volume (bounded by ENDINGDATE index)
-- =============================================================================
SELECT COUNT(*) AS closed_in_window
FROM dbtc.repairs r
WHERE r.endingdate IS NOT NULL
  AND r.endingdate >= :cutover_minus_12m
  AND r.endingdate <  :cutover_ts;
-- Export: p3c_RP1_closed_in_window_<date>.csv
-- Uses IDX_ENDINGDATE (P3B CONFIRMED).
