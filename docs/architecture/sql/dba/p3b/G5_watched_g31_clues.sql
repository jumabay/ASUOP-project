-- P3B Group G5 — Watched / Active clues for G31 (dictionary only — NO membership rows)

-- G5.1 Object names
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
-- Export: p3b_G5_objects_<date>.csv
-- Empty ≠ no matching names visible — still not "Active does not exist" if G3 showed GETACTIVEWAGONLIST.

-- G5.2 WagonPackage Active arguments
SELECT package_name, procedure_name, argument_name, position, data_type, in_out
FROM all_arguments
WHERE owner = 'DBTC'
  AND package_name = 'WAGONPACKAGE'
  AND UPPER(procedure_name) LIKE '%ACTIVE%'
ORDER BY procedure_name, position;
-- Export: p3b_G5_active_args_<date>.csv

-- G5.3 PACKAGE SPEC text snippets (SPEC only; BODY often hidden)
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
-- Export: p3b_G5_spec_snippets_<date>.csv
-- 0 rows for BODY is normal. Do not query TYPE = 'PACKAGE BODY' in this group
-- unless DBA separately grants and APPROVALS allow (still read-only source).

-- G31 CLOSE rule for P3B dictionary phase:
--   CLOSE "procedure exists / signature" if G3/G5 show GETACTIVEWAGONLIST.
--   KEEP OPEN "membership predicate + code list" until separate EXECUTE/export approve
--   or offline wagons.txt analysis (not this SQL group).
