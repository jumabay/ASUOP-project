-- =============================================================================
-- P3B-0 SAFE SCRIPT — Oracle dictionary / catalog READ-ONLY probes (DBTC)
-- =============================================================================
-- Audience: DBA, Oracle SQL Developer (manual Worksheet runs).
-- DO NOT run from the Java app. DO NOT auto-run. DO NOT use RAIL_APP as DBA.
-- Preferred account: dedicated read-only (e.g. RAILWAY_READONLY or migration reader).
--
-- ALLOWED: SELECT on dictionary views (ALL_*/USER_*/DUAL) and SYS_CONTEXT.
-- FORBIDDEN in this file: DDL, DML, MERGE, ALTER SESSION, EXECUTE, anonymous
--   PL/SQL blocks, DBMS_*, dynamic SQL, COUNT(*) on business tables,
--   unbounded SELECT from STATEREPORTRECORDS / archives.
--
-- PREFERRED for manual runs: split worksheets in sql/dba/p3b/G0_*.sql … G5_*.sql
--   Sequence: docs/architecture/34_P3B_MANUAL_EXECUTION_SEQUENCE.md
-- Run blocks ONE AT A TIME. Export results to CSV (no passwords in exports).
-- Execution on RAC still requires owner/DBA scoped approve (P3B execute ≠ prep).
-- =============================================================================

/* ---------------------------------------------------------------------------
   BLOCK A — Session identity (sanity)
   Goal: confirm DB/service/host/user/con_name before any further probes.
   Privs: CREATE SESSION; SELECT on DUAL (public).
   Load: negligible.
   Expected: one row; CURRENT_SCHEMA may be user schema (do not ALTER SESSION).
--------------------------------------------------------------------------- */
SELECT SYS_CONTEXT('USERENV', 'SESSION_USER') AS session_user,
       USER AS user_name,
       SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AS current_schema,
       SYS_CONTEXT('USERENV', 'DB_NAME') AS db_name,
       SYS_CONTEXT('USERENV', 'CON_NAME') AS con_name,
       SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS service_name,
       SYS_CONTEXT('USERENV', 'SERVER_HOST') AS server_host
FROM dual;

/* ---------------------------------------------------------------------------
   BLOCK B — Table structure (columns) for core migration objects
   Goal: ALL_TAB_COLUMNS for confirmed core tables (P3A catalog).
   Privs: SELECT on ALL_TAB_COLUMNS (usually granted to any user who can see objects).
   Load: low (metadata, filtered IN-list).
   Expected: dozens–hundreds of rows; STATEREPORTRECORDS has many columns.
--------------------------------------------------------------------------- */
SELECT owner,
       table_name,
       column_id,
       column_name,
       data_type,
       data_length,
       data_precision,
       data_scale,
       nullable,
       data_default
FROM all_tab_columns
WHERE owner = 'DBTC'
  AND table_name IN (
        'WAGON',
        'IMPORTFILES',
        'STATEREPORTS',
        'STATEREPORTRECORDS',
        'REPAIRS',
        'REPAIRTYPES',
        'CURRENTPARTS',
        'WAGONPARTS',
        'WAREHOUSEDETAILS',
        'WAREHOUSES',
        'STATIONS',
        'WAGONMILEAGE',
        'STATEREPORTRECORDS_TMP',
        'WAGONSTATEREPORTS',
        'WAGONSTATEREPORTSRECORDS'
      )
ORDER BY table_name, column_id;

/* ---------------------------------------------------------------------------
   BLOCK C — PK / UK / FK / CHECK constraints
   Goal: natural keys and FK graph for RAILDB mapping.
   Privs: ALL_CONSTRAINTS, ALL_CONS_COLUMNS.
   Load: low.
--------------------------------------------------------------------------- */
SELECT c.owner,
       c.table_name,
       c.constraint_name,
       c.constraint_type,
       c.r_owner,
       c.r_constraint_name,
       c.status,
       cc.column_name,
       cc.position
FROM all_constraints c
JOIN all_cons_columns cc
  ON c.owner = cc.owner
 AND c.constraint_name = cc.constraint_name
WHERE c.owner = 'DBTC'
  AND c.table_name IN (
        'WAGON',
        'IMPORTFILES',
        'STATEREPORTS',
        'STATEREPORTRECORDS',
        'REPAIRS',
        'REPAIRTYPES',
        'CURRENTPARTS',
        'WAGONPARTS',
        'WAREHOUSEDETAILS',
        'WAREHOUSES',
        'STATIONS'
      )
ORDER BY c.table_name, c.constraint_type, c.constraint_name, cc.position;

/* ---------------------------------------------------------------------------
   BLOCK D — Indexes (headers)
   Goal: existing access paths; confirm IDX_REC_WAGON_ID etc.
   Privs: ALL_INDEXES.
   Load: low.
--------------------------------------------------------------------------- */
SELECT owner,
       table_name,
       index_name,
       index_type,
       uniqueness,
       status,
       partitioned
FROM all_indexes
WHERE owner = 'DBTC'
  AND table_name IN (
        'WAGON',
        'IMPORTFILES',
        'STATEREPORTS',
        'STATEREPORTRECORDS',
        'REPAIRS',
        'CURRENTPARTS',
        'WAGONPARTS',
        'WAREHOUSEDETAILS',
        'STATIONS'
      )
ORDER BY table_name, index_name;

/* ---------------------------------------------------------------------------
   BLOCK E — Index columns (bounded to tape + wagon + kits)
   Goal: column order for migration filter axis (GETTING_DATE vs LOCATION_DATE).
   Privs: ALL_IND_COLUMNS.
   Load: low–moderate (STATEREPORTRECORDS has many indexes).
--------------------------------------------------------------------------- */
SELECT index_owner,
       table_name,
       index_name,
       column_position,
       column_name,
       descend
FROM all_ind_columns
WHERE index_owner = 'DBTC'
  AND table_name IN ('STATEREPORTRECORDS', 'WAGON', 'IMPORTFILES', 'STATEREPORTS', 'REPAIRS')
ORDER BY table_name, index_name, column_position;

/* ---------------------------------------------------------------------------
   BLOCK F — Partitions on DBTC (expect none)
   Goal: confirm ALL_PART_TABLES empty for DBTC (P3A claim).
   Privs: ALL_PART_TABLES.
   Load: negligible.
--------------------------------------------------------------------------- */
SELECT owner, table_name, partitioning_type, partition_count
FROM all_part_tables
WHERE owner = 'DBTC'
ORDER BY table_name;

/* ---------------------------------------------------------------------------
   BLOCK G — Table stats ONLY (NUM_ROWS/BLOCKS) — no COUNT(*)
   Goal: sizing inputs; stale LAST_ANALYZED is OK to note.
   Privs: ALL_TABLES.
   Load: negligible.
--------------------------------------------------------------------------- */
SELECT owner,
       table_name,
       num_rows,
       blocks,
       avg_row_len,
       partitioned,
       last_analyzed
FROM all_tables
WHERE owner = 'DBTC'
  AND table_name IN (
        'WAGON',
        'IMPORTFILES',
        'STATEREPORTS',
        'STATEREPORTRECORDS',
        'REPAIRS',
        'CURRENTPARTS',
        'WAGONPARTS',
        'WAREHOUSEDETAILS',
        'STATIONS',
        'WAGONMILEAGE',
        'STATEREPORTRECORDS_TMP',
        'WAGONSTATEREPORTS',
        'WAGONSTATEREPORTSRECORDS'
      )
ORDER BY table_name;

/* ---------------------------------------------------------------------------
   BLOCK H — Package inventory (spec objects only)
   Goal: list packages relevant to fleet/import/repairs/warehouse.
   Privs: ALL_OBJECTS.
   Load: low.
   Note: PACKAGE BODY text often hidden — do not expect ALL_SOURCE body lines.
--------------------------------------------------------------------------- */
SELECT object_name, object_type, status, created, last_ddl_time
FROM all_objects
WHERE owner = 'DBTC'
  AND object_type IN ('PACKAGE', 'PACKAGE BODY')
  AND object_name IN (
        'IMPORTPKG',
        'IMPORTTYPESPKG',
        'STATEREPORTPACKAGE',
        'WAGONPACKAGE',
        'REPAIRSPKG',
        'REPAIRBUDGETPKG',
        'PARTSPACKAGE',
        'WAREHOUSEPKG',
        'GIVINGPKG',
        'APPROACHPKG',
        'WORKLISTSPKG'
      )
ORDER BY object_name, object_type;

/* ---------------------------------------------------------------------------
   BLOCK I — Package procedure/argument names (dictionary, not bodies)
   Goal: confirm GetActiveWagonList / GetWagonList / Save* entrypoints exist.
   Privs: ALL_ARGUMENTS (or ALL_PROCEDURES if preferred).
   Load: low–moderate.
--------------------------------------------------------------------------- */
SELECT object_name,
       procedure_name,
       argument_name,
       position,
       data_type,
       in_out
FROM all_arguments
WHERE owner = 'DBTC'
  AND package_name IN ('WAGONPACKAGE', 'STATEREPORTPACKAGE', 'IMPORTPKG', 'REPAIRSPKG', 'WAREHOUSEPKG')
  AND (procedure_name IN (
            'GETACTIVEWAGONLIST',
            'GETWAGONLIST',
            'GETWAGONBYCODE',
            'GETWAGONSTATEREPORTS',
            'ISIMPORTEDFILE',
            'SAVEIMPORTFILE',
            'SAVESTATEREPORT',
            'SAVESTATEREPORTLINEKZ'
       )
       OR procedure_name LIKE 'GET%REPAIR%'
       OR procedure_name LIKE 'SAVE%REPAIR%')
ORDER BY package_name, procedure_name, overload, position;

/* ---------------------------------------------------------------------------
   BLOCK J — Dependencies PACKAGE → other objects (often sparse if bodies hidden)
   Goal: see visible deps; empty result is informative (UNKNOWN writers).
   Privs: ALL_DEPENDENCIES.
   Load: low.
--------------------------------------------------------------------------- */
SELECT name AS package_name,
       type,
       referenced_owner,
       referenced_name,
       referenced_type,
       dependency_type
FROM all_dependencies
WHERE owner = 'DBTC'
  AND type IN ('PACKAGE', 'PACKAGE BODY')
  AND name IN (
        'IMPORTPKG',
        'STATEREPORTPACKAGE',
        'WAGONPACKAGE',
        'REPAIRSPKG',
        'WAREHOUSEPKG',
        'PARTSPACKAGE'
      )
ORDER BY name, referenced_type, referenced_name;

/* ---------------------------------------------------------------------------
   BLOCK K — Repair metadata (dictionary only)
   Goal: column names/types for open-repair design; types list.
   Privs: ALL_TAB_COLUMNS on REPAIRS/REPAIRTYPES (+ triggers names).
   Load: low.
--------------------------------------------------------------------------- */
SELECT table_name, column_id, column_name, data_type, nullable
FROM all_tab_columns
WHERE owner = 'DBTC'
  AND table_name IN ('REPAIRS', 'REPAIRTYPES', 'REPAIRPLANTS', 'PLAN_REPAIRS_T')
ORDER BY table_name, column_id;

SELECT trigger_name, table_name, triggering_event, status
FROM all_triggers
WHERE owner = 'DBTC'
  AND table_name IN ('REPAIRS', 'WAGON')
ORDER BY table_name, trigger_name;

/* ---------------------------------------------------------------------------
   BLOCK L — Warehouse / parts metadata (dictionary only)
   Goal: grain candidates for WAREHOUSEDETAILS / CURRENTPARTS / WAGONPARTS.
   Privs: ALL_TAB_COLUMNS, ALL_CONSTRAINTS.
   Load: low.
--------------------------------------------------------------------------- */
SELECT table_name, column_id, column_name, data_type, nullable
FROM all_tab_columns
WHERE owner = 'DBTC'
  AND table_name IN (
        'WAREHOUSES',
        'WAREHOUSEDETAILS',
        'VIRTUALWAREHOUSEDETAILS',
        'CURRENTPARTS',
        'WAGONPARTS',
        'PARTTYPES',
        'ARRIVALDOCUMENTS',
        'PROCUREMENT',
        'TRANSFERDOCUMENTS',
        'WRITINGOFFDOCUMENTS'
      )
ORDER BY table_name, column_id;

SELECT c.table_name, c.constraint_name, c.constraint_type, cc.column_name, cc.position
FROM all_constraints c
JOIN all_cons_columns cc
  ON c.owner = cc.owner AND c.constraint_name = cc.constraint_name
WHERE c.owner = 'DBTC'
  AND c.table_name IN ('WAREHOUSEDETAILS', 'CURRENTPARTS', 'WAGONPARTS', 'WAREHOUSES')
ORDER BY c.table_name, c.constraint_type, c.constraint_name, cc.position;

/* ---------------------------------------------------------------------------
   BLOCK M — Watched-list dictionary clues (G31) — NO business data scan
   Goal: find objects/arguments named Active/Watch/Publish; not the membership rows.
   Privs: ALL_OBJECTS, ALL_ARGUMENTS, ALL_SOURCE (SPEC only if visible).
   Load: low. ALL_SOURCE may return 0 rows for bodies.
--------------------------------------------------------------------------- */
SELECT object_name, object_type, status
FROM all_objects
WHERE owner = 'DBTC'
  AND object_type IN ('TABLE', 'VIEW', 'PACKAGE')
  AND (
        UPPER(object_name) LIKE '%ACTIVE%WAGON%'
     OR UPPER(object_name) LIKE '%WATCH%'
     OR UPPER(object_name) LIKE '%WAGONSTATEREPORT%'
      )
ORDER BY object_type, object_name;

SELECT package_name, procedure_name, argument_name, position, data_type, in_out
FROM all_arguments
WHERE owner = 'DBTC'
  AND package_name = 'WAGONPACKAGE'
  AND UPPER(procedure_name) LIKE '%ACTIVE%'
ORDER BY procedure_name, position;

-- SPEC text only if granted; may be empty. Do not request PACKAGE BODY.
SELECT name, type, line, text
FROM all_source
WHERE owner = 'DBTC'
  AND name = 'WAGONPACKAGE'
  AND type = 'PACKAGE'
  AND (
        UPPER(text) LIKE '%GETACTIVEWAGONLIST%'
     OR UPPER(text) LIKE '%GETWAGONLIST%'
      )
ORDER BY line;

-- =============================================================================
-- BLOCKED outside this script (need separate scoped APPROVALS — NOT dictionary)
-- =============================================================================
-- B1: COUNT(*) or full scan on STATEREPORTRECORDS / archives — FORBIDDEN here.
-- B2: Sample GETTING_DATE vs LOCATION_DATE from tape — only with ROWNUM limit +
--     approve; prefer index-only / sample on copy.
-- B3: Open-repair COUNT with guessed predicate — after BLOCK K columns known.
-- B4: Materialize GetActiveWagonList cursor contents — needs EXECUTE on package
--     + business approve (returns membership data).
-- B5: ALTER SESSION SET CURRENT_SCHEMA — avoid; qualify OWNER=DBTC instead.
-- =============================================================================
