-- P3B ONLY — read-only dictionary probes against DBTC (or copy).
-- Do NOT run from the application. No DML/DDL.
-- Execute by authorized DBA after APPROVALS "DBTC read for migration" / P3B.
-- Redact connection strings; do not paste secrets into tickets.

-- 0) Session identity
SELECT SYS_CONTEXT('USERENV','DB_NAME') AS db_name,
       SYS_CONTEXT('USERENV','SERVICE_NAME') AS service_name,
       SYS_CONTEXT('USERENV','SERVER_HOST') AS server_host,
       SYS_CONTEXT('USERENV','CURRENT_SCHEMA') AS current_schema,
       USER AS session_user
FROM dual;

-- 1) Columns for core tables
SELECT table_name, column_id, column_name, data_type, data_length, nullable
FROM all_tab_columns
WHERE owner = 'DBTC'
  AND table_name IN (
    'WAGON','IMPORTFILES','STATEREPORTS','STATEREPORTRECORDS',
    'REPAIRS','CURRENTPARTS','WAGONPARTS','WAREHOUSEDETAILS',
    'STATIONS','WAGONMILEAGE'
  )
ORDER BY table_name, column_id;

-- 2) Constraints / PK / UK / FK
SELECT c.table_name, c.constraint_name, c.constraint_type, cc.column_name, cc.position
FROM all_constraints c
JOIN all_cons_columns cc
  ON c.owner = cc.owner AND c.constraint_name = cc.constraint_name
WHERE c.owner = 'DBTC'
  AND c.table_name IN (
    'WAGON','IMPORTFILES','STATEREPORTS','STATEREPORTRECORDS',
    'REPAIRS','CURRENTPARTS','WAGONPARTS','WAREHOUSEDETAILS','STATIONS'
  )
ORDER BY c.table_name, c.constraint_type, c.constraint_name, cc.position;

-- 3) Indexes
SELECT table_name, index_name, uniqueness, status
FROM all_indexes
WHERE owner = 'DBTC'
  AND table_name IN (
    'WAGON','IMPORTFILES','STATEREPORTS','STATEREPORTRECORDS',
    'REPAIRS','WAREHOUSEDETAILS'
  )
ORDER BY table_name, index_name;

SELECT index_name, column_position, column_name, descend
FROM all_ind_columns
WHERE index_owner = 'DBTC'
  AND table_name = 'STATEREPORTRECORDS'
ORDER BY index_name, column_position;

-- 4) Volume stats (no COUNT(*))
SELECT table_name, num_rows, blocks, avg_row_len, last_analyzed
FROM all_tables
WHERE owner = 'DBTC'
  AND table_name IN (
    'WAGON','IMPORTFILES','STATEREPORTS','STATEREPORTRECORDS',
    'REPAIRS','CURRENTPARTS','WAGONPARTS','WAREHOUSEDETAILS',
    'STATIONS','WAGONMILEAGE','STATEREPORTRECORDS_TMP'
  )
ORDER BY table_name;

-- 5) Partitions (expect none)
SELECT table_name FROM all_part_tables WHERE owner = 'DBTC';

-- 6) Open-repair candidates (REVIEW carefully — predicates UNKNOWN)
-- Example probes only; adjust after column dump:
-- SELECT COUNT(*) FROM dbtc.repairs WHERE endingdate IS NULL;  -- may be wrong
-- Prefer exporting CURRENTREPAIR_ID join from WAGON after column confirm.

-- 7) Sample distinct GETTING_DATE vs LOCATION_DATE (bounded)
-- SELECT * FROM (
--   SELECT getting_date, location_date
--   FROM dbtc.statereportrecords
--   WHERE getting_date >= ADD_MONTHS(TRUNC(SYSDATE), -1)
--     AND ROWNUM <= 1000
-- );
-- Uncomment only with scoped approve and row limit.
