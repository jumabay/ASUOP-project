-- P3C-1 executable: RP1 open repairs (literals for cutover ~2026-11-16)
-- Prefer WAGON (~50k) → REPAIRS by PK. No package EXECUTE.

SELECT COUNT(*) AS wagons_with_currentrepair
FROM dbtc.wagon w
WHERE w.currentrepair_id IS NOT NULL;

SELECT CASE
         WHEN r.endingdate IS NULL THEN 'ENDING_NULL'
         ELSE 'ENDING_SET'
       END AS ending_bucket,
       COUNT(*) AS cnt
FROM dbtc.wagon w
JOIN dbtc.repairs r
  ON r.id = w.currentrepair_id
GROUP BY CASE WHEN r.endingdate IS NULL THEN 'ENDING_NULL' ELSE 'ENDING_SET' END;

SELECT COUNT(*) AS ending_null_not_current
FROM dbtc.repairs r
WHERE r.endingdate IS NULL
  AND r.beginingdate >= DATE '2025-10-12'
  AND NOT EXISTS (
        SELECT 1 FROM dbtc.wagon w WHERE w.currentrepair_id = r.id
      );

SELECT w.id AS wagon_id,
       w.code AS wagon_code,
       r.id AS repair_id,
       r.type AS repair_type,
       r.beginingdate,
       r.endingdate,
       r.registertype
FROM dbtc.wagon w
JOIN dbtc.repairs r ON r.id = w.currentrepair_id
WHERE r.beginingdate < DATE '2025-11-16'
  AND ROWNUM <= 200;

SELECT COUNT(*) AS closed_in_window
FROM dbtc.repairs r
WHERE r.endingdate IS NOT NULL
  AND r.endingdate >= DATE '2025-11-16'
  AND r.endingdate <  DATE '2026-11-16';
