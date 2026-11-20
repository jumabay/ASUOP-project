-- P3C G31 — Watched / Active dictionary probes (NO EXECUTE of packages)
-- DO NOT RUN until P3C-1 (or with package SELECT grants). Safe dictionary-only.
-- Timeout: 30s. Expected load: ALL_* catalog; low.
-- Forbidden: BEGIN WagonPackage.GetActiveWagonList; any CALL/EXEC.

-- =============================================================================
-- G31.1 Package object visibility
-- =============================================================================
SELECT object_name, object_type, status, created, last_ddl_time
FROM all_objects
WHERE owner = 'DBTC'
  AND object_type IN ('PACKAGE', 'PACKAGE BODY')
  AND object_name IN ('WAGONPACKAGE', 'STATEREPORTPACKAGE', 'WAGONSTATEREPORTSPKG', 'GIVINGPKG')
ORDER BY object_name, object_type;
-- Export: p3c_G31_packages_<date>.csv
-- Empty PACKAGE BODY row with visible PACKAGE = normal hide; empty both = grant gap.

-- =============================================================================
-- G31.2 Arguments (Oracle 12.2: OBJECT_NAME = subprogram name)
-- =============================================================================
SELECT package_name,
       object_name AS procedure_name,
       overload,
       argument_name,
       position,
       data_type,
       in_out
FROM all_arguments
WHERE owner = 'DBTC'
  AND package_name = 'WAGONPACKAGE'
  AND UPPER(object_name) IN ('GETACTIVEWAGONLIST', 'GETWAGONLIST', 'GETWAGONBYCODE')
ORDER BY object_name, overload, position;
-- Export: p3c_G31_wagonpackage_args_<date>.csv

-- =============================================================================
-- G31.3 SPEC text snippets only (BODY often hidden — do not request BODY here
--       unless DBA package-source request approved separately)
-- =============================================================================
SELECT name, type, line, text
FROM all_source
WHERE owner = 'DBTC'
  AND name = 'WAGONPACKAGE'
  AND type = 'PACKAGE'
  AND (
        UPPER(text) LIKE '%GETACTIVEWAGONLIST%'
     OR UPPER(text) LIKE '%GETWAGONLIST%'
      )
  AND ROWNUM <= 200
ORDER BY line;
-- Export: p3c_G31_spec_snippets_<date>.csv

-- =============================================================================
-- G31.4 Document-slice objects (already seen in P3B) — column skim for ENABLED
-- =============================================================================
SELECT column_name, data_type, nullable
FROM all_tab_columns
WHERE owner = 'DBTC'
  AND table_name IN ('WAGONSTATEREPORTS', 'WAGONSTATEREPORTSRECORDS')
ORDER BY table_name, column_id;
-- Export: p3c_G31_doc_slice_cols_<date>.csv

-- Full registry vs watched distinction (offline, no SQL):
--   GetWagonList  = explorer "Все вагоны" (CONFIRMED R4)
--   GetActiveWagonList = Remoting only; UI call-site not found (CONFIRMED R4)
--   wagons.txt = PublishRegistryStateReports* (CONFIRMED other path)
-- Membership rows still require approved export — not this file.
