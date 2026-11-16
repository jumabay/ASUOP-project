-- P3B Group G0 — Session identity + metadata visibility gate
-- Manual SQL Developer only. SELECT dictionary/DUAL only. No DDL/DML.
-- Account: read-only (NOT RAIL_APP admin). Stop if identity mismatch or visibility fail.

-- G0.1 Identity (must run first after Connect)
SELECT SYS_CONTEXT('USERENV', 'SESSION_USER') AS session_user,
       USER AS user_name,
       SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AS current_schema,
       SYS_CONTEXT('USERENV', 'DB_NAME') AS db_name,
       SYS_CONTEXT('USERENV', 'CON_NAME') AS con_name,
       SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS service_name,
       SYS_CONTEXT('USERENV', 'SERVER_HOST') AS server_host
FROM dual;

-- Expected: 1 row. Record values in p3b_G0_identity_<date>.csv
-- Stop if db_name/service_name/con_name/host not the approved environment.
-- Do NOT ALTER SESSION SET CURRENT_SCHEMA.

-- G0.2 Visibility gate for schema DBTC (empty ALL_* is NOT proof of absence)
SELECT 'ALL_TABLES' AS probe,
       COUNT(*) AS visible_rows
FROM all_tables
WHERE owner = 'DBTC'
  AND table_name IN ('WAGON', 'STATEREPORTRECORDS', 'IMPORTFILES')
UNION ALL
SELECT 'ALL_OBJECTS_WAGON',
       COUNT(*)
FROM all_objects
WHERE owner = 'DBTC'
  AND object_name = 'WAGON'
  AND object_type = 'TABLE';

-- Interpretation:
--   visible_rows > 0  → proceed to G1
--   visible_rows = 0  → STOP. Request addressed grants (see runbook):
--     GRANT SELECT ON DBTC.WAGON TO <ro_user>;  -- examples only; DBA chooses
--     and/or catalog visibility so ALL_* shows DBTC objects.
--   Do NOT request DBA, SELECT ANY TABLE, or ANY privileges for research account.
-- Export: p3b_G0_visibility_<date>.csv
