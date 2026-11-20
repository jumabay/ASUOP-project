-- P3B Group G3 — Package inventory, arguments, dependencies (no EXECUTE)

-- G3.1 Package objects
SELECT object_name, object_type, status, created, last_ddl_time
FROM all_objects
WHERE owner = 'DBTC'
  AND object_type IN ('PACKAGE', 'PACKAGE BODY')
  AND object_name IN (
        'IMPORTPKG', 'IMPORTTYPESPKG', 'STATEREPORTPACKAGE', 'WAGONPACKAGE',
        'REPAIRSPKG', 'REPAIRBUDGETPKG', 'PARTSPACKAGE', 'WAREHOUSEPKG',
        'GIVINGPKG', 'APPROACHPKG', 'WORKLISTSPKG'
      )
ORDER BY object_name, object_type;
-- Export: p3b_G3_packages_<date>.csv
-- Empty PACKAGE BODY row ≠ visibility/hidden — not "package missing" if PACKAGE exists.

-- G3.2 Arguments (signatures only — does not run packages)
-- Re-audit 2026-11-16: Oracle 12.2 ALL_ARGUMENTS has OBJECT_NAME (subprogram),
-- not PROCEDURE_NAME (ORA-00904 on first P3B run). Filter uses OBJECT_NAME.
SELECT package_name, object_name AS procedure_name, overload,
       argument_name, position, data_type, in_out
FROM all_arguments
WHERE owner = 'DBTC'
  AND package_name IN ('WAGONPACKAGE', 'STATEREPORTPACKAGE', 'IMPORTPKG', 'REPAIRSPKG', 'WAREHOUSEPKG')
  AND (
        UPPER(object_name) IN (
          'GETACTIVEWAGONLIST', 'GETWAGONLIST', 'GETWAGONBYCODE',
          'GETWAGONSTATEREPORTS', 'ISIMPORTEDFILE', 'SAVEIMPORTFILE',
          'SAVESTATEREPORT', 'SAVESTATEREPORTLINEKZ'
        )
     OR UPPER(object_name) LIKE '%REPAIR%'
     OR UPPER(object_name) LIKE '%ACTIVE%'
      )
ORDER BY package_name, object_name, overload, position;
-- Export: p3b_G3_arguments_<date>.csv
-- G31: confirm GETACTIVEWAGONLIST exists; membership data still BLOCKED (needs EXECUTE later).

-- G3.3 Dependencies (often sparse when bodies hidden)
SELECT name AS package_name, type, referenced_owner, referenced_name,
       referenced_type, dependency_type
FROM all_dependencies
WHERE owner = 'DBTC'
  AND type IN ('PACKAGE', 'PACKAGE BODY')
  AND name IN (
        'IMPORTPKG', 'STATEREPORTPACKAGE', 'WAGONPACKAGE',
        'REPAIRSPKG', 'WAREHOUSEPKG', 'PARTSPACKAGE'
      )
ORDER BY name, referenced_type, referenced_name;
-- Export: p3b_G3_dependencies_<date>.csv
-- Empty result ≠ UNKNOWN writers, not proof of no table use.
