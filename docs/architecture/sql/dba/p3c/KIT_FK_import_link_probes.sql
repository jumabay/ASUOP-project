-- P3C KIT_FK — STATEREPORTS.IMPORTFILES_ID soft link (no DB FK)
-- DO NOT RUN until P3C-1.
-- Prefer filter STATEREPORTS by GETTING_DATE (index STATEREPORTS_IDX$$_008A0007).
-- Timeout: 60s. Avoid unfiltered join of all reports×files.
-- Expected load: day/week of STATEREPORTS (~hundreds–thousands) + PK lookup IMPORTFILES.

-- =============================================================================
-- KIT_FK.1 Orphans: report points to missing/null import file (bounded window)
-- =============================================================================
SELECT sr.id AS report_id,
       sr.getting_date,
       sr.importfiles_id,
       sr.wagoncount,
       sr.last AS last_flag,
       CASE
         WHEN sr.importfiles_id IS NULL THEN 'NULL_IMPORTFILES_ID'
         WHEN f.id IS NULL THEN 'MISSING_PARENT_FILE'
         ELSE 'OK'
       END AS link_status
FROM dbtc.statereports sr
LEFT JOIN dbtc.importfiles f
  ON f.id = sr.importfiles_id
WHERE sr.getting_date >= :d_from
  AND sr.getting_date <  :d_to
  AND (
        sr.importfiles_id IS NULL
     OR f.id IS NULL
      )
  AND ROWNUM <= 500;
-- Export: p3c_KIT_FK_orphans_<date>.csv

-- =============================================================================
-- KIT_FK.2 Link status histogram for same window (all reports in window)
-- =============================================================================
SELECT CASE
         WHEN sr.importfiles_id IS NULL THEN 'NULL_IMPORTFILES_ID'
         WHEN f.id IS NULL THEN 'MISSING_PARENT_FILE'
         ELSE 'OK'
       END AS link_status,
       COUNT(*) AS cnt
FROM dbtc.statereports sr
LEFT JOIN dbtc.importfiles f
  ON f.id = sr.importfiles_id
WHERE sr.getting_date >= :d_from
  AND sr.getting_date <  :d_to
GROUP BY CASE
           WHEN sr.importfiles_id IS NULL THEN 'NULL_IMPORTFILES_ID'
           WHEN f.id IS NULL THEN 'MISSING_PARENT_FILE'
           ELSE 'OK'
         END
ORDER BY 1;
-- Export: p3c_KIT_FK_status_hist_<date>.csv
-- Load: low–medium for ≤31 days of headers.

-- =============================================================================
-- KIT_FK.3 Parent file attributes for OK links (sample)
-- =============================================================================
SELECT sr.id AS report_id,
       sr.getting_date AS report_getting,
       f.id AS importfiles_id,
       f.filename,
       f.type AS file_type,
       f.importdate,
       f.filesize
FROM dbtc.statereports sr
JOIN dbtc.importfiles f
  ON f.id = sr.importfiles_id
WHERE sr.getting_date >= :d_from
  AND sr.getting_date <  :d_to
  AND ROWNUM <= 500;
-- Export: p3c_KIT_FK_ok_sample_<date>.csv

-- =============================================================================
-- KIT_FK.4 Files without any report in window (optional; files by IMPORTDATE)
-- =============================================================================
SELECT f.id AS importfiles_id,
       f.filename,
       f.type,
       f.importdate
FROM dbtc.importfiles f
WHERE f.importdate >= :d_from
  AND f.importdate <  :d_to
  AND NOT EXISTS (
        SELECT 1 FROM dbtc.statereports sr
        WHERE sr.importfiles_id = f.id
      )
  AND ROWNUM <= 500;
-- Export: p3c_KIT_FK_files_without_report_<date>.csv

-- RAILDB draft: store legacy_importfiles_id + legacy_report_id always;
-- kit_id nullable when orphan; migration_flag ORPHAN_REPORT / ORPHAN_FILE.
