-- P3C-1 executable: KIT_FK soft link (7-day STATEREPORTS window, indexed GETTING_DATE)
-- Window: 2026-11-09 .. 2026-11-16. No tape STATEREPORTRECORDS.

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
WHERE sr.getting_date >= TIMESTAMP '2026-11-09 00:00:00'
  AND sr.getting_date <  TIMESTAMP '2026-11-16 00:00:00'
  AND (
        sr.importfiles_id IS NULL
     OR f.id IS NULL
      )
  AND ROWNUM <= 500;

SELECT CASE
         WHEN sr.importfiles_id IS NULL THEN 'NULL_IMPORTFILES_ID'
         WHEN f.id IS NULL THEN 'MISSING_PARENT_FILE'
         ELSE 'OK'
       END AS link_status,
       COUNT(*) AS cnt
FROM dbtc.statereports sr
LEFT JOIN dbtc.importfiles f
  ON f.id = sr.importfiles_id
WHERE sr.getting_date >= TIMESTAMP '2026-11-09 00:00:00'
  AND sr.getting_date <  TIMESTAMP '2026-11-16 00:00:00'
GROUP BY CASE
           WHEN sr.importfiles_id IS NULL THEN 'NULL_IMPORTFILES_ID'
           WHEN f.id IS NULL THEN 'MISSING_PARENT_FILE'
           ELSE 'OK'
         END
ORDER BY 1;

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
WHERE sr.getting_date >= TIMESTAMP '2026-11-09 00:00:00'
  AND sr.getting_date <  TIMESTAMP '2026-11-16 00:00:00'
  AND ROWNUM <= 500;

SELECT f.id AS importfiles_id,
       f.filename,
       f.type,
       f.importdate
FROM dbtc.importfiles f
WHERE f.importdate >= TIMESTAMP '2026-11-09 00:00:00'
  AND f.importdate <  TIMESTAMP '2026-11-16 00:00:00'
  AND NOT EXISTS (
        SELECT 1 FROM dbtc.statereports sr
        WHERE sr.importfiles_id = f.id
      )
  AND ROWNUM <= 500;
