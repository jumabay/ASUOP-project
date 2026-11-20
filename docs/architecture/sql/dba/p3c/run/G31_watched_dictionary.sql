-- P3C-1 executable: G31 dictionary only (NO package EXECUTE)
SELECT object_name, object_type, status, created, last_ddl_time
FROM all_objects
WHERE owner = 'DBTC'
  AND object_type IN ('PACKAGE', 'PACKAGE BODY')
  AND object_name IN ('WAGONPACKAGE', 'STATEREPORTPACKAGE', 'WAGONSTATEREPORTSPKG', 'GIVINGPKG')
ORDER BY object_name, object_type;

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

SELECT column_name, data_type, nullable
FROM all_tab_columns
WHERE owner = 'DBTC'
  AND table_name IN ('WAGONSTATEREPORTS', 'WAGONSTATEREPORTSRECORDS')
ORDER BY table_name, column_id;
